package com.alextdev.mermaidvisualizer.lang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes.*;
import static com.intellij.psi.TokenType.*;

%%

%class _MermaidLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

%{
    private static final java.util.Set<String> KEYWORDS = new java.util.HashSet<>(java.util.Arrays.asList(
        // Flowchart / Graph + general styling/interaction directives (directions LR/RL/TD/TB/BT handled via AFTER_FLOWCHART state)
        "subgraph", "end", "direction", "style", "linkStyle", "classDef", "class",
        "click", "callback", "interpolate",
        // Sequence diagram
        "participant", "actor", "loop", "alt", "else", "opt", "par",
        "critical", "break", "rect", "note", "over", "left", "right",
        "activate", "deactivate", "autonumber", "link", "links",
        "create", "destroy", "box",
        // Class diagram
        "namespace", "annotation",
        // State diagram
        "state",
        // Aliasing (used across diagram types: sequence, state, etc.)
        "as",
        // Gantt
        "title", "section", "dateFormat", "axisFormat", "tickInterval",
        "excludes", "includes", "todayMarker", "weekday",
        // Pie
        "showData",
        // Git graph
        "branch", "checkout", "merge", "commit", "cherry-pick", "tag", "order",
        // Mindmap
        "root",
        // Quadrant chart
        "x-axis", "y-axis", "quadrant-1", "quadrant-2", "quadrant-3", "quadrant-4",
        // XY chart
        "bar", "line",
        // Block diagram
        "columns", "block", "space",
        // Architecture
        "group", "service", "junction", "align", "row", "column",
        // Cynefin
        "complex", "complicated", "clear", "chaotic", "confusion",
        // Railroad (IR constructors, railroad-beta)
        "terminal", "nonterminal", "sequence", "choice",
        "optional", "zeroOrMore", "oneOrMore", "special",
        // Venn
        "set", "union",
        // Wardley
        "component", "pipeline", "evolve", "evolution", "size", "anchor", "source",
        // Event Modeling
        "tf", "timeframe", "rf", "resetframe", "data",
        "ui", "pcr", "processor", "cmd", "command",
        "rmo", "readmodel", "evt", "event",
        // Radar
        "axis", "curve", "showLegend", "max", "min", "graticule", "ticks",
        // Requirement diagram
        "element", "requirement", "functionalRequirement", "interfaceRequirement",
        "performanceRequirement", "designConstraint",
        "verifymethod", "docRef",
        "satisfies", "traces", "derives", "refines", "verifies", "copies",
        // C4
        "Person", "Person_Ext", "System", "System_Ext", "SystemDb", "SystemQueue",
        "Container", "Container_Ext", "ContainerDb", "ContainerQueue",
        "Component", "Component_Ext", "ComponentDb", "ComponentQueue",
        "Boundary", "Enterprise_Boundary", "System_Boundary", "Container_Boundary",
        "Deployment_Node", "Node", "Node_L", "Node_R",
        "Rel", "Rel_U", "Rel_D", "Rel_L", "Rel_R", "Rel_Back", "BiRel",
        "UpdateLayoutConfig", "UpdateRelStyle", "UpdateElementStyle",
        // Accessibility (shared across diagram types)
        "accTitle", "accDescr"
    ));

    private static boolean isKeyword(String text) {
        return KEYWORDS.contains(text);
    }
%}

WHITESPACE = [ \t]+
NEWLINE = \r\n | \r | \n
DIGIT = [0-9]
// Optional sign and leading dot so xychart data like `.98` / `-3.4` are numbers.
NUMBER = "-"? ({DIGIT}+ ("." {DIGIT}+)? | "." {DIGIT}+)
ID_CHAR = [a-zA-Z0-9_]
IDENTIFIER = [a-zA-Z_] {ID_CHAR}*
HYPHEN_ID = [a-zA-Z_] {ID_CHAR}* ("-" {ID_CHAR}+)*

