// Render errors thrown by the bundled Mermaid (src/main/resources/web/mermaid.min.js), captured as
// `message` + `err.hash` for the source given with each fixture, unless marked synthetic.
//
// mermaid-bundle-fixtures.test.mjs replays every fixture of BUNDLE_ERROR_FIXTURES against the real bundle,
// so a Mermaid upgrade that changes a message, a hash or the preprocessing (front matter, directives,
// comments) fails there first. To refresh after an upgrade: `node src/test/js/mermaid-bundle.mjs` prints
// what the bundle now throws for each fixture source; paste the new message/hash here, then check that the
// classification tests of mermaid-core-errors.test.mjs still describe the error sensibly.

export const FLOWCHART_EXPECTED_CLOSERS = ["'SQE'", "'DOUBLECIRCLEEND'", "'PE'", "'-)'", "'STADIUMEND'", "'SUBROUTINEEND'",
    "'PIPE'", "'CYLINDEREND'", "'DIAMOND_STOP'", "'TAGEND'", "'TRAPEND'", "'INVTRAPEND'", "'UNICODE_TEXT'", "'TEXT'", "'TAGSTART'"];

// flowchart TD\n    A[Start  (input ends inside the node label: Jison reports its $end symbol as 1)
export const UNCLOSED_BRACKET = {
    source: 'flowchart TD\n    A[Start',
    message: "Parse error on line 3:\n...hart TD    A[Start\n---------------------^\nExpecting 'SQE', 'DOUBLECIRCLEEND', 'PE', '-)', 'STADIUMEND', 'SUBROUTINEEND', 'PIPE', 'CYLINDEREND', 'DIAMOND_STOP', 'TAGEND', 'TRAPEND', 'INVTRAPEND', 'UNICODE_TEXT', 'TEXT', 'TAGSTART', got '1'",
    hash: { text: '', token: 1, line: 2, loc: { first_line: 2, last_line: 3, first_column: 6, last_column: 0 }, expected: FLOWCHART_EXPECTED_CLOSERS }
};

// flowchart TD\n  A --> B\n  B -->
export const DANGLING_LINK = {
    source: 'flowchart TD\n  A --> B\n  B -->',
    message: "Parse error on line 4:\n...D  A --> B  B -->\n--------------------^\nExpecting 'AMP', 'COLON', 'PIPE', 'TESTSTR', 'DOWN', 'DEFAULT', 'NUM', 'COMMA', 'NODE_STRING', 'BRKT', 'MINUS', 'MULT', 'UNICODE_TEXT', got 'EOF'",
    hash: {
        text: '', token: 'EOF', line: 3, loc: { first_line: 3, last_line: 4, first_column: 3, last_column: 0 },
        expected: ["'AMP'", "'COLON'", "'PIPE'", "'TESTSTR'", "'DOWN'", "'DEFAULT'", "'NUM'", "'COMMA'", "'NODE_STRING'", "'BRKT'", "'MINUS'", "'MULT'", "'UNICODE_TEXT'"]
    }
};

// sequenceDiagram\n  Alice->>Bob: hi\n  Alice->>
export const MISSING_PARTICIPANT = {
    source: 'sequenceDiagram\n  Alice->>Bob: hi\n  Alice->>',
    message: "Parse error on line 3:\n...>>Bob: hi  Alice->>\n----------------------^\nExpecting 'link', '+', '-', '()', 'ACTOR', got 'NEWLINE'",
    hash: { text: '\n', token: 'NEWLINE', line: 3, loc: { first_line: 3, last_line: 3, first_column: 7, last_column: 10 }, expected: ["'link'", "'+'", "'-'", "'()'", "'ACTOR'"] }
};

