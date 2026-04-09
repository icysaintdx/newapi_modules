(function() {
    // Immediately add overlay to prevent flash of unstyled content
    if (document.body) {
        var overlay = document.createElement('div');
        overlay.id = 'theme-loading-overlay';
        overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:#0d0d0d;z-index:999999;transition:opacity 0.3s;';
        document.body.appendChild(overlay);
    }

    var THEMES = ['dark', 'light', 'tech', 'cartoon', 'pixel', 'animated'];
    var COOKIE = 'saint_theme';
    var NAMES = { dark: '深色', light: '浅色', tech: '科技', cartoon: '卡通', pixel: '像素', animated: '动画' };
    var PUBLIC_ROUTES = ['/', '/about', '/announcement', '/privacy'];

    var CSS = {
        dark: [
            'html,body,#root{background:#0d0d0d!important;color:#fff!important}',
            '.semi-layout-header{background:rgba(26,26,26,0.95)!important;border-bottom:1px solid #333!important}',
            '.semi-layout-header .flex.items-center.justify-between{justify-content:center!important;position:relative!important}',
            '.semi-layout-header .flex.items-center.justify-between>div:first-child{position:absolute!important;left:1rem!important}',
            '.semi-layout-header .flex.items-center.justify-between>div:last-child{position:absolute!important;right:1rem!important}',
            '.semi-layout-sider{background:#111!important;border-right:1px solid #222!important}',
            '.semi-navigation{background:transparent!important}',
            '.semi-navigation-item-text{color:#aaa!important}',
            '.semi-navigation-item:hover .semi-navigation-item-text{color:#667eea!important}',
            '.semi-navigation-item.semi-navigation-item-selected .semi-navigation-item-text{color:#667eea!important}',
            '.semi-navigation-item:hover{background:rgba(102,126,234,0.1)!important}',
            '.semi-navigation-item.semi-navigation-item-selected{background:rgba(102,126,234,0.15)!important}',
            '.semi-navigation-item-icon{color:#888!important}',
            '.semi-navigation-item:hover .semi-navigation-item-icon{color:#667eea!important}',
            '.semi-navigation-subtitle{color:#555!important}',
            '.semi-divider{border-color:#222!important}',
            '.semi-card{background:#1a1a1a!important;border:1px solid #333!important;border-radius:12px!important}',
            '.semi-card:hover{border-color:rgba(102,126,234,0.5)!important;box-shadow:0 4px 20px rgba(102,126,234,0.1)!important}',
            '.semi-table-thead>tr>th{background:#222!important;color:#888!important;border-bottom:1px solid #333!important}',
            '.semi-table-tbody>tr>td{border-bottom:1px solid #222!important;color:#ccc!important}',
            '.semi-table-tbody>tr:hover>td{background:rgba(102,126,234,0.05)!important}',
            '.semi-table-wrapper{border:1px solid #333!important;border-radius:12px!important}',
            '.semi-tag{background:#222!important;color:#ccc!important;border:1px solid #444!important}',
            '.semi-tag-green{background:rgba(34,197,94,0.1)!important;color:#22c55e!important;border-color:rgba(34,197,94,0.3)!important}',
            '.semi-tag-blue{background:rgba(102,126,234,0.15)!important;color:#667eea!important;border-color:rgba(102,126,234,0.3)!important}',
            '.semi-button-primary{background:linear-gradient(135deg,#667eea,#764ba2)!important;border:none!important;color:#fff!important}',
            '.semi-button-primary:hover{box-shadow:0 4px 15px rgba(102,126,234,0.3)!important}',
            '.semi-button-secondary{background:#2a2a2a!important;border:1px solid #444!important;color:#ccc!important}',
            '.semi-tabs-tab{color:#888!important}',
            '.semi-tabs-tab-active .semi-tabs-tab-text{color:#667eea!important}',
            '.semi-tabs-bar{border-color:#333!important}',
            '.semi-input{background:#1a1a1a!important;border-color:#333!important;color:#fff!important}',
            '.semi-input:focus{border-color:#667eea!important}',
            '.semi-form-field-label{color:#888!important}',
            '.semi-select{background:#1a1a1a!important;border-color:#333!important;color:#fff!important}',
            '.semi-switch{background:#444!important}',
            '.semi-switch-checked{background:#667eea!important}',
            '.semi-progress-line-rail{background:#333!important}',
            '.semi-progress-line-path{stroke:#667eea!important}',
            '.semi-modal-content{background:#1a1a1a!important;border-color:#333!important}',
            '.semi-modal-header{color:#fff!important}',
            '.semi-modal-body{color:#ccc!important}',
            '.semi-banner{background:rgba(102,126,234,0.1)!important;border:1px solid rgba(102,126,234,0.3)!important;border-radius:12px!important}',
            '.semi-pagination-item{background:#1a1a1a!important;border-color:#333!important;color:#ccc!important}',
            '.semi-pagination-item-active{background:#667eea!important;border-color:#667eea!important;color:#fff!important}',
            'canvas[id*="visactor_window"]{background:#0d0d0d!important}',
            '[class*="visactor"]{background:#0d0d0d!important}',
            '[class*="chart"]{background:#0d0d0d!important}',
            '[class*="apexcharts"]{background:#0d0d0d!important}',
            '.apexcharts-canvas .apexcharts-grid,.apexcharts-canvas .apexcharts-gridlines line{stroke:#333!important}',
            '.apexcharts-canvas .apexcharts-text{fill:#888!important}',
            '.apexcharts-canvas .apexcharts-legend-text{color:#aaa!important}',
            '.semi-avatar{background:#333!important;border:2px solid #667eea!important}',
            '.semi-button.semi-button-borderless{color:#aaa!important}',
            '.semi-button.semi-button-borderless:hover{background:rgba(102,126,234,0.15)!important;color:#667eea!important}',
            '.semi-typography{color:#ccc!important}',
            '.semi-typography-primary{color:#fff!important}',
            '.semi-typography-secondary{color:#888!important}',
            'h1,h2,h3,h4,h5,h6{color:#fff!important}',
            'main{background:#0d0d0d!important}',
            '[data-sonner-toast]{background:#1a1a1a!important;border:1px solid #333!important;color:#fff!important}',
            '::-webkit-scrollbar{width:6px!important;height:6px!important}',
            '::-webkit-scrollbar-track{background:transparent!important}',
            '::-webkit-scrollbar-thumb{background:#333!important;border-radius:3px!important}',
            'footer{display:none!important}',
            'button[aria-label="切换主题"]{display:none!important}'
        ],
        light: [
            'html,body,#root{background:linear-gradient(180deg,#f0f4ff,#fafafa)!important;color:#333!important}',
            '.semi-layout-header{background:rgba(255,255,255,0.95)!important;border-bottom:1px solid #eee!important;box-shadow:0 1px 3px rgba(0,0,0,0.05)!important}',
            '.semi-layout-sider{background:#fff!important;border-right:1px solid #f0f0f0!important;box-shadow:1px 0 3px rgba(0,0,0,0.03)!important}',
            '.semi-navigation{background:transparent!important}',
            '.semi-navigation-item-text{color:#666!important}',
            '.semi-navigation-item:hover .semi-navigation-item-text{color:#5c7cfa!important}',
            '.semi-navigation-item.semi-navigation-item-selected .semi-navigation-item-text{color:#5c7cfa!important}',
            '.semi-navigation-item:hover{background:rgba(92,124,250,0.08)!important}',
            '.semi-navigation-item.semi-navigation-item-selected{background:rgba(92,124,250,0.1)!important}',
            '.semi-navigation-item-icon{color:#999!important}',
            '.semi-navigation-item:hover .semi-navigation-item-icon{color:#5c7cfa!important}',
            '.semi-navigation-subtitle{color:#bbb!important}',
            '.semi-divider{border-color:#f0f0f0!important}',
            '.semi-card{background:#fff!important;border:1px solid #eee!important;border-radius:12px!important;box-shadow:0 2px 8px rgba(0,0,0,0.04)!important}',
            '.semi-card:hover{border-color:rgba(92,124,250,0.3)!important;box-shadow:0 4px 16px rgba(92,124,250,0.08)!important}',
            '.semi-table-thead>tr>th{background:#fafafa!important;color:#888!important;border-bottom:1px solid #eee!important}',
            '.semi-table-tbody>tr>td{border-bottom:1px solid #f0f0f0!important;color:#555!important}',
            '.semi-table-tbody>tr:hover>td{background:rgba(92,124,250,0.03)!important}',
            '.semi-table-wrapper{border:1px solid #eee!important;border-radius:12px!important;background:#fff!important}',
            '.semi-tag{background:#f0f0f0!important;color:#666!important;border:1px solid #e0e0e0!important}',
            '.semi-tag-green{background:rgba(34,197,94,0.08)!important;color:#16a34a!important;border-color:rgba(34,197,94,0.2)!important}',
            '.semi-tag-blue{background:rgba(92,124,250,0.1)!important;color:#5c7cfa!important;border-color:rgba(92,124,250,0.2)!important}',
            '.semi-button-primary{background:#5c7cfa!important;border:none!important;color:#fff!important}',
            '.semi-button-primary:hover{background:#4b6bf5!important;box-shadow:0 4px 12px rgba(92,124,250,0.25)!important}',
            '.semi-button-secondary{background:#f0f0f0!important;border:1px solid #ddd!important;color:#666!important}',
            '.semi-tabs-tab{color:#888!important}',
            '.semi-tabs-tab-active .semi-tabs-tab-text{color:#5c7cfa!important}',
            '.semi-tabs-bar{border-color:#eee!important}',
            '.semi-input{background:#fafafa!important;border-color:#eee!important;color:#333!important}',
            '.semi-input:focus{border-color:#5c7cfa!important}',
            '.semi-form-field-label{color:#888!important}',
            '.semi-select{background:#fafafa!important;border-color:#eee!important;color:#333!important}',
            '.semi-switch{background:#ddd!important}',
            '.semi-switch-checked{background:#5c7cfa!important}',
            '.semi-progress-line-rail{background:#eee!important}',
            '.semi-progress-line-path{stroke:#5c7cfa!important}',
            '.semi-modal-content{background:#fff!important;border-color:#eee!important}',
            '.semi-modal-header{color:#1a1a1a!important}',
            '.semi-modal-body{color:#555!important}',
            '.semi-banner{background:rgba(92,124,250,0.05)!important;border:1px solid rgba(92,124,250,0.2)!important;border-radius:12px!important}',
            '.semi-pagination-item{background:#fff!important;border-color:#eee!important;color:#333!important}',
            '.semi-pagination-item-active{background:#5c7cfa!important;border-color:#5c7cfa!important;color:#fff!important}',
            'canvas[id*="visactor_window"]{background:#fafafa!important}',
            '[class*="visactor"]{background:#fafafa!important}',
            '[class*="chart"]{background:#fafafa!important}',
            '[class*="apexcharts"]{background:#fafafa!important}',
            '.apexcharts-canvas .apexcharts-grid,.apexcharts-canvas .apexcharts-gridlines line{stroke:#eee!important}',
            '.apexcharts-canvas .apexcharts-text{fill:#888!important}',
            '.apexcharts-canvas .apexcharts-legend-text{color:#666!important}',
            '.semi-avatar{background:#f0f0f0!important;border:2px solid #5c7cfa!important}',
            '.semi-button.semi-button-borderless{color:#666!important}',
            '.semi-button.semi-button-borderless:hover{background:rgba(92,124,250,0.1)!important;color:#5c7cfa!important}',
            '.semi-typography{color:#555!important}',
            '.semi-typography-primary{color:#1a1a1a!important}',
            '.semi-typography-secondary{color:#888!important}',
            'h1,h2,h3,h4,h5,h6{color:#1a1a1a!important}',
            'main{background:transparent!important}',
            '[data-sonner-toast]{background:#fff!important;border:1px solid #eee!important;color:#333!important}',
            '::-webkit-scrollbar{width:6px!important;height:6px!important}',
            '::-webkit-scrollbar-track{background:transparent!important}',
            '::-webkit-scrollbar-thumb{background:#ddd!important;border-radius:3px!important}',
            'footer{display:none!important}',
            'button[aria-label="切换主题"]{display:none!important}'
        ],
        tech: [
            '@import url("https://fonts.googleapis.com/css2?family=Orbitron:wght@400;500;600;700&family=Rajdhani:wght@300;400;500;600;700&display=swap")',
            '*{font-family:"Rajdhani",sans-serif!important}',
            'h1,h2,h3,h4,h5,h6{font-family:"Orbitron",sans-serif!important;letter-spacing:1px!important}',
            'html,body,#root{background:#0a0e17!important;color:#00f0ff!important}',
            'body::before{content:"";position:fixed;top:0;left:0;width:100%;height:100%;background:linear-gradient(90deg,transparent 49%,rgba(0,240,255,0.03) 50%,transparent 51%),linear-gradient(transparent 49%,rgba(0,240,255,0.03) 50%,transparent 51%);background-size:50px 50px;pointer-events:none;z-index:0}',
            '.semi-layout-header{background:rgba(10,14,23,0.9)!important;border-bottom:1px solid rgba(0,240,255,0.3)!important;box-shadow:0 0 30px rgba(0,240,255,0.1)!important}',
            '.semi-layout-sider{background:rgba(10,14,23,0.95)!important;border-right:1px solid rgba(0,240,255,0.2)!important}',
            '.semi-navigation{background:transparent!important}',
            '.semi-navigation-item-text{color:rgba(0,240,255,0.6)!important;font-family:"Orbitron",sans-serif!important;font-size:0.7rem!important;letter-spacing:1px!important}',
            '.semi-navigation-item:hover .semi-navigation-item-text{color:#00f0ff!important;text-shadow:0 0 10px rgba(0,240,255,0.5)!important}',
            '.semi-navigation-item.semi-navigation-item-selected .semi-navigation-item-text{color:#00f0ff!important}',
            '.semi-navigation-item:hover{background:rgba(0,240,255,0.08)!important}',
            '.semi-navigation-item.semi-navigation-item-selected{background:rgba(0,240,255,0.12)!important;border-left:2px solid #00f0ff!important}',
            '.semi-navigation-item-icon{color:rgba(0,240,255,0.5)!important}',
            '.semi-navigation-item:hover .semi-navigation-item-icon{color:#00f0ff!important;filter:drop-shadow(0 0 5px rgba(0,240,255,0.5))!important}',
            '.semi-navigation-subtitle{color:rgba(0,240,255,0.4)!important;font-family:"Orbitron",sans-serif!important;letter-spacing:2px!important}',
            '.semi-divider{border-color:rgba(0,240,255,0.15)!important}',
            '.semi-card{background:rgba(10,14,23,0.8)!important;border:1px solid rgba(0,240,255,0.2)!important;border-radius:4px!important}',
            '.semi-card:hover{border-color:rgba(0,240,255,0.5)!important;box-shadow:0 0 20px rgba(0,240,255,0.15)!important}',
            '.semi-table-thead>tr>th{background:rgba(0,240,255,0.05)!important;color:rgba(0,240,255,0.7)!important;border-bottom:1px solid rgba(0,240,255,0.2)!important;font-family:"Orbitron",sans-serif!important;font-size:0.65rem!important;letter-spacing:1px!important}',
            '.semi-table-tbody>tr>td{border-bottom:1px solid rgba(0,240,255,0.1)!important;color:rgba(0,240,255,0.8)!important}',
            '.semi-table-tbody>tr:hover>td{background:rgba(0,240,255,0.05)!important}',
            '.semi-table-wrapper{border:1px solid rgba(0,240,255,0.2)!important;border-radius:4px!important;background:rgba(10,14,23,0.6)!important}',
            '.semi-tag{background:rgba(0,0,0,0.3)!important;color:#00f0ff!important;border:1px solid rgba(0,240,255,0.3)!important;font-family:"Orbitron",sans-serif!important;font-size:0.6rem!important}',
            '.semi-tag-green{background:rgba(0,240,255,0.1)!important;color:#00f0ff!important;border-color:rgba(0,240,255,0.4)!important;box-shadow:0 0 8px rgba(0,240,255,0.2)!important}',
            '.semi-tag-blue{background:rgba(255,0,170,0.1)!important;color:#ff00aa!important;border-color:rgba(255,0,170,0.4)!important;box-shadow:0 0 8px rgba(255,0,170,0.2)!important}',
            '.semi-button-primary{background:transparent!important;border:2px solid #00f0ff!important;color:#00f0ff!important;font-family:"Orbitron",sans-serif!important;letter-spacing:2px!important;text-transform:uppercase!important}',
            '.semi-button-primary:hover{background:rgba(0,240,255,0.1)!important;box-shadow:0 0 30px rgba(0,240,255,0.5)!important}',
            '.semi-button-secondary{background:transparent!important;border:1px solid rgba(0,240,255,0.3)!important;color:rgba(0,240,255,0.7)!important}',
            '.semi-tabs-tab{color:rgba(0,240,255,0.4)!important;font-family:"Orbitron",sans-serif!important;font-size:0.7rem!important;letter-spacing:1px!important}',
            '.semi-tabs-tab-active .semi-tabs-tab-text{color:#00f0ff!important;text-shadow:0 0 8px rgba(0,240,255,0.5)!important}',
            '.semi-tabs-bar{border-color:rgba(0,240,255,0.2)!important}',
            '.semi-input{background:rgba(0,0,0,0.5)!important;border-color:rgba(0,240,255,0.3)!important;color:#00f0ff!important}',
            '.semi-input:focus{border-color:#00f0ff!important;box-shadow:0 0 15px rgba(0,240,255,0.3)!important}',
            '.semi-form-field-label{color:rgba(0,240,255,0.7)!important;font-family:"Orbitron",sans-serif!important;font-size:0.65rem!important;letter-spacing:1px!important}',
            '.semi-select{background:rgba(0,0,0,0.5)!important;border-color:rgba(0,240,255,0.3)!important;color:#00f0ff!important}',
            '.semi-switch{background:rgba(255,255,255,0.1)!important;border:1px solid rgba(0,240,255,0.3)!important}',
            '.semi-switch-checked{background:rgba(0,240,255,0.3)!important;border-color:#00f0ff!important;box-shadow:0 0 10px rgba(0,240,255,0.3)!important}',
            '.semi-progress-line-rail{background:rgba(0,240,255,0.1)!important}',
            '.semi-progress-line-path{stroke:#00f0ff!important;filter:drop-shadow(0 0 5px rgba(0,240,255,0.5))!important}',
            '.semi-modal-content{background:rgba(10,14,23,0.95)!important;border-color:rgba(0,240,255,0.3)!important}',
            '.semi-modal-header{color:#00f0ff!important;font-family:"Orbitron",sans-serif!important}',
            '.semi-modal-body{color:rgba(0,240,255,0.8)!important}',
            '.semi-banner{background:rgba(255,0,170,0.1)!important;border:1px solid rgba(255,0,170,0.3)!important;border-radius:4px!important;color:rgba(255,0,170,0.8)!important}',
            '.semi-pagination-item{background:rgba(10,14,23,0.8)!important;border-color:rgba(0,240,255,0.3)!important;color:#00f0ff!important}',
            '.semi-pagination-item-active{background:rgba(0,240,255,0.2)!important;border-color:#00f0ff!important;box-shadow:0 0 15px rgba(0,240,255,0.3)!important}',
            'canvas[id*="visactor_window"]{background:#0a0e17!important}',
            '[class*="visactor"]{background:#0a0e17!important}',
            '[class*="chart"]{background:#0a0e17!important}',
            '[class*="apexcharts"]{background:#0a0e17!important}',
            '.apexcharts-canvas .apexcharts-grid,.apexcharts-canvas .apexcharts-gridlines line{stroke:rgba(0,240,255,0.15)!important}',
            '.apexcharts-canvas .apexcharts-text{fill:rgba(0,240,255,0.7)!important}',
            '.apexcharts-canvas .apexcharts-legend-text{color:rgba(0,240,255,0.7)!important}',
            '.semi-avatar{background:rgba(0,0,0,0.5)!important;border:2px solid rgba(0,240,255,0.5)!important;box-shadow:0 0 10px rgba(0,240,255,0.2)!important}',
            '.semi-button.semi-button-borderless{color:rgba(0,240,255,0.7)!important}',
            '.semi-button.semi-button-borderless:hover{background:rgba(0,240,255,0.1)!important;color:#00f0ff!important;box-shadow:0 0 10px rgba(0,240,255,0.2)!important}',
            '.semi-typography{color:rgba(0,240,255,0.8)!important}',
            '.semi-typography-primary{color:#00f0ff!important;text-shadow:0 0 10px rgba(0,240,255,0.3)!important}',
            '.semi-typography-secondary{color:rgba(0,240,255,0.5)!important}',
            'h1,h2,h3,h4,h5,h6{color:#00f0ff!important;text-shadow:0 0 10px rgba(0,240,255,0.3)!important}',
            'main{background:transparent!important}',
            '[data-sonner-toast]{background:rgba(10,14,23,0.95)!important;border:1px solid rgba(0,240,255,0.3)!important;color:#00f0ff!important}',
            '::-webkit-scrollbar{width:4px!important;height:4px!important}',
            '::-webkit-scrollbar-track{background:transparent!important}',
            '::-webkit-scrollbar-thumb{background:rgba(0,240,255,0.3)!important;border-radius:2px!important}',
            'footer{display:none!important}',
            'button[aria-label="切换主题"]{display:none!important}'
        ],
        cartoon: [
            '@import url("https://fonts.googleapis.com/css2?family=Fredoka:wght@300;400;500;600;700&display=swap")',
            '*{font-family:"Fredoka",sans-serif!important}',
            'html,body,#root{background:linear-gradient(135deg,#fff5f5,#f0f9ff,#f5f0ff)!important;color:#5a4a4a!important}',
            '.semi-layout-header{background:rgba(255,255,255,0.9)!important;border-bottom:3px solid #ffb6c1!important;box-shadow:0 4px 15px rgba(255,182,193,0.3)!important}',
            '.semi-layout-sider{background:#fff!important;border-right:3px solid #ffe4ec!important;border-radius:0 24px 24px 0!important}',
            '.semi-navigation{background:transparent!important}',
            '.semi-navigation-item-text{color:#9a8a8a!important;font-weight:500!important}',
            '.semi-navigation-item:hover .semi-navigation-item-text{color:#ff6b9d!important}',
            '.semi-navigation-item.semi-navigation-item-selected .semi-navigation-item-text{color:#ff6b9d!important;font-weight:600!important}',
            '.semi-navigation-item{border-radius:50px!important;margin:4px 12px!important}',
            '.semi-navigation-item:hover{background:rgba(255,107,157,0.1)!important;transform:translateX(5px)!important}',
            '.semi-navigation-item.semi-navigation-item-selected{background:linear-gradient(135deg,rgba(255,107,157,0.15),rgba(255,158,196,0.1))!important}',
            '.semi-navigation-item-icon{color:#caa!important}',
            '.semi-navigation-item:hover .semi-navigation-item-icon{color:#ff6b9d!important;transform:rotate(-5deg) scale(1.1)!important}',
            '.semi-navigation-subtitle{color:#caa!important;font-weight:700!important}',
            '.semi-divider{border-color:#ffe4ec!important;margin:8px 16px!important;border-radius:4px!important}',
            '.semi-card{background:#fff!important;border:3px solid #ffe4ec!important;border-radius:24px!important;box-shadow:0 8px 25px rgba(255,182,193,0.15)!important}',
            '.semi-card:hover{border-color:#ff9ec4!important;transform:translateY(-5px) rotate(1deg)!important;box-shadow:0 15px 40px rgba(255,182,193,0.25)!important}',
            '.semi-table-thead>tr>th{background:#fff5f5!important;color:#9a8a8a!important;border-bottom:2px solid #ffe4ec!important;font-weight:600!important}',
            '.semi-table-tbody>tr>td{border-bottom:2px solid #fff5f5!important;color:#5a4a4a!important}',
            '.semi-table-tbody>tr:hover>td{background:#fff9fa!important}',
            '.semi-table-wrapper{border:3px solid #ffe4ec!important;border-radius:24px!important;background:#fff!important}',
            '.semi-tag{background:#fff5f5!important;color:#9a8a8a!important;border:2px solid #ffe4ec!important;border-radius:50px!important;font-weight:500!important}',
            '.semi-tag-green{background:#f0fff4!important;color:#16a34a!important;border-color:#bbf7d0!important;border-radius:50px!important}',
            '.semi-tag-blue{background:#f0f4ff!important;color:#5c7cfa!important;border-color:#c7d2fe!important;border-radius:50px!important}',
            '.semi-button-primary{background:linear-gradient(135deg,#ff6b9d,#ff9ec4)!important;border:none!important;border-radius:50px!important;color:#fff!important;font-weight:600!important;box-shadow:0 6px 20px rgba(255,107,157,0.4)!important}',
            '.semi-button-primary:hover{box-shadow:0 10px 30px rgba(255,107,157,0.5)!important;transform:scale(1.05) translateY(-2px)!important}',
            '.semi-button-secondary{background:#fff5f5!important;border:2px solid #ffe4ec!important;color:#9a8a8a!important;border-radius:50px!important}',
            '.semi-tabs-tab{color:#caa!important;font-weight:500!important;border-radius:50px!important}',
            '.semi-tabs-tab-active .semi-tabs-tab-text{color:#ff6b9d!important;font-weight:600!important}',
            '.semi-tabs-bar{border-color:#ffe4ec!important}',
            '.semi-input{background:#fff9fa!important;border:2px solid #ffe4ec!important;border-radius:50px!important;color:#5a4a4a!important}',
            '.semi-input:focus{border-color:#ff6b9d!important;box-shadow:0 0 20px rgba(255,107,157,0.2)!important}',
            '.semi-form-field-label{color:#9a8a8a!important;font-weight:500!important}',
            '.semi-select{background:#fff9fa!important;border:2px solid #ffe4ec!important;border-radius:50px!important;color:#5a4a4a!important}',
            '.semi-switch{background:#ffe4ec!important;border-radius:50px!important;height:24px!important;width:44px!important}',
            '.semi-switch-checked{background:linear-gradient(135deg,#ff6b9d,#ff9ec4)!important;box-shadow:0 2px 8px rgba(255,107,157,0.3)!important}',
            '.semi-progress-line-rail{background:#ffe4ec!important;border-radius:50px!important;height:8px!important}',
            '.semi-progress-line-path{stroke:#ff6b9d!important;stroke-width:8px!important}',
            '.semi-modal-content{background:#fff!important;border:3px solid #ffe4ec!important;border-radius:24px!important;box-shadow:0 12px 40px rgba(255,182,193,0.3)!important}',
            '.semi-modal-header{color:#5a4a4a!important;font-weight:600!important}',
            '.semi-modal-body{color:#7a6a6a!important}',
            '.semi-banner{background:#fff5f5!important;border:2px solid #ffe4ec!important;border-radius:20px!important;color:#9a8a8a!important}',
            '.semi-pagination-item{background:#fff!important;border:2px solid #ffe4ec!important;border-radius:50%!important;color:#5a4a4a!important}',
            '.semi-pagination-item-active{background:linear-gradient(135deg,#ff6b9d,#ff9ec4)!important;border-color:#ff6b9d!important;color:#fff!important;box-shadow:0 4px 12px rgba(255,107,157,0.3)!important}',
            'canvas[id*="visactor_window"]{background:#fff5f5!important}',
            '[class*="visactor"]{background:#fff5f5!important}',
            '[class*="chart"]{background:#fff5f5!important}',
            '[class*="apexcharts"]{background:#fff5f5!important}',
            '.apexcharts-canvas .apexcharts-grid,.apexcharts-canvas .apexcharts-gridlines line{stroke:#ffe4ec!important}',
            '.apexcharts-canvas .apexcharts-text{fill:#9a8a8a!important}',
            '.apexcharts-canvas .apexcharts-legend-text{color:#9a8a8a!important}',
            '.semi-avatar{background:#fff5f5!important;border:3px solid #ff9ec4!important;border-radius:50%!important;box-shadow:0 4px 12px rgba(255,182,193,0.3)!important}',
            '.semi-button.semi-button-borderless{color:#9a8a8a!important;border-radius:50%!important}',
            '.semi-button.semi-button-borderless:hover{background:rgba(255,107,157,0.1)!important;transform:scale(1.1)!important}',
            '.semi-typography{color:#7a6a6a!important}',
            '.semi-typography-primary{color:#5a4a4a!important}',
            '.semi-typography-secondary{color:#9a8a8a!important}',
            'h1,h2,h3,h4,h5,h6{color:#5a4a4a!important}',
            'main{background:transparent!important}',
            '[data-sonner-toast]{background:#fff!important;border:3px solid #ffe4ec!important;color:#5a4a4a!important;border-radius:16px!important}',
            '::-webkit-scrollbar{width:8px!important;height:8px!important}',
            '::-webkit-scrollbar-track{background:#fff5f5!important;border-radius:4px!important}',
            '::-webkit-scrollbar-thumb{background:linear-gradient(180deg,#ff9ec4,#ffb6c1)!important;border-radius:4px!important}',
            'footer{display:none!important}',
            'button[aria-label="切换主题"]{display:none!important}'
        ],
        pixel: [
            '@import url("https://fonts.googleapis.com/css2?family=Press+Start+2P&display=swap")',
            '*{font-family:"Press Start 2P",cursive!important;image-rendering:pixelated!important}',
            'html,body,#root{background:#4a7fd8!important;background-image:repeating-linear-gradient(0deg,transparent,transparent 19px,rgba(0,0,0,0.4) 19px,rgba(0,0,0,0.4) 20px),repeating-linear-gradient(90deg,transparent,transparent 19px,rgba(0,0,0,0.4) 19px,rgba(0,0,0,0.4) 20px),repeating-linear-gradient(0deg,transparent,transparent 19px,rgba(255,255,255,0.2) 19px,rgba(255,255,255,0.2) 21px),repeating-linear-gradient(90deg,transparent,transparent 19px,rgba(255,255,255,0.2) 19px,rgba(255,255,255,0.2) 21px),linear-gradient(180deg,#4a7fd8 0%,#3d6ab8 100%)!important;background-size:20px 20px,20px 20px,20px 20px,20px 20px,100% 100%!important;color:#fff!important}',
            'body::before{display:none!important}',
            '.semi-layout-header{background:#3d7dd8!important;border-bottom:4px solid #2a5ba8!important;box-shadow:0 4px 0 #000!important}',
            '.semi-layout-sider{background:#3d7dd8!important;border-right:4px solid #2a5ba8!important;border-radius:0!important}',
            '.semi-navigation{background:transparent!important}',
            '.semi-navigation-item-text{color:#888!important;font-size:0.55rem!important;line-height:1.8!important}',
            '.semi-navigation-item:hover .semi-navigation-item-text{color:#4a9eff!important}',
            '.semi-navigation-item.semi-navigation-item-selected .semi-navigation-item-text{color:#4a9eff!important}',
            '.semi-navigation-item{border-radius:0!important;margin:2px 4px!important;border:3px solid transparent!important}',
            '.semi-navigation-item:hover{background:rgba(74,158,255,0.08)!important;border-color:#4a9eff!important;transform:translate(-2px,-2px)!important;box-shadow:4px 4px 0 rgba(74,158,255,0.2)!important}',
            '.semi-navigation-item.semi-navigation-item-selected{background:rgba(74,158,255,0.15)!important;border-color:#4a9eff!important;box-shadow:3px 3px 0 rgba(74,158,255,0.3)!important}',
            '.semi-navigation-item-icon{color:#666!important}',
            '.semi-navigation-item:hover .semi-navigation-item-icon{color:#4a9eff!important}',
            '.semi-navigation-subtitle{color:#444!important;font-size:0.5rem!important;letter-spacing:2px!important}',
            '.semi-divider{border-color:#333!important}',
            '.semi-card{background:#16162a!important;border:4px solid #333!important;border-radius:0!important;box-shadow:6px 6px 0 #1a1a2e!important}',
            '.semi-card:hover{border-color:#4a9eff!important;transform:translate(-4px,-4px)!important;box-shadow:10px 10px 0 rgba(74,158,255,0.2)!important}',
            '.semi-table-thead>tr>th{background:#1a1a2e!important;color:#666!important;border-bottom:4px solid #333!important;font-size:0.5rem!important}',
            '.semi-table-tbody>tr>td{border-bottom:3px solid #222!important;color:#aaa!important;font-size:0.5rem!important}',
            '.semi-table-tbody>tr:hover>td{background:rgba(74,158,255,0.05)!important}',
            '.semi-table-wrapper{border:4px solid #333!important;border-radius:0!important;background:#16162a!important;box-shadow:6px 6px 0 #1a1a2e!important}',
            '.semi-tag{background:#1a1a2e!important;color:#888!important;border:3px solid #444!important;border-radius:0!important;font-size:0.45rem!important}',
            '.semi-tag-green{background:#1a1a2e!important;color:#22c55e!important;border:3px solid #22c55e!important;border-radius:0!important}',
            '.semi-tag-blue{background:#1a1a2e!important;color:#4a9eff!important;border:3px solid #4a9eff!important;border-radius:0!important}',
            '.semi-button-primary{background:#4a9eff!important;border:4px solid #fff!important;border-radius:0!important;color:#fff!important;font-size:0.55rem!important;box-shadow:4px 4px 0 #1a1a2e!important}',
            '.semi-button-primary:hover{transform:translate(3px,3px)!important;box-shadow:1px 1px 0 #1a1a2e!important;background:#3d8ef5!important}',
            '.semi-button-secondary{background:transparent!important;border:3px solid #333!important;color:#888!important;border-radius:0!important}',
            '.semi-tabs-tab{color:#666!important;border:3px solid transparent!important;border-radius:0!important;font-size:0.5rem!important}',
            '.semi-tabs-tab-active .semi-tabs-tab-text{color:#4a9eff!important}',
            '.semi-tabs-bar{border-color:#333!important}',
            '.semi-input{background:#1a1a2e!important;border:4px solid #333!important;border-radius:0!important;color:#fff!important;font-size:0.5rem!important}',
            '.semi-input:focus{border-color:#4a9eff!important;box-shadow:4px 4px 0 rgba(74,158,255,0.2)!important}',
            '.semi-form-field-label{color:#666!important;font-size:0.5rem!important}',
            '.semi-select{background:#1a1a2e!important;border:4px solid #333!important;border-radius:0!important;color:#fff!important;font-size:0.5rem!important}',
            '.semi-switch{background:#333!important;border:3px solid #555!important;border-radius:0!important}',
            '.semi-switch-checked{background:#4a9eff!important;border-color:#fff!important}',
            '.semi-progress-line-rail{background:#333!important;border-radius:0!important;height:12px!important;border:3px solid #444!important}',
            '.semi-progress-line-path{stroke:#4a9eff!important}',
            '.semi-modal-content{background:#16162a!important;border:4px solid #333!important;border-radius:0!important;box-shadow:8px 8px 0 #1a1a2e!important}',
            '.semi-modal-header{color:#fff!important;font-size:0.7rem!important}',
            '.semi-modal-body{color:#aaa!important;font-size:0.5rem!important}',
            '.semi-banner{background:#1a1a2e!important;border:4px solid #ffd93d!important;border-radius:0!important;color:#ffd93d!important}',
            '.semi-pagination-item{background:#16162a!important;border:4px solid #333!important;border-radius:0!important;color:#fff!important;font-size:0.5rem!important}',
            '.semi-pagination-item-active{background:#4a9eff!important;border-color:#fff!important;box-shadow:4px 4px 0 #1a1a2e!important}',
            'canvas[id*="visactor_window"]{background:#1a1a2e!important}',
            '[class*="visactor"]{background:#1a1a2e!important}',
            '[class*="chart"]{background:#1a1a2e!important}',
            '[class*="apexcharts"]{background:#1a1a2e!important}',
            '.apexcharts-canvas .apexcharts-grid,.apexcharts-canvas .apexcharts-gridlines line{stroke:#333!important}',
            '.apexcharts-canvas .apexcharts-text{fill:#666!important}',
            '.apexcharts-canvas .apexcharts-legend-text{color:#888!important}',
            '.semi-avatar{background:#1a1a2e!important;border:4px solid #4a9eff!important;border-radius:0!important;box-shadow:4px 4px 0 #1a1a2e!important}',
            '.semi-button.semi-button-borderless{color:#888!important;border:3px solid #333!important;border-radius:0!important;box-shadow:3px 3px 0 #1a1a2e!important}',
            '.semi-button.semi-button-borderless:hover{background:rgba(74,158,255,0.1)!important;color:#4a9eff!important;border-color:#4a9eff!important;transform:translate(2px,2px)!important;box-shadow:1px 1px 0 #1a1a2e!important}',
            '.semi-typography{color:#aaa!important;font-size:0.5rem!important;line-height:1.8!important}',
            '.semi-typography-primary{color:#fff!important}',
            '.semi-typography-secondary{color:#666!important}',
            'h1,h2,h3,h4,h5,h6{color:#fff!important}',
            'main{background:transparent!important}',
            '[data-sonner-toast]{background:#16162a!important;border:4px solid #333!important;color:#fff!important;border-radius:0!important}',
            '::-webkit-scrollbar{width:8px!important;height:8px!important}',
            '::-webkit-scrollbar-track{background:#1a1a2e!important;border:2px solid #333!important}',
            '::-webkit-scrollbar-thumb{background:#4a9eff!important;border:2px solid #fff!important;border-radius:0!important}',
            'footer{display:none!important}',
            'button[aria-label="切换主题"]{display:none!important}'
        ],
        animated: [
            'html,body,#root{background:#0d0d0d!important;color:#fff!important}',
            '.semi-layout-header{background:rgba(26,26,26,0.95)!important;border-bottom:1px solid #333!important}',
            '.semi-layout-header .flex.items-center.justify-between{justify-content:center!important;position:relative!important}',
            '.semi-layout-header .flex.items-center.justify-between>div:first-child{position:absolute!important;left:1rem!important}',
            '.semi-layout-header .flex.items-center.justify-between>div:last-child{position:absolute!important;right:1rem!important}',
            '.semi-layout-sider{background:#111!important;border-right:1px solid #222!important}',
            '.semi-navigation{background:transparent!important}',
            '.semi-navigation-item-text{color:#aaa!important}',
            '.semi-navigation-item:hover .semi-navigation-item-text{color:#6C3FF5!important}',
            '.semi-navigation-item.semi-navigation-item-selected .semi-navigation-item-text{color:#6C3FF5!important}',
            '.semi-navigation-item:hover{background:rgba(108,63,245,0.1)!important}',
            '.semi-navigation-item.semi-navigation-item-selected{background:rgba(108,63,245,0.15)!important}',
            '.semi-navigation-item-icon{color:#888!important}',
            '.semi-navigation-item:hover .semi-navigation-item-icon{color:#6C3FF5!important}',
            '.semi-navigation-subtitle{color:#555!important}',
            '.semi-divider{border-color:#222!important}',
            '.semi-card{background:#1a1a1a!important;border:1px solid #333!important;border-radius:12px!important}',
            '.semi-card:hover{border-color:rgba(108,63,245,0.5)!important;box-shadow:0 4px 20px rgba(108,63,245,0.1)!important}',
            '.semi-table-thead>tr>th{background:#222!important;color:#888!important;border-bottom:1px solid #333!important}',
            '.semi-table-tbody>tr>td{border-bottom:1px solid #222!important;color:#ccc!important}',
            '.semi-table-tbody>tr:hover>td{background:rgba(108,63,245,0.05)!important}',
            '.semi-table-wrapper{border:1px solid #333!important;border-radius:12px!important}',
            '.semi-tag{background:#222!important;color:#ccc!important;border:1px solid #444!important}',
            '.semi-tag-green{background:rgba(34,197,94,0.1)!important;color:#22c55e!important;border-color:rgba(34,197,94,0.3)!important}',
            '.semi-tag-blue{background:rgba(108,63,245,0.15)!important;color:#6C3FF5!important;border-color:rgba(108,63,245,0.3)!important}',
            '.semi-button-primary{background:linear-gradient(135deg,#6C3FF5,#9333ea)!important;border:none!important;color:#fff!important}',
            '.semi-button-primary:hover{box-shadow:0 4px 15px rgba(108,63,245,0.3)!important}',
            '.semi-button-secondary{background:#2a2a2a!important;border:1px solid #444!important;color:#ccc!important}',
            '.semi-tabs-tab{color:#888!important}',
            '.semi-tabs-tab-active .semi-tabs-tab-text{color:#6C3FF5!important}',
            '.semi-tabs-bar{border-color:#333!important}',
            '.semi-input{background:#1a1a1a!important;border-color:#333!important;color:#fff!important}',
            '.semi-input:focus{border-color:#6C3FF5!important}',
            '.semi-form-field-label{color:#888!important}',
            '.semi-select{background:#1a1a1a!important;border-color:#333!important;color:#fff!important}',
            '.semi-switch{background:#444!important}',
            '.semi-switch-checked{background:#6C3FF5!important}',
            '.semi-progress-line-rail{background:#333!important}',
            '.semi-progress-line-path{stroke:#6C3FF5!important}',
            '.semi-modal-content{background:#1a1a1a!important;border-color:#333!important}',
            '.semi-modal-header{color:#fff!important}',
            '.semi-modal-body{color:#ccc!important}',
            '.semi-banner{background:rgba(108,63,245,0.1)!important;border:1px solid rgba(108,63,245,0.3)!important;border-radius:12px!important}',
            '.semi-pagination-item{background:#1a1a1a!important;border-color:#333!important;color:#ccc!important}',
            '.semi-pagination-item-active{background:#6C3FF5!important;border-color:#6C3FF5!important;color:#fff!important}',
            'canvas[id*="visactor_window"]{background:#0d0d0d!important}',
            '[class*="visactor"]{background:#0d0d0d!important}',
            '.semi-avatar{background:#333!important;border:2px solid #6C3FF5!important}',
            '.semi-button.semi-button-borderless{color:#aaa!important}',
            '.semi-button.semi-button-borderless:hover{background:rgba(108,63,245,0.15)!important;color:#6C3FF5!important}',
            '.semi-typography{color:#ccc!important}',
            '.semi-typography-primary{color:#fff!important}',
            '.semi-typography-secondary{color:#888!important}',
            '.right-panel h1,.right-panel h2,.right-panel h3,.right-panel h4,.right-panel h5,.right-panel h6{color:#1a1a1a!important}',
            'main{background:#0d0d0d!important}',
            '[data-sonner-toast]{background:#1a1a1a!important;border:1px solid #333!important;color:#fff!important}',
            '::-webkit-scrollbar{width:6px!important;height:6px!important}',
            '::-webkit-scrollbar-track{background:transparent!important}',
            '::-webkit-scrollbar-thumb{background:#333!important;border-radius:3!important}',
            'footer{display:none!important}',
            'button[aria-label="切换主题"]{display:none!important}'
        ]
    };

    function getCookie(n) { var v = document.cookie.match('(^|;)\\s*' + n + '\\s*=\\s*([^;]+)'); return v ? v.pop() : null; }
    function getTheme() { return getCookie(COOKIE) || 'dark'; }
    function isDesign() { return !!document.querySelector('meta[name="design-page"]'); }
    function isPublic(p) { p = p.split('?')[0].split('#')[0]; if (p === '') p = '/'; return PUBLIC_ROUTES.indexOf(p) !== -1; }

    if (!isDesign() && isPublic(window.location.pathname)) { window.location.reload(); return; }

    // Inject CSS as <style> tag + sync new-api theme mode
    function injectThemeCSS() {
        var theme = getTheme();
        var rules = CSS[theme];
        if (!rules) return;
        var existing = document.getElementById('saint-theme-inline');
        if (existing) existing.remove();
        var style = document.createElement('style');
        style.id = 'saint-theme-inline';
        style.textContent = rules.join('\n');
        document.head.appendChild(style);

        // Sync new-api's native theme mode (light/dark)
        var isDark = (theme === 'dark' || theme === 'tech' || theme === 'pixel' || theme === 'animated');
        var mode = isDark ? 'dark' : 'light';
        try { localStorage.setItem('theme-mode', mode); } catch(e) {}
        if (isDark) {
            document.body.setAttribute('theme-mode', 'dark');
            document.documentElement.classList.add('dark');
        } else {
            document.body.removeAttribute('theme-mode');
            document.documentElement.classList.remove('dark');
        }

        // Center top navigation: logo left, nav center, icons right
        var headerBar = document.querySelector('header.sticky') || document.querySelector('header[class*="sticky"]');
        if (headerBar) {
            var flexContainer = headerBar.querySelector('.justify-between') || headerBar.querySelector('[class*="justify-between"]');
            if (flexContainer && flexContainer.children.length >= 3) {
                var left = flexContainer.children[0];
                var middle = flexContainer.children[1];
                var right = flexContainer.children[2];
                // Left: fixed width
                left.style.setProperty('flex-shrink', '0', 'important');
                // Middle (Navigation): take all remaining space, center content
                middle.style.setProperty('flex', '1', 'important');
                middle.style.setProperty('display', 'flex', 'important');
                middle.style.setProperty('justify-content', 'center', 'important');
                // Right: fixed width
                right.style.setProperty('flex-shrink', '0', 'important');
            }
        }
    }

    // Hide top nav "首页/关于" and inject "渠道状态" link
    function hideNavItems() {
        // 禁用 logo 链接的点击事件但保持显示
        document.querySelectorAll('a[href="/"]').forEach(function(a) {
            // 如果是 logo 链接，禁用点击事件但不隐藏
            if (a.closest('.logo') || a.closest('.brand') || a.closest('.group') || a.querySelector('img') || a.querySelector('.logo')) {
                // 如果在控制台或模型广场等 SPA 页面，完全阻止跳转
                if (!isPublic(window.location.pathname)) {
                    a.removeAttribute('href');
                    a.style.cursor = 'default';
                    a.onclick = function(e) {
                        e.preventDefault();
                        return false;
                    };
                } else {
                    a.style.cursor = 'pointer';
                }
                return;
            }
            
            // 隐藏普通的首页和关于链接
            if (a.getAttribute('href') === '/' || a.getAttribute('href') === '/about') {
                var parent = a.closest('li, .MuiListItemButton-root, .MuiBox-root');
                if (parent) parent.style.setProperty('display', 'none', 'important');
                else a.style.setProperty('display', 'none', 'important');
            }
        });

        // Inject "渠道状态" link into top nav if not already added
        if (!document.getElementById('nav-status-link')) {
            var navContainer = document.querySelector('header.sticky [class*="justify-between"]');
            if (navContainer && navContainer.children.length >= 2) {
                var middleNav = navContainer.children[1];
                var link = document.createElement('a');
                link.id = 'nav-status-link';
                link.href = '/status/';
                link.textContent = '📡 渠道状态';
                link.style.cssText = 'color:#667eea;font-weight:600;font-size:14px;text-decoration:none;padding:8px 12px;border-radius:8px;transition:background 0.2s;';
                link.onmouseenter = function() { link.style.background = 'rgba(102,126,234,0.1)'; };
                link.onmouseleave = function() { link.style.background = 'transparent'; };
                middleNav.appendChild(link);
            }
        }
    }

    // Hide page content until theme is loaded (prevent flash)
    function hideContentUntilReady() {
        if (isDesign()) return;
        // Add overlay that hides everything until theme CSS is loaded
        var overlay = document.createElement('div');
        overlay.id = 'theme-loading-overlay';
        overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:#0d0d0d;z-index:999999;transition:opacity 0.3s;';
        document.body.appendChild(overlay);
    }

    function showContent() {
        var overlay = document.getElementById('theme-loading-overlay');
        if (overlay) {
            overlay.style.opacity = '0';
            setTimeout(function() { overlay.remove(); }, 300);
        }
    }

    // If overlay was added at start but body wasn't ready yet, add it now
    if (!document.getElementById('theme-loading-overlay') && document.body && !isDesign()) {
        var overlay = document.createElement('div');
        overlay.id = 'theme-loading-overlay';
        overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:#0d0d0d;z-index:999999;transition:opacity 0.3s;';
        document.body.appendChild(overlay);
    }

    // SPA navigation
    var origPush = history.pushState, origRepl = history.replaceState;
    history.pushState = function() { origPush.apply(this, arguments); onRoute(); };
    history.replaceState = function() { origRepl.apply(this, arguments); onRoute(); };
    window.addEventListener('popstate', onRoute);

    function onRoute() {
        if (!isDesign() && isPublic(window.location.pathname)) { window.location.reload(); return; }
        // If SPA navigates to /login (e.g. after logout), force reload to get themed page
        if (!isDesign() && window.location.pathname === '/login') {
            // Show overlay immediately to prevent flash
            var overlay = document.createElement('div');
            overlay.id = 'theme-loading-overlay';
            overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:#0d0d0d;z-index:999999;';
            document.body.appendChild(overlay);
            window.location.reload();
            return;
        }
        injectThemeCSS();
        hideNavItems();
    }

    document.addEventListener('click', function(e) {
        var link = e.target.closest('a');
        if (!link) return;
        var href = link.getAttribute('href');
        if (!href || href.startsWith('#') || href.startsWith('javascript:') || link.target === '_blank') return;
        var path = href.split('?')[0].split('#')[0];
        if (isPublic(path)) { e.preventDefault(); window.location.href = href; }
    }, true);

    // Theme switcher UI
    function createSwitcher() {
        if (document.getElementById('saint-theme-switcher')) return;
        var c = document.createElement('div');
        c.id = 'saint-theme-switcher';
        c.style.cssText = 'position:fixed;bottom:24px;right:24px;z-index:999999;font-family:system-ui,sans-serif;';
        
        var currentTheme = getTheme();
        
        // 根据当前主题定制按钮样式
        var btnStyles = {
            'dark': 'background:#1a1a1a;color:#fff;border:2px solid #333;box-shadow:0 8px 24px rgba(0,0,0,0.5);',
            'light': 'background:#fff;color:#333;border:2px solid #eee;box-shadow:0 8px 24px rgba(0,0,0,0.1);',
            'tech': 'background:rgba(10,14,23,0.9);color:#00f0ff;border:2px solid #00f0ff;box-shadow:0 0 15px rgba(0,240,255,0.3);',
            'cartoon': 'background:#ff9ec4;color:#fff;border:3px solid #ffe4ec;box-shadow:0 8px 24px rgba(255,182,193,0.4);',
            'pixel': 'background:#ffd93d;color:#000;border:4px solid #000;box-shadow:4px 4px 0 #000;border-radius:0;',
            'animated': 'background:linear-gradient(135deg,#ff9a9e 0%,#fecfef 99%,#fecfef 100%);color:#fff;border:none;box-shadow:0 8px 24px rgba(255,154,158,0.4);'
        };
        
        // 主题图标映射
        var themeIcons = {
            'dark': '🌙',
            'light': '☀️',
            'tech': '⚡',
            'cartoon': '🎀',
            'pixel': '🎮',
            'animated': '✨'
        };
        
        var btn = document.createElement('button');
        var isPixel = currentTheme === 'pixel';
        btn.innerHTML = themeIcons[currentTheme] || '🎨';
        btn.style.cssText = 'width:56px;height:56px;border-radius:' + (isPixel ? '0' : '50%') + ';display:flex;align-items:center;justify-content:center;font-size:24px;cursor:pointer;transition:all 0.3s ease;position:relative;overflow:hidden;' + (btnStyles[currentTheme] || btnStyles['light']);
        
        // 按钮悬浮效果
        btn.onmouseover = function() {
            if (isPixel) {
                btn.style.transform = 'translate(-2px, -2px)';
                btn.style.boxShadow = '6px 6px 0 #000';
            } else {
                btn.style.transform = 'scale(1.1) rotate(15deg)';
            }
        };
        btn.onmouseout = function() {
            if (isPixel) {
                btn.style.transform = 'translate(0, 0)';
                btn.style.boxShadow = '4px 4px 0 #000';
            } else {
                btn.style.transform = 'scale(1) rotate(0deg)';
            }
        };
        
        // 根据当前主题定制菜单样式
        var menuStyles = {
            'dark': 'background:#1a1a1a;border:1px solid #333;box-shadow:0 8px 32px rgba(0,0,0,0.8);',
            'light': 'background:#fff;border:1px solid #eee;box-shadow:0 8px 32px rgba(0,0,0,0.1);',
            'tech': 'background:rgba(10,14,23,0.95);border:1px solid #00f0ff;box-shadow:0 0 20px rgba(0,240,255,0.2);backdrop-filter:blur(10px);',
            'cartoon': 'background:#fff;border:3px solid #ffe4ec;box-shadow:0 8px 32px rgba(255,182,193,0.2);',
            'pixel': 'background:#16162a;border:4px solid #333;box-shadow:8px 8px 0 #000;border-radius:0;',
            'animated': 'background:rgba(255,255,255,0.9);border:1px solid rgba(255,255,255,0.5);backdrop-filter:blur(10px);box-shadow:0 8px 32px rgba(31,38,135,0.1);'
        };
        
        var menu = document.createElement('div');
        menu.style.cssText = 'display:none;position:absolute;bottom:72px;right:0;border-radius:' + (isPixel ? '0' : '16px') + ';overflow:hidden;min-width:160px;z-index:1000000;' + (menuStyles[currentTheme] || menuStyles['light']);
        
        // 不同的主题选项样式
        var itemBaseStyles = {
            'dark': 'color:#aaa;border-bottom:1px solid #333;',
            'light': 'color:#666;border-bottom:1px solid #f5f5f5;',
            'tech': 'color:rgba(0,240,255,0.6);border-bottom:1px solid rgba(0,240,255,0.2);font-family:"Orbitron",sans-serif;',
            'cartoon': 'color:#9a8a8a;border-bottom:2px dashed #ffe4ec;font-family:"Fredoka",sans-serif;font-weight:500;',
            'pixel': 'color:#888;border-bottom:2px solid #333;font-family:"Press Start 2P",cursive;font-size:10px;',
            'animated': 'color:#666;border-bottom:1px solid rgba(255,255,255,0.5);backdrop-filter:blur(5px);'
        };
        
        var itemHoverStyles = {
            'dark': 'background:#222;color:#fff;',
            'light': 'background:#f9f9f9;color:#333;',
            'tech': 'background:rgba(0,240,255,0.1);color:#00f0ff;text-shadow:0 0 8px #00f0ff;',
            'cartoon': 'background:#fff5f5;color:#ff6b9d;',
            'pixel': 'background:#333;color:#fff;',
            'animated': 'background:rgba(255,255,255,0.5);color:#333;'
        };
        
        var itemActiveStyles = {
            'dark': 'background:#333;color:#fff;border-left:4px solid #fff;',
            'light': 'background:#f0f4ff;color:#5c7cfa;border-left:4px solid #5c7cfa;',
            'tech': 'background:rgba(0,240,255,0.2);color:#00f0ff;border-left:4px solid #00f0ff;text-shadow:0 0 8px #00f0ff;',
            'cartoon': 'background:#ffe4ec;color:#ff6b9d;border-left:4px solid #ff6b9d;',
            'pixel': 'background:#4a9eff;color:#fff;border-left:4px solid #fff;',
            'animated': 'background:linear-gradient(90deg,rgba(255,154,158,0.2),transparent);color:#ff9a9e;border-left:4px solid #ff9a9e;'
        };
        
        THEMES.forEach(function(t, index) {
            var item = document.createElement('div');
            var icon = themeIcons[t] || '🎨';
            item.innerHTML = '<span style="margin-right:12px;font-size:' + (isPixel ? '12px' : '16px') + ';">' + icon + '</span><span>' + NAMES[t] + '</span>';
            
            var baseStyle = itemBaseStyles[currentTheme] || itemBaseStyles['light'];
            item.style.cssText = 'padding:14px 16px;cursor:pointer;display:flex;align-items:center;position:relative;transition:all 0.2s ease;' + baseStyle;
            
            // 最后一项去掉底部边框
            if (index === THEMES.length - 1) {
                item.style.borderBottom = 'none';
            }
            
            // 当前选中主题样式
            if (t === currentTheme) {
                var activeStyle = itemActiveStyles[currentTheme] || itemActiveStyles['light'];
                item.style.cssText += activeStyle;
                item.style.paddingLeft = '12px';
            }
            
            (function(theme) {
                item.onmouseover = function() {
                    if (theme !== currentTheme) {
                        var hoverStyle = itemHoverStyles[currentTheme] || itemHoverStyles['light'];
                        var oldCss = item.style.cssText;
                        item.style.cssText += hoverStyle;
                        if (!isPixel) {
                            item.style.transform = 'translateX(4px)';
                        }
                    }
                };
                item.onmouseout = function() {
                    if (theme !== currentTheme) {
                        item.style.cssText = 'padding:14px 16px;cursor:pointer;display:flex;align-items:center;position:relative;transition:all 0.2s ease;' + baseStyle;
                        if (index === THEMES.length - 1) {
                            item.style.borderBottom = 'none';
                        }
                    }
                };
                item.onclick = function() {
                    item.style.transform = 'scale(0.95)';
                    setTimeout(function() {
                        document.cookie = COOKIE + '=' + theme + ';path=/;max-age=31536000';
                        window.location.reload();
                    }, 150);
                };
            })(t);
            menu.appendChild(item);
        });
        
        btn.onclick = function(e) {
            e.stopPropagation();
            var isVisible = menu.style.display === 'block';
            menu.style.display = isVisible ? 'none' : 'block';
            if (!isVisible && !isPixel) {
                menu.style.animation = 'slideUp 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275)';
            }
        };
        
        // 添加动画样式
        if (!document.getElementById('theme-switcher-styles')) {
            var style = document.createElement('style');
            style.id = 'theme-switcher-styles';
            style.textContent = '@keyframes slideUp{from{opacity:0;transform:translateY(15px) scale(0.95)}to{opacity:1;transform:translateY(0) scale(1)}}';
            document.head.appendChild(style);
        }
        
        c.appendChild(menu);
        c.appendChild(btn);
        document.body.appendChild(c);
        document.addEventListener('click', function(e) { if (!c.contains(e.target)) menu.style.display = 'none'; });
    }

    // Init - hide content immediately
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            if (!isDesign()) hideContentUntilReady();
            createSwitcher();
            injectThemeCSS();
            hideNavItems();
            if (!isDesign()) setTimeout(showContent, 600);
        });
    } else {
        if (!isDesign()) hideContentUntilReady();
        createSwitcher();
        injectThemeCSS();
        hideNavItems();
        if (!isDesign()) setTimeout(showContent, 600);
    }

    // Retry for SPA
    setTimeout(injectThemeCSS, 500);
    setTimeout(injectThemeCSS, 1500);
    setTimeout(injectThemeCSS, 3000);
    setTimeout(hideNavItems, 1000);
    setTimeout(hideNavItems, 2000);
    setTimeout(hideNavItems, 3000);
    setTimeout(showContent, 1500);

    // Watch for DOM changes
    var timer;
    var observer = new MutationObserver(function() {
        clearTimeout(timer);
        timer = setTimeout(function() { injectThemeCSS(); hideNavItems(); }, 200);
    });
    
    // Only observe if document.body exists
    if (document.body) {
        observer.observe(document.body, { childList: true, subtree: true });
    } else {
        // Wait for body to be available
        document.addEventListener('DOMContentLoaded', function() {
            if (document.body) {
                observer.observe(document.body, { childList: true, subtree: true });
            }
        });
    }
})();
