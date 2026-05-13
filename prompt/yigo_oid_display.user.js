// ==UserScript==
// @name         YIGO OID Display
// @namespace    http://tampermonkey.net/
// @version      0.1
// @description  Display OID for forms and grids in YIGO ERP
// @author       You
// @match        http://localhost:8089/erp/*
// @icon         data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==
// @grant        none
// ==/UserScript==

(function () {
    'use strict';

    // Create floating button
    const btn = document.createElement('button');
    btn.innerHTML = '显示/刷新 OID';
    btn.style.position = 'fixed';
    btn.style.bottom = '20px';
    btn.style.right = '20px';
    btn.style.zIndex = '999999';
    btn.style.padding = '10px';
    btn.style.backgroundColor = '#ff4d4f';
    btn.style.color = '#fff';
    btn.style.border = 'none';
    btn.style.borderRadius = '4px';
    btn.style.cursor = 'pointer';
    btn.style.boxShadow = '0 2px 8px rgba(0,0,0,0.15)';

    // Prevent click events from bubbling up and closing YIGO's modals (jsPanel click-away logic)
    const preventPropagation = (e) => {
        e.stopPropagation();
    };
    btn.addEventListener('mousedown', preventPropagation);
    btn.addEventListener('mouseup', preventPropagation);
    btn.addEventListener('pointerdown', preventPropagation);

    document.body.appendChild(btn);

    const getColIndex = (dataTable, key) => {
        return dataTable.indexByKey(key)
    };

    btn.addEventListener('click', function (e) {
        e.stopPropagation();
        e.preventDefault();

        // Remove existing oid markers
        document.querySelectorAll('.yigo-oid-marker').forEach(el => el.remove());

        if (!window.YIUI || !window.YIUI.FormStack || !window.YIUI.FormStack.activeForm) {
            alert('未能获取到 YIUI.FormStack.activeForm');
            return;
        }

        const activeForm = window.YIUI.FormStack.activeForm;

        // 1. Show Form OID
        const formOid = activeForm.document && activeForm.document.oid !== undefined ? activeForm.document.oid : '';
        const formKey = activeForm.formKey;
        if (formKey) {
            const formDom = document.querySelector(`[formkey="${formKey}"]`);
            if (formDom) {
                const toolbar = formDom.querySelector('.ui-tbr');
                const attachTarget = toolbar || formDom;
                const attachTargetStyle = window.getComputedStyle(attachTarget);
                
                // 不要强行覆盖 fixed 或 absolute 的模态窗定位，否则模态窗会跑到视口外面去导致“消失”的错觉！
                if (attachTargetStyle.position === 'static') {
                    attachTarget.style.position = 'relative';
                }
                
                // 如果是挂在非 form 身上，才谨慎覆盖 overflow，不要破坏弹窗结构
                if (attachTarget !== formDom && attachTargetStyle.overflow === 'hidden') {
                    attachTarget.style.setProperty('overflow', 'visible', 'important');
                }

                const oidMarker = document.createElement('div');
                oidMarker.className = 'yigo-oid-marker';
                oidMarker.style.position = 'absolute';
                
                // 将位置绑定在工具栏最右侧稍微靠里的地方
                oidMarker.style.top = '10px';
                oidMarker.style.right = '40px'; 
                oidMarker.style.backgroundColor = 'rgba(24, 144, 255, 0.9)'; 
                oidMarker.style.color = '#fff';
                oidMarker.style.padding = '2px 4px';
                oidMarker.style.fontSize = '12px';
                oidMarker.style.zIndex = '999999';
                oidMarker.style.borderRadius = '2px';
                oidMarker.style.whiteSpace = 'nowrap';
                oidMarker.style.boxShadow = '1px 1px 3px rgba(0,0,0,0.3)';
                
                // 仅针对 ID 文字开启一键全选，防止复制到前缀
                // 有些弹窗是没有单独 OID 的 (比如明细选取窗)，此时展示个提示
                const displayOid = formOid || '无(列表窗)';
                oidMarker.innerHTML = `Form OID: <span style="user-select: all; font-weight: bold; margin-left: 4px; cursor: text;">${displayOid}</span>`;
                attachTarget.appendChild(oidMarker);
            }
        }

        // 2. Show Grid OIDs
        if (activeForm.formAdapt && activeForm.formAdapt.gridArray) {
            const gridArray = activeForm.formAdapt.gridArray;
            gridArray.forEach(grid => {
                const gridKey = grid.key;

                let gridDom = null;
                if (grid.el instanceof HTMLElement) {
                    gridDom = grid.el;
                } else if (grid.el && grid.el[0] instanceof HTMLElement) {
                    gridDom = grid.el[0];
                } else if (typeof grid.getEl === 'function' && grid.getEl() && grid.getEl()[0]) {
                    gridDom = grid.getEl()[0];
                }

                if (!gridDom) {
                    gridDom = document.querySelector(`[meta-key="${gridKey}"]`);
                }

                if (!gridDom) {
                    console.warn(`YIGO OID Display: Unable to find DOM for grid ${gridKey}`);
                    return;
                }
                const tableKey = grid.tableKey;
                let dataTable = null;
                if (activeForm.document.tbls && Array.isArray(activeForm.document.tbls)) {
                    dataTable = activeForm.document.tbls.find(t => t.key === tableKey);
                } else if (activeForm.document.tbls) {
                    dataTable = activeForm.document.tbls[tableKey] || Object.values(activeForm.document.tbls).find(t => t.key === tableKey);
                }
                if (!dataTable) return;

                // Find OID and POID column indices
                const oidColIndex = getColIndex(dataTable, 'OID');
                const poidColIndex = getColIndex(dataTable, 'POID');

                const allData = grid.dataModel ? grid.dataModel.allData : [];
                const trs = gridDom.querySelectorAll('table.ui-ygrid-btable tbody tr.ygrow');

                for (let i = 0; i < allData.length; i++) {
                    const rowData = allData[i];
                    if (!rowData || !rowData.bkmkRow) continue;

                    const bookmark = rowData.bkmkRow.bookmark;

                    let tr = gridDom.querySelector(`tr.ygrow[id="ygd${i}"]`);
                    if (!tr && i < trs.length) {
                        tr = trs[i];
                    }

                    if (tr) {
                        let rowOid = '';
                        let rowPoid = '';

                        let tblRow = null;
                        if (Array.isArray(dataTable.allRows)) {
                            tblRow = dataTable.allRows.find(r => r.bookmark === bookmark) ||
                                dataTable.allRows.find(r => typeof r.getRowID === 'function' && r.getRowID() === bookmark) ||
                                dataTable.allRows[bookmark];
                        }

                        if (tblRow && tblRow.vals) {
                            rowOid = oidColIndex !== -1 ? tblRow.vals[oidColIndex] : '';
                            rowPoid = poidColIndex !== -1 ? tblRow.vals[poidColIndex] : '';
                        }

                        if (rowOid || rowPoid) {
                            const firstTd = tr.querySelector('td.always-show') || tr.querySelector('td:not([style*="display: none"])') || tr.querySelector('td');
                            if (firstTd) {
                                firstTd.style.position = 'relative';
                                // 必定要覆盖掉由 ui-ygrid 定义在这个单元格上的 overflow:hidden，否则绝对定位出去的部分会被裁切消失！
                                firstTd.style.setProperty('overflow', 'visible', 'important');
                                tr.style.setProperty('overflow', 'visible', 'important');

                                const oidMarker = document.createElement('div');
                                oidMarker.className = 'yigo-oid-marker';
                                oidMarker.style.position = 'absolute';
                                // 将标签稍微往右偏移一点放置在格子上，防止 left: 100% 超出过远被祖先的容器裁切
                                oidMarker.style.top = '10px';
                                oidMarker.style.left = '35px';
                                oidMarker.style.backgroundColor = 'rgba(24, 144, 255, 0.9)';
                                oidMarker.style.color = '#fff';
                                oidMarker.style.padding = '2px 4px';
                                oidMarker.style.fontSize = '12px';
                                oidMarker.style.zIndex = '999999';
                                oidMarker.style.borderRadius = '2px';
                                oidMarker.style.whiteSpace = 'nowrap';
                                oidMarker.style.boxShadow = '1px 1px 3px rgba(0,0,0,0.3)';

                                let text = [];
                                if (rowOid) text.push(`OID: <span style="user-select: all; font-weight: bold; cursor: text;">${rowOid}</span>`);
                                if (rowPoid) text.push(`POID: <span style="user-select: all; font-weight: bold; cursor: text;">${rowPoid}</span>`);
                                oidMarker.innerHTML = text.join(' | ');

                                // We hover it inside the cell to prevent overlap
                                firstTd.appendChild(oidMarker);
                            }
                        }
                    }
                }
            });
        }
    });

})();
