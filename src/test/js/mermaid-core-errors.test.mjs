// Runtime tests for the render error classification of src/main/resources/web/mermaid-core.js.
// Run with `node --test src/test/js/*.test.mjs` (MermaidCoreJsRuntimeTest runs them from `./gradlew test`).
// Fixtures (mermaid-error-fixtures.mjs) are real errors thrown by the bundled Mermaid (message + err.hash)
// unless marked synthetic; mermaid-bundle-fixtures.test.mjs keeps them in sync with the bundle.
import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { jisonError, loadCore, plain } from './mermaid-core-sandbox.mjs';
import {
    ARROW_AT_LINE_START, BROKEN_ARROW, DANGLING_LINK, EDGE_LIMIT, LEXICAL_AT_END, LEXICAL_MID_LINE,
    MISSING_PARTICIPANT, UNCLOSED_BRACKET, UNEXPECTED_PAREN, UNKNOWN_DIAGRAM_MESSAGE, unknownDiagramError
} from './mermaid-error-fixtures.mjs';

describe('Jison errors with err.hash', () => {
    const core = loadCore();

    test('end of input inside a node label is incomplete, positioned on the label', () => {
        const info = plain(core.classifyRenderError(jisonError(UNCLOSED_BRACKET.message, UNCLOSED_BRACKET.hash)));
        assert.equal(info.category, 'incomplete');
        assert.equal(info.token, 'EOF');
        assert.equal(info.line, 2, 'loc.first_line is the unfinished line (the message says line 3)');
        assert.equal(info.column, 7, 'column = loc.first_column + 1');
        assert.equal(info.raw, UNCLOSED_BRACKET.message);
        assert.equal(info.contextBefore, 'hart TD    A[Start');
        assert.equal(info.contextAfter, '');
        assert.deepEqual(info.expected.slice(0, 3), ['SQE', 'DOUBLECIRCLEEND', 'PE']);
        assert.equal(info.summary, 'Line 2 looks unfinished: expected ], text, ), | or … after "A[Start"');
    });

    test('EOF token is incomplete with the column from loc', () => {
        const info = plain(core.classifyRenderError(jisonError(DANGLING_LINK.message, DANGLING_LINK.hash)));
        assert.equal(info.category, 'incomplete');
        assert.equal(info.token, 'EOF');
        assert.equal(info.line, 3);
        assert.equal(info.column, 4);
        assert.match(info.summary, /^Line 3 looks unfinished: expected a node id, text, /);
        assert.match(info.summary, / after "B -->"$/);
    });

    test('NEWLINE token is incomplete', () => {
        const info = plain(core.classifyRenderError(jisonError(MISSING_PARTICIPANT.message, MISSING_PARTICIPANT.hash)));
        assert.equal(info.category, 'incomplete');
        assert.equal(info.token, 'NEWLINE');
        assert.equal(info.line, 3);
        assert.equal(info.column, 8);
        assert.equal(info.summary, 'Line 3 looks unfinished: expected a participant name, "link", "+", "-" or … after "Alice->>"');
    });

    test('unexpected closing parenthesis is a syntax error', () => {
        const info = plain(core.classifyRenderError(jisonError(UNEXPECTED_PAREN.message, UNEXPECTED_PAREN.hash)));
        assert.equal(info.category, 'syntax');
        assert.equal(info.token, 'PE');
        assert.deepEqual(info.expected, ['NEWLINE', 'SEMI']);
        assert.equal(info.line, 12);
        assert.equal(info.column, 9);
        assert.equal(info.summary, 'Syntax error at line 12: unexpected ")" (expected a line break or ;)');
    });

    test('the offending token is quoted from its text and arrow terminals collapse into one label', () => {
        const info = plain(core.classifyRenderError(jisonError(BROKEN_ARROW.message, BROKEN_ARROW.hash)));
        assert.equal(info.category, 'syntax');
        assert.equal(info.token, '-');
        assert.equal(info.summary, 'Syntax error at line 3: unexpected "-" (expected an arrow, a line break, text, "," or …)');
    });

    test('a token after an accepted line break is reported on the next line, without column', () => {
        const info = plain(core.classifyRenderError(jisonError(ARROW_AT_LINE_START.message, ARROW_AT_LINE_START.hash)));
        assert.equal(info.category, 'syntax');
        assert.equal(info.line, 3);
        assert.equal(info.column, null);
        assert.equal(info.summary, 'Syntax error at line 3: unexpected "->>" (expected a participant name, a line break, "end", "title" or …)');
    });
});

