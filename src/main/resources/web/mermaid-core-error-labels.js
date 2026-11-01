/**
 * Module `errorLabels`: vocabulary of the one-line summaries — Jison terminal names to words, ordering of the
 * "expected" alternatives, and the three summary shapes (unfinished / syntax / unrecognized).
 */
'use strict';

window.__mermaidCore.define('errorLabels', ['text'], function ({ truncate, truncateStart }) {

    // Jison reports its $end symbol by number when it is missing from terminals_ ("got '1'").
    const JISON_END_SYMBOL = '1';
    const MAX_EXPECTED_ITEMS = 4;
    // null = never listed (Jison's error-recovery terminal); names missing here and in TOKEN_LABEL_PATTERNS are shown quoted.
    const JISON_TOKEN_LABELS = Object.freeze({
        EOF: 'end of input',
        NEWLINE: 'a line break',
        NL: 'a line break',
        SPACE: 'a space',
        SEMI: ';',
        COLON: ':',
        COMMA: ',',
        PIPE: '|',
        AMP: '&',
        MINUS: '-',
        DOT: '.',
        SQS: '[',
        SQE: ']',
        PS: '(',
        PE: ')',
        DIAMOND_START: '{',
        DIAMOND_STOP: '}',
        STRUCT_START: '{',
        STRUCT_STOP: '}',
        BLOCK_START: '{',
        BLOCK_STOP: '}',
        TAGSTART: '<',
        TAGEND: '>',
        LINK: 'a link (-->)',
        START_LINK: 'a link (--)',
        ARROW_POINT: 'an arrow (-->)',
        ARROW_CIRCLE: 'a circle arrow (--o)',
        ARROW_CROSS: 'a cross arrow (--x)',
        ARROW_OPEN: 'an open link (---)',
        TXT: 'text',
        TEXT: 'text',
        UNICODE_TEXT: 'text',
        EDGE_TEXT: 'text',
        STR: 'a quoted string',
        STRING: 'a quoted string',
        MD_STR: 'a Markdown string',
        NODE_STRING: 'a node id',
        ID: 'an id',
        ACTOR: 'a participant name',
        ENTITY_NAME: 'an entity name',
        ATTRIBUTE_WORD: 'an attribute',
        EDGE_STATE: '[*]',
        ALPHA: 'a letter',
        NUM: 'a number',
        INT: 'a number',
        DIR: 'a direction (TB, LR...)',
        SHAPE_DATA: 'shape data (@{ ... })',
        end: '"end"',
        subgraph: '"subgraph"',
        title: '"title"',
        section: '"section"',
        acc_title: '"accTitle"',
        acc_descr: '"accDescr"',
        acc_descr_multiline_value: '"accDescr"',
        STYLE: '"style"',
        LINKSTYLE: '"linkStyle"',
        CLASS: '"class"',
        CLASSDEF: '"classDef"',
        CLICK: '"click"',
        HREF: '"href"',
        DEFAULT: '"default"',
        error: null
    });

    // Families collapsed into one label (the sequence parser alone has ~25 arrow terminals).
    const TOKEN_LABEL_PATTERNS = [
        { pattern: /ARROW|^(SOLID|DOTTED)_(CROSS|POINT|OPEN)$/, label: 'an arrow' },
        { pattern: /^direction_/, label: 'a direction (TB, LR...)' },
        { pattern: /^NUMBER|_NUM$/, label: 'a number' }
    ];

    // First, in this order: what the user most likely has to type next.
    const HIGH_PRIORITY_LABELS = ['a node id', 'a participant name', 'an id', 'an arrow', 'a link (-->)', 'a line break', 'text', 'a quoted string'];
    // Last: valid but rarely what is missing.
    const LOW_PRIORITY_LABELS = ['a space'];
    const BRACKET_CLOSERS = { '[': ']', '(': ')', '{': '}' };

    function unquoteTerminal(name) {
        const s = String(name);
        return s.length >= 2 && s[0] === "'" && s[s.length - 1] === "'" ? s.slice(1, -1) : s;
    }

    function normalizeToken(token) {
        if (token === null || token === undefined || token === '') return null;
        const s = String(token);
        return s === JISON_END_SYMBOL || s === '$end' ? 'EOF' : s;
    }

    // string | null (never shown) | undefined (unknown terminal)
    function tokenLabel(name) {
        if (Object.prototype.hasOwnProperty.call(JISON_TOKEN_LABELS, name)) return JISON_TOKEN_LABELS[name];
        for (let i = 0; i < TOKEN_LABEL_PATTERNS.length; i++) {
            if (TOKEN_LABEL_PATTERNS[i].pattern.test(name)) return TOKEN_LABEL_PATTERNS[i].label;
        }
        return undefined;
    }

    // Innermost bracket left open before the error: "A[Start" → "]".
    function expectedCloser(text) {
        if (!text) return null;
        const stack = [];
        for (const ch of text) {
            if (BRACKET_CLOSERS[ch]) {
                stack.push(BRACKET_CLOSERS[ch]);
            } else if ((ch === ']' || ch === ')' || ch === '}') && stack[stack.length - 1] === ch) {
                stack.pop();
            }
        }
        return stack.length > 0 ? stack[stack.length - 1] : null;
    }

    // Order: the bracket left open (or `end` at end of input), HIGH_PRIORITY_LABELS, the other known labels,
    // LOW_PRIORITY_LABELS, unknown names quoted; each group in Mermaid's order.
    function describeExpected(names, contextBefore, receivedToken) {
        const high = [];
        const known = [];
        const lowPriority = [];
        const unknown = [];
        const seen = new Set();
        for (let i = 0; i < names.length; i++) {
            const name = names[i];
            if (receivedToken === 'EOF' && name === 'EOF') continue;
            const label = tokenLabel(name);
            if (label === null) continue;
            const text = label === undefined ? '"' + name + '"' : label;
            if (seen.has(text)) continue;
            seen.add(text);
            if (label === undefined) unknown.push(text);
            else if (HIGH_PRIORITY_LABELS.indexOf(text) >= 0) high.push(text);
            else if (LOW_PRIORITY_LABELS.indexOf(text) >= 0) lowPriority.push(text);
            else known.push(text);
        }
        high.sort(function (a, b) { return HIGH_PRIORITY_LABELS.indexOf(a) - HIGH_PRIORITY_LABELS.indexOf(b); });
        const items = high.concat(known, lowPriority, unknown);
        const preferred = [];
        const closer = expectedCloser(contextBefore);
        if (closer) preferred.push(closer);
        if (receivedToken === 'EOF') preferred.push(JISON_TOKEN_LABELS.end);
        for (let i = preferred.length - 1; i >= 0; i--) {
            const index = items.indexOf(preferred[i]);
            if (index > 0) {
                items.splice(index, 1);
                items.unshift(preferred[i]);
            }
        }
        return items;
    }

    // "a, b, c or d"; beyond MAX_EXPECTED_ITEMS: "a, b, c, d or …"
    function formatAlternatives(items) {
        if (items.length === 0) return '';
        if (items.length > MAX_EXPECTED_ITEMS) {
            return items.slice(0, MAX_EXPECTED_ITEMS).join(', ') + ' or \u2026';
        }
        if (items.length === 1) return items[0];
        return items.slice(0, -1).join(', ') + ' or ' + items[items.length - 1];
    }

    function describeReceived(token, text) {
        const visible = typeof text === 'string' ? text.trim() : '';
        if (visible) return '"' + truncate(visible, 30) + '"';
        if (!token) return 'input';
        const label = tokenLabel(token);
        if (!label) return '"' + token + '"';
        if (!/[A-Za-z]/.test(label)) return '"' + label + '"';
        return label.replace(/^an? /, '');
    }

    // What the user was typing when the input stopped, quoted in "looks unfinished" summaries.
    function unfinishedSnippet(contextBefore, lineText) {
        const squash = function (s) { return s.replace(/\s+/g, ''); };
        // contextBefore has no line breaks: prefer the source line when it is part of what Mermaid read.
        if (typeof lineText === 'string' && lineText.trim()) {
            const line = lineText.trim();
            const tail = squash(line).slice(-12);
            if (tail && squash(contextBefore || '').includes(tail)) return truncateStart(line, 40);
        }
        if (!contextBefore) return null;
        // Without line breaks, the last run of 2+ spaces (indentation) is the most likely line boundary.
        const parts = contextBefore.split(/\s{2,}/);
        const last = parts[parts.length - 1].trim();
        return last ? truncateStart(last, 40) : null;
    }

    function unfinishedSummary(line, expectedText, snippet) {
        let summary = line !== null ? 'Line ' + line + ' looks unfinished' : 'The diagram looks unfinished';
        if (expectedText) summary += ': expected ' + expectedText;
        if (snippet) summary += ' after "' + snippet + '"';
        return summary;
    }

    function syntaxSummary(line, receivedText, expectedText) {
        let summary = 'Syntax error' + (line !== null ? ' at line ' + line : '') + ': unexpected ' + receivedText;
        if (expectedText) summary += ' (expected ' + expectedText + ')';
        return summary;
    }

    function unrecognizedSummary(line, near) {
        let summary = 'Unrecognized text' + (line !== null ? ' at line ' + line : '');
        if (near) summary += ' near "' + near + '"';
        return summary;
    }

    return {
        JISON_TOKEN_LABELS: JISON_TOKEN_LABELS,
        unquoteTerminal: unquoteTerminal,
        normalizeToken: normalizeToken,
        describeExpected: describeExpected,
        formatAlternatives: formatAlternatives,
        describeReceived: describeReceived,
        unfinishedSnippet: unfinishedSnippet,
        unfinishedSummary: unfinishedSummary,
        syntaxSummary: syntaxSummary,
        unrecognizedSummary: unrecognizedSummary
    };
});
