package controller

import (
	"crypto/md5"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/QuantumNous/new-api/common"
	"github.com/QuantumNous/new-api/logger"
	"github.com/QuantumNous/new-api/model"
	"github.com/QuantumNous/new-api/service"
	"github.com/QuantumNous/new-api/setting/operation_setting"
	"github.com/QuantumNous/new-api/setting/system_setting"
	"github.com/gin-gonic/gin"
	"github.com/shopspring/decimal"
)

// PayProClient PayPro 客户端
type PayProClient struct {
	Address string
	Secret  string
}

// PayProOrderRequest PayPro 创建订单请求
type PayProOrderRequest struct {
	OrderNo       string  `json:"orderNo"`
	Amount        float64 `json:"amount"`
	PayType       string  `json:"payType"`
	NickName      string  `json:"nickName,omitempty"`
	Description   string  `json:"description,omitempty"`
	Email         string  `json:"email,omitempty"`
	NotifyUrl     string  `json:"notifyUrl,omitempty"`
	UserId        string  `json:"userId,omitempty"`
	ProductId     int64   `json:"productId,omitempty"`
	Timestamp     int64   `json:"timestamp"`
	ExpireSeconds int     `json:"expireSeconds,omitempty"`
	Sign          string  `json:"sign"`
}

// PayProOrderResponse PayPro 创建订单响应
type PayProOrderResponse struct {
	Code int    `json:"code"`
	Msg  string `json:"msg"`
	Data struct {
		OrderNo   string  `json:"orderNo"`
		Amount    float64 `json:"amount"`
		PayType   string  `json:"payType"`
		PayNum    string  `json:"payNum"`
		State     int     `json:"state"`
		Message   string  `json:"message"`
		Timestamp int64   `json:"timestamp"`
		QrCodeUrl string  `json:"qrCodeUrl"`
		ReturnUrl string  `json:"returnUrl"`
	} `json:"data"`
}

// GetPayProClient 获取 PayPro 客户端
func GetPayProClient() *PayProClient {
	if operation_setting.PayProAddress == "" || operation_setting.PayProSecret == "" {
		return nil
	}
	return &PayProClient{
		Address: operation_setting.PayProAddress,
		Secret:  operation_setting.PayProSecret,
	}
}

// GenerateSign 生成签名
func (c *PayProClient) GenerateSign(params map[string]interface{}) string {
	// 获取所有键并排序
	keys := make([]string, 0, len(params))
	for k := range params {
		if k != "sign" && params[k] != nil && params[k] != "" {
			keys = append(keys, k)
		}
	}
	sort.Strings(keys)

	// 拼接参数
	var sb strings.Builder
	for _, k := range keys {
		v := params[k]
		if v != nil && v != "" {
			sb.WriteString(k)
			sb.WriteString("=")
			sb.WriteString(fmt.Sprintf("%v", v))
			sb.WriteString("&")
		}
	}
	sb.WriteString("key=")
	sb.WriteString(c.Secret)

	// MD5 加密并转大写
	signStr := sb.String()
	hash := md5.Sum([]byte(signStr))
	return strings.ToUpper(hex.EncodeToString(hash[:]))
}

