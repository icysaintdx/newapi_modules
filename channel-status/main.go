package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"strings"
	"sync"
	"time"

	_ "github.com/lib/pq"
)

// ===== Data types =====

type Channel struct {
	ID       int
	Name     string
	Type     int
	Status   int
	BaseURL  string
	Models   string
	Group    string
	Key      string
	Priority int64
}

// Single check result for one model
type CheckPoint struct {
	Status    string `json:"status"`    // "operational","degraded","failed","maintenance"
	Latency   int    `json:"latency"`   // ms
	Error     string `json:"error,omitempty"`
	Timestamp string `json:"timestamp"` // HH:MM:SS
}

// A model's current + history
type ModelInfo struct {
	Name       string       `json:"name"`
	Status     string       `json:"status"`
	Latency    int          `json:"latency"`
	Error      string       `json:"error,omitempty"`
	Timeline   []CheckPoint `json:"timeline"` // last 60 checks, index 0 = newest
	Uptime     float64      `json:"uptime"`   // percentage 0-100
	TotalChecks int         `json:"total_checks"`
	SuccessCount int        `json:"success_count"`
}

// Channel with all its models
type ChannelInfo struct {
	ID       int         `json:"id"`
	Name     string      `json:"name"`
	Type     string      `json:"type"`
	Group    string      `json:"group"`
	Status   string      `json:"status"` // "operational","partial","down","maintenance"
	Priority int64       `json:"priority"`
	Models   []ModelInfo `json:"models"`
}

// ===== Globals =====

var (
	db *sql.DB

	// History ring buffer: channelID -> modelName -> []CheckPoint (max 60)
	historyMu sync.RWMutex
	history   = make(map[string][]CheckPoint) // key = "chID:modelName"

	// Current aggregated status
	cacheMu   sync.RWMutex
	cacheData []ChannelInfo
	cacheTime time.Time
	pollCount int

	maxHistory = 60
)

var channelTypes = map[int]string{
	1: "OpenAI", 14: "Anthropic", 15: "Baidu", 16: "Zhipu", 17: "Ali",
	18: "Xunfei", 19: "AIProxy", 20: "Tencent", 21: "Gemini", 22: "Mistral",
	23: "OhMyGPT", 24: "Custom", 25: "IMAGINE", 26: "Midjourney", 27: "Verge",
	28: "Cohere", 29: "DeepSeek", 30: "Cloudflare", 31: "Together", 32: "SiliconFlow",
	33: "Ollama", 34: "Groq", 35: "VertexAI",
}

func historyKey(chID int, model string) string {
	return fmt.Sprintf("%d:%s", chID, model)
}

func appendHistory(key string, cp CheckPoint) {
	historyMu.Lock()
	defer historyMu.Unlock()
	h := history[key]
	// prepend (newest first)
	h = append([]CheckPoint{cp}, h...)
	if len(h) > maxHistory {
		h = h[:maxHistory]
	}
	history[key] = h
}

func getHistory(key string) []CheckPoint {
	historyMu.RLock()
	defer historyMu.RUnlock()
	h := history[key]
	if h == nil {
		return []CheckPoint{}
	}
	// return copy
	cp := make([]CheckPoint, len(h))
	copy(cp, h)
	return cp
}

// ===== Main =====

func main() {
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		dsn = "postgresql://root:123456@postgres:5432/new-api?sslmode=disable"
	}

	var err error
	db, err = sql.Open("postgres", dsn)
	if err != nil {
		log.Fatal("DB connect error:", err)
	}
	db.SetMaxOpenConns(10)
	db.SetMaxIdleConns(5)
	db.SetConnMaxLifetime(time.Hour)

	if err = db.Ping(); err != nil {
		log.Fatal("DB ping error:", err)
	}
	log.Println("Connected to database")

	// Initial test
	testAllChannels()

	// Run test every 5 minutes
	go func() {
		ticker := time.NewTicker(5 * time.Minute)
		for range ticker.C {
			testAllChannels()
		}
	}()

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	http.HandleFunc("/", serveStatusPage)
	http.HandleFunc("/api/status", serveAPI)
	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
	})

	log.Printf("Server starting on :%s", port)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}

// ===== API =====

func serveAPI(w http.ResponseWriter, r *http.Request) {
	cacheMu.RLock()
	data := cacheData
	t := cacheTime
	cnt := pollCount
	cacheMu.RUnlock()

	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Access-Control-Allow-Origin", "*")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success":      true,
		"data":         data,
		"updated_at":   t.Format("2006-01-02 15:04:05"),
		"poll_count":   cnt,
		"poll_interval": 300,
	})
}

