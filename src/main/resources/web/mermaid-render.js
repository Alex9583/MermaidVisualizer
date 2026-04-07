(function () {
    'use strict';

    // Attribute schema:
    // - data-mermaid-processed: set on the original <code> element to skip empty blocks on re-scan
    // - data-processed (ATTR_PROCESSED): set on .mermaid-diagram container after render completes
    //   (success or error) — used by reInitAndRenderAll to find re-renderable diagrams (theme change and config update)
    // - data-source (ATTR_SOURCE): stores the original Mermaid source text on the container,
    //   used for re-rendering on theme change without needing the original <code> element
    const SELECTOR_UNPROCESSED = 'code.language-mermaid:not([data-mermaid-processed])';
    const CLASS_DIAGRAM = 'mermaid-diagram';
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

    let rendering = false;

    function createMarkdownToolbar(container) {
        if (!window.__IntelliJTools || !window.__IntelliJTools.messagePipe) return null;
        const pipe = window.__IntelliJTools.messagePipe;
        const core = window.__mermaidCore;
        return core.createExportToolbar({
            extractSvg: function () { return core.extractSvg(container); },
            extractPng: function (scale, cb) { core.extractPng(container, scale, isDarkTheme, cb); },
            copySvg: function (b64) { pipe.post('mermaid/copy-svg', b64); },
            copyPng: function (b64) { pipe.post('mermaid/copy-png', b64); },
            save: function (payload) { pipe.post('mermaid/save', payload); }
        });
    }

    async function renderSingleDiagram(container, source, isDark) {
        const core = window.__mermaidCore;
        const renderId = core.nextRenderId();
        try {
            const result = await mermaid.render(renderId, source);
            if (result && typeof result.svg === 'string') {
                core.injectSvg(container, shadowCss, result.svg, isDark);
                const toolbar = createMarkdownToolbar(container);
                if (toolbar) container.shadowRoot.appendChild(toolbar);
                if (window.__initMermaidZoom) {
                    window.__initMermaidZoom(container.shadowRoot, {
                        layoutMode: 'inline',
                        toolbarEl: container.shadowRoot.querySelector('.mermaid-export-toolbar'),
                        wheelRequiresModifier: true
                    });
                }
            } else {
                core.showError(container, shadowCss, 'Unexpected render result', renderId, isDark);
            }
        } catch (err) {
            console.warn('[MermaidVisualizer] Render failed for diagram', err);
            core.showError(container, shadowCss, err.message || String(err), renderId, isDark);
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

    async function reInitAndRenderAll(retries) {
        if (rendering) {
            if ((retries || 0) >= 10) {
                console.warn('[MermaidVisualizer] reInitAndRenderAll: abandoned after 10 retries — a render may be stuck');
                return;
            }
            setTimeout(function () { reInitAndRenderAll((retries || 0) + 1); }, 50);
            return;
        }
        rendering = true;

        try {
            const core = window.__mermaidCore;
            const isDark = isDarkTheme();
            try {
                core.initMermaid(isDark ? 'dark' : 'default');
            } catch (e) {
                console.error('[MermaidVisualizer] mermaid.initialize failed:', e);
                return;
            }

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

    let themeChangeTimer = null;

    function onThemeChange() {
        clearTimeout(themeChangeTimer);
        themeChangeTimer = setTimeout(function () {
            const isDark = isDarkTheme();
            const newTheme = isDark ? 'dark' : 'default';
            if (newTheme !== window.__mermaidCore.getCurrentTheme()) reInitAndRenderAll();
        }, 100);
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
        const core = window.__mermaidCore;
        const isDark = isDarkTheme();
        const codeEls = document.querySelectorAll('code.language-mermaid');
        for (let i = 0; i < codeEls.length; i++) {
            const target = getPreWrapper(codeEls[i]);
            if (!target.parentElement) continue;
            const container = document.createElement('div');
            container.className = CLASS_DIAGRAM;
            target.parentElement.replaceChild(container, target);
            core.showError(container, shadowCss, 'Mermaid failed to load. Please try reopening the preview.', null, isDark);
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
            window.__mermaidCore.initMermaid(isDarkTheme() ? 'dark' : 'default');
        } catch (e) {
            console.error('bootstrap: mermaid.initialize() failed', e);
            showLoadError();
            return;
        }

        if (!hookIncrementalDOM()) {
            setupMutationObserver();
        }

        setupThemeObserver();
        setupConfigListener();
        renderDiagrams();

        // Delayed theme sync: IntelliJ may apply theme styles after initial page load,
        // so we re-check the theme shortly after bootstrap to catch late changes.
        setTimeout(onThemeChange, 500);
    }

    function setupConfigListener() {
        if (!window.__IntelliJTools || !window.__IntelliJTools.messagePipe) {
            console.debug('[MermaidVisualizer] messagePipe not available — live config updates disabled');
            return;
        }
        window.__IntelliJTools.messagePipe.subscribe('mermaid/config', function (jsonStr) {
            try {
                window.__MERMAID_CONFIG = JSON.parse(jsonStr);
            } catch (e) {
                console.warn('[MermaidVisualizer] Failed to parse config update', e);
                return;
            }
            reInitAndRenderAll();
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', bootstrap);
    } else {
        bootstrap();
    }
})();
