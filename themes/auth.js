/**
 * 统一登录模块 - 支持 Turnstile 验证
 * 所有主题共享此登录逻辑
 */
(function() {
    'use strict';
    
    var config = null;
    var turnstileToken = null;
    var turnstileWidgetId = null;

    // 1. 获取系统配置
    function loadConfig() {
        return fetch('/api/status')
            .then(function(r) { return r.json(); })
            .then(function(response) {
                // API 返回格式: {success: true, data: {...}}
                config = response.data || response;
                return config;
            })
            .catch(function(err) {
                console.error('Failed to load config:', err);
                return null;
            });
    }

    // 2. 初始化 Turnstile
    function initTurnstile() {
        if (!config || !config.turnstile_check) return;
        
        // 动态加载 Turnstile 脚本
        var script = document.createElement('script');
        script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js';
        script.async = true;
        script.defer = true;
        script.onload = function() {
            renderTurnstile();
        };
        document.head.appendChild(script);
    }

    function renderTurnstile() {
        if (!window.turnstile || !config.turnstile_site_key) return;
        
        // 在登录按钮上方插入 Turnstile 容器
        var form = document.getElementById('login-form');
        var submitBtn = document.getElementById('login-btn');
        
        var container = document.createElement('div');
        container.id = 'turnstile-container';
        container.style.cssText = 'margin-bottom:1rem;display:flex;justify-content:center;';
        
        form.insertBefore(container, submitBtn);
        
        // 渲染 Turnstile 组件
        turnstileWidgetId = window.turnstile.render('#turnstile-container', {
            sitekey: config.turnstile_site_key,
            theme: 'auto',
            callback: function(token) {
                turnstileToken = token;
            },
            'error-callback': function() {
                turnstileToken = null;
                showError('Turnstile 验证失败，请刷新页面重试');
            },
            'expired-callback': function() {
                turnstileToken = null;
                showError('Turnstile 验证已过期，请重新验证');
            }
        });
    }

    // 3. 显示错误信息
    function showError(message) {
        var err = document.getElementById('error-msg');
        if (err) {
            err.textContent = message;
            err.style.display = 'block';
        }
    }

    function hideError() {
        var err = document.getElementById('error-msg');
        if (err) {
            err.style.display = 'none';
        }
    }

    // 4. 执行登录
    function performLogin(username, password) {
        var btn = document.getElementById('login-btn');
        var originalText = btn.textContent;
        
        btn.disabled = true;
        btn.textContent = '登录中...';
        hideError();

        // 检查 Turnstile
        if (config && config.turnstile_check && !turnstileToken) {
            showError('请完成人机验证');
            btn.disabled = false;
            btn.textContent = originalText;
            return;
        }

        // 构建请求 URL（Turnstile token 作为查询参数）
        var url = '/api/user/login';
        if (turnstileToken) {
            url += '?turnstile=' + encodeURIComponent(turnstileToken);
        }

        fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ username: username, password: password })
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.success) {
                // 检查是否需要 2FA
                if (data.data && data.data.require_2fa) {
                    // 跳转到 2FA 验证页面（使用原生页面，因为自定义主题没有 2FA 页面）
                    alert('您的账号启用了二次验证，将跳转到验证页面');
                    window.location.href = '/console/login';
                    return;
                }
                
                // 保存用户信息到 localStorage
                if (data.data) {
                    try {
                        localStorage.setItem('user', JSON.stringify(data.data));
                    } catch(e) {
                        console.error('Failed to save user data:', e);
                    }
                }
                
                // 登录成功，跳转到控制台
                window.location.href = '/console';
            } else {
                // 登录失败
                showError(data.message || '登录失败，请检查用户名和密码');
                btn.disabled = false;
                btn.textContent = originalText;
                
                // 重置 Turnstile
                if (turnstileToken && window.turnstile && turnstileWidgetId !== null) {
                    window.turnstile.reset(turnstileWidgetId);
                    turnstileToken = null;
                }
            }
        })
        .catch(function(err) {
            console.error('Login error:', err);
            showError('网络错误，请检查连接后重试');
            btn.disabled = false;
            btn.textContent = originalText;
            
            // 重置 Turnstile
            if (turnstileToken && window.turnstile && turnstileWidgetId !== null) {
                window.turnstile.reset(turnstileWidgetId);
                turnstileToken = null;
            }
        });
    }

    // 5. 初始化登录表单
    function initLoginForm() {
        var form = document.getElementById('login-form');
        if (!form) {
            console.error('Login form not found');
            return;
        }

        form.addEventListener('submit', function(e) {
            e.preventDefault();
            
            var username = document.getElementById('username').value.trim();
            var password = document.getElementById('password').value;
            
            if (!username || !password) {
                showError('请输入用户名和密码');
                return;
            }
            
            performLogin(username, password);
        });
    }

    // 6. 主初始化函数
    function init() {
        loadConfig().then(function() {
            initTurnstile();
            initLoginForm();
        });
    }

    // 7. 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
