// ==UserScript==
// @name         变更截图预热
// @namespace    http://tampermonkey.net/
// @version      0.1
// @description  try to take over the world!
// @author       You
// @match        http://k8stest.erp.bokesoft.com/public/erp_regression-testing/testResult/*
// @match        http://dev.bokesoft.com:8000/files/*
// @icon         https://www.google.com/s2/favicons?sz=64&domain=bokesoft.com
// @grant        none
// ==/UserScript==
(function () {
    'use strict';

    const PRELOAD_CONFIG = {
        cache: new Set(),
        loadingOverlay: null,
        isLocking: false // 新增：记录当前是否处于锁定状态
    };

    /**
     * 键盘事件处理器：在锁定状态下拦截所有按键
     */
    function keyboardBlocker(e) {
        if (PRELOAD_CONFIG.isLocking) {
            e.stopImmediatePropagation(); // 阻止事件传递给其他监听器
            e.preventDefault();           // 阻止默认行为（如滚动）
            return false;
        }
    }

    /**
     * 控制界面锁定和遮罩
     */
    function toggleLock(show, text = '资源预热中，请稍候...') {
        PRELOAD_CONFIG.isLocking = show;

        // 1. 键盘锁定逻辑：在捕获阶段拦截事件
        if (show) {
            // 使用 capture: true 确保我们的拦截运行在网站原有逻辑之前
            window.addEventListener('keydown', keyboardBlocker, true);
            window.addEventListener('keyup', keyboardBlocker, true);
            window.addEventListener('keypress', keyboardBlocker, true);
        } else {
            window.removeEventListener('keydown', keyboardBlocker, true);
            window.removeEventListener('keyup', keyboardBlocker, true);
            window.removeEventListener('keypress', keyboardBlocker, true);
        }

        // 2. UI 遮罩逻辑
        if (!PRELOAD_CONFIG.loadingOverlay) {
            const overlay = document.createElement('div');
            overlay.id = 'preload-overlay';
            overlay.innerHTML = `
                <div style="background: rgba(0,0,0,0.8); color: white; padding: 25px 40px; border-radius: 12px; text-align: center; box-shadow: 0 4px 15px rgba(0,0,0,0.5);">
                    <div class="spinner" style="border: 4px solid rgba(255,255,255,0.3); border-top: 4px solid #3498db; border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; margin: 0 auto 15px;"></div>
                    <div id="preload-msg" style="font-size: 16px; font-family: sans-serif;">${text}</div>
                    <div style="font-size: 12px; color: #aaa; margin-top: 8px;">键盘与鼠标操作已锁定</div>
                </div>
                <style>
                    #preload-overlay {
                        position: fixed; top: 0; left: 0; width: 100%; height: 100%;
                        background: rgba(0,0,0,0.1); backdrop-filter: blur(3px);
                        display: flex; justify-content: center; align-items: center;
                        z-index: 100000; cursor: wait;
                    }
                    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
                </style>
            `;
            document.body.appendChild(overlay);
            PRELOAD_CONFIG.loadingOverlay = overlay;
        }

        document.getElementById('preload-msg').innerText = text;
        PRELOAD_CONFIG.loadingOverlay.style.display = show ? 'flex' : 'none';
    }

    function getDynamicBaseUrl() {
        const currentUrl = window.location.href.replace(/\/$/, "");
        return currentUrl + '/1';
    }

    async function preloadStepImages(lineNumber) {
        const baseUrl = getDynamicBaseUrl();
        const stepNodes = Array.from(document.querySelectorAll('.step-number'));
        const stepTitles = stepNodes.map(node => node.getAttribute('title')).filter(t => t);

        const newUrls = stepTitles
            .map(title => `${baseUrl}/${lineNumber}/${encodeURIComponent(title)}`)
            .filter(url => !PRELOAD_CONFIG.cache.has(url));

        if (newUrls.length === 0) return;

        // 激活锁定
        toggleLock(true, `正在为您加载 ${newUrls.length} 个步骤图...`);

        const promises = newUrls.map(url => {
            return new Promise((resolve) => {
                const img = new Image();
                img.onload = () => {
                    PRELOAD_CONFIG.cache.add(url);
                    resolve();
                };
                img.onerror = () => resolve();
                img.src = url;
            });
        });

        try {
            await Promise.all(promises);
        } finally {
            // 解除锁定
            toggleLock(false);
        }
    }

    const observer = new MutationObserver((mutations) => {
        // 如果当前已经由于预热处于锁定状态，不触发新的预热逻辑，防止循环
        if (PRELOAD_CONFIG.isLocking) return;

        for (const mutation of mutations) {
            if (mutation.attributeName === 'class') {
                const target = mutation.target;
                if (target.classList.contains('active') && target.classList.contains('line-number')) {
                    const lineNo = target.getAttribute('title');
                    if (lineNo) {
                        preloadStepImages(lineNo);
                    }
                }
            }
        }
    });

    observer.observe(document.body, {
        attributes: true,
        subtree: true,
        attributeFilter: ['class']
    });
})();