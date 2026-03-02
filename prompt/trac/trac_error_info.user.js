// ==UserScript==
// @name         Trac Query Error Info
// @namespace    http://tampermonkey.net/
// @version      0.1
// @description  在Trac查询页面中，概述列后增加一列“错误信息”，自动拉取对应任务单中的错误日志。
// @author       You
// @match        http://dev.bokesoft.com:8000/trac/eri-erp/query*
// @grant        none
// ==/UserScript==

(function () {
    'use strict';

    // ==========================================
    // 请在此处填写您的账号和密码
    // ==========================================
    const USERNAME = 'YOUR_USERNAME';
    const PASSWORD = 'YOUR_PASSWORD';

    // 生成 Basic Auth 头
    const authHeader = 'Basic ' + btoa(USERNAME + ':' + PASSWORD);

    // 1. 在表头“概述”列后加入“错误信息”列
    const theadRow = document.querySelector('thead.trac-query-heading tr.trac-columns');
    if (!theadRow) return;

    const summaryTh = theadRow.querySelector('th.summary');
    if (summaryTh) {
        const errorTh = document.createElement('th');
        errorTh.className = 'error-info';
        errorTh.innerHTML = '<a title="错误信息">错误信息</a>';
        summaryTh.insertAdjacentElement('afterend', errorTh);
    }

    // 2. 遍历每一行，增加列，并请求获取错误信息
    const tbodyRows = document.querySelectorAll('tbody.trac-query-results tr');
    tbodyRows.forEach(row => {
        const summaryTd = row.querySelector('td.summary');
        const idTd = row.querySelector('td.id a');

        if (summaryTd && idTd) {
            // 创建新的单元格
            const errorTd = document.createElement('td');
            errorTd.className = 'error-info';
            errorTd.textContent = '加载中...';
            // 添加一点样式以防内容过长撑爆表格
            errorTd.style.maxWidth = '300px';
            errorTd.style.whiteSpace = 'pre-wrap';
            errorTd.style.wordBreak = 'break-all';
            errorTd.style.fontSize = '12px';
            errorTd.style.color = '#d32f2f'; // 稍微标记为红色更醒目一些
            summaryTd.insertAdjacentElement('afterend', errorTd);

            const ticketUrl = idTd.href; // 例如: http://dev.bokesoft.com:8000/trac/eri-erp/ticket/164441

            // 3. 请求 ticket 页面
            // credentials: 'include' 相当于 Kotlin 代码中的 cookies(cachedCookies)，直接携带浏览器当前站点的所有 Cookie
            fetch(ticketUrl, {
                method: 'GET',
                headers: {
                    'Authorization': authHeader
                },
                credentials: 'include'
            })
                .then(res => {
                    if (!res.ok) throw new Error(`HTTP ${res.status}`);
                    return res.text();
                })
                .then(html => {
                    // 将返回体解析为 DOM
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');

                    // 查找包含错误信息的 pre.wiki
                    const preElements = doc.querySelectorAll('pre.wiki');
                    let errorText = '';
                    for (let pre of preElements) {
                        const text = pre.textContent;
                        // 通过常见错误关键字来定位错误块
                        if (text.includes('错误代码:') || text.includes('Exception:') || text.includes('at com.bokesoft.')) {
                            errorText = text.trim();
                            break;
                        }
                    }

                    // 4. 将提取到错误信息回填到单元格中
                    if (errorText) {
                        // 一般错误栈很长，只截取前几行显示以保持排版，鼠标悬停可以查看完整信息
                        const lines = errorText.split('\n');
                        const previewLines = lines.slice(0, 7).join('\n');
                        errorTd.textContent = previewLines + (lines.length > 7 ? '\n...' : '');
                        errorTd.title = errorText; // 补充原生 tooltip
                    } else {
                        // 没有抓取到异常
                        errorTd.textContent = '-';
                        errorTd.style.color = '#999';
                    }
                })
                .catch(err => {
                    errorTd.textContent = '获取失败: ' + err.message;
                });
        }
    });
})();
