/**
 * Module `banner`: the render error banner. Layout (styled in mermaid-shadow.css):
 *   div.mermaid-error-banner[data-category] (.mermaid-error-overlay over a kept diagram | .mermaid-error-block)
 *     div.mermaid-error-main > div.mermaid-error-header (label + summary) + div.mermaid-error-actions (buttons)
 *     pre.mermaid-error-raw (collapsed until "Details")
 * No <svg> inside: extractSvg takes the first <svg> of the shadow root.
 */
'use strict';

window.__mermaidCore.define('banner', ['text', 'shadow', 'errors'], function (
    { firstLine },
    { CLASS_ERROR, CLASS_BANNER, CLASS_STALE, setShadowContent },
    { CATEGORY_TITLES }
) {

    function hasRenderedDiagram(shadow) {
        if (!shadow) return false;
        const children = shadow.children;
        for (let i = 0; i < children.length; i++) {
            const el = children[i];
            if (el.tagName === 'STYLE' || el.classList.contains('mermaid-export-toolbar') ||
                el.classList.contains(CLASS_BANNER) || el.classList.contains(CLASS_ERROR)) {
                continue;
            }
            if (el.tagName.toLowerCase() === 'svg' || el.querySelector('svg')) return true;
        }
        return false;
    }

    function createBannerButton(label, onClick) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'mermaid-error-btn';
        btn.textContent = label;
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            e.preventDefault();
            try {
                onClick(btn);
            } catch (err) {
                console.error('[MermaidVisualizer] Error banner action failed:', err);
            }
        });
        return btn;
    }

    function createErrorBanner(info, actions, detailsOpen) {
        const category = Object.prototype.hasOwnProperty.call(CATEGORY_TITLES, info.category) ? info.category : 'generic';
        const raw = typeof info.raw === 'string' ? info.raw : '';

        const banner = document.createElement('div');
        banner.className = CLASS_BANNER;
        banner.setAttribute('data-category', category);
        banner.setAttribute('role', 'alert');

        const main = document.createElement('div');
        main.className = 'mermaid-error-main';

        const header = document.createElement('div');
        header.className = 'mermaid-error-header';
        const title = CATEGORY_TITLES[category];
        const summaryText = info.summary || firstLine(raw) || title;
        const label = document.createElement('span');
        label.className = 'mermaid-error-label';
        const summary = document.createElement('span');
        summary.className = 'mermaid-error-summary';
        if (summaryText.toLowerCase().startsWith(title.toLowerCase())) {
            // The summary already starts with the label: show it once.
            label.classList.add('mermaid-error-label-merged');
            label.textContent = summaryText.slice(0, title.length);
            summary.textContent = summaryText.slice(title.length);
        } else {
            label.textContent = title;
            summary.textContent = summaryText;
        }
        header.appendChild(label);
        header.appendChild(summary);
        main.appendChild(header);

        const rawPre = document.createElement('pre');
        rawPre.className = 'mermaid-error-raw';
        rawPre.textContent = raw;
        rawPre.hidden = !detailsOpen;

        const buttons = document.createElement('div');
        buttons.className = 'mermaid-error-actions';
        if (raw) {
            const details = createBannerButton(detailsOpen ? 'Hide details' : 'Details', function (btn) {
                rawPre.hidden = !rawPre.hidden;
                btn.textContent = rawPre.hidden ? 'Details' : 'Hide details';
                btn.setAttribute('aria-expanded', String(!rawPre.hidden));
            });
            details.setAttribute('aria-expanded', String(!!detailsOpen));
            buttons.appendChild(details);
        }
        if (raw && typeof actions.copyText === 'function') {
            buttons.appendChild(createBannerButton('Copy error', function () { actions.copyText(raw); }));
        }
        if (typeof actions.reportError === 'function') {
            buttons.appendChild(createBannerButton('Report on GitHub', function () {
                actions.reportError(JSON.stringify({
                    raw: raw,
                    summary: info.summary || '',
                    category: category,
                    line: Number.isInteger(info.line) ? info.line : null,
                    column: Number.isInteger(info.column) ? info.column : null
                }));
            }));
        }
        if (buttons.childNodes.length > 0) main.appendChild(buttons);

        banner.appendChild(main);
        if (raw) banner.appendChild(rawPre);
        return banner;
    }

    // Over the kept diagram (untouched: zoom state, toolbar) when the shadow root still shows one, otherwise as a
    // block replacing the content. actions = { copyText?(raw), reportError?(json) }: a button per function given.
    // The next successful render (setShadowContent) removes the banner.
    function showErrorBanner(container, shadowCss, info, isDark, actions) {
        const shadow = container.shadowRoot;
        const previous = shadow ? shadow.querySelector('.' + CLASS_BANNER) : null;
        const previousRaw = previous ? previous.querySelector('.mermaid-error-raw') : null;
        // Keep "Details" expanded while the user keeps typing.
        const detailsOpen = !!(previousRaw && !previousRaw.hidden);
        const banner = createErrorBanner(info || {}, actions || {}, detailsOpen);

        if (hasRenderedDiagram(shadow)) {
            if (previous) previous.remove();
            container.classList.toggle('dark', isDark);
            container.classList.add(CLASS_STALE);
            banner.classList.add('mermaid-error-overlay');
            shadow.appendChild(banner);
        } else {
            banner.classList.add('mermaid-error-block');
            setShadowContent(container, shadowCss, banner, isDark);
        }
        return banner;
    }

    return {
        showErrorBanner: showErrorBanner
    };
});
