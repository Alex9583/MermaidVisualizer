/**
 * Module `errors`: classifyRenderError() turns whatever mermaid.render() threw into a category and a one-line
 * summary (limits, UnknownDiagramError, Langium messages, Jison parse/lexical errors, generic). Pure functions.
 */
'use strict';

window.__mermaidCore.define('errors', ['text', 'runtime', 'errorPositions', 'errorLabels'], function (
    { firstLine, truncate },
    { describeRenderError, isTextSizeMessage, isEdgeLimitMessage },
    { mapPosition },
    { unquoteTerminal, normalizeToken, describeExpected, formatAlternatives, describeReceived,
      unfinishedSnippet, unfinishedSummary, syntaxSummary, unrecognizedSummary }
) {

    const CATEGORY_TITLES = {
        incomplete: 'Incomplete diagram',
        syntax: 'Syntax error',
        'unknown-diagram': 'Unknown diagram type',
        limit: 'Rendering limit',
        generic: 'Render error'
    };

    const UNKNOWN_DIAGRAM_TEXT = /No diagram type detected matching given configuration for text:\s*([\s\S]*)$/;
    const DIAGRAM_NOT_FOUND = /Diagram (\S+) not found/;
    // Jison (showPosition): "Parse error on line N:\n<pastInput><upcomingInput>\n<dashes>^\nExpecting 'A', 'B', got 'X'"
    // and "Lexical error on line N. Unrecognized text.\n<past><upcoming>\n<dashes>^"; past/upcoming = 20 chars each,
    // "..." when cut, line breaks removed.
    const JISON_PARSE_POSITION = /Parse error on line (\d+):\n([^\n]*)\n(-*)\^(?:\nExpecting ([^\n]*), got '([^\n]*)')?/;
    const JISON_PARSE_SHORT = /Parse error on line (\d+): Unexpected (?:end of input|'([^\n]*)')/;
    const JISON_LEXICAL = /Lexical error on line (\d+)\. Unrecognized text\.(?:\n([^\n]*)\n(-*)\^)?/;
    // Langium (pie, gitGraph, packet, architecture…): "Parsing failed: Lexer error on line L, column C: unexpected
    // character: ->x<- …" / "Parse error on line L, column C: Expecting token of type 'T' but found `x`."
    const LANGIUM_FAILURE = /^Parsing failed:/;
    const LANGIUM_ERROR = /(Lexer|Parse) error on line (\d+), column (\d+): /;
    const LANGIUM_LEXER_CHAR = /^unexpected character: ->([\s\S]*?)<-/;
    const LANGIUM_EXPECTING_ONE = /^Expecting token of type '([^']*)' but found `([\s\S]*?)`/;
    const LANGIUM_EXPECTING_SEQUENCES = /^Expecting: one of these possible Token sequences:\n([\s\S]*?)\nbut found: '([\s\S]*?)'/;
    const END_OF_LINE_TOKENS = ['EOF', 'NEWLINE', 'NL'];

    function errorMessage(err) {
        if (typeof err === 'string') return err;
        if (err && typeof err.message === 'string' && err.message) return err.message;
        if (err === null || err === undefined) return 'Unknown error';
        return String(err);
    }

    function splitPositionContext(snippet, caretColumn) {
        let before = snippet.slice(0, caretColumn);
        let after = snippet.slice(caretColumn);
        if (before.startsWith('...')) before = before.slice(3);
        if (after.endsWith('...')) after = after.slice(0, -3);
        return { before: before, after: after };
    }

    // loc lines are 1-based, hash.line is 0-based.
    function jisonHashPosition(hash, token) {
        const loc = hash.loc;
        if (loc && Number.isInteger(loc.first_line) && loc.first_line >= 1) {
            // loc is the last accepted token. When it swallowed a line break, the offending token sits on the next
            // line — except at end of input, where the unfinished construct is where that token starts.
            const endsWithLineBreak = Number.isInteger(loc.last_line) && loc.last_line > loc.first_line && loc.last_column === 0;
            if (endsWithLineBreak && token !== 'EOF') return { line: loc.last_line, column: null };
            return { line: loc.first_line, column: Number.isInteger(loc.first_column) ? loc.first_column + 1 : null };
        }
        if (Number.isInteger(hash.line)) return { line: hash.line + 1, column: null };
        return null;
    }

    function classifyJisonParse(err, raw, context, info) {
        const hash = err && typeof err === 'object' && err.hash && typeof err.hash === 'object' ? err.hash : null;
        const positioned = JISON_PARSE_POSITION.exec(raw);
        const short = positioned ? null : JISON_PARSE_SHORT.exec(raw);
        if (!positioned && !short) return false;

        let token = null;
        let expected = [];
        let line = parseInt((positioned || short)[1], 10);
        let column = null;
        if (positioned) {
            const ctx = splitPositionContext(positioned[2], positioned[3].length);
            info.contextBefore = ctx.before;
            info.contextAfter = ctx.after;
            if (positioned[4] !== undefined) {
                expected = (positioned[4].match(/'[^']*'/g) || []).map(unquoteTerminal);
                token = normalizeToken(positioned[5]);
            }
        } else {
            token = short[2] === undefined ? 'EOF' : normalizeToken(short[2]);
        }
        if (hash) {
            if (hash.token !== undefined && hash.token !== null) token = normalizeToken(hash.token);
            if (Array.isArray(hash.expected)) expected = hash.expected.map(unquoteTerminal);
            const position = jisonHashPosition(hash, token);
            if (position) {
                line = position.line;
                column = position.column;
            }
        }

        const mapped = mapPosition(line, column, context, true);
        info.line = mapped.line;
        info.column = mapped.column;
        info.token = token;
        info.expected = expected;

        const expectedText = formatAlternatives(describeExpected(expected, info.contextBefore, token));
        if (token !== null && END_OF_LINE_TOKENS.indexOf(token) >= 0) {
            info.category = 'incomplete';
            info.summary = unfinishedSummary(info.line, expectedText, unfinishedSnippet(info.contextBefore, mapped.text));
        } else {
            info.category = 'syntax';
            const text = hash && typeof hash.text === 'string' ? hash.text : null;
            info.summary = syntaxSummary(info.line, describeReceived(token, text), expectedText);
        }
        return true;
    }

    function classifyJisonLexical(err, raw, context, info) {
        const match = JISON_LEXICAL.exec(raw);
        if (!match) return false;
        const hash = err && typeof err === 'object' && err.hash && typeof err.hash === 'object' ? err.hash : null;
        let line = parseInt(match[1], 10);
        if (hash && Number.isInteger(hash.line)) line = hash.line + 1;
        if (match[2] !== undefined) {
            const ctx = splitPositionContext(match[2], match[3].length);
            info.contextBefore = ctx.before;
            info.contextAfter = ctx.after;
        }
        const mapped = mapPosition(line, null, context, true);
        info.line = mapped.line;
        if (info.contextAfter === '') {
            // Nothing after the unrecognized spot: a token being typed.
            info.category = 'incomplete';
            info.summary = unfinishedSummary(info.line, '', unfinishedSnippet(info.contextBefore, mapped.text));
        } else {
            info.category = 'syntax';
            const near = info.contextAfter ? truncate(info.contextAfter.trim(), 20) : null;
            info.summary = unrecognizedSummary(info.line, near);
        }
        return true;
    }

    function classifyLangium(raw, context, info) {
        if (!LANGIUM_FAILURE.test(raw)) return false;
        const match = LANGIUM_ERROR.exec(raw);
        if (!match) return false;
        const mapped = mapPosition(parseInt(match[2], 10), parseInt(match[3], 10), context, true);
        info.line = mapped.line;
        info.column = mapped.column;
        const rest = raw.slice(match.index + match[0].length);

        if (match[1] === 'Lexer') {
            const ch = LANGIUM_LEXER_CHAR.exec(rest);
            info.category = 'syntax';
            info.summary = unrecognizedSummary(info.line, ch && ch[1].trim() ? truncate(ch[1].trim(), 20) : null);
            return true;
        }

        let found = null;
        const one = LANGIUM_EXPECTING_ONE.exec(rest);
        const sequences = one ? null : LANGIUM_EXPECTING_SEQUENCES.exec(rest);
        if (one) {
            info.expected = [one[1]];
            found = one[2];
        } else if (sequences) {
            info.expected = (sequences[1].match(/\[([^\]]*)\]/g) || [])
                .map(function (s) { return s.slice(1, -1).split(',')[0].trim(); })
                .filter(function (s) { return s.length > 0; });
            found = sequences[2];
        }
        const expectedText = formatAlternatives(describeExpected(info.expected, null, null));
        if (found === null || found.trim() === '') {
            info.category = 'incomplete';
            info.summary = unfinishedSummary(info.line, expectedText, null);
        } else {
            info.category = 'syntax';
            info.summary = syntaxSummary(info.line, describeReceived(null, found), expectedText);
        }
        return true;
    }

    /**
     * context (optional) = { source: exact text given to mermaid.render(), lineOffset: lines removed before it }:
     * with it, line/column are relative to the caller's document. Returns { category: incomplete|syntax|
     * unknown-diagram|limit|generic, summary, raw (untouched message), line, column (1-based | null),
     * token, expected (unquoted Jison terminals), contextBefore, contextAfter }.
     */
    function classifyRenderError(err, context) {
        const raw = errorMessage(err);
        const info = {
            category: 'generic',
            summary: '',
            raw: raw,
            line: null,
            column: null,
            token: null,
            expected: [],
            contextBefore: null,
            contextAfter: null
        };

        if (isTextSizeMessage(raw)) {
            info.category = 'limit';
            info.summary = raw;
            return info;
        }
        if (isEdgeLimitMessage(raw)) {
            info.category = 'limit';
            info.summary = describeRenderError(err);
            return info;
        }

        const unknownText = UNKNOWN_DIAGRAM_TEXT.exec(raw);
        const notFound = unknownText ? null : DIAGRAM_NOT_FOUND.exec(raw);
        if ((err && err.name === 'UnknownDiagramError') || unknownText || notFound) {
            let name = null;
            if (unknownText) name = unknownText[1].trim().split(/\s+/)[0] || null;
            else if (notFound) name = notFound[1];
            info.category = 'unknown-diagram';
            info.summary = name
                ? 'Unknown diagram type "' + truncate(name, 40) + '": check the first line of the diagram'
                : 'Unknown diagram type: check the first line of the diagram';
            return info;
        }

        if (classifyLangium(raw, context, info)) return info;
        if (classifyJisonParse(err, raw, context, info)) return info;
        if (classifyJisonLexical(err, raw, context, info)) return info;

        info.summary = truncate(firstLine(raw), 200) || CATEGORY_TITLES.generic;
        const loc = err && typeof err === 'object' && err.hash && err.hash.loc;
        if (loc && Number.isInteger(loc.first_line)) {
            // Database errors (e.g. unknown shape) are positioned on the source, front matter included.
            const mapped = mapPosition(loc.first_line, Number.isInteger(loc.first_column) ? loc.first_column + 1 : null, context, false);
            info.line = mapped.line;
            info.column = mapped.column;
        } else {
            const lineMatch = raw.match(/on line (\d+)/i) || raw.match(/\bline (\d+)/i);
            const columnMatch = raw.match(/\bcolumn (\d+)/i);
            const mapped = mapPosition(
                lineMatch ? parseInt(lineMatch[1], 10) : null,
                columnMatch ? parseInt(columnMatch[1], 10) : null,
                context, false);
            info.line = mapped.line;
            info.column = mapped.column;
        }
        return info;
    }

    // Payload of the Kotlin error bridge (the standalone adapter adds `gen`).
    function toRenderErrorPayload(classified) {
        return {
            status: 'error',
            message: classified.raw,
            summary: classified.summary,
            category: classified.category,
            line: classified.line,
            column: classified.column
        };
    }

    function parseErrorInfo(err, context) {
        return toRenderErrorPayload(classifyRenderError(err, context));
    }

    return {
        CATEGORY_TITLES: CATEGORY_TITLES,
        classifyRenderError: classifyRenderError,
        toRenderErrorPayload: toRenderErrorPayload,
        parseErrorInfo: parseErrorInfo
    };
});
