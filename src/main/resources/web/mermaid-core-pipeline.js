/**
 * Module `pipeline`: the render flow shared by both previews (template method). renderInto() calls
 * mermaid.render() once, then mounts the SVG (toolbar, zoom, links) or shows the banner over the last valid
 * diagram. The adapter's `host` supplies what differs per context:
 *   shadowCss, isDark
 *   createToolbar(container) → Element | null
 *   zoomOptions(container) → options for __initMermaidZoom (toolbarEl added here) | null
 *   openLink(url); errorActions() → { copyText?, reportError? }
 *   previousRendering?(source) → SVG to restore under the banner when the host element was recreated (Markdown)
 */
'use strict';

window.__mermaidCore.define('pipeline', ['shadow', 'runtime', 'errors', 'banner'], function (shadow, runtime, errors, banner) {

    const TOOLBAR_SELECTOR = '.mermaid-export-toolbar';

    function mountDiagram(container, svg, host) {
        shadow.injectSvg(container, host.shadowCss, svg, host.isDark);
        const toolbar = host.createToolbar(container);
        if (toolbar) container.shadowRoot.appendChild(toolbar);
        const zoomOptions = host.zoomOptions(container);
        if (zoomOptions && typeof window.__initMermaidZoom === 'function') {
            zoomOptions.toolbarEl = container.shadowRoot.querySelector(TOOLBAR_SELECTOR);
            window.__initMermaidZoom(container.shadowRoot, zoomOptions);
        }
        shadow.installLinkHandler(container.shadowRoot, host.openLink);
    }

    // err: Error or message; context: see prepareSource, or null; renderId: the failed mermaid.render() id, or null.
    function showRenderError(container, err, context, renderId, host) {
        const classified = errors.classifyRenderError(err, context);
        shadow.removeRenderLeftover(renderId);
        if (typeof host.previousRendering === 'function') {
            const previousSvg = host.previousRendering(context ? context.source : null);
            if (previousSvg) mountDiagram(container, previousSvg, host);
        }
        banner.showErrorBanner(container, host.shadowCss, classified, host.isDark, host.errorActions());
        return classified;
    }

    // Never throws: { ok: true, svg } once mounted, { ok: false, error: classified } once the banner is shown.
    async function renderInto(container, source, context, host) {
        const renderId = runtime.nextRenderId();
        try {
            const result = await mermaid.render(renderId, source);
            const limitError = result ? runtime.detectTextSizeExceeded(source, result.svg) : null;
            if (limitError) {
                return { ok: false, error: showRenderError(container, limitError, context, renderId, host) };
            }
            if (result && typeof result.svg === 'string') {
                mountDiagram(container, result.svg, host);
                return { ok: true, svg: result.svg };
            }
            return { ok: false, error: showRenderError(container, 'Unexpected render result', context, renderId, host) };
        } catch (err) {
            console.warn('[MermaidVisualizer] Render failed', err);
            return { ok: false, error: showRenderError(container, err, context, renderId, host) };
        }
    }

    return {
        mountDiagram: mountDiagram,
        showRenderError: showRenderError,
        renderInto: renderInto
    };
});
