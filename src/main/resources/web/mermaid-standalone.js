// NOTE: Shadow DOM, error handling, init logic, export toolbar (icons, extraction, toolbar creation)
// are intentionally duplicated from mermaid-render.js. The standalone editor loads resources via
// loadHTML() with inlined scripts, while the Markdown extension serves resources via
// PreviewStaticServer. Shadow styles are shared via mermaid-shadow.css.
// If modifying shared logic, update both files.
(function () {
    'use strict';

    const CLASS_ERROR = 'mermaid-error';

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

    // Shadow CSS is loaded from a <style id="mermaid-shadow-styles"> element
    // inlined in the HTML by MermaidPreviewPanel (Kotlin).
    const shadowCssEl = document.getElementById('mermaid-shadow-styles');
    const shadowCss = shadowCssEl ? shadowCssEl.textContent : '';
    if (!shadowCss) {
        console.warn('[MermaidVisualizer] Shadow CSS not loaded — toolbar and zoom controls may not display correctly');
    }

    // --- Export: icon constants and toolbar ---

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

    function createExportToolbar() {
        if (typeof window.__copySvgBridge !== 'function' &&
            typeof window.__copyPngBridge !== 'function' &&
            typeof window.__saveBridge !== 'function') {
            return null;
        }

        const toolbar = document.createElement('div');
        toolbar.className = 'mermaid-export-toolbar';

        function createButton(iconPaths, title, onClick) {
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

        if (typeof window.__copySvgBridge === 'function') {
            toolbar.appendChild(createButton(ICON_COPY, 'Copy SVG', function () {
                try {
                    const svgString = window.__extractSvg();
                    if (!svgString) {
                        console.warn('[MermaidVisualizer] Copy SVG: no SVG found in container');
                        window.__copySvgBridge('');
                        return;
                    }
                    window.__copySvgBridge(utf8ToBase64(svgString));
                } catch (e) {
                    console.error('[MermaidVisualizer] Copy SVG failed:', e);
                    window.__copySvgBridge('');
                }
            }));
        }

        if (typeof window.__copyPngBridge === 'function') {
            toolbar.appendChild(createButton(ICON_IMAGE, 'Copy PNG', function () {
                try {
                    window.__extractPng(2, function (b64) {
                        try {
                            window.__copyPngBridge(b64 || '');
                        } catch (e) {
                            console.error('[MermaidVisualizer] Copy PNG post failed:', e);
                        }
                    });
                } catch (e) {
                    console.error('[MermaidVisualizer] Copy PNG failed:', e);
                    window.__copyPngBridge('');
                }
            }));
        }

        if (typeof window.__saveBridge === 'function') {
            toolbar.appendChild(createButton(ICON_SAVE, 'Save\u2026', function () {
                try {
                    const svgString = window.__extractSvg();
                    if (!svgString) {
                        console.warn('[MermaidVisualizer] Save: no SVG found in container');
                        return;
                    }
                    window.__extractPng(2, function (pngB64) {
                        try {
                            const payload = JSON.stringify({
                                svg: utf8ToBase64(svgString),
                                png: pngB64 || ''
                            });
                            window.__saveBridge(payload);
                        } catch (e) {
                            console.error('[MermaidVisualizer] Save encoding failed:', e);
                        }
                    });
                } catch (e) {
                    console.error('[MermaidVisualizer] Save failed:', e);
                    window.__saveBridge(JSON.stringify({svg: '', png: ''}));
                }
            }));
        }

        return toolbar;
    }

    function setShadowContent(el, contentEl, isDark) {
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

    function injectSvg(container, svgString, isDark) {
        const svgContainer = document.createElement('div');
        svgContainer.innerHTML = svgString; // eslint-disable-line no-unsanitized/property — sanitized by Mermaid DOMPurify + Shadow DOM isolation
        setShadowContent(container, svgContainer, isDark);
    }

    function showError(container, message, renderId, isDark) {
        const errorPre = document.createElement('pre');
        errorPre.className = CLASS_ERROR;
        errorPre.textContent = message;
        setShadowContent(container, errorPre, isDark);
        if (renderId) {
            const leftover = document.getElementById(renderId);
            if (leftover) leftover.remove();
        }
    }

    let currentTheme = 'default';
    let renderCounter = 0;

    function initMermaid(theme) {
        currentTheme = theme;
        try {
            mermaid.initialize({
                startOnLoad: false,
                maxTextSize: 100000,
                theme: currentTheme,
                securityLevel: 'strict'
            });
        } catch (e) {
            console.error('[MermaidVisualizer] mermaid.initialize failed:', e);
            const container = document.getElementById('mermaid-container');
            if (container) showError(container, 'Failed to initialize Mermaid: ' + e.message, null, theme === 'dark');
        }
    }

    window.renderDiagram = async function (base64Source, forceThemeRefresh) {
        const container = document.getElementById('mermaid-container');
        if (!container) {
            console.error('[MermaidVisualizer] mermaid-container element not found in DOM');
            return;
        }

        const isDark = document.body.classList.contains('dark-theme');
        const theme = isDark ? 'dark' : 'default';

        if (forceThemeRefresh || theme !== currentTheme) {
            initMermaid(theme);
        }

        let source;
        try {
            source = base64ToUtf8(base64Source);
        } catch (e) {
            showError(container, 'Failed to decode diagram source', null, isDark);
            return;
        }

        source = source.trim();
        if (!source) {
            if (container.shadowRoot) container.shadowRoot.textContent = '';
            return;
        }

        const renderId = 'mermaid-render-' + (renderCounter++);
        try {
            const result = await mermaid.render(renderId, source);
            if (result && typeof result.svg === 'string') {
                injectSvg(container, result.svg, isDark);
                const toolbar = createExportToolbar();
                if (toolbar) container.shadowRoot.appendChild(toolbar);
                if (window.__initMermaidZoom) {
                    window.__initMermaidZoom(container.shadowRoot, {
                        fitMode: 'width',
                        toolbarEl: container.shadowRoot.querySelector('.mermaid-export-toolbar'),
                        wheelRequiresModifier: true,
                        enableKeyboard: true,
                        scrollSyncCallback: function (fraction) {
                            if (scrollGuardActive) return;
                            if (typeof window.__mermaidScrollBridge === 'function') {
                                try {
                                    window.__mermaidScrollBridge(String(fraction));
                                } catch (e) {
                                    console.error('[MermaidVisualizer] scroll bridge call failed:', e);
                                }
                            }
                        }
                    });
                }
            } else {
                showError(container, 'Unexpected render result', renderId, isDark);
            }
        } catch (err) {
            console.error('[MermaidVisualizer] Render failed', err);
            showError(container, err.message || String(err), renderId, isDark);
        }
    };

    // Expose showError for Kotlin-side fallback error display
    window.__showError = showError;

    initMermaid('default');

    // --- Scroll synchronization (routed through zoom module pan at scale ~1.0) ---
    const SCROLL_GUARD_RESET_MS = 100;
    let scrollGuardActive = false;

    // Called from Kotlin to scroll preview to a proportional position
    window.__scrollPreviewTo = function (fraction) {
        if (typeof fraction !== 'number' || !isFinite(fraction)) return;
        const container = document.getElementById('mermaid-container');
        if (!container || !container.shadowRoot || !window.__scrollZoomTo) return;
        scrollGuardActive = true;
        window.__scrollZoomTo(container.shadowRoot, fraction);
        requestAnimationFrame(function () {
            setTimeout(function () { scrollGuardActive = false; }, SCROLL_GUARD_RESET_MS);
        });
    };

    // --- Export functions ---

    window.__extractSvg = function() {
        const container = document.getElementById('mermaid-container');
        const shadow = container ? container.shadowRoot : null;
        const svg = shadow ? shadow.querySelector('svg') : null;
        if (!svg) return null;
        const clone = svg.cloneNode(true);
        if (!clone.getAttribute('xmlns')) {
            clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
        }
        return new XMLSerializer().serializeToString(clone);
    };

    // scale: multiplier for HiDPI output (2 = 2x resolution). Fallback 800x600 when SVG has no measured size.
    window.__extractPng = function(scale, bridgeFn) {
        const svgString = window.__extractSvg();
        if (!svgString) { bridgeFn(''); return; }

        const container = document.getElementById('mermaid-container');
        const renderedSvg = container.shadowRoot.querySelector('svg');
        if (!renderedSvg) { bridgeFn(''); return; }
        const vb = renderedSvg.viewBox ? renderedSvg.viewBox.baseVal : null;
        const w = (vb && vb.width > 0) ? vb.width : (parseFloat(renderedSvg.getAttribute('width')) || 800);
        const h = (vb && vb.height > 0) ? vb.height : (parseFloat(renderedSvg.getAttribute('height')) || 600);

        const canvas = document.createElement('canvas');
        canvas.width = Math.ceil(w * scale);
        canvas.height = Math.ceil(h * scale);
        const ctx = canvas.getContext('2d');
        if (!ctx) {
            console.error('[MermaidVisualizer] Failed to get 2D canvas context');
            bridgeFn('');
            return;
        }

        const isDark = document.body.classList.contains('dark-theme');
        ctx.fillStyle = isDark ? '#2b2b2b' : '#ffffff';
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        const svgDataUrl = 'data:image/svg+xml;base64,' + utf8ToBase64(svgString);
        const img = new Image();
        img.onload = function() {
            try {
                ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
                const dataUrl = canvas.toDataURL('image/png');
                const b64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
                bridgeFn(b64);
            } catch (e) {
                console.error('[MermaidVisualizer] PNG extraction failed:', e);
                bridgeFn('');
            }
        };
        img.onerror = function() {
            console.error('[MermaidVisualizer] PNG extraction failed: SVG image load error');
            bridgeFn('');
        };
        img.src = svgDataUrl;
    };
})();