// The 30 message/arrow alternatives of the sequence parser after an actor name.
export const SEQUENCE_ARROWS = [
    "'NEWLINE'", "','", "'()'", "'SOLID_OPEN_ARROW'", "'DOTTED_OPEN_ARROW'", "'SOLID_ARROW'",
    "'SOLID_ARROW_TOP'", "'SOLID_ARROW_BOTTOM'", "'STICK_ARROW_TOP'", "'STICK_ARROW_BOTTOM'", "'SOLID_ARROW_TOP_DOTTED'", "'SOLID_ARROW_BOTTOM_DOTTED'",
    "'STICK_ARROW_TOP_DOTTED'", "'STICK_ARROW_BOTTOM_DOTTED'", "'SOLID_ARROW_TOP_REVERSE'", "'SOLID_ARROW_BOTTOM_REVERSE'", "'STICK_ARROW_TOP_REVERSE'", "'STICK_ARROW_BOTTOM_REVERSE'",
    "'SOLID_ARROW_TOP_REVERSE_DOTTED'", "'SOLID_ARROW_BOTTOM_REVERSE_DOTTED'", "'STICK_ARROW_TOP_REVERSE_DOTTED'", "'STICK_ARROW_BOTTOM_REVERSE_DOTTED'", "'BIDIRECTIONAL_SOLID_ARROW'", "'DOTTED_ARROW'",
    "'BIDIRECTIONAL_DOTTED_ARROW'", "'SOLID_CROSS'", "'DOTTED_CROSS'", "'SOLID_POINT'", "'DOTTED_POINT'", "'TXT'"];

// sequenceDiagram\n  Alice->>Bob: hi\n  Alice-
export const BROKEN_ARROW = {
    source: 'sequenceDiagram\n  Alice->>Bob: hi\n  Alice-',
    message: "Parse error on line 3:\n...ce->>Bob: hi  Alice-\n----------------------^\nExpecting " + SEQUENCE_ARROWS.join(', ') + ", got '-'",
    hash: { text: '-', token: '-', line: 2, loc: { first_line: 3, last_line: 3, first_column: 2, last_column: 7 }, expected: SEQUENCE_ARROWS }
};

// The 34 statement starters of the sequence parser (what may follow a line break).
export const SEQUENCE_STATEMENT_STARTERS = [
    "'SPACE'", "'NEWLINE'", "'INVALID'", "'create'", "'box'", "'end'",
    "'autonumber'", "'activate'", "'deactivate'", "'title'", "'legacy_title'", "'acc_title'",
    "'acc_descr'", "'acc_descr_multiline_value'", "'loop'", "'rect'", "'opt'", "'alt'",
    "'par'", "'par_over'", "'critical'", "'break'", "'option'", "'and'",
    "'else'", "'participant'", "'participant_actor'", "'destroy'", "'note'", "'links'",
    "'link'", "'properties'", "'details'", "'ACTOR'"];

// sequenceDiagram\n  Alice->>Bob: hi\n  ->> x   (the last accepted token is the line break ending line 2)
export const ARROW_AT_LINE_START = {
    source: 'sequenceDiagram\n  Alice->>Bob: hi\n  ->> x',
    message: "Parse error on line 3:\n...  Alice->>Bob: hi  ->> x\n----------------------^\nExpecting " + SEQUENCE_STATEMENT_STARTERS.join(', ') + ", got 'SOLID_ARROW'",
    hash: { text: '->>', token: 'SOLID_ARROW', line: 2, loc: { first_line: 2, last_line: 3, first_column: 17, last_column: 0 }, expected: SEQUENCE_STATEMENT_STARTERS }
};

// Synthetic, shaped like the Jison flowchart errors: an unexpected closing parenthesis.
export const UNEXPECTED_PAREN = {
    message: "Parse error on line 12:\n...A --> B)\n--------^\nExpecting 'NEWLINE', 'SEMI', got 'PE'",
    hash: { text: ')', token: 'PE', line: 11, loc: { first_line: 12, last_line: 12, first_column: 8, last_column: 9 }, expected: ["'NEWLINE'", "'SEMI'"] }
};

// flowchart TD\n  A --> B\n  B --> C ) D
export const LEXICAL_MID_LINE = {
    source: 'flowchart TD\n  A --> B\n  B --> C ) D',
    message: 'Lexical error on line 3. Unrecognized text.\n...  A --> B  B --> C ) D\n----------------------^',
    hash: { text: '', token: null, line: 2 }
};

// Synthetic: the lexer stopped with nothing left after the unrecognized spot (caret after the last char).
export const LEXICAL_AT_END_PAST_INPUT = '...flowchart TD  A["Sta';
export const LEXICAL_AT_END = {
    message: 'Lexical error on line 2. Unrecognized text.\n' + LEXICAL_AT_END_PAST_INPUT + '\n' +
        '-'.repeat(LEXICAL_AT_END_PAST_INPUT.length) + '^',
    hash: { text: '', token: null, line: 1 }
};

export const UNKNOWN_DIAGRAM_MESSAGE = 'No diagram type detected matching given configuration for text: flowchar TD\n A-->B';