// ===== Testing logic =====

func testAllChannels() {
	rows, err := db.Query(`SELECT id, name, type, status, base_url, models, "group", key, priority FROM channels WHERE status != 3 ORDER BY priority DESC, id ASC`)
	if err != nil {
		log.Println("Query error:", err)
		return
	}
	defer rows.Close()

	var channels []Channel
	for rows.Next() {
		var ch Channel
		rows.Scan(&ch.ID, &ch.Name, &ch.Type, &ch.Status, &ch.BaseURL, &ch.Models, &ch.Group, &ch.Key, &ch.Priority)
		channels = append(channels, ch)
	}

	log.Printf("Testing %d channels...", len(channels))

	var results []ChannelInfo
	var wg sync.WaitGroup
	var mu sync.Mutex

	for _, ch := range channels {
		wg.Add(1)
		go func(c Channel) {
			defer wg.Done()
			info := testChannel(c)
			mu.Lock()
			results = append(results, info)
			mu.Unlock()
		}(ch)
	}

	wg.Wait()

	cacheMu.Lock()
	cacheData = results
	cacheTime = time.Now()
	pollCount++
	cacheMu.Unlock()

	log.Printf("Test #%d completed at %s", pollCount, cacheTime.Format("15:04:05"))
}

func testChannel(ch Channel) ChannelInfo {
	typeName := channelTypes[ch.Type]
	if typeName == "" {
		typeName = fmt.Sprintf("Type%d", ch.Type)
	}

	info := ChannelInfo{
		ID:       ch.ID,
		Name:     ch.Name,
		Type:     typeName,
		Group:    ch.Group,
		Priority: ch.Priority,
	}

	if ch.Status == 2 {
		info.Status = "maintenance"
		return info
	}

	// Parse all models
	models := parseModels(ch.Models)
	if len(models) == 0 {
		info.Status = "operational"
		return info
	}

	baseURL := ch.BaseURL
	if baseURL == "" {
		baseURL = "https://api.openai.com"
	}
	if !strings.HasSuffix(baseURL, "/") {
		baseURL += "/"
	}

	// Test ALL models (not just first 3)
	var modelResults []ModelInfo
	var mwg sync.WaitGroup
	var mmu sync.Mutex
	sem := make(chan struct{}, 5) // concurrency limit

	for _, model := range models {
		mwg.Add(1)
		go func(m string) {
			defer mwg.Done()
			sem <- struct{}{}
			defer func() { <-sem }()

			mi := testModel(ch.ID, m, baseURL, ch.Key, ch.Type)
			mmu.Lock()
			modelResults = append(modelResults, mi)
			mmu.Unlock()
		}(model)
	}
	mwg.Wait()

	// Determine channel overall status
	onlineCount := 0
	for _, m := range modelResults {
		if m.Status == "operational" || m.Status == "degraded" {
			onlineCount++
		}
	}
	if onlineCount == len(modelResults) {
		info.Status = "operational"
	} else if onlineCount > 0 {
		info.Status = "partial"
	} else {
		info.Status = "down"
	}

	info.Models = modelResults
	return info
}

func testModel(chID int, model, baseURL, apiKey string, chType int) ModelInfo {
	now := time.Now().Format("15:04:05")
	key := historyKey(chID, model)

	cp := CheckPoint{Timestamp: now}

	start := time.Now()
	client := &http.Client{Timeout: 15 * time.Second}

	// Build request based on channel type
	var endpoint string
	var body string
	var authHeader string

	switch chType {
	case 14: // Anthropic
		endpoint = baseURL + "v1/messages"
		body = fmt.Sprintf(`{"model":"%s","messages":[{"role":"user","content":"hi"}],"max_tokens":5}`, model)
		authHeader = "x-api-key"
	default: // OpenAI compatible
		endpoint = baseURL + "v1/chat/completions"
		body = fmt.Sprintf(`{"model":"%s","messages":[{"role":"user","content":"hi"}],"max_tokens":5}`, model)
		authHeader = "Authorization"
	}

	req, err := http.NewRequest("POST", endpoint, strings.NewReader(body))
	if err != nil {
		cp.Status = "failed"
		cp.Error = "request build failed"
		appendHistory(key, cp)
		return buildModelInfo(model, cp, key)
	}

	req.Header.Set("Content-Type", "application/json")
	if authHeader == "x-api-key" {
		req.Header.Set("x-api-key", apiKey)
		req.Header.Set("anthropic-version", "2023-06-01")
	} else {
		req.Header.Set("Authorization", "Bearer "+apiKey)
	}

	resp, err := client.Do(req)
	latency := int(time.Since(start).Milliseconds())
	cp.Latency = latency

	if err != nil {
		cp.Status = "failed"
		if strings.Contains(err.Error(), "timeout") {
			cp.Error = "timeout"
		} else {
			cp.Error = "network error"
		}
	} else {
		defer resp.Body.Close()
		if resp.StatusCode == 200 {
			if latency > 5000 {
				cp.Status = "degraded"
			} else {
				cp.Status = "operational"
			}
		} else {
			cp.Status = "failed"
			cp.Error = fmt.Sprintf("HTTP %d", resp.StatusCode)
		}
	}

	appendHistory(key, cp)
	return buildModelInfo(model, cp, key)
}

