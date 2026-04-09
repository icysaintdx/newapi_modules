/**
 * 统一注册模块 - 支持邮箱验证 + Turnstile
 * 所有主题共享此注册逻辑
 */
(function() {
    'use strict';
    
    var config = null;
    var verificationSent = false;
    var countdown = 0;
    var countdownTimer = null;
    var turnstileToken = null;
    var turnstileWidgetId = null;

    // 1. 获取系统配置
    function loadConfig() {
        return fetch('/api/status')
            .then(function(r) { return r.json(); })
            .then(function(response) {
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
        
        var form = document.getElementById('register-form');
        var submitBtn = document.getElementById('register-btn');
        
        var container = document.createElement('div');
        container.id = 'turnstile-container';
        container.style.cssText = 'margin-bottom:1rem;display:flex;justify-content:center;';
        
        form.insertBefore(container, submitBtn);
        
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

    function resetTurnstile() {
        if (turnstileToken && window.turnstile && turnstileWidgetId !== null) {
            window.turnstile.reset(turnstileWidgetId);
            turnstileToken = null;
        }
    }

    // 3. 显示/隐藏错误信息
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

    // 4. 显示/隐藏成功信息
    function showSuccess(message) {
        var success = document.getElementById('success-msg');
        if (success) {
            success.textContent = message;
            success.style.display = 'block';
        }
    }

    function hideSuccess() {
        var success = document.getElementById('success-msg');
        if (success) {
            success.style.display = 'none';
        }
    }

    // 5. 邮箱验证码倒计时
    function startCountdown(seconds) {
        countdown = seconds;
        var btn = document.getElementById('send-code-btn');
        if (!btn) return;

        btn.disabled = true;
        
        function updateButton() {
            if (countdown > 0) {
                btn.textContent = countdown + '秒后重发';
                countdown--;
                countdownTimer = setTimeout(updateButton, 1000);
            } else {
                btn.textContent = '发送验证码';
                btn.disabled = false;
                clearTimeout(countdownTimer);
            }
        }
        
        updateButton();
    }

    // 6. 发送邮箱验证码
    function sendVerificationCode() {
        var email = document.getElementById('email').value.trim();
        
        if (!email) {
            showError('请输入邮箱地址');
            return;
        }

        var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            showError('请输入有效的邮箱地址');
            return;
        }

        // 检查 Turnstile
        if (config && config.turnstile_check && !turnstileToken) {
            showError('请完成人机验证');
            return;
        }

        var btn = document.getElementById('send-code-btn');
        var originalText = btn.textContent;
        btn.disabled = true;
        btn.textContent = '发送中...';
        hideError();

        // 构建 URL，添加 turnstile token
        var url = '/api/verification?email=' + encodeURIComponent(email);
        if (turnstileToken) {
            url += '&turnstile=' + encodeURIComponent(turnstileToken);
        }

        fetch(url, {
            method: 'GET',
            credentials: 'same-origin'
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.success) {
                verificationSent = true;
                showSuccess('验证码已发送到您的邮箱，请查收');
                startCountdown(60);
                // 不重置 Turnstile，注册时还要用
            } else {
                showError(data.message || '发送验证码失败');
                btn.disabled = false;
                btn.textContent = originalText;
                resetTurnstile();
            }
        })
        .catch(function(err) {
            console.error('Send verification error:', err);
            showError('网络错误，请重试');
            btn.disabled = false;
            btn.textContent = originalText;
            resetTurnstile();
        });
    }

    // 7. 执行注册
    function performRegister(username, password, email, verificationCode) {
        var btn = document.getElementById('register-btn');
        var originalText = btn.textContent;
        
        btn.disabled = true;
        btn.textContent = '注册中...';
        hideError();
        hideSuccess();

        // 检查 Turnstile
        if (config && config.turnstile_check && !turnstileToken) {
            showError('请完成人机验证');
            btn.disabled = false;
            btn.textContent = originalText;
            return;
        }

        var requestBody = {
            username: username,
            password: password
        };

        // 如果启用了邮箱验证，添加邮箱和验证码
        if (config && config.email_verification) {
            if (!email) {
                showError('请输入邮箱地址');
                btn.disabled = false;
                btn.textContent = originalText;
                return;
            }
            if (!verificationCode) {
                showError('请输入邮箱验证码');
                btn.disabled = false;
                btn.textContent = originalText;
                return;
            }
            requestBody.email = email;
            requestBody.verification_code = verificationCode;
        }

        // 构建 URL，添加 turnstile token
        var url = '/api/user/register';
        if (turnstileToken) {
            url += '?turnstile=' + encodeURIComponent(turnstileToken);
        }

        fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(requestBody)
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.success) {
                showSuccess('注册成功！3秒后跳转到登录页面...');
                setTimeout(function() {
                    window.location.href = '/login';
                }, 3000);
            } else {
                showError(data.message || '注册失败，请重试');
                btn.disabled = false;
                btn.textContent = originalText;
                resetTurnstile();
            }
        })
        .catch(function(err) {
            console.error('Register error:', err);
            showError('网络错误，请检查连接后重试');
            btn.disabled = false;
            btn.textContent = originalText;
            resetTurnstile();
        });
    }

    // 8. 初始化注册表单
    function initRegisterForm() {
        var form = document.getElementById('register-form');
        if (!form) {
            console.error('Register form not found');
            return;
        }

        // 如果启用了邮箱验证，显示邮箱和验证码字段
        if (config && config.email_verification) {
            var emailGroup = document.getElementById('email-group');
            var codeGroup = document.getElementById('code-group');
            
            if (emailGroup) emailGroup.style.display = 'block';
            if (codeGroup) codeGroup.style.display = 'block';

            // 绑定发送验证码按钮
            var sendCodeBtn = document.getElementById('send-code-btn');
            if (sendCodeBtn) {
                sendCodeBtn.addEventListener('click', function(e) {
                    e.preventDefault();
                    sendVerificationCode();
                });
            }
        }

        // 绑定表单提交
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            
            var username = document.getElementById('username').value.trim();
            var password = document.getElementById('password').value;
            var password2 = document.getElementById('password2') ? document.getElementById('password2').value : password;
            var email = document.getElementById('email') ? document.getElementById('email').value.trim() : '';
            var verificationCode = document.getElementById('verification-code') ? document.getElementById('verification-code').value.trim() : '';
            
            // 基本验证
            if (!username || !password) {
                showError('请输入用户名和密码');
                return;
            }

            if (username.length < 3) {
                showError('用户名至少需要3个字符');
                return;
            }

            if (password.length < 6) {
                showError('密码至少需要6个字符');
                return;
            }

            if (password2 && password !== password2) {
                showError('两次输入的密码不一致');
                return;
            }

            // 如果启用了邮箱验证，检查验证码
            if (config && config.email_verification && !verificationSent) {
                showError('请先发送邮箱验证码');
                return;
            }
            
            performRegister(username, password, email, verificationCode);
        });
    }

    // 9. 主初始化函数
    function init() {
        loadConfig().then(function() {
            initTurnstile();
            initRegisterForm();
        });
    }

    // 10. 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
