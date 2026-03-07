// NOTE: Shadow DOM, error handling, and init logic are intentionally duplicated from mermaid-render.js.
// The standalone editor loads resources via loadHTML() with inlined scripts, while the Markdown extension
// serves resources via PreviewStaticServer. If modifying shared logic, update both files.
(function () {
    'use strict';

    const CLASS_ERROR = 'mermaid-error';

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

    function setShadowContent(el, contentEl, isDark) {
        const shadow = el.shadowRoot || el.attachShadow({ mode: 'open' });
        const style = document.createElement('style');
        style.textContent = isDark ? SHADOW_STYLES_DARK : SHADOW_STYLES_LIGHT;
        shadow.textContent = '';
        shadow.appendChild(style);
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
            var container = document.getElementById('mermaid-container');
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
            source = atob(base64Source);
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

    // --- Scroll synchronization ---
    const SCROLL_GUARD_RESET_MS = 100;
    let scrollGuardActive = false;

    function getScrollableHeight() {
        return document.documentElement.scrollHeight - window.innerHeight;
    }

    // Called from Kotlin to scroll preview to a proportional position
    window.__scrollPreviewTo = function (fraction) {
        if (typeof fraction !== 'number' || !isFinite(fraction)) return;
        const scrollableHeight = getScrollableHeight();
        if (scrollableHeight <= 0) return;
        scrollGuardActive = true;
        window.scrollTo(0, fraction * scrollableHeight);
        requestAnimationFrame(function () {
            setTimeout(function () { scrollGuardActive = false; }, SCROLL_GUARD_RESET_MS);
        });
    };

    // Throttled scroll listener — reports position to Kotlin via JBCefJSQuery bridge
    let scrollRAF = null;
    window.addEventListener('scroll', function () {
        if (scrollGuardActive || scrollRAF) return;
        scrollRAF = requestAnimationFrame(function () {
            scrollRAF = null;
            if (scrollGuardActive) return;
            const scrollableHeight = getScrollableHeight();
            if (scrollableHeight <= 0) return;
            const fraction = window.scrollY / scrollableHeight;
            if (typeof window.__mermaidScrollBridge === 'function') {
                try {
                    window.__mermaidScrollBridge(String(fraction));
                } catch (e) {
                    console.error('[MermaidVisualizer] scroll bridge call failed:', e);
                }
            }
        });
    }, { passive: true });
})();
