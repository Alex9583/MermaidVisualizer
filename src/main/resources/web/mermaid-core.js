(function () {
    'use strict';

    const CLASS_ERROR = 'mermaid-error';

    // --- Encoding ---

    function utf8ToBase64(str) {
        const bytes = new TextEncoder().encode(str);
        let binary = '';
        for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
        return btoa(binary);
    }

    function base64ToUtf8(b64) {
        const binary = atob(b64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        return new TextDecoder('utf-8').decode(bytes);
    }

    // --- SVG icon creation ---

    const SVG_NS = 'http://www.w3.org/2000/svg';
    const ICON_COPY = ['M5.5 2H12.5V12.5H5.5Z', 'M3.5 4.5V14H10.5'];
    const ICON_IMAGE = ['M2 3H14V13H2Z', 'M2 11L5.5 7.5L8 10L10.5 7.5L14 11'];
    const ICON_SAVE = ['M8 2V10', 'M4.5 7L8 10.5L11.5 7', 'M3 14H13'];

    function createSvgIcon(paths) {
        const svg = document.createElementNS(SVG_NS, 'svg');
        svg.setAttribute('width', '14');
        svg.setAttribute('height', '14');
        svg.setAttribute('viewBox', '0 0 16 16');
        svg.setAttribute('fill', 'none');
        svg.setAttribute('stroke', 'currentColor');
        svg.setAttribute('stroke-width', '1.5');
        svg.setAttribute('stroke-linecap', 'round');
        svg.setAttribute('stroke-linejoin', 'round');
        for (let i = 0; i < paths.length; i++) {
            const path = document.createElementNS(SVG_NS, 'path');
            path.setAttribute('d', paths[i]);
            svg.appendChild(path);
        }
        return svg;
    }

    // --- Shadow DOM ---

    function setShadowContent(el, shadowCss, contentEl, isDark) {
        const shadow = el.shadowRoot || el.attachShadow({ mode: 'open' });
        el.classList.toggle('dark', isDark);
        shadow.textContent = '';
        if (shadowCss) {
            const style = document.createElement('style');
            style.textContent = shadowCss;
            shadow.appendChild(style);
        }
        shadow.appendChild(contentEl);
    }

    /**
     * Injects SVG markup from mermaid.render() into a Shadow DOM container.
     * The svgString is sanitized by Mermaid's built-in DOMPurify (securityLevel: 'strict')
     * and further isolated via Shadow DOM, preventing style/script leakage.
     * innerHTML is necessary here because Mermaid outputs complex SVG that cannot be
     * constructed via createElement.
     */
    function injectSvg(container, shadowCss, svgString, isDark) {
        const svgContainer = document.createElement('div');
        svgContainer.innerHTML = svgString; // eslint-disable-line no-unsanitized/property — sanitized by Mermaid DOMPurify + Shadow DOM isolation
        setShadowContent(container, shadowCss, svgContainer, isDark);
    }

    // --- Error display ---

    function showError(container, shadowCss, message, renderId, isDark) {
        const errorPre = document.createElement('pre');
        errorPre.className = CLASS_ERROR;
        errorPre.textContent = message;
        setShadowContent(container, shadowCss, errorPre, isDark);
        if (renderId) {
            const leftover = document.getElementById(renderId);
            if (leftover) leftover.remove();
        }
    }

    // --- Mermaid initialization ---

    let currentTheme = 'default';
    let renderCounter = 0;

    function initMermaid(theme) {
        const cfg = window.__MERMAID_CONFIG || {};
        currentTheme = cfg.theme || theme;
        const opts = {
            startOnLoad: false,
            maxTextSize: cfg.maxTextSize ?? 100000,
            theme: currentTheme,
            look: cfg.look || 'classic',
            securityLevel: 'strict'
        };
        if (cfg.fontFamily) opts.fontFamily = cfg.fontFamily;
        mermaid.initialize(opts);
    }

    // --- SVG/PNG extraction ---

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

    function createPngCanvas(width, height, scale, isDark) {
        const canvas = document.createElement('canvas');
        canvas.width = Math.ceil(width * scale);
        canvas.height = Math.ceil(height * scale);
        const ctx = canvas.getContext('2d');
        if (!ctx) {
            console.error('[MermaidVisualizer] Failed to get 2D canvas context');
            return null;
        }
        ctx.fillStyle = isDark ? '#2b2b2b' : '#ffffff';
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

    // scale: multiplier for HiDPI output (2 = 2x resolution). Fallback 800x600 when SVG has no measured size.
    // isDarkFn: function returning boolean — abstracts theme detection per context.
    function extractPng(container, scale, isDarkFn, callback) {
        const svgString = extractSvg(container);
        if (!svgString) { callback(''); return; }

        const renderedSvg = container.shadowRoot.querySelector('svg');
        if (!renderedSvg) { callback(''); return; }

        const dim = getSvgDimensions(renderedSvg);
        const canvasInfo = createPngCanvas(dim.w, dim.h, scale, isDarkFn());
        if (!canvasInfo) { callback(''); return; }

        const svgDataUrl = 'data:image/svg+xml;base64,' + utf8ToBase64(svgString);
        renderSvgToImage(svgDataUrl, canvasInfo, callback);
    }

    // --- Export toolbar (callbacks-based abstraction) ---
    // callbacks: { extractSvg, extractPng, copySvg?, copyPng?, save? }
    // Buttons are created only for non-null callbacks.

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

    // --- Error info parsing ---

    function parseErrorInfo(err) {
        const message = err.message || String(err);
        const info = { status: 'error', message: message, line: null, column: null };
        const lineMatch = message.match(/on line (\d+)/i) || message.match(/line (\d+)/i);
        if (lineMatch) {
            info.line = parseInt(lineMatch[1], 10);
        }
        const colMatch = message.match(/column (\d+)/i);
        if (colMatch) {
            info.column = parseInt(colMatch[1], 10);
        }
        return info;
    }

    // --- Public API ---

    window.__mermaidCore = {
        base64ToUtf8: base64ToUtf8,
        createSvgIcon: createSvgIcon,
        injectSvg: injectSvg,
        showError: showError,
        initMermaid: initMermaid,
        extractSvg: extractSvg,
        extractPng: extractPng,
        createExportToolbar: createExportToolbar,
        parseErrorInfo: parseErrorInfo,
        getCurrentTheme: function () { return currentTheme; },
        nextRenderId: function () { return 'mermaid-render-' + (renderCounter++); },
        SVG_NS: SVG_NS,
        ICON_COPY: ICON_COPY,
        ICON_IMAGE: ICON_IMAGE,
        ICON_SAVE: ICON_SAVE
    };
})();