func buildModelInfo(model string, latest CheckPoint, key string) ModelInfo {
	timeline := getHistory(key)

	// Calculate uptime
	total := len(timeline)
	success := 0
	for _, t := range timeline {
		if t.Status == "operational" || t.Status == "degraded" {
			success++
		}
	}
	uptime := float64(0)
	if total > 0 {
		uptime = float64(success) / float64(total) * 100
	}

	return ModelInfo{
		Name:         model,
		Status:       latest.Status,
		Latency:      latest.Latency,
		Error:        latest.Error,
		Timeline:     timeline,
		Uptime:       uptime,
		TotalChecks:  total,
		SuccessCount: success,
	}
}

func parseModels(modelsStr string) []string {
	if modelsStr == "" {
		return nil
	}
	var result []string
	for _, m := range strings.Split(modelsStr, ",") {
		m = strings.TrimSpace(m)
		if m != "" {
			result = append(result, m)
		}
	}
	return result
}

// ===== HTML Page =====

func serveStatusPage(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Write([]byte(statusHTML))
}

var statusHTML = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Saint Code渠道状态监控</title>
<link rel="icon" type="image/x-icon" href="https://isaint.cc/static-assets/favicon.ico">
<link rel="icon" type="image/png" sizes="32x32" href="https://isaint.cc/static-assets/favicon-32x32.png">
<link rel="apple-touch-icon" sizes="180x180" href="https://isaint.cc/static-assets/apple-touch-icon.png">
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{
  font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',sans-serif;
  background:#0f0f0f;color:#fafafa;min-height:100vh;-webkit-font-smoothing:antialiased;
}
body::before{
  content:'';position:fixed;inset:0;pointer-events:none;z-index:0;
  background-image:linear-gradient(rgba(255,255,255,0.03) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,0.03) 1px,transparent 1px);
  background-size:40px 40px;
  -webkit-mask:linear-gradient(to bottom,#000 80%,transparent);
  mask:linear-gradient(to bottom,#000 80%,transparent);
}

.header{position:relative;z-index:1;padding:2rem 2rem 1.5rem;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:1rem}
.header-left{flex:1;min-width:200px}
.header h1{font-size:1.75rem;font-weight:800;color:#fafafa;margin:0;letter-spacing:-0.02em}
.header p{color:#a3a3a3;font-size:0.875rem;margin-top:0.25rem}
.poll-badge{display:inline-flex;align-items:center;gap:6px;margin-top:10px;padding:4px 14px;
  background:rgba(16,185,129,0.1);border:1px solid rgba(16,185,129,0.2);color:#10b981;
  border-radius:9999px;font-size:0.75rem;font-weight:600}
.poll-dot{width:6px;height:6px;border-radius:50%;background:#10b981;animation:pulse 2s infinite}
@keyframes pulse{0%,100%{opacity:1}50%{opacity:0.4}}

.summary{display:flex;justify-content:flex-end;gap:12px;flex-wrap:wrap}
.summary-item{background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.08);
  border-radius:12px;padding:10px 18px;text-align:center;min-width:80px;backdrop-filter:blur(8px)}
.summary-item .val{font-size:1.5rem;font-weight:800}
.summary-item .lbl{font-size:0.65rem;color:#a3a3a3;margin-top:2px;text-transform:uppercase;letter-spacing:0.05em}
.val-green{color:#10b981}.val-amber{color:#f59e0b}.val-red{color:#f43f5e}.val-blue{color:#a3a3a3}

.container{max-width:1400px;margin:0 auto;padding:1rem 1.5rem;position:relative;z-index:1}

/* ===== Channel Panel (collapsible) ===== */
.ch-panel{margin-bottom:16px}
.ch-head{
  display:flex;align-items:center;gap:12px;padding:14px 20px;
  background:#000;border:1px solid rgba(255,255,255,0.08);
  border-radius:24px;cursor:pointer;user-select:none;
  transition:background 0.2s,border-color 0.2s;backdrop-filter:blur(4px);
}
.ch-head:hover{background:rgba(255,255,255,0.03);border-color:rgba(255,255,255,0.12)}
.ch-arrow{width:32px;height:32px;display:flex;align-items:center;justify-content:center;
  background:rgba(255,255,255,0.06);border-radius:10px;font-size:0.7rem;color:#a3a3a3;
  transition:transform 0.25s;flex-shrink:0;border:1px solid rgba(255,255,255,0.06)}
.ch-head.shut .ch-arrow{transform:rotate(-90deg)}
.ch-info{flex:1;min-width:0}
.ch-name{font-size:1rem;font-weight:700;color:#fafafa}
.ch-meta{font-size:0.7rem;color:#737373;margin-top:2px}
.ch-stats{display:flex;align-items:center;gap:6px;flex-shrink:0}
.sdot{width:8px;height:8px;border-radius:50%;flex-shrink:0}
.sdot-g{background:#10b981}.sdot-a{background:#f59e0b}.sdot-r{background:#f43f5e}
.ch-stat-text{font-size:0.7rem;color:#737373;white-space:nowrap}
.ch-badge{padding:2px 10px;border-radius:9999px;font-size:0.65rem;font-weight:600;
  letter-spacing:0.05em;text-transform:uppercase;flex-shrink:0;backdrop-filter:blur(12px)}
.b-op{background:rgba(16,185,129,0.15);color:#4ade80;border:1px solid rgba(16,185,129,0.2)}
.b-deg{background:rgba(245,158,11,0.15);color:#fbbf24;border:1px solid rgba(245,158,11,0.2)}
.b-fail{background:rgba(244,63,94,0.15);color:#f87171;border:1px solid rgba(244,63,94,0.2)}
.b-maint{background:rgba(59,130,246,0.15);color:#60a5fa;border:1px solid rgba(59,130,246,0.2)}
.ch-body{overflow:hidden;transition:max-height 0.35s ease,opacity 0.3s ease;max-height:12000px;opacity:1;padding-top:12px}
.ch-body.shut{max-height:0;opacity:0;padding-top:0}

/* ===== Model Card Grid ===== */
.m-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(360px,1fr));gap:16px}

/* ===== Model Card ===== */
.m-card{
  background:#000;border:1px solid rgba(255,255,255,0.08);
  border-radius:16px;overflow:hidden;display:flex;flex-direction:column;
  backdrop-filter:blur(24px);transition:transform 0.3s,border-color 0.3s,box-shadow 0.3s;
}
.m-card:hover{transform:translateY(-3px);border-color:rgba(229,229,229,0.18);box-shadow:0 12px 24px rgba(0,0,0,0.25)}
.m-top{padding:16px 20px 12px;display:flex;justify-content:space-between;align-items:flex-start}
.m-name{font-size:0.85rem;font-weight:600;color:#fafafa;font-family:'SF Mono',SFMono-Regular,Menlo,monospace;word-break:break-all}
.m-badge{padding:2px 10px;border-radius:8px;font-size:10px;font-weight:600;
  letter-spacing:0.05em;text-transform:uppercase;backdrop-filter:blur(12px);flex-shrink:0;margin-left:8px}

/* ===== Metrics ===== */
.m-metrics{display:grid;grid-template-columns:1fr 1fr;gap:8px;padding:0 20px 14px}
.m-metric{background:rgba(20,20,20,0.8);border-radius:12px;padding:10px 12px;transition:background 0.2s}
.m-card:hover .m-metric{background:rgba(30,30,30,0.9)}
.m-mlabel{font-size:0.65rem;color:#737373;font-weight:500}
.m-mval{font-size:1.1rem;font-weight:700;margin-top:3px}
.m-munit{font-size:0.65rem;font-weight:400;color:#737373}

/* ===== Timeline ===== */
.tl-wrap{border-top:1px solid rgba(255,255,255,0.06);background:rgba(10,10,10,0.8);padding:14px 20px 12px}
.tl-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:8px}
.tl-label{font-size:0.6rem;color:rgba(255,255,255,0.3);text-transform:uppercase;letter-spacing:0.1em}
.tl-uptime{font-size:0.8rem;font-weight:700}
.tl-bars{display:flex;flex-direction:row-reverse;gap:2px;height:32px;align-items:stretch;
  border-radius:2px;overflow:hidden;background:rgba(255,255,255,0.02)}
.tb{flex:1;border-radius:1px;min-width:1px;transition:transform 0.15s;cursor:pointer;position:relative}
.tb:hover{transform:scaleY(1.12);z-index:10}
.tb-g{background:#10b981}.tb-a{background:#f59e0b}.tb-r{background:#f43f5e}.tb-b{background:#3b82f6}
.tb-e{background:rgba(255,255,255,0.03)}
.tl-axis{display:flex;justify-content:space-between;margin-top:5px}
.tl-axis span{font-size:9px;color:rgba(255,255,255,0.2);text-transform:uppercase;letter-spacing:0.12em}

/* Tooltip */
.tt{display:none;position:absolute;bottom:calc(100% + 8px);left:50%;transform:translateX(-50%);
  background:#2e2e2e;border:1px solid rgba(255,255,255,0.1);border-radius:10px;padding:8px 14px;
  white-space:nowrap;font-size:0.7rem;z-index:100;box-shadow:0 8px 24px rgba(0,0,0,0.4)}
.tb:hover .tt{display:block}
.tt-s{font-weight:600;margin-bottom:2px}.tt-d{color:#a3a3a3}

.footer-info{text-align:center;padding:2rem;color:#fff;font-size:0.7rem;position:relative;z-index:1}
.loading{text-align:center;padding:4rem;color:#737373}
.spinner{display:inline-block;width:36px;height:36px;border:3px solid rgba(255,255,255,0.06);border-top-color:#a3a3a3;border-radius:50%;animation:spin 0.8s linear infinite;margin-bottom:1rem}
@keyframes spin{to{transform:rotate(360deg)}}

@media(max-width:768px){.header{flex-direction:column;align-items:flex-start}.summary{justify-content:flex-start}}
@media(max-width:500px){.m-grid{grid-template-columns:1fr}.ch-stat-text{display:none}}
</style>
</head>
<body>
<div class="header">
<div class="header-left">
<h1>Saint Code渠道状态监控</h1>
<p>实时检测各渠道模型可用性与延迟</p>
<div class="poll-badge" id="poll-info"><span class="poll-dot"></span>加载中...</div>
</div>
<div class="summary" id="summary"></div>
</div>
<div class="container" id="content">
<div class="loading"><div class="spinner"></div><p>正在加载状态数据...</p></div>
</div>
<div class="footer-info" id="footer"></div>

<script>
function uptimeColor(p){var h=Math.max(0,Math.min(120,p/100*120));return 'hsl('+h+',80%,45%)'}
function bc(s){return s==='operational'?'b-op':s==='degraded'?'b-deg':s==='maintenance'?'b-maint':'b-fail'}
function bt(s){return s==='operational'?'正常':s==='degraded'?'延迟':s==='maintenance'?'维护':'异常'}
function tc(s){return s==='operational'?'tb-g':s==='degraded'?'tb-a':s==='maintenance'?'tb-b':(s==='failed'||s==='error')?'tb-r':'tb-e'}
function se(s){return s==='operational'?'✓':s==='degraded'?'⚠':s==='maintenance'?'⏸':'✕'}

var cd=300,pi;
function startCd(s){cd=s;clearInterval(pi);pi=setInterval(function(){cd--;if(cd<=0){cd=0;clearInterval(pi);loadStatus()}document.getElementById('poll-info').innerHTML='<span class="poll-dot"></span>'+cd+'s 后刷新'},1000)}

function toggleCh(i){
  var h=document.getElementById('ch-'+i);
  var b=document.getElementById('cb-'+i);
  h.classList.toggle('shut');b.classList.toggle('shut');
}

async function loadStatus(){
try{
  var r=await fetch('/api/status');var d=await r.json();if(!d.success)return;
  var chs=d.data||[];
  // Sort by priority DESC, then by id ASC
  chs.sort(function(a,b){
    if(b.priority!==a.priority)return b.priority-a.priority;
    return a.id-b.id;
  });
  var tM=0,oM=0,dM=0,fM=0;
  chs.forEach(function(c){(c.models||[]).forEach(function(m){tM++;if(m.status==='operational')oM++;else if(m.status==='degraded')dM++;else fM++})});

  document.getElementById('summary').innerHTML=
    '<div class="summary-item"><div class="val val-blue">'+tM+'</div><div class="lbl">Models</div></div>'+
    '<div class="summary-item"><div class="val val-green">'+oM+'</div><div class="lbl">Online</div></div>'+
    '<div class="summary-item"><div class="val val-amber">'+dM+'</div><div class="lbl">Degraded</div></div>'+
    '<div class="summary-item"><div class="val val-red">'+fM+'</div><div class="lbl">Down</div></div>';

  var html='';
  chs.forEach(function(ch,ci){
    var ms=ch.models||[],mo=0,md=0,mf=0;
    ms.forEach(function(m){if(m.status==='operational')mo++;else if(m.status==='degraded')md++;else mf++});
    var dots='';
    if(mo>0)dots+='<span class="sdot sdot-g"></span>';
    if(md>0)dots+='<span class="sdot sdot-a"></span>';
    if(mf>0)dots+='<span class="sdot sdot-r"></span>';
    var sp=[];if(mo>0)sp.push(mo+' ok');if(md>0)sp.push(md+' slow');if(mf>0)sp.push(mf+' down');

    html+='<div class="ch-panel">';
    html+='<div class="ch-head" onclick="toggleCh('+ci+')" id="ch-'+ci+'">';
    html+='<div class="ch-arrow">▼</div>';
    html+='<div class="ch-info"><div class="ch-name">'+ch.name+'</div><div class="ch-meta">'+ch.type+(ch.group?' · '+ch.group:'')+' · '+ms.length+' models</div></div>';
    html+='<div class="ch-stats">'+dots+'<span class="ch-stat-text">'+sp.join(' / ')+'</span></div>';
    html+='<span class="ch-badge '+bc(ch.status)+'">'+bt(ch.status)+'</span>';
    html+='</div>';
    html+='<div class="ch-body" id="cb-'+ci+'"><div class="m-grid">';

    ms.forEach(function(m){
      var tl=m.timeline||[];while(tl.length<60)tl.push({status:'',latency:0,timestamp:''});
      var bars='';
      for(var i=0;i<60;i++){var t=tl[i];var cls=t&&t.status?tc(t.status):'tb-e';var tip='';
        if(t&&t.status){tip='<div class="tt"><div class="tt-s">'+se(t.status)+' '+bt(t.status)+'</div><div class="tt-d">'+t.timestamp+' · '+t.latency+'ms'+(t.error?' · '+t.error:'')+'</div></div>'}
        bars+='<div class="tb '+cls+'">'+tip+'</div>'}
      var up=m.uptime||0;

      html+='<div class="m-card"><div class="m-top"><div class="m-name">'+m.name+'</div>';
      html+='<span class="m-badge '+bc(m.status)+'">'+bt(m.status)+'</span></div>';
      html+='<div class="m-metrics">';
      html+='<div class="m-metric"><div class="m-mlabel">Latency</div><div class="m-mval" style="color:'+(m.latency>5000?'#f43f5e':m.latency>2000?'#f59e0b':'#10b981')+'">'+m.latency+'<span class="m-munit"> ms</span></div></div>';
      html+='<div class="m-metric"><div class="m-mlabel">Uptime</div><div class="m-mval" style="color:'+uptimeColor(up)+'">'+up.toFixed(1)+'<span class="m-munit"> %</span></div></div>';
      html+='</div>';
      html+='<div class="tl-wrap"><div class="tl-head"><span class="tl-label">History · '+Math.min(m.total_checks,60)+' checks</span>';
      html+='<span class="tl-uptime" style="color:'+uptimeColor(up)+'">'+(up>=99.5?'All Clear':up.toFixed(1)+'%')+'</span></div>';
      html+='<div class="tl-bars">'+bars+'</div>';
      html+='<div class="tl-axis"><span>Past</span><span>Now</span></div></div>';
      html+='</div>';
    });

    html+='</div></div></div>';
  });

  document.getElementById('content').innerHTML=html;
  document.getElementById('footer').textContent='Last update: '+d.updated_at+' · Poll #'+d.poll_count+' · Auto-refresh every 5 min';
  startCd(d.poll_interval||300);
}catch(e){document.getElementById('content').innerHTML='<p style="text-align:center;color:#f43f5e;padding:2rem">Failed: '+e.message+'</p>'}}
loadStatus();
</script>
</body>
</html>`
