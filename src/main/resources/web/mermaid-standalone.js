/**
 * Adapter of the standalone .mmd preview (MermaidPreviewPanel): one diagram in #mermaid-container, rendered by
 * the shared pipeline with the `host` built in createHost(). Entry points for Kotlin: window.renderDiagram,
 * __showError, __scrollPreviewTo, __extractSvg, __extractPng.
 */
(function () {
    'use strict';

    const core = window.__mermaidCore;
    const CONTAINER_ID = 'mermaid-container';
    const DARK_CLASS = 'dark-theme';

    // Inlined by MermaidPreviewPanel.
    const shadowCssEl = document.getElementById('mermaid-shadow-styles');
    const shadowCss = shadowCssEl ? shadowCssEl.textContent : '';
    if (!shadowCss) {
        console.warn('[MermaidVisualizer] Shadow CSS not loaded — toolbar and zoom controls may not display correctly');
    }

    function diagramContainer() {
        return document.getElementById(CONTAINER_ID);
    }

    function isDarkTheme() {
        return document.body.classList.contains(DARK_CLASS);
    }

    // --- Kotlin bridges (JBCefJSQuery) ---
    // window.__xxxBridge(string) functions injected by Kotlin, possibly after this script runs: looked up at call time.

    function hasBridge(name) {
        return typeof window[name] === 'function';
    }

    function bridge(name) {
        if (!hasBridge(name)) return null;
        return function (arg) {
            try {
                window[name](arg);
            } catch (e) {
                console.error('[MermaidVisualizer] ' + name + ' call failed:', e);
            }
        };
    }

    function reportRenderResult(info, generation) {
        const report = bridge('__mermaidErrorBridge');
        if (!report) return;
        info.gen = generation;
        report(JSON.stringify(info));
    }

    // --- Host strategy of the shared pipeline ---

    function createToolbar(container) {
        const openInTab = bridge('__openInTabBridge');
        const copySvg = bridge('__copySvgBridge');
        const copyPng = bridge('__copyPngBridge');
        const save = bridge('__saveBridge');
        if (!openInTab && !copySvg && !copyPng && !save) return null;
        return core.createExportToolbar({
            extractSvg: function () { return core.extractSvg(container); },
            extractPng: function (scale, cb) { core.extractPng(container, scale, isDarkTheme, cb); },
            openInTab: openInTab ? function () { openInTab(''); } : null,
            copySvg: copySvg,
            copyPng: copyPng,
            save: save
        });
    }

    function zoomOptions() {
        return {
            fitMode: 'fit',
            wheelRequiresModifier: true,
            enableKeyboard: true,
            scrollSyncCallback: syncScrollToEditor
        };
    }

    function errorActions() {
        return {
            copyText: bridge('__copyTextBridge'),
            reportError: bridge('__reportErrorBridge')
        };
    }

    function openLink(url) {
        const open = bridge('__openLinkBridge');
        if (open) open(url);
    }

    function createHost(isDark) {
        return {
            shadowCss: shadowCss,
            isDark: isDark,
            createToolbar: createToolbar,
            zoomOptions: zoomOptions,
            openLink: openLink,
            errorActions: errorActions
        };
    }

    // --- Rendering ---

    try {
        core.initMermaid('default');
    } catch (e) {
        console.error('[MermaidVisualizer] mermaid.initialize failed:', e);
        const container = diagramContainer();
        if (container) core.showError(container, shadowCss, 'Failed to initialize Mermaid: ' + e.message, null, false);
    }

    function applyBackground() {
        const cfg = window.__MERMAID_CONFIG || {};
        document.body.style.background = cfg.backgroundColor || '';
    }

    function ensureTheme(forceThemeRefresh, isDark, container, host, generation) {
        const autoTheme = isDark ? 'dark' : 'default';
        const effectiveTheme = (window.__MERMAID_CONFIG || {}).theme || autoTheme;
        if (!forceThemeRefresh && core.getCurrentTheme() === effectiveTheme) return true;
        try {
            core.initMermaid(autoTheme);
            return true;
        } catch (e) {
            console.error('[MermaidVisualizer] mermaid.initialize failed:', e);
            const error = core.showRenderError(container, 'Failed to initialize Mermaid: ' + e.message, null, null, host);
            reportRenderResult(core.toRenderErrorPayload(error), generation);
            return false;
        }
    }

    // Called by Kotlin; `generation` is echoed back in the result so stale renders are ignored.
    window.renderDiagram = async function (base64Source, forceThemeRefresh, generation) {
        const container = diagramContainer();
        if (!container) {
            console.error('[MermaidVisualizer] mermaid-container element not found in DOM');
            reportRenderResult(core.parseErrorInfo('Internal error: container not found'), generation);
            return;
        }

        applyBackground();
        const isDark = isDarkTheme();
        const host = createHost(isDark);
        if (!ensureTheme(forceThemeRefresh, isDark, container, host, generation)) return;

        let decoded;
        try {
            decoded = core.base64ToUtf8(base64Source);
        } catch (e) {
            const error = core.showRenderError(container, 'Failed to decode diagram source', null, null, host);
            reportRenderResult(core.toRenderErrorPayload(error), generation);
            return;
        }

        const context = core.prepareSource(decoded);
        if (!context.source) {
            core.clearShadowContent(container);
            reportRenderResult({ status: 'ok' }, generation);
            return;
        }

        const result = await core.renderInto(container, context.source, context, host);
        reportRenderResult(result.ok ? { status: 'ok' } : core.toRenderErrorPayload(result.error), generation);
    };

    // Kotlin fallback when the renderDiagram promise rejected.
    window.__showError = function (container, message, renderId, isDark) {
        core.showRenderError(container, message, null, renderId, createHost(isDark));
    };

    // --- Scroll synchronization (routed through zoom module pan at scale ~1.0) ---

    const SCROLL_GUARD_RESET_MS = 100;
    let scrollGuardActive = false;

    // Preview → editor.
    function syncScrollToEditor(fraction) {
        if (scrollGuardActive) return;
        const scroll = bridge('__mermaidScrollBridge');
        if (scroll) scroll(String(fraction));
    }

    // Editor → preview.
    window.__scrollPreviewTo = function (fraction) {
        if (typeof fraction !== 'number' || !isFinite(fraction)) return;
        const container = diagramContainer();
        if (!container || !container.shadowRoot || !window.__scrollZoomTo) return;
        scrollGuardActive = true;
        window.__scrollZoomTo(container.shadowRoot, fraction);
        requestAnimationFrame(function () {
            setTimeout(function () { scrollGuardActive = false; }, SCROLL_GUARD_RESET_MS);
        });
    };

    // --- Export entry points called by Kotlin ---

    window.__extractSvg = function () {
        return core.extractSvg(diagramContainer());
    };

    window.__extractPng = function (scale, bridgeFn) {
        core.extractPng(diagramContainer(), scale, isDarkTheme, bridgeFn);
    };
})();
