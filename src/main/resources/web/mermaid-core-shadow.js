/** Module `shadow`: diagram host + Shadow DOM (isolates Mermaid's <style>), SVG icons, plain error box, link clicks. */
'use strict';

window.__mermaidCore.define('shadow', [], function () {

    const CLASS_ERROR = 'mermaid-error';
    const CLASS_BANNER = 'mermaid-error-banner';
    // On the host while a banner covers the kept diagram (dims it).
    const CLASS_STALE = 'mermaid-stale';
    const SVG_NS = 'http://www.w3.org/2000/svg';

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

    function setShadowContent(el, shadowCss, contentEl, isDark) {
        const shadow = el.shadowRoot || el.attachShadow({ mode: 'open' });
        el.classList.toggle('dark', isDark);
        el.classList.remove(CLASS_STALE);
        shadow.textContent = '';
        if (shadowCss) {
            const style = document.createElement('style');
            style.textContent = shadowCss;
            shadow.appendChild(style);
        }
        shadow.appendChild(contentEl);
    }

    // innerHTML is unavoidable for Mermaid's SVG; it is sanitized by Mermaid's DOMPurify (securityLevel 'strict')
    // and isolated by the shadow root.
    function injectSvg(container, shadowCss, svgString, isDark) {
        const svgContainer = document.createElement('div');
        svgContainer.innerHTML = svgString; // eslint-disable-line no-unsanitized/property — sanitized by Mermaid DOMPurify + Shadow DOM isolation
        setShadowContent(container, shadowCss, svgContainer, isDark);
    }

    function clearShadowContent(el) {
        if (!el) return;
        el.classList.remove(CLASS_STALE);
        if (el.shadowRoot) el.shadowRoot.textContent = '';
    }

    // mermaid.render() leaves an element with the render id in the document when it fails.
    function removeRenderLeftover(renderId) {
        if (!renderId) return;
        const leftover = document.getElementById(renderId);
        if (leftover) leftover.remove();
    }

    // Plain error box for failures outside a render (mermaid.initialize at page load); render errors use the banner.
    function showError(container, shadowCss, message, renderId, isDark) {
        const errorPre = document.createElement('pre');
        errorPre.className = CLASS_ERROR;
        errorPre.textContent = message;
        setShadowContent(container, shadowCss, errorPre, isDark);
        removeRenderLeftover(renderId);
    }

    // Clicks inside a shadow root retarget to the host: document-level link interceptors never see them.
    // Shadow roots persist across re-renders — install once so listeners don't stack.
    const linkHandledRoots = new WeakSet();

    function findAnchorHref(anchor) {
        // Raw attribute first — the resolved property absolutizes relative hrefs against the page origin.
        const raw = anchor.getAttribute('href') || anchor.getAttribute('xlink:href');
        if (raw) return raw;
        if (typeof anchor.href === 'string') return anchor.href;
        return (anchor.href && anchor.href.baseVal) || '';
    }

    function isHttpUrl(url) {
        return /^https?:\/\//i.test(url);
    }

    function installLinkHandler(shadowRoot, openFn) {
        if (!shadowRoot || linkHandledRoots.has(shadowRoot)) return;
        linkHandledRoots.add(shadowRoot);

        let mouseDownPos = null;

        shadowRoot.addEventListener('mousedown', function (e) {
            if (e.button === 0) mouseDownPos = { x: e.clientX, y: e.clientY };
        });

        shadowRoot.addEventListener('click', function (e) {
            const anchor = e.target && e.target.closest ? e.target.closest('a') : null;
            if (!anchor) {
                mouseDownPos = null;
                return;
            }
            e.preventDefault();
            e.stopPropagation();

            const downPos = mouseDownPos;
            mouseDownPos = null;

            if (e.detail > 1) return; // double-click: the first click already opened
            // Swallow clicks ending a pan drag; keyboard activation has detail 0.
            if (e.detail !== 0 && downPos) {
                const dist = Math.hypot(e.clientX - downPos.x, e.clientY - downPos.y);
                if (dist > 5) return;
            }

            const url = findAnchorHref(anchor).trim();
            if (!isHttpUrl(url)) return;
            openFn(url);
        });

        // Block middle-click popups (they bypass the click path).
        shadowRoot.addEventListener('auxclick', function (e) {
            if (e.target && e.target.closest && e.target.closest('a')) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    }

    return {
        CLASS_ERROR: CLASS_ERROR,
        CLASS_BANNER: CLASS_BANNER,
        CLASS_STALE: CLASS_STALE,
        SVG_NS: SVG_NS,
        createSvgIcon: createSvgIcon,
        setShadowContent: setShadowContent,
        injectSvg: injectSvg,
        clearShadowContent: clearShadowContent,
        removeRenderLeftover: removeRenderLeftover,
        showError: showError,
        installLinkHandler: installLinkHandler
    };
});
