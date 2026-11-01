/** Module `export`: SVG/PNG extraction and the hover export toolbar, built from the context's callbacks (one button per callback given). */
'use strict';

window.__mermaidCore.define('export', ['text', 'shadow'], function ({ utf8ToBase64 }, { createSvgIcon }) {

    const ICON_COPY = ['M5.5 2H12.5V12.5H5.5Z', 'M3.5 4.5V14H10.5'];
    const ICON_IMAGE = ['M2 3H14V13H2Z', 'M2 11L5.5 7.5L8 10L10.5 7.5L14 11'];
    const ICON_SAVE = ['M8 2V10', 'M4.5 7L8 10.5L11.5 7', 'M3 14H13'];
    const ICON_OPEN = ['M13.5 9.5V13.5H2.5V2.5H6.5', 'M9.5 2.5H13.5V6.5', 'M7 9L13.5 2.5'];

    function extractSvg(container) {
        const shadow = container ? container.shadowRoot : null;
        const svg = shadow ? shadow.querySelector('svg') : null;
        if (!svg) return null;
        const clone = svg.cloneNode(true);
        if (!clone.getAttribute('xmlns')) {
            clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
        }
        return new XMLSerializer().serializeToString(clone);
    }

    function getSvgDimensions(svgElement) {
        const vb = svgElement.viewBox ? svgElement.viewBox.baseVal : null;
        return {
            w: (vb && vb.width > 0) ? vb.width : (parseFloat(svgElement.getAttribute('width')) || 800),
            h: (vb && vb.height > 0) ? vb.height : (parseFloat(svgElement.getAttribute('height')) || 600)
        };
    }

    function createPngCanvas(width, height, scale, bgColor) {
        const canvas = document.createElement('canvas');
        canvas.width = Math.ceil(width * scale);
        canvas.height = Math.ceil(height * scale);
        const ctx = canvas.getContext('2d');
        if (!ctx) {
            console.error('[MermaidVisualizer] Failed to get 2D canvas context');
            return null;
        }
        ctx.fillStyle = bgColor;
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        return { canvas: canvas, ctx: ctx };
    }

    function renderSvgToImage(svgDataUrl, canvasInfo, callback) {
        const img = new Image();
        img.onload = function () {
            try {
                canvasInfo.ctx.drawImage(img, 0, 0, canvasInfo.canvas.width, canvasInfo.canvas.height);
                const dataUrl = canvasInfo.canvas.toDataURL('image/png');
                const b64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
                callback(b64);
            } catch (e) {
                console.error('[MermaidVisualizer] PNG extraction failed:', e);
                callback('');
            }
        };
        img.onerror = function () {
            console.error('[MermaidVisualizer] PNG extraction failed: SVG image load error');
            callback('');
        };
        img.src = svgDataUrl;
    }

    // scale = HiDPI multiplier; 800x600 when the SVG has no measured size; cfg.backgroundColor beats the theme default.
    function extractPng(container, scale, isDarkFn, callback) {
        const svgString = extractSvg(container);
        if (!svgString) { callback(''); return; }

        const renderedSvg = container.shadowRoot.querySelector('svg');
        if (!renderedSvg) { callback(''); return; }

        const cfg = window.__MERMAID_CONFIG || {};
        const bgColor = cfg.backgroundColor || (isDarkFn() ? '#2b2b2b' : '#ffffff');
        const dim = getSvgDimensions(renderedSvg);
        const canvasInfo = createPngCanvas(dim.w, dim.h, scale, bgColor);
        if (!canvasInfo) { callback(''); return; }

        const svgDataUrl = 'data:image/svg+xml;base64,' + utf8ToBase64(svgString);
        renderSvgToImage(svgDataUrl, canvasInfo, callback);
    }

    function createToolbarButton(iconPaths, title, onClick) {
        const btn = document.createElement('button');
        btn.className = 'mermaid-export-btn';
        btn.setAttribute('title', title);
        btn.appendChild(createSvgIcon(iconPaths));
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            e.preventDefault();
            onClick();
        });
        return btn;
    }

    function handleCopySvg(callbacks) {
        try {
            const svgString = callbacks.extractSvg();
            if (!svgString) {
                console.warn('[MermaidVisualizer] Copy SVG: no SVG found in container');
                callbacks.copySvg('');
                return;
            }
            callbacks.copySvg(utf8ToBase64(svgString));
        } catch (e) {
            console.error('[MermaidVisualizer] Copy SVG failed:', e);
            callbacks.copySvg('');
        }
    }

    function handleCopyPng(callbacks) {
        try {
            callbacks.extractPng(2, function (b64) {
                try {
                    callbacks.copyPng(b64 || '');
                } catch (e) {
                    console.error('[MermaidVisualizer] Copy PNG post failed:', e);
                }
            });
        } catch (e) {
            console.error('[MermaidVisualizer] Copy PNG failed:', e);
            callbacks.copyPng('');
        }
    }

    function handleSave(callbacks) {
        try {
            const svgString = callbacks.extractSvg();
            if (!svgString) {
                console.warn('[MermaidVisualizer] Save: no SVG found in container');
                return;
            }
            callbacks.extractPng(2, function (pngB64) {
                try {
                    const payload = JSON.stringify({
                        svg: utf8ToBase64(svgString),
                        png: pngB64 || ''
                    });
                    callbacks.save(payload);
                } catch (e) {
                    console.error('[MermaidVisualizer] Save encoding failed:', e);
                }
            });
        } catch (e) {
            console.error('[MermaidVisualizer] Save failed:', e);
            callbacks.save(JSON.stringify({svg: '', png: ''}));
        }
    }

    function createExportToolbar(callbacks) {
        const toolbar = document.createElement('div');
        toolbar.className = 'mermaid-export-toolbar';

        if (callbacks.openInTab) {
            toolbar.appendChild(createToolbarButton(ICON_OPEN, 'Open in new tab', function () { callbacks.openInTab(); }));
        }
        if (callbacks.copySvg) {
            toolbar.appendChild(createToolbarButton(ICON_COPY, 'Copy SVG', function () { handleCopySvg(callbacks); }));
        }
        if (callbacks.copyPng) {
            toolbar.appendChild(createToolbarButton(ICON_IMAGE, 'Copy PNG', function () { handleCopyPng(callbacks); }));
        }
        if (callbacks.save) {
            toolbar.appendChild(createToolbarButton(ICON_SAVE, 'Save\u2026', function () { handleSave(callbacks); }));
        }

        return toolbar;
    }

    return {
        ICON_COPY: ICON_COPY,
        ICON_IMAGE: ICON_IMAGE,
        ICON_SAVE: ICON_SAVE,
        ICON_OPEN: ICON_OPEN,
        extractSvg: extractSvg,
        extractPng: extractPng,
        createExportToolbar: createExportToolbar
    };
});
