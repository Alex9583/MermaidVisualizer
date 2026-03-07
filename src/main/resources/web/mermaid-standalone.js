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
        mermaid.initialize({
            startOnLoad: false,
            maxTextSize: 100000,
            theme: currentTheme,
            securityLevel: 'strict'
        });
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

    initMermaid('default');
})();
