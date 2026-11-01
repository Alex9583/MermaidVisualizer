/**
 * Module `runtime`: mermaid.initialize() from window.__MERMAID_CONFIG, render ids, and the maxTextSize/maxEdges
 * limits (issue #34) — both in Mermaid's `secure` list, so only initialize() can raise them.
 */
'use strict';

window.__mermaidCore.define('runtime', [], function () {

    // Mirror the defaults of MermaidSettings.kt.
    const DEFAULT_MAX_TEXT_SIZE = 500000;
    const DEFAULT_MAX_EDGES = 2000;
    const SETTINGS_PATH = 'Settings > Tools > Mermaid';

    let currentTheme = 'default';

    let renderCounter = 0;

    function initMermaid(theme) {
        const cfg = window.__MERMAID_CONFIG || {};
        currentTheme = cfg.theme || theme;
        const opts = {
            startOnLoad: false,
            maxTextSize: cfg.maxTextSize ?? DEFAULT_MAX_TEXT_SIZE,
            maxEdges: cfg.maxEdges ?? DEFAULT_MAX_EDGES,
            theme: currentTheme,
            look: cfg.look || 'classic',
            securityLevel: 'strict',
            // The previews show their own banner: skip Mermaid's bomb diagram, laid out on every failed render.
            suppressErrorRendering: true
        };
        if (cfg.fontFamily) opts.fontFamily = cfg.fontFamily;
        // Absent = Mermaid's default (ELK, bundled since Mermaid 12).
        if (cfg.layout) opts.layout = cfg.layout;
        if (cfg.lineColor) {
            opts.themeVariables = opts.themeVariables || {};
            opts.themeVariables.lineColor = cfg.lineColor;
        }
        mermaid.initialize(opts);
    }

    const TEXT_SIZE_FALLBACK_MARKER = 'Maximum text size in diagram exceeded';
    const EDGE_LIMIT_PATTERN = /^Edge limit exceeded\. (\d+) edges found, but the limit is (\S+)\./;
    const TEXT_SIZE_PATTERN = /^Diagram text size exceeds the limit/;

    // Over maxTextSize Mermaid does not throw: it returns a fallback diagram as a success. Both checks are
    // needed: the marker alone could be a node label, the raw length alone over-counts comments and front matter.
    function detectTextSizeExceeded(source, svg) {
        if (typeof svg !== 'string' || !svg.includes(TEXT_SIZE_FALLBACK_MARKER)) return null;
        const cfg = window.__MERMAID_CONFIG || {};
        const limit = cfg.maxTextSize ?? DEFAULT_MAX_TEXT_SIZE;
        const size = typeof source === 'string' ? source.length : 0;
        if (size <= limit) return null;
        return 'Diagram text size exceeds the limit (' + size + ' > ' + limit + ' characters). ' +
            'Increase "Max text size" in ' + SETTINGS_PATH + '.';
    }

    // Mermaid's maxEdges error says to call mermaid.initialize(): point at the setting instead.
    function describeRenderError(err) {
        const raw = (err && err.message) || String(err);
        const match = EDGE_LIMIT_PATTERN.exec(raw);
        if (!match) return raw;
        const cfg = window.__MERMAID_CONFIG || {};
        const limit = /^\d+$/.test(match[2]) ? match[2] : String(cfg.maxEdges ?? DEFAULT_MAX_EDGES);
        return 'Diagram has more than ' + limit + ' edges. ' +
            'Increase "Max edges" in ' + SETTINGS_PATH + '.';
    }

    function getCurrentTheme() {
        return currentTheme;
    }

    function nextRenderId() {
        return 'mermaid-render-' + (renderCounter++);
    }

    function isTextSizeMessage(raw) {
        return TEXT_SIZE_PATTERN.test(raw);
    }

    function isEdgeLimitMessage(raw) {
        return EDGE_LIMIT_PATTERN.test(raw);
    }

    return {
        DEFAULT_MAX_TEXT_SIZE: DEFAULT_MAX_TEXT_SIZE,
        DEFAULT_MAX_EDGES: DEFAULT_MAX_EDGES,
        SETTINGS_PATH: SETTINGS_PATH,
        initMermaid: initMermaid,
        getCurrentTheme: getCurrentTheme,
        nextRenderId: nextRenderId,
        detectTextSizeExceeded: detectTextSizeExceeded,
        describeRenderError: describeRenderError,
        isTextSizeMessage: isTextSizeMessage,
        isEdgeLimitMessage: isEdgeLimitMessage
    };
});