// CreateOrder 创建订单
func (c *PayProClient) CreateOrder(req *PayProOrderRequest) (*PayProOrderResponse, error) {
	// 构建请求参数
	params := make(map[string]interface{})
	params["orderNo"] = req.OrderNo
	params["amount"] = req.Amount
	params["payType"] = req.PayType
	if req.NickName != "" {
		params["nickName"] = req.NickName
	}
	if req.Description != "" {
		params["description"] = req.Description
	}
	if req.Email != "" {
		params["email"] = req.Email
	}
	if req.NotifyUrl != "" {
		params["notifyUrl"] = req.NotifyUrl
	}
	if req.UserId != "" {
		params["userId"] = req.UserId
	}
	if req.ProductId > 0 {
		params["productId"] = req.ProductId
	}
	if req.ExpireSeconds > 0 {
		params["expireSeconds"] = req.ExpireSeconds
	}
	params["timestamp"] = req.Timestamp

	// 生成签名
	req.Sign = c.GenerateSign(params)
	params["sign"] = req.Sign

	// 发送请求
	jsonData, err := json.Marshal(params)
	if err != nil {
		return nil, fmt.Errorf("序列化请求参数失败: %v", err)
	}

	apiUrl := c.Address + "/api/openapi/add"
	resp, err := http.Post(apiUrl, "application/json", strings.NewReader(string(jsonData)))
	if err != nil {
		return nil, fmt.Errorf("发送请求失败: %v", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("读取响应失败: %v", err)
	}

	var result PayProOrderResponse
	if err := json.Unmarshal(body, &result); err != nil {
		return nil, fmt.Errorf("解析响应失败: %v, 响应内容: %s", err, string(body))
	}

	if result.Code != 200 {
		return nil, fmt.Errorf("创建订单失败: %s", result.Msg)
	}

	return &result, nil
}

// RequestPayPro 请求 PayPro 支付
func RequestPayPro(c *gin.Context) {
	var req EpayRequest
	err := c.ShouldBindJSON(&req)
	if err != nil {
		c.JSON(200, gin.H{"message": "error", "data": "参数错误"})
		return
	}

	if req.Amount < getMinTopup() {
		c.JSON(200, gin.H{"message": "error", "data": fmt.Sprintf("充值数量不能小于 %d", getMinTopup())})
		return
	}

	id := c.GetInt("id")
	group, err := model.GetUserGroup(id, true)
	if err != nil {
		c.JSON(200, gin.H{"message": "error", "data": "获取用户分组失败"})
		return
	}

	payMoney := getPayMoney(req.Amount, group)
	if payMoney < 0.01 {
		c.JSON(200, gin.H{"message": "error", "data": "充值金额过低"})
		return
	}

	// 验证支付方式
	validPayTypes := []string{"alipay", "wechat", "alipay_dmf", "wechat_zs"}
	isValidPayType := false
	for _, pt := range validPayTypes {
		if req.PaymentMethod == pt {
			isValidPayType = true
			break
		}
	}
	if !isValidPayType {
		c.JSON(200, gin.H{"message": "error", "data": "不支持的支付方式"})
		return
	}

	client := GetPayProClient()
	if client == nil {
		c.JSON(200, gin.H{"message": "error", "data": "当前管理员未配置 PayPro 支付信息"})
		return
	}

	// 生成订单号
	tradeNo := fmt.Sprintf("%s%d", common.GetRandomString(6), time.Now().Unix())
	tradeNo = fmt.Sprintf("USR%dNO%s", id, tradeNo)

	// 构建回调地址
	callBackAddress := service.GetCallbackAddress()
	notifyUrl, _ := url.Parse(callBackAddress + "/api/user/paypro/notify")

	// 创建 PayPro 订单
	payProReq := &PayProOrderRequest{
		OrderNo:       tradeNo,
		Amount:        payMoney,
		PayType:       req.PaymentMethod,
		UserId:        strconv.Itoa(id),
		NotifyUrl:     notifyUrl.String(),
		Timestamp:     time.Now().UnixMilli(),
		ExpireSeconds: 1800, // 30分钟过期
	}

	payProResp, err := client.CreateOrder(payProReq)
	if err != nil {
		log.Printf("PayPro 创建订单失败: %v", err)
		c.JSON(200, gin.H{"message": "error", "data": "创建订单失败"})
		return
	}

	// 保存订单到数据库
	amount := req.Amount
	if operation_setting.GetQuotaDisplayType() == operation_setting.QuotaDisplayTypeTokens {
		dAmount := decimal.NewFromInt(int64(amount))
		dQuotaPerUnit := decimal.NewFromFloat(common.QuotaPerUnit)
		amount = dAmount.Div(dQuotaPerUnit).IntPart()
	}

	topUp := &model.TopUp{
		UserId:        id,
		Amount:        amount,
		Money:         payMoney,
		TradeNo:       tradeNo,
		PaymentMethod: req.PaymentMethod,
		CreateTime:    time.Now().Unix(),
		Status:        "pending",
	}

	err = topUp.Insert()
	if err != nil {
		c.JSON(200, gin.H{"message": "error", "data": "创建订单失败"})
		return
	}

	// 返回支付链接
	c.JSON(200, gin.H{
		"message": "success",
		"data":    payProResp.Data.ReturnUrl,
		"url":     payProResp.Data.ReturnUrl,
	})
}

// PayProNotify PayPro 支付回调
func PayProNotify(c *gin.Context) {
	// PayPro 的回调通知
	// 根据文档，回调会发送订单状态变更通知
	var params map[string]string

	if c.Request.Method == "POST" {
		if err := c.Request.ParseForm(); err != nil {
			log.Println("PayPro 回调 POST 解析失败:", err)
			c.String(200, "fail")
			return
		}
		params = make(map[string]string)
		for k, v := range c.Request.PostForm {
			if len(v) > 0 {
				params[k] = v[0]
			}
		}
	} else {
		params = make(map[string]string)
		for k, v := range c.Request.URL.Query() {
			if len(v) > 0 {
				params[k] = v[0]
			}
		}
	}

	if len(params) == 0 {
		log.Println("PayPro 回调参数为空")
		c.String(200, "fail")
		return
	}

	// 验证签名
	client := GetPayProClient()
	if client == nil {
		log.Println("PayPro 回调失败：未找到配置信息")
		c.String(200, "fail")
		return
	}

	// 提取订单号
	tradeNo := params["orderNo"]
	if tradeNo == "" {
		log.Println("PayPro 回调：订单号为空")
		c.String(200, "fail")
		return
	}

	// 验证签名
	signParams := make(map[string]interface{})
	for k, v := range params {
		if k != "sign" {
			signParams[k] = v
		}
	}
	expectedSign := client.GenerateSign(signParams)
	if expectedSign != params["sign"] {
		log.Printf("PayPro 回调签名验证失败：期望 %s，实际 %s", expectedSign, params["sign"])
		c.String(200, "fail")
		return
	}

	// 检查订单状态
	state := params["state"]
	if state != "1" && state != "3" { // 1-已支付，3-已支付
		log.Printf("PayPro 回调：订单状态不是已支付: %s", state)
		c.String(200, "success")
		return
	}

	// 处理订单
	LockOrder(tradeNo)
	defer UnlockOrder(tradeNo)

	topUp := model.GetTopUpByTradeNo(tradeNo)
	if topUp == nil {
		log.Printf("PayPro 回调：未找到订单 %s", tradeNo)
		c.String(200, "fail")
		return
	}

	if topUp.Status == "pending" {
		topUp.Status = "success"
		err := topUp.Update()
		if err != nil {
			log.Printf("PayPro 回调：更新订单失败 %v", topUp)
			c.String(200, "fail")
			return
		}

		// 增加用户额度
		dAmount := decimal.NewFromInt(int64(topUp.Amount))
		dQuotaPerUnit := decimal.NewFromFloat(common.QuotaPerUnit)
		quotaToAdd := int(dAmount.Mul(dQuotaPerUnit).IntPart())
		err = model.IncreaseUserQuota(topUp.UserId, quotaToAdd, true)
		if err != nil {
			log.Printf("PayPro 回调：更新用户失败 %v", topUp)
			c.String(200, "fail")
			return
		}

		log.Printf("PayPro 回调：更新用户成功 %v", topUp)
		model.RecordLog(topUp.UserId, model.LogTypeTopup, fmt.Sprintf("使用 PayPro 充值成功，充值金额: %v，支付金额：%f", logger.LogQuota(quotaToAdd), topUp.Money))
	}

	c.String(200, "success")
}
