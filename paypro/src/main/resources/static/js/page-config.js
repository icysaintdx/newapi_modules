/**
 * 页面配置动态加载器
 * 从数据库加载配置并应用到页面
 */
(function() {
    let config = {};
    
    async function loadAndApply() {
        try {
            const res = await fetch('/api/config/public');
            const result = await res.json();
            if (result.code === 200) {
                config = result.data;
                applyToPage();
            }
        } catch (e) {
            console.error('配置加载失败:', e);
        }
    }
    
    function applyToPage() {
        // 更新标题
        if (config.index_redirect_title && document.title.includes('跳转')) {
            document.title = config.index_redirect_title;
        } else if (config.payment_title && document.title.includes('支付')) {
            document.title = config.payment_title;
        } else if (config.recharge_title && (document.title.includes('捐赠') || document.title.includes('购卡'))) {
            document.title = config.recharge_title;
        } else if (config.help_title && document.title.includes('帮助')) {
            document.title = config.help_title;
        }
        
        // 替换常见文本
        replaceInPage('PayPro', config.siteName || config.common_logo || 'PayPro');
        replaceInPage('个人收款支付系统', config.siteTitle || '个人收款支付系统');
        
        // 更新页脚
        if (config.common_footer) {
            document.querySelectorAll('footer, .footer, [class*="footer"]').forEach(el => {
                if (el.textContent.includes('©') || el.textContent.includes('PayPro')) {
                    el.textContent = config.common_footer;
                }
            });
        }
    }
    
    function replaceInPage(oldText, newText) {
        if (!newText || oldText === newText) return;
        
        const walk = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
            acceptNode: n => {
                if (n.parentElement.tagName === 'SCRIPT' || n.parentElement.tagName === 'STYLE') {
                    return NodeFilter.FILTER_REJECT;
                }
                return n.textContent.includes(oldText) ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_SKIP;
            }
        });
        
        const nodes = [];
        let node;
        while (node = walk.nextNode()) nodes.push(node);
        nodes.forEach(n => n.textContent = n.textContent.replace(new RegExp(oldText, 'g'), newText));
    }
    
    window.PageConfig = { load: loadAndApply, get: k => config[k] };
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadAndApply);
    } else {
        loadAndApply();
    }
})();