describe('Jison errors without err.hash (message parsing)', () => {
    const core = loadCore();

    test('parse error with Expecting/got', () => {
        const info = plain(core.classifyRenderError(new Error(DANGLING_LINK.message)));
        assert.equal(info.category, 'incomplete');
        assert.equal(info.token, 'EOF');
        assert.equal(info.line, 4, 'without hash the line of the message is used');
        assert.equal(info.column, null);
        assert.equal(info.expected.length, 13);
        assert.equal(info.expected[0], 'AMP');
    });

    test('the numeric $end symbol in the message means end of input', () => {
        const info = plain(core.classifyRenderError(new Error(UNCLOSED_BRACKET.message)));
        assert.equal(info.category, 'incomplete');
        assert.equal(info.token, 'EOF');
        assert.equal(info.line, 3);
    });

    test('syntax error from the message', () => {
        const info = plain(core.classifyRenderError(new Error(UNEXPECTED_PAREN.message)));
        assert.equal(info.category, 'syntax');
        assert.equal(info.token, 'PE');
        assert.equal(info.summary, 'Syntax error at line 12: unexpected ")" (expected a line break or ;)');
    });

    test('short form: Unexpected end of input', () => {
        const info = plain(core.classifyRenderError(new Error('Parse error on line 5: Unexpected end of input')));
        assert.equal(info.category, 'incomplete');
        assert.equal(info.token, 'EOF');
        assert.equal(info.line, 5);
        assert.equal(info.summary, 'Line 5 looks unfinished');
    });

    test("short form: Unexpected 'X'", () => {
        const info = plain(core.classifyRenderError(new Error("Parse error on line 2: Unexpected 'PE'")));
        assert.equal(info.category, 'syntax');
        assert.equal(info.token, 'PE');
        assert.equal(info.summary, 'Syntax error at line 2: unexpected ")"');
    });

    test('lexical error with text after the caret is a syntax error', () => {
        const info = plain(core.classifyRenderError(jisonError(LEXICAL_MID_LINE.message, LEXICAL_MID_LINE.hash)));
        assert.equal(info.category, 'syntax');
        assert.equal(info.line, 3);
        assert.equal(info.column, null);
        assert.equal(info.contextBefore, '  A --> B  B --> C ');
        assert.equal(info.contextAfter, ') D');
        assert.equal(info.summary, 'Unrecognized text at line 3 near ") D"');
    });

    test('lexical error at the end of the input is incomplete', () => {
        const info = plain(core.classifyRenderError(new Error(LEXICAL_AT_END.message)));
        assert.equal(info.category, 'incomplete');
        assert.equal(info.line, 2);
        assert.equal(info.contextAfter, '');
        assert.equal(info.summary, 'Line 2 looks unfinished after "A["Sta"');
    });
});

describe('Langium parser errors', () => {
    const core = loadCore();

    test('found a line break → incomplete with line and column', () => {
        const message = "Parsing failed:  Parse error on line 3, column 8: Expecting token of type 'NUMBER_PIE' but found `\n`.";
        const info = plain(core.classifyRenderError(new Error(message)));
        assert.equal(info.category, 'incomplete');
        assert.equal(info.line, 3);
        assert.equal(info.column, 8);
        assert.deepEqual(info.expected, ['NUMBER_PIE']);
        assert.equal(info.summary, 'Line 3 looks unfinished: expected a number');
    });

    test('lexer error → syntax naming the character', () => {
        const message = 'Parsing failed: Lexer error on line 3, column 9: unexpected character: ->x<- at offset: 22, skipped 1 characters. Parse error on line 3, column 10: Expecting token of type \'NUMBER_PIE\' but found `\n`.';
        const info = plain(core.classifyRenderError(new Error(message)));
        assert.equal(info.category, 'syntax');
        assert.equal(info.line, 3);
        assert.equal(info.column, 9);
        assert.equal(info.summary, 'Unrecognized text at line 3 near "x"');
    });

    test('unexpected token sequence → syntax', () => {
        const message = "Parsing failed: Parse error on line 2, column 20: Expecting: one of these possible Token sequences:\n  1. [NEWLINE]\n  2. [EOF]\nbut found: 'API'";
        const info = plain(core.classifyRenderError(new Error(message)));
        assert.equal(info.category, 'syntax');
        assert.deepEqual(info.expected, ['NEWLINE', 'EOF']);
        assert.equal(info.summary, 'Syntax error at line 2: unexpected "API" (expected a line break or end of input)');
    });
});

