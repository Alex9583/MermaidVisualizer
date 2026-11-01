/**
 * Module `errorPositions`: maps positions reported by Mermaid back onto the user's text. Mermaid parses a
 * preprocessed copy (front matter, %%{directives}%% and %% comment lines — with the blank lines before them —
 * removed, then trimStart), so reported lines are too low; the same steps are replayed here keeping each kept
 * character's source offset. The regexes mirror the bundle: re-check them at every Mermaid upgrade.
 */
'use strict';

window.__mermaidCore.define('errorPositions', [], function () {

    // Mirrors the bundle's matchFrontMatter(): indent, '---', body, same indent + '---', trailing whitespace run.
    const FRONTMATTER_PREFIX = /^([ \t]*)---[ \t]*\n[\s\S]*?\n\1---(?:[ \t]*\n)+/;
    // Copied from the bundle (directiveRegex, cleanupComments).
    const MERMAID_DIRECTIVE = /%{2}{\s*(?:(\w+)\s*:|(\w+))\s*(?:(\w+)|((?:(?!}%{2}).|\r?\n)*))?\s*(?:}%{2})?/gi;
    const MERMAID_COMMENT_LINE = /^\s*%%(?!{)[^\n]+\n?/gm;

    function dropRange(model, start, end) {
        return {
            text: model.text.slice(0, start) + model.text.slice(end),
            offsets: model.offsets.slice(0, start).concat(model.offsets.slice(end))
        };
    }

    // One pass: diagrams can hold thousands of comment lines.
    function dropMatches(model, regex) {
        const parts = [];
        const offsets = [];
        const keep = function (from, to) {
            parts.push(model.text.slice(from, to));
            for (let i = from; i < to; i++) offsets.push(model.offsets[i]);
        };
        regex.lastIndex = 0;
        let last = 0;
        let dropped = false;
        let match;
        while ((match = regex.exec(model.text)) !== null) {
            if (match[0].length === 0) {
                regex.lastIndex++;
                continue;
            }
            keep(last, match.index);
            last = match.index + match[0].length;
            dropped = true;
        }
        if (!dropped) return model;
        keep(last, model.text.length);
        return { text: parts.join(''), offsets: offsets };
    }

    // offsets[i] = offset of text[i] in the (line-break normalized) source.
    function preprocessLikeMermaid(normalized) {
        const offsets = new Array(normalized.length);
        for (let i = 0; i < normalized.length; i++) offsets[i] = i;
        let model = { text: normalized, offsets: offsets };
        const frontmatter = FRONTMATTER_PREFIX.exec(model.text);
        if (frontmatter) model = dropRange(model, 0, frontmatter[0].length);
        model = dropMatches(model, MERMAID_DIRECTIVE);
        model = dropMatches(model, MERMAID_COMMENT_LINE);
        const leading = model.text.length - model.text.replace(/^\s+/, '').length;
        return leading > 0 ? dropRange(model, 0, leading) : model;
    }

    // null past the end of the preprocessed text (Mermaid appends a line break: errors at the very end point one line further).
    function sourcePosition(normalized, line, column) {
        const model = preprocessLikeMermaid(normalized);
        let start = 0;
        for (let l = 1; l < line; l++) {
            const lineBreak = model.text.indexOf('\n', start);
            if (lineBreak < 0) return null;
            start = lineBreak + 1;
        }
        if (start >= model.text.length) return null;
        let offset = model.offsets[start];
        if (column !== null) {
            // A column past the end of the line points at its line break, or just after the last character.
            const lineBreak = model.text.indexOf('\n', start);
            const lineEnd = lineBreak < 0 ? model.text.length : lineBreak;
            const index = Math.min(start + column - 1, lineEnd);
            offset = index < model.text.length ? model.offsets[index] : model.offsets[model.text.length - 1] + 1;
        }
        const lineStart = normalized.lastIndexOf('\n', offset - 1) + 1;
        return {
            line: (normalized.slice(0, lineStart).match(/\n/g) || []).length + 1,
            column: column !== null ? offset - lineStart + 1 : null
        };
    }

    // mermaidRelative: the position is relative to Mermaid's preprocessed text (parse errors) rather than to the
    // source (database errors). Returns { line, column, text of the source line | null }.
    function mapPosition(line, column, context, mermaidRelative) {
        if (!Number.isInteger(line) || line < 1) return { line: null, column: null, text: null };
        let l = line;
        let c = Number.isInteger(column) && column >= 1 ? column : null;
        let text = null;
        const source = context && typeof context.source === 'string' ? context.source : null;
        if (source !== null) {
            const normalized = source.replace(/\r\n?/g, '\n');
            const lines = normalized.split('\n');
            if (mermaidRelative) {
                const position = sourcePosition(normalized, l, c);
                if (position) {
                    l = position.line;
                    c = position.column;
                } else {
                    l = lines.length;
                    c = null;
                }
            }
            if (l > lines.length) {
                l = lines.length;
                c = null;
            }
            text = lines[l - 1];
            if (c !== null && c > text.length + 1) c = null;
        }
        const offset = context && Number.isInteger(context.lineOffset) && context.lineOffset > 0 ? context.lineOffset : 0;
        return { line: l + offset, column: c, text: text };
    }

    // Mermaid needs the front matter at the very start: trim, and remember the leading lines removed.
    function prepareSource(text) {
        const value = typeof text === 'string' ? text : '';
        const start = value.length - value.replace(/^\s+/, '').length;
        const leading = value.slice(0, start);
        return {
            source: value.trim(),
            lineOffset: (leading.match(/\r\n?|\n/g) || []).length
        };
    }

    return {
        mapPosition: mapPosition,
        prepareSource: prepareSource
    };
});
