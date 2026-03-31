(function () {
    'use strict';

    const core = window.__mermaidCore;

    // Shadow CSS is loaded from a <style id="mermaid-shadow-styles"> element
    // inlined in the HTML by MermaidPreviewPanel (Kotlin).
    const shadowCssEl = document.getElementById('mermaid-shadow-styles');
    const shadowCss = shadowCssEl ? shadowCssEl.textContent : '';
    if (!shadowCss) {
        console.warn('[MermaidVisualizer] Shadow CSS not loaded — toolbar and zoom controls may not display correctly');
    }

    function createStandaloneToolbar() {
        if (typeof window.__copySvgBridge !== 'function' &&
            typeof window.__copyPngBridge !== 'function' &&
            typeof window.__saveBridge !== 'function') {
            return null;
        }

        const container = document.getElementById('mermaid-container');

        return core.createExportToolbar({
            extractSvg: function () { return core.extractSvg(container); },
            extractPng: function (scale, cb) {
                core.extractPng(container, scale, function () {
                    return document.body.classList.contains('dark-theme');
                }, cb);
            },
            copySvg: typeof window.__copySvgBridge === 'function' ? function (b64) { window.__copySvgBridge(b64); } : null,
            copyPng: typeof window.__copyPngBridge === 'function' ? function (b64) { window.__copyPngBridge(b64); } : null,
            save: typeof window.__saveBridge === 'function' ? function (payload) { window.__saveBridge(payload); } : null
        });
    }

    try {
        core.initMermaid('default');
    } catch (e) {
        console.error('[MermaidVisualizer] mermaid.initialize failed:', e);
        const container = document.getElementById('mermaid-container');
        if (container) core.showError(container, shadowCss, 'Failed to initialize Mermaid: ' + e.message, null, false);
    }

    function reportRenderResult(info, generation) {
        if (typeof window.__mermaidErrorBridge === 'function') {
            try {
                info.gen = generation;
                window.__mermaidErrorBridge(JSON.stringify(info));
            } catch (e) {
                console.error('[MermaidVisualizer] error bridge call failed:', e);
            }
        }
    }

    window.renderDiagram = async function (base64Source, forceThemeRefresh, generation) {
        const container = document.getElementById('mermaid-container');
        if (!container) {
            console.error('[MermaidVisualizer] mermaid-container element not found in DOM');
            reportRenderResult({ status: 'error', message: 'Internal error: container not found', line: null, column: null }, generation);
            return;
        }

        const isDark = document.body.classList.contains('dark-theme');
        const autoTheme = isDark ? 'dark' : 'default';
        const effectiveTheme = (window.__MERMAID_CONFIG || {}).theme || autoTheme;

        if (forceThemeRefresh || core.getCurrentTheme() !== effectiveTheme) {
            try {
                core.initMermaid(autoTheme);
            } catch (e) {
                console.error('[MermaidVisualizer] mermaid.initialize failed:', e);
                core.showError(container, shadowCss, 'Failed to initialize Mermaid: ' + e.message, null, isDark);
                reportRenderResult({ status: 'error', message: 'Failed to initialize Mermaid: ' + e.message, line: null, column: null }, generation);
                return;
            }
        }

        let source;
        try {
            source = core.base64ToUtf8(base64Source);
        } catch (e) {
            core.showError(container, shadowCss, 'Failed to decode diagram source', null, isDark);
            reportRenderResult({ status: 'error', message: 'Failed to decode diagram source', line: null, column: null }, generation);
            return;
        }

        source = source.trim();
        if (!source) {
            if (container.shadowRoot) container.shadowRoot.textContent = '';
            reportRenderResult({ status: 'ok' }, generation);
            return;
        }

        const renderId = core.nextRenderId();
        try {
            const result = await mermaid.render(renderId, source);
            if (result && typeof result.svg === 'string') {
                core.injectSvg(container, shadowCss, result.svg, isDark);
                reportRenderResult({ status: 'ok' }, generation);
                const toolbar = createStandaloneToolbar();
                if (toolbar) container.shadowRoot.appendChild(toolbar);
                if (window.__initMermaidZoom) {
                    window.__initMermaidZoom(container.shadowRoot, {
                        fitMode: 'fit',
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
                core.showError(container, shadowCss, 'Unexpected render result', renderId, isDark);
                reportRenderResult({ status: 'error', message: 'Unexpected render result', line: null, column: null }, generation);
            }
        } catch (err) {
            console.error('[MermaidVisualizer] Render failed', err);
            core.showError(container, shadowCss, err.message || String(err), renderId, isDark);
            reportRenderResult(core.parseErrorInfo(err), generation);
        }
    };

    // Expose showError for Kotlin-side fallback error display
    window.__showError = function (container, message, renderId, isDark) {
        core.showError(container, shadowCss, message, renderId, isDark);
    };

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

    // --- Export functions (thin wrappers for Kotlin JBCefJSQuery bridges) ---

    window.__extractSvg = function () {
        const container = document.getElementById('mermaid-container');
        return core.extractSvg(container);
    };

    window.__extractPng = function (scale, bridgeFn) {
        const container = document.getElementById('mermaid-container');
        core.extractPng(container, scale, function () {
            return document.body.classList.contains('dark-theme');
        }, bridgeFn);
    };
})();