describe('unknown diagram type', () => {
    const core = loadCore();

    test('UnknownDiagramError names the first word of the text', () => {
        const info = plain(core.classifyRenderError(unknownDiagramError()));
        assert.equal(info.category, 'unknown-diagram');
        assert.equal(info.summary, 'Unknown diagram type "flowchar": check the first line of the diagram');
        assert.equal(info.raw, UNKNOWN_DIAGRAM_MESSAGE);
        assert.equal(info.line, null);
    });

    test('the message alone is enough', () => {
        const info = plain(core.classifyRenderError(new Error(UNKNOWN_DIAGRAM_MESSAGE)));
        assert.equal(info.category, 'unknown-diagram');
    });

    test('legacy "Diagram xyz not found." form', () => {
        const info = plain(core.classifyRenderError(new Error('Diagram flowchar not found.')));
        assert.equal(info.category, 'unknown-diagram');
        assert.equal(info.summary, 'Unknown diagram type "flowchar": check the first line of the diagram');
    });
});

describe('rendering limits', () => {
    test('text size limit: raw and summary are the limit message', () => {
        const core = loadCore({ maxTextSize: 1000 });
        const message = core.detectTextSizeExceeded('x'.repeat(1001), '<svg>Maximum text size in diagram exceeded</svg>');
        assert.equal(message, 'Diagram text size exceeds the limit (1001 > 1000 characters). Increase "Max text size" in Settings > Tools > Mermaid.');
        const info = plain(core.classifyRenderError(message));
        assert.equal(info.category, 'limit');
        assert.equal(info.raw, message);
        assert.equal(info.summary, message);
    });

    test('text size fallback marker alone is not a limit error', () => {
        const core = loadCore({ maxTextSize: 1000 });
        assert.equal(core.detectTextSizeExceeded('short', '<svg>Maximum text size in diagram exceeded</svg>'), null);
        assert.equal(core.detectTextSizeExceeded('x'.repeat(2000), '<svg>ok</svg>'), null);
    });

    test('edge limit: raw untouched, summary names the setting', () => {
        const core = loadCore();
        const raw = 'Edge limit exceeded. 2001 edges found, but the limit is 2000.\nIncrease the limit by setting maxEdges in mermaid.initialize().';
        const info = plain(core.classifyRenderError(new Error(raw)));
        assert.equal(info.category, 'limit');
        assert.equal(info.raw, raw);
        assert.equal(info.summary, 'Diagram has more than 2000 edges. Increase "Max edges" in Settings > Tools > Mermaid.');
    });

    test('the real edge limit message of the bundle is a limit error', () => {
        const core = loadCore();
        const info = plain(core.classifyRenderError(new Error(EDGE_LIMIT.message)));
        assert.equal(info.category, 'limit');
        assert.equal(info.raw, EDGE_LIMIT.message);
        assert.equal(info.summary, 'Diagram has more than 2000 edges. Increase "Max edges" in Settings > Tools > Mermaid.');
        assert.equal(core.describeRenderError(new Error(EDGE_LIMIT.message)), info.summary);
    });

    test('edge limit with a non-numeric limit falls back to the configured maxEdges', () => {
        const core = loadCore({ maxEdges: 3000 });
        const info = plain(core.classifyRenderError(new Error('Edge limit exceeded. 5 edges found, but the limit is undefined.')));
        assert.equal(info.summary, 'Diagram has more than 3000 edges. Increase "Max edges" in Settings > Tools > Mermaid.');
    });
});

describe('generic errors', () => {
    const core = loadCore();

    test('first line of the message as summary', () => {
        const info = plain(core.classifyRenderError(new TypeError('Cannot read properties of undefined (reading \'x\')\n    at foo (bar.js:1:2)')));
        assert.equal(info.category, 'generic');
        assert.equal(info.summary, 'Cannot read properties of undefined (reading \'x\')');
        assert.equal(info.line, null);
        assert.equal(info.column, null);
        assert.equal(info.token, null);
        assert.deepEqual(info.expected, []);
        assert.equal(info.contextBefore, null);
    });

    test('non-Error values', () => {
        assert.equal(core.classifyRenderError('Unexpected render result').summary, 'Unexpected render result');
        assert.equal(core.classifyRenderError(undefined).raw, 'Unknown error');
        assert.equal(core.classifyRenderError({}).category, 'generic');
    });

    test('positioned database errors keep their own line and column', () => {
        const err = jisonError('No such shape: foo.', { text: '', token: null, line: 5, loc: { first_line: 6, last_line: 6, first_column: 4, last_column: 9 }, expected: [] });
        const info = plain(core.classifyRenderError(err, { source: '---\ntitle: x\n---\nflowchart TD\n  A --> B\n  C@{ shape: foo }' }));
        assert.equal(info.category, 'generic');
        assert.equal(info.line, 6, 'Mermaid already added the front matter offset: no remapping');
        assert.equal(info.column, 5);
    });
});

