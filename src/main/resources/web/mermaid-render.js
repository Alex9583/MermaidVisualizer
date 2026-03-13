(function () {
    'use strict';

    // Attribute schema:
    // - data-mermaid-processed: set on the original <code> element to skip empty blocks on re-scan
    // - data-processed (ATTR_PROCESSED): set on .mermaid-diagram container after render completes
    //   (success or error) — used by onThemeChange to find re-renderable diagrams
    // - data-source (ATTR_SOURCE): stores the original Mermaid source text on the container,
    //   used for re-rendering on theme change without needing the original <code> element
    const SELECTOR_UNPROCESSED = 'code.language-mermaid:not([data-mermaid-processed])';
    const CLASS_DIAGRAM = 'mermaid-diagram';
    const CLASS_ERROR = 'mermaid-error';
    const ATTR_PROCESSED = 'data-processed';
    const ATTR_SOURCE = 'data-source';
    const DARK_CLASSES = ['darcula', 'dark', 'dark-mode', 'dark-theme', 'theme-dark'];

    function computeBrightness(el) {
        if (!el) return -1;
        const bg = window.getComputedStyle(el).backgroundColor;
        if (!bg || bg === 'transparent' || bg === 'rgba(0, 0, 0, 0)') return -1;
        const match = bg.match(/\d+/g);
        if (!match || match.length < 3) return -1;
        // ITU-R BT.601 luminance formula — weights green highest because human vision is most sensitive to green
        return (parseInt(match[0]) * 299 + parseInt(match[1]) * 587 + parseInt(match[2]) * 114) / 1000;
    }

    function isDarkTheme() {
        try {
            const cs = window.getComputedStyle(document.documentElement).colorScheme;
            if (cs && cs.includes('dark') && !cs.includes('light')) return true;

            const meta = document.querySelector('meta[name="color-scheme"]');
            if (meta) {
                const content = meta.getAttribute('content') || '';
                if (content.includes('dark') && !content.includes('light')) return true;
            }

            if (DARK_CLASSES.some(function (c) { return document.documentElement.classList.contains(c); })) return true;
            if (document.body) {
                if (DARK_CLASSES.some(function (c) { return document.body.classList.contains(c); })) return true;
                if (document.body.hasAttribute('data-darcula')) return true;
            }

            const bodyBrightness = computeBrightness(document.body);
            if (bodyBrightness >= 0) return bodyBrightness < 128;
            const htmlBrightness = computeBrightness(document.documentElement);
            if (htmlBrightness >= 0) return htmlBrightness < 128;

            if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) return true;
        } catch (e) {
            console.warn('[MermaidVisualizer] isDarkTheme() detection failed', e);
        }

        return false;
    }

    function utf8ToBase64(str) {
        const bytes = new TextEncoder().encode(str);
        let binary = '';
        for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
        return btoa(binary);
    }

    // Shadow CSS is set by mermaid-shadow-css-init.js, a virtual script generated at runtime
    // by MermaidBrowserExtension (Kotlin) that inlines mermaid-shadow.css into window.__MERMAID_SHADOW_CSS.
    // It is served via PreviewStaticServer and loaded before this script.
    const shadowCss = window.__MERMAID_SHADOW_CSS || '';
    if (!shadowCss) {
        console.warn('[MermaidVisualizer] Shadow CSS not loaded — toolbar and zoom controls may not display correctly');
    }

    /**
     * Returns the parent pre wrapper if it exists, otherwise the element itself.
     */
    function getPreWrapper(codeEl) {
        return codeEl.parentElement && codeEl.parentElement.tagName === 'PRE'
            ? codeEl.parentElement
            : codeEl;
    }

    /**
     * Injects rendered content into an isolated Shadow DOM.
     */
    function setShadowContent(el, contentEl, isDark) {
        const shadow = el.shadowRoot || el.attachShadow({ mode: 'open' });
        el.classList.toggle('dark', isDark);
        const style = document.createElement('style');
        style.textContent = shadowCss;
        shadow.textContent = '';
        shadow.appendChild(style);
        shadow.appendChild(contentEl);
    }

    /**
     * Injects SVG markup from mermaid.render() into a Shadow DOM container.
     * The svgString is sanitized by Mermaid's built-in DOMPurify (securityLevel: 'strict')
     * and further isolated via Shadow DOM, preventing style/script leakage.
     * innerHTML is necessary because Mermaid outputs complex SVG that cannot be
     * constructed via createElement.
     */
    function injectSvg(container, svgString, isDark) {
        const svgContainer = document.createElement('div');
        svgContainer.innerHTML = svgString; // eslint-disable-line no-unsanitized/property
        setShadowContent(container, svgContainer, isDark);
    }

    function showRenderError(container, message, renderId, isDark) {
        const errorPre = document.createElement('pre');
        errorPre.className = CLASS_ERROR;
        errorPre.textContent = message;
        setShadowContent(container, errorPre, isDark);
        if (renderId) {
            const leftover = document.getElementById(renderId);
            if (leftover) leftover.remove();
        }
    }

    // --- Export: extraction functions ---
    // In the Markdown preview context, export data is sent to Kotlin via window.__IntelliJTools.messagePipe (BrowserPipe).

    function extractSvgFromContainer(container) {
        const shadow = container ? container.shadowRoot : null;
        const svg = shadow ? shadow.querySelector('svg') : null;
        if (!svg) return null;
        const clone = svg.cloneNode(true);
        if (!clone.getAttribute('xmlns')) {
            clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
        }
        return new XMLSerializer().serializeToString(clone);
    }

    // scale: multiplier for HiDPI output (2 = 2x resolution). Fallback 800x600 when SVG has no measured size.
    function extractPngFromContainer(container, scale, callback) {
        const svgString = extractSvgFromContainer(container);
        if (!svgString) { callback(''); return; }

        const renderedSvg = container.shadowRoot.querySelector('svg');
        if (!renderedSvg) { callback(''); return; }
        const vb = renderedSvg.viewBox ? renderedSvg.viewBox.baseVal : null;
        const w = (vb && vb.width > 0) ? vb.width : (parseFloat(renderedSvg.getAttribute('width')) || 800);
        const h = (vb && vb.height > 0) ? vb.height : (parseFloat(renderedSvg.getAttribute('height')) || 600);

        const canvas = document.createElement('canvas');
        canvas.width = Math.ceil(w * scale);
        canvas.height = Math.ceil(h * scale);
        const ctx = canvas.getContext('2d');
        if (!ctx) {
            console.error('[MermaidVisualizer] Failed to get 2D canvas context');
            callback('');
            return;
        }

        const dark = isDarkTheme();
        ctx.fillStyle = dark ? '#2b2b2b' : '#ffffff';
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        const svgDataUrl = 'data:image/svg+xml;base64,' + utf8ToBase64(svgString);
        const img = new Image();
        img.onload = function () {
            try {
                ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
                const dataUrl = canvas.toDataURL('image/png');
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

    // --- Export: toolbar creation ---

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

    function createExportToolbar(container) {
        if (!window.__IntelliJTools || !window.__IntelliJTools.messagePipe) return null;
        const pipe = window.__IntelliJTools.messagePipe;

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

        toolbar.appendChild(createButton(ICON_COPY, 'Copy SVG', function () {
            try {
                const svgString = extractSvgFromContainer(container);
                if (!svgString) {
                    console.warn('[MermaidVisualizer] Copy SVG: no SVG found in container');
                    pipe.post('mermaid/copy-svg', '');
                    return;
                }
                pipe.post('mermaid/copy-svg', utf8ToBase64(svgString));
            } catch (e) {
                console.error('[MermaidVisualizer] Copy SVG failed:', e);
                pipe.post('mermaid/copy-svg', '');
            }
        }));

        toolbar.appendChild(createButton(ICON_IMAGE, 'Copy PNG', function () {
            try {
                extractPngFromContainer(container, 2, function (b64) {
                    try {
                        pipe.post('mermaid/copy-png', b64 || '');
                    } catch (e) {
                        console.error('[MermaidVisualizer] Copy PNG post failed:', e);
                    }
                });
            } catch (e) {
                console.error('[MermaidVisualizer] Copy PNG failed:', e);
                pipe.post('mermaid/copy-png', '');
            }
        }));

        toolbar.appendChild(createButton(ICON_SAVE, 'Save\u2026', function () {
            try {
                const svgString = extractSvgFromContainer(container);
                if (!svgString) {
                    console.warn('[MermaidVisualizer] Save: no SVG found in container');
                    return;
                }
                extractPngFromContainer(container, 2, function (pngB64) {
                    try {
                        const payload = JSON.stringify({
                            svg: utf8ToBase64(svgString),
                            png: pngB64 || ''
                        });
                        pipe.post('mermaid/save', payload);
                    } catch (e) {
                        console.error('[MermaidVisualizer] Save encoding failed:', e);
                    }
                });
            } catch (e) {
                console.error('[MermaidVisualizer] Save failed:', e);
                pipe.post('mermaid/save', JSON.stringify({svg: '', png: ''}));
            }
        }));

        return toolbar;
    }

    let currentTheme = 'default';
    let renderCounter = 0;

    function initMermaid(theme) {
        currentTheme = theme;
        mermaid.initialize({
            startOnLoad: false,
            maxTextSize: 100000,
            theme: currentTheme,
            securityLevel: 'strict'
        });
    }

    let rendering = false;

    async function renderSingleDiagram(container, source, isDark) {
        const renderId = 'mermaid-render-' + (renderCounter++);
        try {
            const result = await mermaid.render(renderId, source);
            if (result && typeof result.svg === 'string') {
                injectSvg(container, result.svg, isDark);
                const toolbar = createExportToolbar(container);
                if (toolbar) container.shadowRoot.appendChild(toolbar);
                if (window.__initMermaidZoom) {
                    window.__initMermaidZoom(container.shadowRoot, {
                        fitMode: 'width',
                        toolbarEl: container.shadowRoot.querySelector('.mermaid-export-toolbar'),
                        wheelRequiresModifier: true,
                        constrainSvg: false
                    });
                }
            } else {
                showRenderError(container, 'Unexpected render result', renderId, isDark);
            }
        } catch (err) {
            console.warn('[MermaidVisualizer] Render failed for diagram', err);
            showRenderError(container, err.message || String(err), renderId, isDark);
        }
        container.setAttribute(ATTR_PROCESSED, 'true');
    }

    /**
     * Find <code class="language-mermaid"> elements rendered by the Markdown plugin,
     * replace the parent <pre> with a diagram container, and render via Mermaid.js.
     */
    async function renderDiagrams() {
        if (rendering) return;
        rendering = true;

        try {
            const isDark = isDarkTheme();
            const codeEls = document.querySelectorAll(SELECTOR_UNPROCESSED);

            for (let i = 0; i < codeEls.length; i++) {
                try {
                    const codeEl = codeEls[i];
                    const source = (codeEl.textContent || '').trim();

                    if (!source) {
                        codeEl.setAttribute('data-mermaid-processed', 'true');
                        continue;
                    }

                    const container = document.createElement('div');
                    container.className = CLASS_DIAGRAM;
                    container.setAttribute(ATTR_SOURCE, source);

                    const target = getPreWrapper(codeEl);
                    if (!target.parentElement) continue;
                    target.parentElement.replaceChild(container, target);

                    await renderSingleDiagram(container, source, isDark);
                } catch (domErr) {
                    console.warn('[MermaidVisualizer] DOM error processing diagram element', domErr);
                }
            }
        } finally {
            rendering = false;
            if (document.querySelector(SELECTOR_UNPROCESSED)) {
                setTimeout(renderDiagrams, 0);
            }
        }
    }

    function hookIncrementalDOM() {
        if (typeof IncrementalDOM === 'undefined' || !IncrementalDOM.notifications) return false;
        if (!Array.isArray(IncrementalDOM.notifications.afterPatchListeners)) {
            IncrementalDOM.notifications.afterPatchListeners = [];
        }
        IncrementalDOM.notifications.afterPatchListeners.push(renderDiagrams);
        return true;
    }

    function setupMutationObserver() {
        let pendingRender = false;
        new MutationObserver(function () {
            if (!pendingRender && document.querySelector(SELECTOR_UNPROCESSED)) {
                pendingRender = true;
                setTimeout(function () {
                    pendingRender = false;
                    renderDiagrams();
                }, 50);
            }
        }).observe(document.body, { childList: true, subtree: true });
    }

    let themeChangeTimer = null;

    function onThemeChange() {
        clearTimeout(themeChangeTimer);
        themeChangeTimer = setTimeout(doThemeChange, 100);
    }

    async function doThemeChange() {
        const isDark = isDarkTheme();
        const newTheme = isDark ? 'dark' : 'default';
        if (newTheme === currentTheme) return;

        if (rendering) {
            setTimeout(onThemeChange, 50);
            return;
        }
        rendering = true;

        try {
            initMermaid(newTheme);

            const diagrams = document.querySelectorAll('.' + CLASS_DIAGRAM + '[' + ATTR_PROCESSED + ']');
            for (let i = 0; i < diagrams.length; i++) {
                const el = diagrams[i];
                const source = el.getAttribute(ATTR_SOURCE);
                if (!source) continue;

                el.removeAttribute(ATTR_PROCESSED);
                if (el.shadowRoot) el.shadowRoot.textContent = '';

                await renderSingleDiagram(el, source, isDark);
            }
        } finally {
            rendering = false;
        }
    }

    function setupThemeObserver() {
        const attrObserver = new MutationObserver(onThemeChange);
        attrObserver.observe(document.body, { attributes: true, attributeFilter: ['class', 'style', 'data-darcula'] });
        attrObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['class', 'style'] });

        new MutationObserver(onThemeChange)
            .observe(document.head, { childList: true, subtree: true, attributes: true, attributeFilter: ['content'] });

        if (window.matchMedia) {
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', onThemeChange);
        }
    }

    let bootstrapAttempts = 0;

    function showLoadError() {
        const isDark = isDarkTheme();
        const codeEls = document.querySelectorAll('code.language-mermaid');
        for (let i = 0; i < codeEls.length; i++) {
            const target = getPreWrapper(codeEls[i]);
            if (!target.parentElement) continue;
            const container = document.createElement('div');
            container.className = CLASS_DIAGRAM;
            target.parentElement.replaceChild(container, target);
            showRenderError(container, 'Mermaid failed to load. Please try reopening the preview.', null, isDark);
        }
    }

    function bootstrap() {
        bootstrapAttempts++;

        if (typeof mermaid === 'undefined') {
            if (bootstrapAttempts < 50) {
                setTimeout(bootstrap, 200);
            } else {
                console.error('bootstrap: mermaid.js failed to load after ' + bootstrapAttempts + ' attempts');
                showLoadError();
            }
            return;
        }

        try {
            initMermaid(isDarkTheme() ? 'dark' : 'default');
        } catch (e) {
            console.error('bootstrap: mermaid.initialize() failed', e);
            showLoadError();
            return;
        }

        if (!hookIncrementalDOM()) {
            setupMutationObserver();
        }

        setupThemeObserver();
        renderDiagrams();

        // Delayed theme sync: IntelliJ may apply theme styles after initial page load,
        // so we re-check the theme shortly after bootstrap to catch late changes.
        setTimeout(onThemeChange, 500);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', bootstrap);
    } else {
        bootstrap();
    }
})();