export function unknownDiagramError() {
    const err = new Error(UNKNOWN_DIAGRAM_MESSAGE);
    err.name = 'UnknownDiagramError';
    return err;
}

// Real: front matter and a comment line precede the diagram. Mermaid parses the preprocessed text
// (both removed) and reports line 2 for the 6th source line: classifyRenderError must add them back.
export const FRONT_MATTER_AND_COMMENT = {
    source: '---\ntitle: x\n---\n%% c\nflowchart TD\n  A[Start',
    message: "Parse error on line 3:\n...wchart TD  A[Start\n---------------------^\nExpecting 'SQE', 'DOUBLECIRCLEEND', 'PE', '-)', 'STADIUMEND', 'SUBROUTINEEND', 'PIPE', 'CYLINDEREND', 'DIAMOND_STOP', 'TAGEND', 'TRAPEND', 'INVTRAPEND', 'UNICODE_TEXT', 'TEXT', 'TAGSTART', got '1'",
    hash: { text: '', token: 1, line: 2, loc: { first_line: 2, last_line: 3, first_column: 4, last_column: 0 }, expected: FLOWCHART_EXPECTED_CLOSERS }
};

// Real: a leading %%{init}%% directive is removed by Mermaid before parsing (same error as DANGLING_LINK).
export const DIRECTIVE_AND_DANGLING_LINK = {
    source: '%%{init: {"theme": "dark"}}%%\nflowchart TD\n  A --> B\n  B -->',
    message: DANGLING_LINK.message,
    hash: DANGLING_LINK.hash
};

// Real: Langium/Chevrotain parser (pie), no err.hash, position in the message only.
export const PIE_INCOMPLETE = {
    source: 'pie title Pets\n    "Dogs" : 386\n    "Cats" :\n',
    message: "Parsing failed:  Parse error on line 3, column 13: Expecting token of type 'NUMBER_PIE' but found `\n`."
};

// Real: the flowchart edge limit (maxEdges 2000, the plugin default) is enforced while parsing.
export const EDGE_LIMIT = {
    source: ['flowchart TD'].concat(Array.from({ length: 2001 }, (_, i) => '  N' + i + ' --> N' + (i + 1))).join('\n'),
    message: 'Edge limit exceeded. 2000 edges found, but the limit is 2000.\n\n' +
        'Initialize mermaid with maxEdges set to a higher number to allow more edges.\n' +
        'You cannot set this config via configuration inside the diagram as it is a secure config.\n' +
        'You have to call mermaid.initialize.'
};

// Real: unknown diagram type (the error embeds the whole diagram text).
export const UNKNOWN_DIAGRAM = {
    source: 'flowchar TD\n A-->B',
    message: UNKNOWN_DIAGRAM_MESSAGE,
    errorName: 'UnknownDiagramError'
};

/**
 * Every real fixture, replayed against the bundle by mermaid-bundle-fixtures.test.mjs.
 * `sourceLine`/`sourceColumn` are the 1-based position that classifyRenderError must report for the
 * fixture's source (after Mermaid's preprocessing has been undone), when the error carries one.
 */
export const BUNDLE_ERROR_FIXTURES = [
    { name: 'flowchart: unclosed node label', fixture: UNCLOSED_BRACKET, sourceLine: 2 },
    { name: 'flowchart: dangling link at end of input', fixture: DANGLING_LINK, sourceLine: 3 },
    { name: 'sequence: missing participant after an arrow', fixture: MISSING_PARTICIPANT, sourceLine: 3 },
    { name: 'sequence: broken arrow', fixture: BROKEN_ARROW, sourceLine: 3, sourceColumn: 3 },
    { name: 'sequence: arrow at line start', fixture: ARROW_AT_LINE_START, sourceLine: 3 },
    { name: 'flowchart: lexical error in the middle of a line', fixture: LEXICAL_MID_LINE, sourceLine: 3 },
    { name: 'flowchart: front matter and comment line removed by Mermaid', fixture: FRONT_MATTER_AND_COMMENT, sourceLine: 6, sourceColumn: 5 },
    { name: 'flowchart: leading directive removed by Mermaid', fixture: DIRECTIVE_AND_DANGLING_LINK, sourceLine: 4 },
    { name: 'pie: Langium parse error', fixture: PIE_INCOMPLETE, sourceLine: 3, sourceColumn: 13 },
    { name: 'flowchart: edge limit', fixture: EDGE_LIMIT },
    { name: 'unknown diagram type', fixture: UNKNOWN_DIAGRAM }
];