describe('token labels', () => {
    const core = loadCore();

    test('the common Jison terminals are translated', () => {
        const required = ['EOF', 'NEWLINE', 'NL', 'SPACE', 'SEMI', 'COLON', 'COMMA', 'PIPE', 'AMP', 'SQS', 'SQE', 'PS', 'PE',
            'DIAMOND_START', 'DIAMOND_STOP', 'STRUCT_START', 'STRUCT_STOP', 'TAGSTART', 'TAGEND', 'LINK', 'START_LINK',
            'ARROW_POINT', 'ARROW_CIRCLE', 'ARROW_CROSS', 'ARROW_OPEN', 'TXT', 'TEXT', 'STR', 'MD_STR', 'NODE_STRING', 'ALPHA',
            'NUM', 'DIR', 'end', 'error', 'acc_title', 'acc_descr', 'title', 'section', 'STYLE', 'CLASS', 'CLASSDEF', 'CLICK',
            'HREF', 'DEFAULT', 'MINUS', 'DOT', 'UNICODE_TEXT', 'SHAPE_DATA'];
        const labels = core.JISON_TOKEN_LABELS;
        for (const name of required) {
            assert.ok(Object.prototype.hasOwnProperty.call(labels, name), name + ' should have a label');
        }
        assert.equal(labels.error, null, 'the error-recovery terminal is never listed');
    });

    test('unknown names are quoted, duplicates removed after translation, at most 4 items', () => {
        const err = jisonError("Parse error on line 1:\nA\n-^\nExpecting 'TEXT', 'UNICODE_TEXT', 'TXT', 'FOO', 'error', 'BAR', got 'PE'");
        const info = plain(core.classifyRenderError(err));
        assert.equal(info.summary, 'Syntax error at line 1: unexpected ")" (expected text, "FOO" or "BAR")');
        assert.deepEqual(info.expected, ['TEXT', 'UNICODE_TEXT', 'TXT', 'FOO', 'error', 'BAR']);

        const many = jisonError("Parse error on line 1:\nA\n-^\nExpecting 'SEMI', 'COLON', 'COMMA', 'PIPE', 'AMP', got 'PE'");
        assert.equal(core.classifyRenderError(many).summary, 'Syntax error at line 1: unexpected ")" (expected ;, :, ,, | or …)');
    });

    test('end of input inside a block puts "end" first and drops EOF from the alternatives', () => {
        const err = jisonError("Parse error on line 4:\n... B  subgraph s  C\n--------------------^\nExpecting 'SEMI', 'EOF', 'end', got '1'");
        const info = plain(core.classifyRenderError(err));
        assert.equal(info.category, 'incomplete');
        assert.equal(info.summary, 'Line 4 looks unfinished: expected "end" or ; after "C"');
    });
});

