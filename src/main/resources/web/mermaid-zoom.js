// Shared zoom, pan & fit-to-window module for Mermaid diagrams.
// Two layout modes:
//   - standalone (default): full-viewport editor (.mmd), height from CSS (100vh)
//   - inline: document-flow context (Markdown preview), height computed from SVG dimensions
// Both modes use CSS transform-based zoom/pan for identical visual behavior.
// Initialized per shadow root via window.__initMermaidZoom(shadowRoot, options).
(function () {
    'use strict';

    const core = window.__mermaidCore;

    // ====== Constants ======

    const MIN_SCALE = 0.1;
    const MAX_SCALE = 5.0;
    const ZOOM_FACTOR = 1.25;

    // Per-shadowRoot state stored in a WeakMap
    const stateMap = new WeakMap();

    // ====== Shared Utilities ======

    /**
     * Reads the SVG's intrinsic (authored) dimensions from viewBox or HTML attributes.
     * Never reads clientWidth/clientHeight — those change with zoom.
     * Returns { width, height } or null if dimensions are unreadable.
     */
    function getSvgIntrinsicSize(svg) {
        const vb = svg.viewBox ? svg.viewBox.baseVal : null;
        if (vb && vb.width > 0 && vb.height > 0) {
            return { width: vb.width, height: vb.height };
        }
        const w = parseFloat(svg.getAttribute('width'));
        const h = parseFloat(svg.getAttribute('height'));
        if (w > 0 && h > 0) {
            return { width: w, height: h };
        }
        return null;
    }

    function clampScale(s) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
    }

    // --- SVG natural dimensions (uses clientWidth for transform-stable reading) ---

    function getSvgNaturalSize(svg) {
        const cw = svg.clientWidth;
        const ch = svg.clientHeight;
        if (cw > 0 && ch > 0) {
            return { width: cw, height: ch };
        }
        const intrinsic = getSvgIntrinsicSize(svg);
        if (intrinsic) return intrinsic;
        return { width: 800, height: 600 };
    }

    // --- Toolbar button creation ---

    function createZoomButton(iconPaths, title, onClick) {
        const btn = document.createElement('button');
        btn.className = 'mermaid-export-btn';
        btn.setAttribute('title', title);
        btn.appendChild(core.createSvgIcon(iconPaths));
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            e.preventDefault();
            onClick();
        });
        return btn;
    }

    // Icon paths for zoom buttons
    const ICON_FIT = ['M3 3H7', 'M3 3V7', 'M13 3H9', 'M13 3V7', 'M3 13H7', 'M3 13V9', 'M13 13H9', 'M13 13V9'];
    const ICON_ZOOM_IN = ['M8 4V12', 'M4 8H12'];
    const ICON_ZOOM_OUT = ['M4 8H12'];
    const ICON_ACTUAL = ['M4 4H5', 'M7 4H8', 'M5 5V10', 'M4 10H6', 'M8 10H9', 'M10 4H12', 'M10 10H12', 'M11 4V10'];

    /**
     * Builds zoom toolbar controls (1:1, -, label, +, fit) and inserts them before existing
     * export buttons. Mode-agnostic via the actions callback object.
     */
    function buildZoomControls(state, toolbarEl, actions) {
        const fitBtn = createZoomButton(ICON_FIT, 'Fit to window', actions.fitToWindow);
        const zoomInBtn = createZoomButton(ICON_ZOOM_IN, 'Zoom in', actions.zoomIn);
        const zoomOutBtn = createZoomButton(ICON_ZOOM_OUT, 'Zoom out', actions.zoomOut);
        const actualBtn = createZoomButton(ICON_ACTUAL, '1:1 Actual size', actions.actualSize);

        const label = document.createElement('span');
        label.className = 'mermaid-zoom-label';
        label.textContent = '100%';
        state.zoomLabel = label;

        const divider = document.createElement('span');
        divider.className = 'mermaid-toolbar-divider';

        // Insert zoom controls before existing export buttons — order: 1:1, -, label, +, fit, divider
        const firstChild = toolbarEl.firstChild;
        const items = [actualBtn, zoomOutBtn, label, zoomInBtn, fitBtn, divider];
        for (let i = 0; i < items.length; i++) {
            toolbarEl.insertBefore(items[i], firstChild);
        }
    }

    function updateZoomLabel(state) {
        if (state.zoomLabel) {
            state.zoomLabel.textContent = Math.round(state.scale * 100) + '%';
        }
    }

    // ====== Transform-based Zoom/Pan (shared by both modes) ======

    // --- Scale computation ---

    function computeFitScale(viewport, svg, fitMode) {
        const vw = viewport.clientWidth;
        const vh = viewport.clientHeight;
        const size = getSvgNaturalSize(svg);
        if (size.width <= 0 || size.height <= 0) return 1;
        if (fitMode === 'width') {
            return vw / size.width;
        }
        return Math.min(vw / size.width, vh / size.height);
    }

    // --- Scroll sync helpers ---

    function getScrollableRange(state) {
        const contentHeight = state.contentEl.scrollHeight;
        const viewportHeight = state.viewportEl.clientHeight;
        return Math.max(0, contentHeight - viewportHeight);
    }

    // --- Transform application ---

    function applyTransform(state) {
        state.contentEl.style.transform = 'translate(' + state.panX + 'px, ' + state.panY + 'px) scale(' + state.scale + ')';
        updateZoomLabel(state);
        updatePanCursor(state);
        if (state.scrollSyncCallback && Math.abs(state.scale - 1.0) < 0.01) {
            const range = getScrollableRange(state);
            if (range > 0) {
                const fraction = Math.max(0, Math.min(1, -state.panY / range));
                state.scrollSyncCallback(fraction);
            }
        }
    }

    function updatePanCursor(state) {
        state.viewportEl.classList.add('can-pan');
    }

    // --- Zoom actions ---

    function zoomIn(state) {
        state.scale = clampScale(state.scale * ZOOM_FACTOR);
        state.mode = 'manual';
        applyTransform(state);
    }

    function zoomOut(state) {
        state.scale = clampScale(state.scale / ZOOM_FACTOR);
        state.mode = 'manual';
        applyTransform(state);
    }

    function fitToWindow(state) {
        const svg = state.viewportEl.querySelector('svg');
        if (state.fixedFitScale) {
            state.fitScale = state.fixedFitScale;
        } else if (svg) {
            state.fitScale = computeFitScale(state.viewportEl, svg, state.fitMode);
        }
        state.scale = state.fitScale;
        if (state.fixedFitScale || !svg) {
            state.panX = 0;
            state.panY = 0;
        } else {
            const size = getSvgNaturalSize(svg);
            const vw = state.viewportEl.clientWidth;
            const vh = state.viewportEl.clientHeight;
            const cssOffsetX = state.contentEl.offsetLeft;
            const scaledW = size.width * state.scale;
            const scaledH = size.height * state.scale;
            state.panX = -cssOffsetX + Math.max(0, (vw - scaledW) / 2);
            state.panY = state.fitMode === 'width' ? 0 : Math.max(0, (vh - scaledH) / 2);
        }
        state.mode = 'fit';
        applyTransform(state);
    }

    function actualSize(state) {
        state.scale = 1.0;
        state.panX = 0;
        state.panY = 0;
        state.mode = 'actual';
        applyTransform(state);
    }

    // --- Wheel zoom (zooms at cursor position) ---

    function onWheel(e, state) {
        if (state.wheelRequiresModifier && !e.ctrlKey && !e.metaKey) return;

        if (!state.wheelRequiresModifier) {
            e.preventDefault();
            e.stopPropagation();
        }

        const rect = state.viewportEl.getBoundingClientRect();
        const cursorX = e.clientX - rect.left;
        const cursorY = e.clientY - rect.top;

        const oldScale = state.scale;
        const factor = e.deltaY < 0 ? ZOOM_FACTOR : (1 / ZOOM_FACTOR);
        const newScale = clampScale(oldScale * factor);
        if (newScale === oldScale) return;

        state.panX = cursorX - (cursorX - state.panX) * (newScale / oldScale);
        state.panY = cursorY - (cursorY - state.panY) * (newScale / oldScale);
        state.scale = newScale;
        state.mode = 'manual';
        applyTransform(state);
    }

    // --- Pan (drag) ---

    function onMouseDown(e, state) {
        if (e.button !== 0) return;

        state.isPanning = true;
        state.panStartX = e.clientX - state.panX;
        state.panStartY = e.clientY - state.panY;
        state.viewportEl.classList.add('panning');
        e.preventDefault();
    }

    function onMouseMove(e, state) {
        if (!state.isPanning) return;
        state.panX = e.clientX - state.panStartX;
        state.panY = e.clientY - state.panStartY;
        applyTransform(state);
    }

    function onMouseUp(state) {
        if (!state.isPanning) return;
        state.isPanning = false;
        state.viewportEl.classList.remove('panning');
    }

    // --- Double-click: toggle fit <-> actual size ---

    function onDblClick(state) {
        if (state.mode === 'fit') {
            actualSize(state);
        } else {
            fitToWindow(state);
        }
    }

    // --- Keyboard shortcuts ---

    function onKeyDown(e, state) {
        if (!e.ctrlKey && !e.metaKey) return;

        const key = e.key;
        if (key === '+' || key === '=') {
            e.preventDefault();
            zoomIn(state);
        } else if (key === '-') {
            e.preventDefault();
            zoomOut(state);
        } else if (key === '0') {
            e.preventDefault();
            fitToWindow(state);
        }
    }

    // --- Viewport event binding ---

    function attachViewportListeners(viewport, state) {
        viewport.addEventListener('wheel', function (e) { onWheel(e, state); }, { passive: state.wheelRequiresModifier });
        viewport.addEventListener('mousedown', function (e) { onMouseDown(e, state); });
        viewport.addEventListener('mousemove', function (e) { onMouseMove(e, state); });
        viewport.addEventListener('mouseup', function () { onMouseUp(state); });
        viewport.addEventListener('mouseleave', function () { onMouseUp(state); });
        viewport.addEventListener('dblclick', function () { onDblClick(state); });
    }

    // --- DOM wrapping ---

    function wrapInZoomViewport(shadowRoot) {
        let svgContainer = null;
        const children = shadowRoot.childNodes;
        for (let i = 0; i < children.length; i++) {
            const node = children[i];
            if (node.nodeType === 1 && node.tagName === 'DIV' && !node.classList.contains('mermaid-export-toolbar')) {
                svgContainer = node;
                break;
            }
        }
        if (!svgContainer) return null;

        const viewport = document.createElement('div');
        viewport.className = 'mermaid-zoom-viewport';

        const content = document.createElement('div');
        content.className = 'mermaid-zoom-content';

        content.appendChild(svgContainer);
        viewport.appendChild(content);

        const toolbar = shadowRoot.querySelector('.mermaid-export-toolbar');
        if (toolbar) {
            shadowRoot.insertBefore(viewport, toolbar);
        } else {
            shadowRoot.appendChild(viewport);
        }

        return { viewport: viewport, content: content };
    }

    // ====== Standalone Mode ======

    function initStandaloneZoom(shadowRoot, options) {
        const fitMode = (options && options.fitMode) || 'fit';
        const toolbarEl = (options && options.toolbarEl) || null;
        const wheelRequiresModifier = (options && options.wheelRequiresModifier) || false;
        const enableKeyboard = (options && options.enableKeyboard) || false;
        const constrainSvg = !(options && options.constrainSvg === false);
        const scrollSyncCallback = (options && options.scrollSyncCallback) || null;
        const fixedFitScale = (options && typeof options.fixedFitScale === 'number') ? options.fixedFitScale : null;

        const wrapped = wrapInZoomViewport(shadowRoot);
        if (!wrapped) return;

        if (!constrainSvg) {
            wrapped.viewport.classList.add('unconstrained');
        }

        const svg = wrapped.viewport.querySelector('svg');
        const initialFitScale = fixedFitScale || (svg ? computeFitScale(wrapped.viewport, svg, fitMode) : 1);

        const state = {
            scale: 1,
            fitScale: initialFitScale,
            panX: 0,
            panY: 0,
            mode: 'manual',
            isPanning: false,
            panStartX: 0,
            panStartY: 0,
            viewportEl: wrapped.viewport,
            contentEl: wrapped.content,
            fitMode: fitMode,
            fixedFitScale: fixedFitScale,
            wheelRequiresModifier: wheelRequiresModifier,
            zoomLabel: null,
            resizeObserver: null,
            keydownHandler: null,
            scrollSyncCallback: scrollSyncCallback,
        };

        stateMap.set(shadowRoot, state);

        if (toolbarEl) {
            buildZoomControls(state, toolbarEl, {
                zoomIn: function () { zoomIn(state); },
                zoomOut: function () { zoomOut(state); },
                fitToWindow: function () { fitToWindow(state); },
                actualSize: function () { actualSize(state); }
            });
        }

        applyTransform(state);

        attachViewportListeners(wrapped.viewport, state);

        if (enableKeyboard) {
            state.keydownHandler = function (e) { onKeyDown(e, state); };
            document.addEventListener('keydown', state.keydownHandler);
        }

        state.resizeObserver = setupStandaloneResizeObserver(state);
    }

    function setupStandaloneResizeObserver(state) {
        let rafId = null;
        const observer = new ResizeObserver(function () {
            if (state.mode !== 'fit') return;
            if (rafId) return;
            rafId = requestAnimationFrame(function () {
                rafId = null;
                fitToWindow(state);
            });
        });
        observer.observe(state.viewportEl);
        return observer;
    }

    // ====== Inline Mode ======
    // Uses the SAME transform-based zoom/pan/fit as standalone.
    // Computes explicit viewport height because the host element in Markdown has no defined height
    // (unlike standalone's CSS 100vh). Two-phase sizing:
    //   1. Cap proportional height at 60% of window (prevents diagram from dominating text)
    //   2. After layout, expand if the page has available space (diagram-only fills the window)

    function initInlineZoom(shadowRoot, options) {
        const toolbarEl = (options && options.toolbarEl) || null;
        const wheelRequiresModifier = (options && options.wheelRequiresModifier) || false;

        // Read SVG intrinsic dimensions before wrapping (needed for height computation)
        const tempSvg = shadowRoot.querySelector('svg');
        if (!tempSvg) return;
        const intrinsic = getSvgIntrinsicSize(tempSvg);
        if (!intrinsic) return; // Fallback: no zoom, SVG shown with CSS max-width: 100%

        // Reuse the same viewport/content DOM structure as standalone
        const wrapped = wrapInZoomViewport(shadowRoot);
        if (!wrapped) return;

        // Phase 1: set viewport height from SVG proportions, capped at 60% of window.
        // This ensures the diagram is contained when text surrounds it.
        function computeViewportHeight() {
            const vw = wrapped.viewport.clientWidth;
            if (vw <= 0 || intrinsic.width <= 0) return;
            const proportionalHeight = intrinsic.height * (vw / intrinsic.width);
            const maxHeight = Math.max(300, window.innerHeight * 0.6);
            wrapped.viewport.style.height = Math.min(proportionalHeight, maxHeight) + 'px';
        }

        // Phase 2: expand viewport if the page has unused vertical space.
        // Computes available space from actual DOM measurements — no hardcoded pixel margins.
        function expandIfSpaceAvailable() {
            if (!document.body) return;

            const windowHeight = window.innerHeight;
            const currentHeight = wrapped.viewport.offsetHeight || 0;

            // Actual body margins (not included in scrollHeight)
            const bodyStyle = window.getComputedStyle(document.body);
            const bodyMargins = (parseFloat(bodyStyle.marginTop) || 0)
                              + (parseFloat(bodyStyle.marginBottom) || 0);

            // Total space occupied: body margins + body scroll content (host padding, other elements)
            // Reclaim current viewport height since we may resize it
            const totalUsed = bodyMargins + document.body.scrollHeight - currentHeight;
            const availableHeight = windowHeight - totalUsed - 4; // 4px subpixel safety

            if (availableHeight > currentHeight + 10) {
                wrapped.viewport.style.height = Math.floor(availableHeight) + 'px';
            }
        }

        // Set height synchronously (triggers reflow, giving correct dimensions for fitToWindow)
        computeViewportHeight();

        const state = {
            scale: 1,
            fitScale: 1,
            panX: 0,
            panY: 0,
            mode: 'fit',
            isPanning: false,
            panStartX: 0,
            panStartY: 0,
            viewportEl: wrapped.viewport,
            contentEl: wrapped.content,
            fitMode: 'fit',
            fixedFitScale: null,
            wheelRequiresModifier: wheelRequiresModifier,
            zoomLabel: null,
            resizeObserver: null,
            keydownHandler: null,
            scrollSyncCallback: null,
        };

        stateMap.set(shadowRoot, state);

        // Build toolbar — same actions as standalone
        if (toolbarEl) {
            buildZoomControls(state, toolbarEl, {
                zoomIn: function () { zoomIn(state); },
                zoomOut: function () { zoomOut(state); },
                fitToWindow: function () { fitToWindow(state); },
                actualSize: function () { actualSize(state); }
            });
        }

        // Initial fit (viewport height is set, so fitToWindow works correctly)
        fitToWindow(state);

        // Correction pass: after layout settles, expand if space is available and re-fit
        requestAnimationFrame(function () {
            computeViewportHeight();
            expandIfSpaceAvailable();
            if (state.mode === 'fit') {
                fitToWindow(state);
            }
        });

        // Event listeners — same as standalone, no keyboard shortcuts (avoids editor interference)
        attachViewportListeners(wrapped.viewport, state);

        // ResizeObserver: recompute viewport height and re-fit when host width changes
        state.resizeObserver = setupInlineResizeObserver(state, computeViewportHeight, expandIfSpaceAvailable);
    }

    function setupInlineResizeObserver(state, computeViewportHeight, expandIfSpaceAvailable) {
        let rafId = null;
        let lastWidth = 0;
        const observer = new ResizeObserver(function (entries) {
            // Only react to width changes (height changes are caused by our own resizing)
            const newWidth = entries[0].contentRect.width;
            if (Math.abs(newWidth - lastWidth) < 1) return;
            lastWidth = newWidth;

            if (rafId) return;
            rafId = requestAnimationFrame(function () {
                rafId = null;
                computeViewportHeight();
                expandIfSpaceAvailable();
                if (state.mode === 'fit') {
                    fitToWindow(state);
                }
            });
        });
        observer.observe(state.viewportEl);
        return observer;
    }

    // ====== Public API ======

    window.__initMermaidZoom = function (shadowRoot, options) {
        if (!shadowRoot) return;

        // Tear down previous instance for this shadowRoot (prevents listener accumulation on re-render)
        const prev = stateMap.get(shadowRoot);
        if (prev) {
            if (prev.resizeObserver) prev.resizeObserver.disconnect();
            if (prev.keydownHandler) document.removeEventListener('keydown', prev.keydownHandler);
        }

        if (options && options.layoutMode === 'inline') {
            initInlineZoom(shadowRoot, options);
        } else {
            initStandaloneZoom(shadowRoot, options);
        }
    };

    // Scroll sync: set vertical pan from a 0–1 fraction (only at scale ~1.0, standalone only)
    window.__scrollZoomTo = function (shadowRoot, fraction) {
        const state = stateMap.get(shadowRoot);
        if (!state) return;
        if (Math.abs(state.scale - 1.0) >= 0.01) return;
        const range = getScrollableRange(state);
        if (range <= 0) return;
        fraction = Math.max(0, Math.min(1, fraction));
        state.panY = -fraction * range;
        applyTransform(state);
    };
})();