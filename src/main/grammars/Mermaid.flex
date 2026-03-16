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
        "participant", "actor", "loop", "alt", "else", "opt", "par", "and",
        "critical", "break", "rect", "note", "over", "left", "right", "of",
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
        "group", "service", "junction",
        // Venn
        "set", "union",
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
NUMBER = {DIGIT}+ ("." {DIGIT}+)?
ID_CHAR = [a-zA-Z0-9_]
IDENTIFIER = [a-zA-Z_] {ID_CHAR}*
HYPHEN_ID = [a-zA-Z_] {ID_CHAR}* ("-" {ID_CHAR}+)*

%state NORMAL
%state AFTER_FLOWCHART
%state STRING_D
%state STRING_S
%state DIRECTIVE_STATE

%%

<YYINITIAL> {
    {WHITESPACE}                    { return WHITE_SPACE; }
    {NEWLINE}                       { return WHITE_SPACE; }

    "%%{"                           { yybegin(DIRECTIVE_STATE); return DIRECTIVE; }

    "%%"[^\r\n{][^\r\n]*           { return COMMENT; }
    "%%"                            { return COMMENT; }

    "flowchart"
    | "graph"                       { yybegin(AFTER_FLOWCHART); return DIAGRAM_TYPE; }

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
    | "ishikawa-beta"              { yybegin(NORMAL); return DIAGRAM_TYPE; }

    {HYPHEN_ID}                     { yybegin(NORMAL);
                                      String t = yytext().toString();
                                      if (isKeyword(t)) return KEYWORD;
                                      return IDENTIFIER; }

    .                               { yypushback(1); yybegin(NORMAL); }
}

// After "flowchart" or "graph" — direction keywords recognized here only
<AFTER_FLOWCHART> {
    {WHITESPACE}                    { return WHITE_SPACE; }
    "LR" | "RL" | "TD" | "TB" | "BT"  { yybegin(NORMAL); return KEYWORD; }
    {NEWLINE}                       { yybegin(YYINITIAL); return WHITE_SPACE; }
    .                               { yypushback(1); yybegin(NORMAL); }
}

<NORMAL> {
    {NEWLINE}                       { yybegin(YYINITIAL); return WHITE_SPACE; }
    {WHITESPACE}                    { return WHITE_SPACE; }

    "%%{"                           { yybegin(DIRECTIVE_STATE); return DIRECTIVE; }
    "%%"[^\r\n{][^\r\n]*           { return COMMENT; }
    "%%"                            { return COMMENT; }

    \"                              { yybegin(STRING_D); return STRING_DOUBLE; }
    \'                              { yybegin(STRING_S); return STRING_SINGLE; }

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
    | "-->"
    | "---"
    | "==>"
    | "-.->"
    | "->>"
    | "<--"
    | "-.-"
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

    {NUMBER}                        { return NUMBER; }

    {HYPHEN_ID}                     { String t = yytext().toString();
                                      if (isKeyword(t)) return KEYWORD;
                                      return IDENTIFIER; }

    [^ \t\r\n\"\'\[\]\{\}\(\)\:\;\|]+  { return IDENTIFIER; }
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