describe('positions relative to the source', () => {
    const core = loadCore();

    test('front matter lines are added back', () => {
        const source = '---\nconfig:\n  layout: elk\n---\nflowchart TD\n  A --> B\n  B --> C[x';
        const message = "Parse error on line 4:\n...A --> B  B --> C[x\n---------------------^\nExpecting 'SQE', 'PE', 'TEXT', got '1'";
        const hash = { text: '', token: 1, line: 3, loc: { first_line: 3, last_line: 4, first_column: 10, last_column: 0 }, expected: ["'SQE'", "'PE'", "'TEXT'"] };
        assert.equal(core.classifyRenderError(jisonError(message, hash)).line, 3, 'without context: Mermaid-relative line');
        const info = plain(core.classifyRenderError(jisonError(message, hash), { source: source }));
        assert.equal(info.line, 7);
        assert.equal(info.column, 11);
        assert.equal(info.summary, 'Line 7 looks unfinished: expected ], text or ) after "B --> C[x"');
    });

    test('comment lines and leading directives removed by Mermaid are added back', () => {
        // Mermaid reports line 3 for the 6th line: the directive line and the blank + comment lines are dropped.
        const source = "%%{init: {'theme':'dark'}}%%\nsequenceDiagram\n\n  %% c\n  Alice->>Bob: hi\n  Alice-";
        const err = jisonError(BROKEN_ARROW.message, BROKEN_ARROW.hash);
        const info = plain(core.classifyRenderError(err, { source: source }));
        assert.equal(info.line, 6);
        assert.equal(info.column, 3);
    });

    test('lexical error line is remapped too', () => {
        const source = '---\ntitle: x\n---\nflowchart TD\n  A --> B)';
        const err = jisonError('Lexical error on line 2. Unrecognized text.\n...owchart TD  A --> B)\n----------------------^', { text: '', token: null, line: 1 });
        assert.equal(core.classifyRenderError(err, { source: source }).line, 5);
    });

    test('Langium positions are remapped and a column at the end of the line is kept', () => {
        const source = '---\ntitle: x\n---\npie\n  "a" : 1\n  "b" :';
        const message = "Parsing failed:  Parse error on line 3, column 8: Expecting token of type 'NUMBER_PIE' but found `\n`.";
        const info = plain(core.classifyRenderError(new Error(message), { source: source }));
        assert.equal(info.line, 6);
        assert.equal(info.column, 8);
    });

    test('positions past the end are clamped to the last line, without column', () => {
        const source = 'sequenceDiagram\n  Alice->>Bob: hi\n  loop x\n  Alice->>Bob: x';
        const message = "Parse error on line 6:\n...x  Alice->>Bob: x\n--------------------^\nExpecting 'SPACE', 'NEWLINE', 'end', 'ACTOR', got '1'";
        const hash = { text: '', token: 1, line: 5, loc: { first_line: 6, last_line: 6, first_column: 0, last_column: 0 }, expected: ["'SPACE'", "'NEWLINE'", "'end'", "'ACTOR'"] };
        const info = plain(core.classifyRenderError(jisonError(message, hash), { source: source }));
        assert.equal(info.line, 4);
        assert.equal(info.column, null);
        assert.equal(info.summary, 'Line 4 looks unfinished: expected "end", a participant name, a line break or a space after "Alice->>Bob: x"');
    });

    test('a token spanning several lines quotes the reported line', () => {
        const source = 'flowchart TD\n  A --> B\n  B --> C[x\n  D --> E';
        const message = "Parse error on line 5:\n...B --> C[x  D --> E\n---------------------^\nExpecting 'SQE', 'PE', 'TEXT', got '1'";
        const hash = { text: '', token: 1, line: 4, loc: { first_line: 3, last_line: 5, first_column: 10, last_column: 0 }, expected: ["'SQE'", "'PE'", "'TEXT'"] };
        const info = plain(core.classifyRenderError(jisonError(message, hash), { source: source }));
        assert.equal(info.line, 3);
        assert.match(info.summary, /after "B --> C\[x"$/);
    });

    test('lineOffset shifts the line and the summary', () => {
        const info = plain(core.classifyRenderError(jisonError(UNEXPECTED_PAREN.message, UNEXPECTED_PAREN.hash), { source: 'x\n'.repeat(20), lineOffset: 2 }));
        assert.equal(info.line, 14);
        assert.match(info.summary, /^Syntax error at line 14:/);
    });

    test('prepareSource trims and counts the removed leading lines', () => {
        assert.deepEqual(plain(core.prepareSource('\n\n  flowchart TD\n  A\n\n')), { source: 'flowchart TD\n  A', lineOffset: 2 });
        assert.deepEqual(plain(core.prepareSource('graph LR')), { source: 'graph LR', lineOffset: 0 });
        assert.deepEqual(plain(core.prepareSource(undefined)), { source: '', lineOffset: 0 });
    });
});

describe('parseErrorInfo payload', () => {
    const core = loadCore();

    test('exact shape sent to the Kotlin error bridge', () => {
        const info = plain(core.parseErrorInfo(jisonError(UNEXPECTED_PAREN.message, UNEXPECTED_PAREN.hash)));
        assert.deepEqual(Object.keys(info), ['status', 'message', 'summary', 'category', 'line', 'column']);
        assert.equal(info.status, 'error');
        assert.equal(info.message, UNEXPECTED_PAREN.message, 'message is always the raw Mermaid text');
        assert.equal(info.category, 'syntax');
        assert.equal(info.line, 12);
        assert.equal(info.column, 9);
    });

    test('limit and unknown-diagram payloads', () => {
        const edge = plain(core.parseErrorInfo(new Error('Edge limit exceeded. 2001 edges found, but the limit is 2000.')));
        assert.equal(edge.category, 'limit');
        assert.equal(edge.message, 'Edge limit exceeded. 2001 edges found, but the limit is 2000.');
        assert.equal(edge.line, null);

        const unknown = plain(core.parseErrorInfo(unknownDiagramError()));
        assert.equal(unknown.category, 'unknown-diagram');
        assert.equal(unknown.line, null, 'words such as "line 5" in the embedded diagram text are not positions');
    });

    test('the context is forwarded', () => {
        const info = plain(core.parseErrorInfo(jisonError(BROKEN_ARROW.message, BROKEN_ARROW.hash), { source: BROKEN_ARROW.source, lineOffset: 3 }));
        assert.equal(info.line, 6);
    });
});
