// Shared zoom, pan & fit-to-window module for Mermaid diagrams.
// Used by both standalone editor (mermaid-standalone.js) and Markdown preview (mermaid-render.js).
// Initialized per shadow root via window.__initMermaidZoom(shadowRoot, options).
(function () {
    'use strict';

    const SVG_NS = 'http://www.w3.org/2000/svg';
    const MIN_SCALE = 0.1;
    const MAX_SCALE = 5.0;
    const ZOOM_FACTOR = 1.25;

    // Per-shadowRoot state stored in a WeakMap
    const stateMap = new WeakMap();

    // --- SVG natural dimensions ---

    function getSvgNaturalSize(svg) {
        // Prefer rendered dimensions (respects CSS constraints like max-width: 100%)
        const rect = svg.getBoundingClientRect();
        if (rect.width > 0 && rect.height > 0) {
            return { width: rect.width, height: rect.height };
        }
        // Fallback: viewBox or explicit attributes
        const vb = svg.viewBox ? svg.viewBox.baseVal : null;
        if (vb && vb.width > 0 && vb.height > 0) {
            return { width: vb.width, height: vb.height };
        }
        const w = parseFloat(svg.getAttribute('width'));
        const h = parseFloat(svg.getAttribute('height'));
        if (w > 0 && h > 0) {
            return { width: w, height: h };
        }
        return { width: 800, height: 600 };
    }

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

    function clampScale(s) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
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

    function updateZoomLabel(state) {
        if (state.zoomLabel) {
            state.zoomLabel.textContent = Math.round(state.scale * 100) + '%';
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
        if (svg) {
            state.fitScale = computeFitScale(state.viewportEl, svg, state.fitMode);
        }
        state.scale = state.fitScale;
        state.panX = 0;
        state.panY = 0;
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

        // Adjust pan so the point under the cursor remains fixed
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

    // --- Keyboard shortcuts (standalone only) ---

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

    // --- ResizeObserver ---

    function setupResizeObserver(state) {
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

    // --- Toolbar button creation ---

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

    function createZoomButton(iconPaths, title, onClick) {
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

    // Icon paths for zoom buttons
    const ICON_FIT = ['M3 3H7', 'M3 3V7', 'M13 3H9', 'M13 3V7', 'M3 13H7', 'M3 13V9', 'M13 13H9', 'M13 13V9'];
    const ICON_ZOOM_IN = ['M8 4V12', 'M4 8H12'];
    const ICON_ZOOM_OUT = ['M4 8H12'];
    const ICON_ACTUAL = ['M4 4H5', 'M7 4H8', 'M5 5V10', 'M4 10H6', 'M8 10H9', 'M10 4H12', 'M10 10H12', 'M11 4V10'];

    function buildZoomControls(state, toolbarEl) {
        const fitBtn = createZoomButton(ICON_FIT, 'Fit to window', function () { fitToWindow(state); });
        const zoomInBtn = createZoomButton(ICON_ZOOM_IN, 'Zoom in', function () { zoomIn(state); });
        const zoomOutBtn = createZoomButton(ICON_ZOOM_OUT, 'Zoom out', function () { zoomOut(state); });
        const actualBtn = createZoomButton(ICON_ACTUAL, '1:1 Actual size', function () { actualSize(state); });

        const label = document.createElement('span');
        label.className = 'mermaid-zoom-label';
        label.textContent = '100%';
        state.zoomLabel = label;

        const divider = document.createElement('span');
        divider.className = 'mermaid-toolbar-divider';

        // Insert zoom controls before existing export buttons
        const firstChild = toolbarEl.firstChild;
        // Each item is inserted before the same stable reference, producing forward order: fit, +, label, -, 1:1, divider
        const items = [fitBtn, zoomInBtn, label, zoomOutBtn, actualBtn, divider];
        for (let i = 0; i < items.length; i++) {
            toolbarEl.insertBefore(items[i], firstChild);
        }
    }

    // --- DOM wrapping ---

    function wrapInZoomViewport(shadowRoot) {
        // Find the SVG container div (first div that is not the toolbar)
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

        // Move SVG container into content wrapper
        content.appendChild(svgContainer);
        viewport.appendChild(content);

        // Insert viewport before toolbar (or at end of shadow)
        const toolbar = shadowRoot.querySelector('.mermaid-export-toolbar');
        if (toolbar) {
            shadowRoot.insertBefore(viewport, toolbar);
        } else {
            shadowRoot.appendChild(viewport);
        }

        return { viewport: viewport, content: content };
    }

    // --- Public API ---

    window.__initMermaidZoom = function (shadowRoot, options) {
        if (!shadowRoot) return;

        // Tear down previous instance for this shadowRoot (prevents listener accumulation on re-render)
        const prev = stateMap.get(shadowRoot);
        if (prev) {
            if (prev.resizeObserver) prev.resizeObserver.disconnect();
            if (prev.keydownHandler) document.removeEventListener('keydown', prev.keydownHandler);
        }

        const fitMode = (options && options.fitMode) || 'fit';
        const toolbarEl = (options && options.toolbarEl) || null;
        const wheelRequiresModifier = (options && options.wheelRequiresModifier) || false;
        const enableKeyboard = (options && options.enableKeyboard) || false;
        const constrainSvg = !(options && options.constrainSvg === false);
        const scrollSyncCallback = (options && options.scrollSyncCallback) || null;

        const wrapped = wrapInZoomViewport(shadowRoot);
        if (!wrapped) return;

        if (!constrainSvg) {
            wrapped.viewport.classList.add('unconstrained');
        }

        const svg = wrapped.viewport.querySelector('svg');
        const initialFitScale = svg ? computeFitScale(wrapped.viewport, svg, fitMode) : 1;
        const initialScale = fitMode === 'fit' ? initialFitScale : 1;

        const state = {
            scale: initialScale,
            fitScale: initialFitScale,
            panX: 0,
            panY: 0,
            mode: fitMode === 'fit' ? 'fit' : 'manual',
            isPanning: false,
            panStartX: 0,
            panStartY: 0,
            viewportEl: wrapped.viewport,
            contentEl: wrapped.content,
            fitMode: fitMode,
            wheelRequiresModifier: wheelRequiresModifier,
            zoomLabel: null,
            resizeObserver: null,
            keydownHandler: null,
            scrollSyncCallback: scrollSyncCallback,
        };

        stateMap.set(shadowRoot, state);

        // Build toolbar zoom controls
        if (toolbarEl) {
            buildZoomControls(state, toolbarEl);
        }

        // Apply initial transform
        applyTransform(state);

        // Event listeners (viewport listeners are auto-cleaned when DOM is cleared on re-render)
        wrapped.viewport.addEventListener('wheel', function (e) { onWheel(e, state); }, { passive: wheelRequiresModifier });
        wrapped.viewport.addEventListener('mousedown', function (e) { onMouseDown(e, state); });
        wrapped.viewport.addEventListener('mousemove', function (e) { onMouseMove(e, state); });
        wrapped.viewport.addEventListener('mouseup', function () { onMouseUp(state); });
        wrapped.viewport.addEventListener('mouseleave', function () { onMouseUp(state); });
        wrapped.viewport.addEventListener('dblclick', function () { onDblClick(state); });

        // Keyboard (standalone only) — stored for cleanup on re-init
        if (enableKeyboard) {
            state.keydownHandler = function (e) { onKeyDown(e, state); };
            document.addEventListener('keydown', state.keydownHandler);
        }

        // ResizeObserver for responsive fit — stored for cleanup on re-init
        state.resizeObserver = setupResizeObserver(state);
    };

    // Scroll sync: set vertical pan from a 0–1 fraction (only at scale ~1.0)
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