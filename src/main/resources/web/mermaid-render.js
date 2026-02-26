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

    function buildShadowStyles(dark) {
        const colors = dark
            ? 'color: #e07060; background: #3c2020; border: 1px solid #5c3030'
            : 'color: #c0392b; background: #fdf0ef; border: 1px solid #e6b0aa';
        return 'svg { max-width: 100%; height: auto; }\n' +
            '.' + CLASS_ERROR + ' {\n' +
            '  ' + colors + ';\n' +
            '  border-radius: 4px; padding: 12px; font-family: monospace;\n' +
            '  font-size: 12px; white-space: pre-wrap; text-align: left; margin: 8px 0;\n' +
            '}';
    }

    const SHADOW_STYLES_LIGHT = buildShadowStyles(false);
    const SHADOW_STYLES_DARK = buildShadowStyles(true);

    function shadowStyles(dark) {
        return dark ? SHADOW_STYLES_DARK : SHADOW_STYLES_LIGHT;
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
        const style = document.createElement('style');
        style.textContent = shadowStyles(isDark);
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