// Free text token (node ids with unicode/punctuation, labels, dates, `#f9f`, `$tags`, `br/`...).
// Excludes every character that can start or continue an arrow (`- . < > = ~`), `*`, `@`, `&`, `+`, `!`, `?`
// so an identifier stops right before a glued arrow (`Alice-->>Bob`, `A..>B`) or a metadata block (`B@{ ... }`).
// `.` and `-` are only allowed inside the token when followed by a plain text char (keeps `x.com`,
// `v1.2.3`, `Réseau-local`, `2024-01-01` whole, but stops before `..>`, `-.->`, `.->`). At least one
// letter/digit is required, so pure punctuation runs (`/`, `...`) fall through to the SYMBOL rule instead.
// Every punctuation char in the class is escaped: JFlex 1.9 treats `&&`, `||`, `--`, `~~` as set operators.
TEXT_CHAR = [^ \t\r\n\"\'\[\]\{\}\(\)\:\;\|\,\-\.\<\>\=\~\*\@\&\+\!\?]
TEXT_UNIT = {TEXT_CHAR} | "." {TEXT_CHAR}
WORD_CHAR = [:letter:] | [:digit:] | "_"
TEXT      = {TEXT_UNIT}* {WORD_CHAR} {TEXT_UNIT}* ("-" {TEXT_CHAR} {TEXT_UNIT}*)*

%state NORMAL
%state AFTER_FLOWCHART
%state STRING_D
%state STRING_S
%state DIRECTIVE_STATE
%state FRONTMATTER

%%

<YYINITIAL> {
    {WHITESPACE}                    { return WHITE_SPACE; }
    {NEWLINE}                       { return WHITE_SPACE; }

    "%%{"                           { yybegin(DIRECTIVE_STATE); return DIRECTIVE; }

    "%%"[^\r\n{][^\r\n]*           { return COMMENT; }
    "%%"                            { return COMMENT; }

    "end"                           { yybegin(NORMAL); return END_KW; }

    "flowchart"
    | "graph"
    | "swimlane-beta"               { yybegin(AFTER_FLOWCHART); return DIAGRAM_TYPE; }

    "sequenceDiagram"
    | "classDiagram"
    | "stateDiagram-v2"
    | "stateDiagram"
    | "erDiagram"
    | "gantt"
    | "pie"
    | "gitGraph"
    | "mindmap"
    | "timeline"
    | "journey"
    | "sankey-beta"
    | "xychart-beta"
    | "quadrantChart"
    | "requirementDiagram"
    | "C4Context"
    | "C4Container"
    | "C4Component"
    | "C4Dynamic"
    | "C4Deployment"
    | "zenuml"
    | "kanban"
    | "block-beta"
    | "packet-beta"
    | "architecture-beta"
    | "venn-beta"
    | "ishikawa-beta"
    | "wardley-beta"
    | "treeView-beta"
    | "treemap-beta"
    | "eventmodeling"
    | "radar-beta"
    | "cynefin-beta"
    | "railroad-beta"
    | "railroad-ebnf-beta"
    | "railroad-abnf-beta"
    | "railroad-peg-beta"          { yybegin(NORMAL); return DIAGRAM_TYPE; }

    "---"                           { yybegin(FRONTMATTER); return DIRECTIVE; }

    {HYPHEN_ID}                     { yybegin(NORMAL);
                                      String t = yytext().toString();
                                      if (isKeyword(t)) return KEYWORD;
                                      return IDENTIFIER; }

    // Numbers and non-ASCII identifiers at line start (`2002 : LinkedIn`, `Réseau --> X`) — NUMBER first so an
    // equal-length tie resolves to it.
    {NUMBER}                        { yybegin(NORMAL); return NUMBER; }
    {TEXT}                          { yybegin(NORMAL); return IDENTIFIER; }

    // `[^]` (not `.`): under %unicode `.` excludes \u000B \u000C \u0085 \u2028 \u2029, which would otherwise be unmatched.
    [^]                             { yypushback(1); yybegin(NORMAL); }
}

// After "flowchart" or "graph" — direction keywords recognized here only
<AFTER_FLOWCHART> {
    {WHITESPACE}                    { return WHITE_SPACE; }
    "LR" | "RL" | "TD" | "TB" | "BT"  { yybegin(NORMAL); return KEYWORD; }
    {NEWLINE}                       { yybegin(YYINITIAL); return WHITE_SPACE; }
    [^]                             { yypushback(1); yybegin(NORMAL); }
}

// JFlex picks the longest match; on equal length the EARLIER rule wins; the order of alternatives
// inside one `|` rule is irrelevant. Arrows are listed before the text rules so they win at equal
// length (`->` vs a lone `-`), and TEXT cannot contain arrow characters, so glued arrows
// (`A-->B`, `Alice->>Bob`) split correctly. Known limitation: `Alice-xBob` stays one HYPHEN_ID
// (excluding an `x`-initial tail would break flowchart ids like `pre-xfer`); `Alice -x Bob`,
// `Alice--xBob` and `Alice-)Bob` work.
<NORMAL> {
    {NEWLINE}                       { yybegin(YYINITIAL); return WHITE_SPACE; }
    {WHITESPACE}                    { return WHITE_SPACE; }

    "%%{"                           { yybegin(DIRECTIVE_STATE); return DIRECTIVE; }
    "%%"[^\r\n{][^\r\n]*           { return COMMENT; }
    "%%"                            { return COMMENT; }

    \"                              { yybegin(STRING_D); return STRING_DOUBLE; }
    \'                              { yybegin(STRING_S); return STRING_SINGLE; }

    // Invisible link
    "~~~"                           { return ARROW; }

    // Variable-length long arrows (3+ dashes/equals — not matched by fixed patterns)
    "---" "-"* ">>"                 { return ARROW; }
    "---" "-"* ">"                  { return ARROW; }
    "<--" "-"+ ">"                  { return ARROW; }
    "===" "="* ">"                  { return ARROW; }

    "||--o{"
    | "||--|{"
    | "}o--||"
    | "}|--||"
    | "||--||"
    | "}o--o{"
    | "}|--|{"
    | "<|--"
    | "--|>"
    | "*--"
    | "--*"
    | "o--"
    | "--o"
    | "<|.."
    | "..|>"
    | "..>"
    | "<.."
    | "<-->"
    | "<==>"
    | "-->>+"
    | "-->>-"
    | "->>+"
    | "->>-"
    | "--|\\"
    | "--|/"
    | "--\\\\"
    | "--//"
    | "-|\\"
    | "-|/"
    | "/|-"
    | "\\|-"
    | "-\\\\"
    | "-//"
    | "//-"
    | "\\\\-"
    | "-->>"
    | "-->+"
    | "-->-"
    | "-->"
    | "---"
    | "==>"
    | "-.->"
    | "->>"
    | "->+"
    | "->-"
    | "->"
    | "<--"
    | "<-"
    | "-.-"
    | "-."
    | ".->"
    | ".."
    | "=="
    | "--"
    | "--x"
    | "-x"
    | "--)"
    | "-)"                          { return ARROW; }

    "[" | "{" | "("                 { return BRACKET_OPEN; }
    "]" | "}" | ")"                 { return BRACKET_CLOSE; }
    ":"                             { return COLON; }
    "|"                             { return PIPE; }
    ";"                             { return SEMICOLON; }
    ","                             { return COMMA; }

    // ZenUML stereotypes (`@Actor`, `@Boundary`, `@Starter(...)`) are one symbol, never a node ref.
    // A bare `@` (`A@{ shape: ... }`, edge ids `e1@-->`) falls through to the SYMBOL fallback below.
    "@" [a-zA-Z_] {ID_CHAR}*        { return SYMBOL; }

    {NUMBER}                        { return NUMBER; }

    {HYPHEN_ID}                     { String t = yytext().toString();
                                      if (isKeyword(t)) return KEYWORD;
                                      return IDENTIFIER; }

    {TEXT}                          { return IDENTIFIER; }

    // Last resort: any other single character (`@`, `=`, `<`, `>`, `~`, `*`, lone `-`, `&`, `/`...).
    // Must be `[^]` so nothing is ever unmatched (see YYINITIAL).
    [^]                             { return SYMBOL; }
}

<STRING_D> {
    \"                              { yybegin(NORMAL); return STRING_DOUBLE; }
    [^\"\r\n]+                      { return STRING_DOUBLE; }
    {NEWLINE}                       { yybegin(YYINITIAL); return WHITE_SPACE; }
}

<STRING_S> {
    \'                              { yybegin(NORMAL); return STRING_SINGLE; }
    [^\'\r\n]+                      { return STRING_SINGLE; }
    {NEWLINE}                       { yybegin(YYINITIAL); return WHITE_SPACE; }
}

<DIRECTIVE_STATE> {
    "}%%"                           { yybegin(YYINITIAL); return DIRECTIVE; }
    [^}]+ | "}"                     { return DIRECTIVE; }
    <<EOF>>                         { yybegin(YYINITIAL); return DIRECTIVE; }
}

<FRONTMATTER> {
    "---"                           { yybegin(YYINITIAL); return DIRECTIVE; }
    [^\r\n]+                        { return DIRECTIVE; }
    {NEWLINE}                       { return WHITE_SPACE; }
    <<EOF>>                         { yybegin(YYINITIAL); return DIRECTIVE; }
}
