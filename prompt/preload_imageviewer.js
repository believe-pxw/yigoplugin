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

    // 核心状态与配置
    const CONFIG = {
        radius: 10,               // 预热前后 10 行
        maxConcurrent: 3,         // 后台并发请求数（保持3，留出带宽给当前操作）
        cache: new Set(),         // 图片 URL 缓存
        stepMapCache: {},         // 每行的 step 列表缓存（防重复请求目录）
        pool: [],                 // Image 对象强引用池，防内存回收
        queue: [],                // 后台预热的行号队列
        queueId: 0,               // 队列版本号（用于快速切换时中断旧任务）
        currentLine: null,        // 当前所在行
        isLocking: false,         // UI 锁定状态
        loadingOverlay: null      // 遮罩层 DOM
    };

    // --- 1. UI 遮罩层控制 ---
    function keyboardBlocker(e) {
        if (CONFIG.isLocking) {
            e.stopImmediatePropagation();
            e.preventDefault();
            return false;
        }
    }

    function toggleLock(show, text = '校验当前行图片资源...') {
        CONFIG.isLocking = show;

        if (show) {
            window.addEventListener('keydown', keyboardBlocker, true);
            window.addEventListener('keyup', keyboardBlocker, true);
        } else {
            window.removeEventListener('keydown', keyboardBlocker, true);
            window.removeEventListener('keyup', keyboardBlocker, true);
        }

        if (!CONFIG.loadingOverlay) {
            const overlay = document.createElement('div');
            overlay.id = 'preload-overlay';
            overlay.innerHTML = `
                <div style="background: rgba(0,0,0,0.85); color: white; padding: 25px 40px; border-radius: 12px; text-align: center; z-index: 100000; box-shadow: 0 4px 15px rgba(0,0,0,0.5);">
                    <div class="spinner" style="border: 4px solid rgba(255,255,255,0.2); border-top: 4px solid #00ffcc; border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; margin: 0 auto 15px;"></div>
                    <div id="preload-msg" style="font-size: 16px; font-weight: bold;">${text}</div>
                    <div style="font-size: 12px; color: #aaa; margin-top: 8px;">确保数据一致性，请勿操作</div>
                </div>
                <style>
                    #preload-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.1); backdrop-filter: blur(2px); display: none; justify-content: center; align-items: center; z-index: 99999; cursor: wait; }
                    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
                </style>
            `;
            document.body.appendChild(overlay);
            CONFIG.loadingOverlay = overlay;
        }

        document.getElementById('preload-msg').innerText = text;
        CONFIG.loadingOverlay.style.display = show ? 'flex' : 'none';
    }

    function getDynamicBaseUrl() {
        return window.location.href.replace(/\/$/, "") + '/1';
    }

    // --- 2. 核心数据层：动态获取 Step 列表 (完美融合你的源码逻辑) ---
    async function fetchStepsForLine(lineNo) {
        if (!lineNo || lineNo.trim() === '') return [];
        if (CONFIG.stepMapCache[lineNo]) return CONFIG.stepMapCache[lineNo];

        // 优先尝试页面原生方法
        if (typeof window.readSteps === 'function') {
            try {
                const res = window.readSteps(lineNo);
                if (res && res.length > 0) {
                    CONFIG.stepMapCache[lineNo] = res;
                    return res;
                }
            } catch (e) { }
        }

        // 降级：走原生 Fetch 请求并正则解析
        try {
            const baseUrl = getDynamicBaseUrl();
            const response = await fetch(`${baseUrl}/${lineNo}`);
            const text = await response.text();

            const regExp = /.*\.png">(.*\.png)<\/a>.*/;
            const steps = text.split('\n')
                .map(s => regExp.test(s) ? regExp.exec(s)[1] : '')
                .filter(s => s !== '')
                .map(s => s.replace(/\/$/, ''));

            CONFIG.stepMapCache[lineNo] = steps;
            return steps;
        } catch (error) {
            console.error(`获取行号 ${lineNo} 的目录失败:`, error);
            return [];
        }
    }

    // --- 3. 图片加载引擎 ---
    async function forceLoadUrls(urls) {
        if (urls.length === 0) return Promise.resolve();

        return Promise.all(urls.map(url => {
            return new Promise((resolve) => {
                const img = new Image();
                CONFIG.pool.push(img); // 强引用，防失效

                img.onload = () => { CONFIG.cache.add(url); resolve(); };
                img.onerror = () => resolve(); // 失败也放行，防永久死锁

                if ('decode' in img) {
                    img.src = url;
                    img.decode().then(resolve).catch(resolve);
                } else {
                    img.src = url;
                }
            });
        }));
    }

    // --- 4. 后台静默预热引擎 (滑动窗口) ---
    function rebuildBackgroundQueue(centerLineTitle) {
        const lineNodes = Array.from(document.querySelectorAll('.line-number'));
        const lines = lineNodes.map(node => node.getAttribute('title')).filter(t => t);
        const centerIndex = lines.indexOf(centerLineTitle);

        if (centerIndex === -1) return;

        CONFIG.queue = [];
        // 以当前行为中心，向前后辐射 50 行
        for (let offset = 1; offset <= CONFIG.radius; offset++) {
            if (centerIndex + offset < lines.length) {
                CONFIG.queue.push(lines[centerIndex + offset]);
            }
            if (centerIndex - offset >= 0) {
                CONFIG.queue.push(lines[centerIndex - offset]);
            }
        }
    }

    async function processBackgroundQueue(workerQueueId) {
        // 如果队列为空，或用户已切换到新行（版本号变了），直接终止当前后台任务
        if (workerQueueId !== CONFIG.queueId || CONFIG.queue.length === 0) return;

        const lineNo = CONFIG.queue.shift();

        try {
            // 动态获取这行的 steps
            const steps = await fetchStepsForLine(lineNo);

            // 再次校验：获取 steps 的网络请求期间，用户有没有切走？如果切走了就不加载图片了
            if (workerQueueId !== CONFIG.queueId) return;

            const baseUrl = getDynamicBaseUrl();
            const urlsToLoad = steps
                .map(step => `${baseUrl}/${lineNo}/${encodeURIComponent(step)}`)
                .filter(url => !CONFIG.cache.has(url));

            // 加载图片
            await forceLoadUrls(urlsToLoad);
        } catch (e) {
            console.error(`后台预热行 ${lineNo} 失败`, e);
        }

        // 处理完一行，继续处理下一行
        if (workerQueueId === CONFIG.queueId) {
            processBackgroundQueue(workerQueueId);
        }
    }

    // --- 5. DOM 触发器 (核心控制台) ---
    async function onLineActivated(lineTitle) {
        if (!lineTitle || lineTitle === CONFIG.currentLine) return;

        CONFIG.currentLine = lineTitle;
        CONFIG.queueId++; // 核心：增加版本号，直接截断并终止之前所有的后台预热任务

        // 1. 立即锁定 UI
        toggleLock(true, `正在校验第 ${lineTitle} 行图片...`);

        try {
            // 2. 动态获取当前行的真实 Steps（由于使用了缓存，如果后台预热过，这里是秒出的）
            const steps = await fetchStepsForLine(lineTitle);
            const baseUrl = getDynamicBaseUrl();

            const currentLineUrls = steps.map(step => `${baseUrl}/${lineTitle}/${encodeURIComponent(step)}`);
            const missingUrls = currentLineUrls.filter(url => !CONFIG.cache.has(url));

            // 3. 阻塞式加载缺少的图片
            if (missingUrls.length > 0) {
                console.log(`[阻塞加载] 行号 ${lineTitle} 缺失 ${missingUrls.length} 张图片...`);
                await forceLoadUrls(missingUrls);
            } else {
                console.log(`[秒开] 行号 ${lineTitle} 所有资源已在缓存中。`);
            }
        } catch (e) {
            console.error("处理当前行发生错误", e);
        } finally {
            // 4. 当前行所需的所有图都准备好了，解除 UI 锁定
            toggleLock(false);
        }

        // 5. 重建滑动窗口并启动后台并发 worker
        rebuildBackgroundQueue(lineTitle);
        for (let i = 0; i < CONFIG.maxConcurrent; i++) {
            processBackgroundQueue(CONFIG.queueId);
        }
    }

    // --- 6. 绑定 DOM 监听 ---
    const observer = new MutationObserver((mutations) => {
        // 锁定期间忽略 DOM 变动，防止死循环
        if (CONFIG.isLocking) return;

        for (const mutation of mutations) {
            if (mutation.attributeName === 'class') {
                const target = mutation.target;
                if (target.classList.contains('active') && target.classList.contains('line-number')) {
                    const lineNo = target.getAttribute('title');
                    // 使用微小延迟，等待框架原生事件冒泡完毕
                    setTimeout(() => onLineActivated(lineNo), 20);
                }
            }
        }
    });

    observer.observe(document.body, {
        attributes: true,
        subtree: true,
        attributeFilter: ['class']
    });

    console.log("🚀 [ERP 终极混合引擎] 启动：DOM监听 + 遮罩阻塞 + API动态解析 + 前后50行静默预热。");

})();