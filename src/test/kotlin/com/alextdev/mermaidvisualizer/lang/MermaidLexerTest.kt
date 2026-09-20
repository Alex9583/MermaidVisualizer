package com.alextdev.mermaidvisualizer.lang

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MermaidLexerTest {

    private fun tokenize(input: String): List<Pair<IElementType, String>> {
        val lexer = MermaidLexer()
        lexer.start(input)
        val tokens = mutableListOf<Pair<IElementType, String>>()
        while (lexer.tokenType != null) {
            tokens.add(lexer.tokenType!! to lexer.tokenText)
            lexer.advance()
        }
        return tokens
    }

    private fun nonWhitespaceTokens(input: String): List<Pair<IElementType, String>> =
        tokenize(input).filter { it.first != TokenType.WHITE_SPACE }

    @Test
    fun testEmptyInput() {
        assertEquals(emptyList<Pair<IElementType, String>>(), tokenize(""))
    }

    @Test
    fun testWhitespaceOnly() {
        val tokens = tokenize("   \t  ")
        assertTrue(tokens.all { it.first == TokenType.WHITE_SPACE })
    }

    @Test
    fun testNewlines() {
        val tokens = tokenize("\n\r\n\n")
        assertTrue(tokens.all { it.first == TokenType.WHITE_SPACE })
    }

    @Test
    fun testLineComment() {
        val tokens = nonWhitespaceTokens("%% this is a comment")
        assertEquals(1, tokens.size)
        assertEquals(MermaidTokenTypes.COMMENT, tokens[0].first)
        assertEquals("%% this is a comment", tokens[0].second)
    }

    @Test
    fun testEmptyComment() {
        val tokens = nonWhitespaceTokens("%%")
        assertEquals(1, tokens.size)
        assertEquals(MermaidTokenTypes.COMMENT, tokens[0].first)
    }

    @Test
    fun testSinglePercentIsNotComment() {
        val tokens = nonWhitespaceTokens("% not a comment")
        assertFalse(tokens.any { it.first == MermaidTokenTypes.COMMENT },
            "Single % must never produce a COMMENT token (got: $tokens)")
    }

    @Test
    fun testDirective() {
        val tokens = nonWhitespaceTokens("%%{init: {'theme': 'dark'}}%%")
        assertTrue(tokens.all { it.first == MermaidTokenTypes.DIRECTIVE })
    }

    @Test
    fun testDirectiveMultiToken() {
        val input = "%%{init: {'theme': 'dark'}}%%"
        val tokens = tokenize(input)
        val dirTokens = tokens.filter { it.first == MermaidTokenTypes.DIRECTIVE }
        assertTrue(dirTokens.isNotEmpty())
        assertEquals(input, dirTokens.joinToString("") { it.second })
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "flowchart", "graph", "sequenceDiagram", "classDiagram",
        "stateDiagram", "stateDiagram-v2", "erDiagram", "gantt",
        "pie", "gitGraph", "mindmap", "timeline", "journey",
        "sankey-beta", "xychart-beta", "quadrantChart",
        "requirementDiagram", "C4Context", "C4Container",
        "C4Component", "C4Dynamic", "C4Deployment", "zenuml",
        "kanban", "block-beta", "packet-beta", "architecture-beta",
        "venn-beta", "ishikawa-beta",
        "wardley-beta", "treeView-beta", "treemap-beta",
        "eventmodeling", "radar-beta", "cynefin-beta",
        "railroad-beta", "railroad-ebnf-beta", "railroad-abnf-beta",
        "railroad-peg-beta", "swimlane-beta", "usecase-beta", "agentflow-beta", "flowchart-elk"
    ])
    fun testDiagramTypeAtLineStart(diagramType: String) {
        val tokens = nonWhitespaceTokens(diagramType)
        assertEquals(1, tokens.size)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals(diagramType, tokens[0].second)
    }

    @Test
    fun testDiagramTypeNotInMiddleOfLine() {
        val tokens = nonWhitespaceTokens("A flowchart")
        val flowchartToken = tokens.find { it.second == "flowchart" }
        assertNotNull(flowchartToken)
        assertEquals(MermaidTokenTypes.IDENTIFIER, flowchartToken!!.first)
    }

    @Test
    fun testDoubleQuotedString() {
        val tokens = nonWhitespaceTokens("\"Hello world\"")
        assertTrue(tokens.all { it.first == MermaidTokenTypes.STRING_DOUBLE })
        assertEquals("\"Hello world\"", tokens.joinToString("") { it.second })
    }

    @Test
    fun testSingleQuotedString() {
        val tokens = nonWhitespaceTokens("'dark'")
        assertTrue(tokens.all { it.first == MermaidTokenTypes.STRING_SINGLE })
        assertEquals("'dark'", tokens.joinToString("") { it.second })
    }

    @Test
    fun testUnterminatedStringEndsAtNewline() {
        val tokens = tokenize("\"unterminated\nfoo")
        val stringTokens = tokens.filter { it.first == MermaidTokenTypes.STRING_DOUBLE }
        assertTrue(stringTokens.isNotEmpty())
        assertFalse(stringTokens.any { it.second.contains("foo") })
    }

    @Test
    fun testArrowSimple() {
        val tokens = nonWhitespaceTokens("A --> B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken)
        assertEquals("-->", arrowToken!!.second)
    }

    @Test
    fun testArrowThickArrow() {
        val tokens = nonWhitespaceTokens("A ==> B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken)
        assertEquals("==>", arrowToken!!.second)
    }

    @Test
    fun testArrowDotted() {
        val tokens = nonWhitespaceTokens("A -.-> B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken)
        assertEquals("-.->", arrowToken!!.second)
    }

    @Test
    fun testArrowAsync() {
        val tokens = nonWhitespaceTokens("A ->> B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken)
        assertEquals("->>", arrowToken!!.second)
    }

    @Test
    fun testArrowInheritance() {
        val tokens = nonWhitespaceTokens("A <|-- B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken)
        assertEquals("<|--", arrowToken!!.second)
    }

    @Test
    fun testArrowErRelation() {
        val tokens = nonWhitespaceTokens("A ||--o{ B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken)
        assertEquals("||--o{", arrowToken!!.second)
    }

    @Test
    fun testBrackets() {
        val tokens = nonWhitespaceTokens("[test]")
        assertEquals(MermaidTokenTypes.BRACKET_OPEN, tokens[0].first)
        assertEquals(MermaidTokenTypes.BRACKET_CLOSE, tokens[tokens.size - 1].first)
    }

    @Test
    fun testColon() {
        val tokens = nonWhitespaceTokens("Alice: Hello")
        assertTrue(tokens.any { it.first == MermaidTokenTypes.COLON })
    }

    @Test
    fun testPipe() {
        val tokens = nonWhitespaceTokens("|Yes|")
        assertTrue(tokens.any { it.first == MermaidTokenTypes.PIPE })
    }

    @Test
    fun testSemicolon() {
        val tokens = nonWhitespaceTokens("A;")
        assertTrue(tokens.any { it.first == MermaidTokenTypes.SEMICOLON })
    }

    @Test
    fun testIntegerNumber() {
        val tokens = nonWhitespaceTokens("386")
        assertEquals(1, tokens.size)
        assertEquals(MermaidTokenTypes.NUMBER, tokens[0].first)
    }

    @Test
    fun testDecimalNumber() {
        val tokens = nonWhitespaceTokens("0.45")
        assertEquals(1, tokens.size)
        assertEquals(MermaidTokenTypes.NUMBER, tokens[0].first)
    }

    @Test
    fun testKeywordSubgraph() {
        val tokens = nonWhitespaceTokens("    subgraph test")
        val subgraphToken = tokens.find { it.second == "subgraph" }
        assertNotNull(subgraphToken)
        assertEquals(MermaidTokenTypes.KEYWORD, subgraphToken!!.first)
    }

    @Test
    fun testEndKeywordAtLineStart() {
        val tokens = nonWhitespaceTokens("end")
        val endToken = tokens.find { it.second == "end" }
        assertNotNull(endToken)
        assertEquals(MermaidTokenTypes.END_KW, endToken!!.first)
    }

    @Test
    fun testEndKeywordAfterIndent() {
        val tokens = nonWhitespaceTokens("    end")
        val endToken = tokens.find { it.second == "end" }
        assertNotNull(endToken)
        assertEquals(MermaidTokenTypes.END_KW, endToken!!.first)
    }

    @Test
    fun testEndKeywordMidLine() {
        val tokens = nonWhitespaceTokens("flowchart LR\n    A end B")
        val endToken = tokens.filter { it.second == "end" }
        assertEquals(1, endToken.size)
        assertEquals(MermaidTokenTypes.KEYWORD, endToken[0].first,
            "'end' mid-line in NORMAL state should be KEYWORD, not END_KW")
    }

    @Test
    fun testEndpointNotEndKw() {
        val tokens = nonWhitespaceTokens("endpoint")
        val token = tokens.find { it.second == "endpoint" }
        assertNotNull(token)
        assertNotEquals(MermaidTokenTypes.END_KW, token!!.first,
            "'endpoint' should not be END_KW")
    }

    @Test
    fun testFlowchartWithSubgraphEnd() {
        val input = "flowchart LR\n    subgraph test\n        A --> B\n    end"
        val tokens = nonWhitespaceTokens(input)
        val endToken = tokens.last()
        assertEquals("end", endToken.second)
        assertEquals(MermaidTokenTypes.END_KW, endToken.first,
            "'end' as first token on line (YYINITIAL state) should be END_KW")
    }

    @Test
    fun testKeywordParticipant() {
        val tokens = nonWhitespaceTokens("    participant Alice")
        val token = tokens.find { it.second == "participant" }
        assertNotNull(token)
        assertEquals(MermaidTokenTypes.KEYWORD, token!!.first)
    }

    @Test
    fun testIdentifier() {
        val tokens = nonWhitespaceTokens("Alice")
        assertEquals(1, tokens.size)
        assertEquals(MermaidTokenTypes.IDENTIFIER, tokens[0].first)
    }

    @Test
    fun testFlowchartSnippet() {
        val input = """
            flowchart LR
                A[Start] --> B[End]
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("flowchart", tokens[0].second)
        assertTrue(tokens.any { it.first == MermaidTokenTypes.ARROW })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.BRACKET_OPEN })
    }

    @Test
    fun testSequenceDiagramSnippet() {
        val input = """
            sequenceDiagram
                participant Alice
                Alice ->> Bob: Hello
                Bob -->> Alice: Hi
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("sequenceDiagram", tokens[0].second)
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "participant" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.ARROW })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.COLON })
    }

    @Test
    fun testCommentBeforeDiagram() {
        val input = """
            %% A comment
            flowchart LR
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.COMMENT, tokens[0].first)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[1].first)
    }

    @Test
    fun testNoBadCharactersInTypicalInput() {
        val input = """
            flowchart LR
                A[Start] --> B{Decision}
                B -->|Yes| C[OK]
                B -->|No| D[Error]
                U@{ shape: person, label: "User" }
                S@{ shape: console, label: "API server" }
                U --> S
                subgraph legacy [Legacy services]
                    L1[Batch job] --> L2[Mainframe]
                end
                legacy@{ view: collapsed }
        """.trimIndent()
        val tokens = tokenize(input)
        assertFalse(tokens.any { it.first == TokenType.BAD_CHARACTER },
            "Expected no BAD_CHARACTER tokens, but found: ${tokens.filter { it.first == TokenType.BAD_CHARACTER }}")
    }

    @ParameterizedTest
    @ValueSource(strings = ["LR", "RL", "TD", "TB", "BT"])
    fun testDirectionKeywordsAfterFlowchart(dir: String) {
        val tokens = nonWhitespaceTokens("flowchart $dir")
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals(MermaidTokenTypes.KEYWORD, tokens[1].first,
            "$dir should be KEYWORD after flowchart")
    }

    @ParameterizedTest
    @ValueSource(strings = ["LR", "RL", "TD", "TB", "BT"])
    fun testDirectionKeywordsAfterGraph(dir: String) {
        val tokens = nonWhitespaceTokens("graph $dir")
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals(MermaidTokenTypes.KEYWORD, tokens[1].first,
            "$dir should be KEYWORD after graph")
    }

    @ParameterizedTest
    @ValueSource(strings = ["LR", "RL", "TD", "TB", "BT"])
    fun testDirectionKeywordsAfterSwimlane(dir: String) {
        val tokens = nonWhitespaceTokens("swimlane-beta $dir")
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals(MermaidTokenTypes.KEYWORD, tokens[1].first,
            "$dir should be KEYWORD after swimlane-beta")
    }

    @ParameterizedTest
    @ValueSource(strings = ["LR", "RL", "TD", "TB", "BT"])
    fun testDirectionKeywordsAfterFlowchartElk(dir: String) {
        val tokens = nonWhitespaceTokens("flowchart-elk $dir")
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("flowchart-elk", tokens[0].second, "flowchart-elk must not be split into flowchart + -elk")
        assertEquals(MermaidTokenTypes.KEYWORD, tokens[1].first,
            "$dir should be KEYWORD after flowchart-elk")
    }

    @ParameterizedTest
    @ValueSource(strings = ["LR", "RL", "TD", "TB", "BT"])
    fun testDirectionKeywordsAfterAgentflow(dir: String) {
        val tokens = nonWhitespaceTokens("agentflow-beta $dir")
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals(MermaidTokenTypes.KEYWORD, tokens[1].first,
            "$dir should be KEYWORD after agentflow-beta")
    }

    @ParameterizedTest
    @ValueSource(strings = ["LR", "RL", "TD", "TB", "BT"])
    fun testDirectionNotKeywordAsNodeName(dir: String) {
        // On a subsequent line, direction abbreviations are just identifiers
        val tokens = nonWhitespaceTokens("flowchart LR\n    ${dir}[Exit]")
        val nodeToken = tokens.last { it.second == dir }
        assertEquals(MermaidTokenTypes.IDENTIFIER, nodeToken.first,
            "$dir should be IDENTIFIER when used as node name")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        // Flowchart + general styling/interaction
        "subgraph", "direction", "style", "linkStyle", "classDef", "class",
        "click", "callback", "interpolate",
        // Sequence
        "participant", "actor", "loop", "alt", "else", "opt", "par",
        "critical", "break", "rect", "note", "over", "left", "right",
        "activate", "deactivate", "autonumber", "link", "links",
        "create", "destroy", "box",
        // Class
        "namespace", "annotation",
        // State
        "state",
        // Aliasing
        "as",
        // Gantt
        "title", "section", "dateFormat", "axisFormat", "tickInterval",
        "excludes", "includes", "todayMarker", "weekday",
        // Pie
        "showData",
        // Git
        "branch", "checkout", "merge", "commit", "tag", "order",
        // Mindmap
        "root",
        // XY chart
        "bar", "line",
        // Block
        "columns", "block", "space",
        // Architecture
        "group", "service", "junction", "align", "row", "column",
        // Cynefin
        "complex", "complicated", "clear", "chaotic", "confusion",
        // Railroad (IR constructors)
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
        // Requirement
        "element", "requirement", "functionalRequirement", "interfaceRequirement",
        "performanceRequirement", "designConstraint",
        "verifymethod", "docRef",
        "satisfies", "traces", "derives", "refines", "verifies", "copies",
        // Use case (usecase-beta)
        "systemBoundary", "for", "json", "include", "extend",
        // Agentflow (agentflow-beta)
        "flow", "connector", "global",
        // Accessibility
        "accTitle", "accDescr"
    ])
    fun testKeywords(keyword: String) {
        val tokens = nonWhitespaceTokens("    $keyword")
        val token = tokens.find { it.second == keyword }
        assertNotNull(token, "Token for '$keyword' not found")
        assertEquals(MermaidTokenTypes.KEYWORD, token!!.first,
            "'$keyword' should be KEYWORD")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "x-axis", "y-axis",
        "quadrant-1", "quadrant-2", "quadrant-3", "quadrant-4",
        "cherry-pick"
    ])
    fun testHyphenatedKeywords(keyword: String) {
        val tokens = nonWhitespaceTokens("    $keyword")
        val token = tokens.find { it.second == keyword }
        assertNotNull(token, "Token for '$keyword' not found")
        assertEquals(MermaidTokenTypes.KEYWORD, token!!.first,
            "'$keyword' should be KEYWORD")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "Person", "Person_Ext", "System", "System_Ext", "SystemDb", "SystemQueue",
        "Container", "Container_Ext", "ContainerDb", "ContainerQueue",
        "Component", "Component_Ext", "ComponentDb", "ComponentQueue",
        "Boundary", "Enterprise_Boundary", "System_Boundary", "Container_Boundary",
        "Deployment_Node", "Node", "Node_L", "Node_R",
        "Rel", "Rel_U", "Rel_D", "Rel_L", "Rel_R", "Rel_Back", "BiRel",
        "UpdateLayoutConfig", "UpdateRelStyle", "UpdateElementStyle"
    ])
    fun testC4Keywords(keyword: String) {
        val tokens = nonWhitespaceTokens("    $keyword")
        val token = tokens.find { it.second == keyword }
        assertNotNull(token, "Token for '$keyword' not found")
        assertEquals(MermaidTokenTypes.KEYWORD, token!!.first,
            "C4 keyword '$keyword' should be KEYWORD")
    }

    @Test
    fun testIdentifierIsTokenized() {
        val tokens = nonWhitespaceTokens("    Alice")
        val token = tokens.find { it.second == "Alice" }
        assertNotNull(token)
        assertEquals(MermaidTokenTypes.IDENTIFIER, token!!.first)
    }

    @Test
    fun testUnterminatedDirectiveAtEof() {
        val tokens = nonWhitespaceTokens("%%{init: {'theme': 'dark'}")
        assertTrue(tokens.all { it.first == MermaidTokenTypes.DIRECTIVE })
    }

    @Test
    fun testUnterminatedSingleQuotedStringEndsAtNewline() {
        val tokens = tokenize("'unterminated\nfoo")
        val stringTokens = tokens.filter { it.first == MermaidTokenTypes.STRING_SINGLE }
        assertTrue(stringTokens.isNotEmpty())
        assertFalse(stringTokens.any { it.second.contains("foo") })
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "-|\\", "-|/", "/|-", "\\|-",
        "-\\\\", "-//", "//-", "\\\\-",
        "--|\\", "--|/", "--\\\\", "--//"
    ])
    fun testHalfArrowPatterns(arrow: String) {
        val tokens = nonWhitespaceTokens("A $arrow B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken, "Arrow token for '$arrow' not found")
        assertEquals(arrow, arrowToken!!.second)
    }

    @Test
    fun testVennDiagramSnippet() {
        val input = """
            venn-beta
              set A["Alpha"]
              union A,B["AB"]
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("venn-beta", tokens[0].second)
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "set" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "union" })
    }

    @Test
    fun testIshikawaDiagramSnippet() {
        val input = """
            ishikawa-beta
              Blurry Photo
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("ishikawa-beta", tokens[0].second)
    }

    @Test
    fun testWardleyDiagramSnippet() {
        val input = """
            wardley-beta
              title Tea Shop
              component Cup [0.79, 0.61]
              evolve Cup 0.75
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("wardley-beta", tokens[0].second)
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "title" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "component" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "evolve" })
    }

    @Test
    fun testTreeViewDiagramSnippet() {
        val input = """
            treeView-beta
              "src"
                "main"
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("treeView-beta", tokens[0].second)
    }

    @Test
    fun testEventModelingDiagramSnippet() {
        val input = """
            eventmodeling

            tf 01 ui CartUI
            tf 02 cmd AddItem
            tf 03 evt ItemAdded
            tf 04 rmo CartView ->> 03
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("eventmodeling", tokens[0].second)
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "tf" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "ui" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "cmd" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "evt" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "rmo" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.ARROW && it.second == "->>" })
    }

    @Test
    fun testRadarDiagramSnippet() {
        val input = """
            radar-beta
              title Product Performance
              axis A, B, C, D, E
              curve Product1{1, 2, 3, 4, 5}
              showLegend true
              max 10
              ticks 5
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("radar-beta", tokens[0].second)
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "axis" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "curve" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "showLegend" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "max" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "ticks" })
    }

    @Test
    fun testTreemapDiagramSnippet() {
        val input = """
            treemap-beta
            "Products"
                "Phones": 50
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("treemap-beta", tokens[0].second)
    }

    @Test
    fun testCynefinDiagramSnippet() {
        val input = """
            cynefin-beta
              title Incident Response
              complex
                "Investigate root cause"
              clear
                "Restart service"
              complex --> complicated : "Pattern identified"
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("cynefin-beta", tokens[0].second)
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "complex" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "clear" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "complicated" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.ARROW && it.second == "-->" })
    }

    @Test
    fun testRailroadEbnfDiagramSnippet() {
        val input = """
            railroad-ebnf-beta
            title "Digit Definition"

            digit = "0" | "1" | "2" ;
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("railroad-ebnf-beta", tokens[0].second)
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "title" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.PIPE })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.SEMICOLON })
    }

    @Test
    fun testRailroadPegAssignmentArrow() {
        val input = """
            railroad-peg-beta
            Expression <- Term ;
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("railroad-peg-beta", tokens[0].second)
        assertTrue(tokens.any { it.first == MermaidTokenTypes.ARROW && it.second == "<-" },
            "PEG assignment operator <- should be ARROW (got: $tokens)")
    }

    @Test
    fun testRailroadIrConstructorKeywords() {
        val input = """
            railroad-beta
            digit = choice(terminal("0"), terminal("1")) ;
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("railroad-beta", tokens[0].second)
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "choice" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "terminal" })
    }

    @Test
    fun testSwimlaneDiagramSnippet() {
        val input = """
            swimlane-beta LR
              subgraph sales [Sales team]
                lead[Qualify lead]
              end
              lead --> quote
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("swimlane-beta", tokens[0].second)
        assertEquals(MermaidTokenTypes.KEYWORD, tokens[1].first, "LR should be KEYWORD after swimlane-beta")
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "subgraph" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.END_KW && it.second == "end" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.ARROW && it.second == "-->" })
    }

    @Test
    fun testFlowchartElkDiagramSnippet() {
        val input = """
            flowchart-elk TD
              subgraph wrap [Wrap-up]
                A --> B
              end
              B -->|done| C
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("flowchart-elk", tokens[0].second)
        assertEquals(MermaidTokenTypes.KEYWORD, tokens[1].first, "TD should be KEYWORD after flowchart-elk")
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "subgraph" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.END_KW && it.second == "end" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.ARROW && it.second == "-->" })
    }

    // ── Mermaid 12: use case + agentflow ────────────────────────────────

    @Test
    fun testUsecaseDiagramSnippet() {
        val input = """
            usecase-beta
            direction LR
            actor Staff("Order staff")@{ type: hollow, business: true } <<Employee>>
            systemBoundary ordering["Ordering System"]@{ type: package }:::system
              Checkout("Checkout") <<Core>>:::critical
            end
            note for Checkout "Validates the cart"
            Staff --> Checkout
            Admin --|> Staff
            Checkout pays@..> : include Payment
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("usecase-beta", tokens[0].second)
        for (kw in listOf("direction", "actor", "systemBoundary", "note", "for", "include")) {
            assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == kw }, "$kw should be KEYWORD")
        }
        assertTrue(tokens.any { it.first == MermaidTokenTypes.END_KW && it.second == "end" })
        for (arrow in listOf("-->", "--|>", "..>")) {
            assertTrue(tokens.any { it.first == MermaidTokenTypes.ARROW && it.second == arrow }, "$arrow should be ARROW")
        }
        assertTrue(tokens.any { it.first == MermaidTokenTypes.SYMBOL && it.second == "<<Employee>>" },
            "stereotype should be one SYMBOL (got: $tokens)")
        assertTrue(tokens.any { it.first == MermaidTokenTypes.SYMBOL && it.second == "<<Core>>" })
        assertFalse(tokens.any { it.first == MermaidTokenTypes.IDENTIFIER && (it.second == "Employee" || it.second == "Core") },
            "stereotype text must not leak as an identifier")
        assertTrue(tokens.any { it.first == MermaidTokenTypes.IDENTIFIER && it.second == "pays" }, "edge id stays an identifier")
    }

    @Test
    fun testAgentflowDiagramSnippet() {
        val input = """
            agentflow-beta TB
              connector llm["LLM API"]
              global
                corpus["Shared corpus"]@{ shape: refdoc }
              end
              flow writer["Drafting Agent"]
                draft["Draft"]@{ shape: task }
                draft -.- corpus
              end
              check -- yes --> writer
              fix --x check
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("agentflow-beta", tokens[0].second)
        assertEquals(MermaidTokenTypes.KEYWORD, tokens[1].first, "TB should be KEYWORD after agentflow-beta")
        for (kw in listOf("connector", "global", "flow")) {
            assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == kw }, "$kw should be KEYWORD")
        }
        assertEquals(2, tokens.count { it.first == MermaidTokenTypes.END_KW }, "both blocks end with END_KW")
        for (arrow in listOf("-.-", "--", "-->", "--x")) {
            assertTrue(tokens.any { it.first == MermaidTokenTypes.ARROW && it.second == arrow }, "$arrow should be ARROW")
        }
    }

    @Test
    fun testClassStereotypeIsSingleSymbol() {
        val tokens = nonWhitespaceTokens("classDiagram\n    class Shape{\n        <<interface>>\n        noOfVertices\n    }")
        assertTrue(tokens.any { it.first == MermaidTokenTypes.SYMBOL && it.second == "<<interface>>" },
            "<<interface>> should be one SYMBOL (got: $tokens)")
        assertFalse(tokens.any { it.first == MermaidTokenTypes.IDENTIFIER && it.second == "interface" })
    }

    @Test
    fun testSequenceBidirectionalArrowIsNotAStereotype() {
        val tokens = nonWhitespaceTokens("sequenceDiagram\n    Alice<<->>Bob: ping")
        // `<<->>` is not in the ARROW catalog (documented limitation: it lexes as `<`, `<-`, `>`, `>`); the
        // stereotype rule must not turn it into one SYMBOL that would swallow `->>Bob`.
        assertFalse(tokens.any { it.first == MermaidTokenTypes.SYMBOL && it.second.length > 1 },
            "<<->> must not be lexed as a stereotype (got: $tokens)")
        assertTrue(tokens.any { it.first == MermaidTokenTypes.IDENTIFIER && it.second == "Alice" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.IDENTIFIER && it.second == "Bob" })
    }

    @Test
    fun testUnclosedStereotypeFallsBackToSymbols() {
        val tokens = nonWhitespaceTokens("usecase-beta\n    actor A <<Broken\n    B")
        assertTrue(tokens.any { it.first == MermaidTokenTypes.IDENTIFIER && it.second == "B" },
            "an unclosed << must not swallow the next line (got: $tokens)")
    }

    @Test
    fun testInvisibleLinkArrow() {
        val tokens = nonWhitespaceTokens("A ~~~ B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken, "~~~ should be recognized as ARROW")
        assertEquals("~~~", arrowToken!!.second)
    }

    @Test
    fun testLongArrow() {
        val tokens = nonWhitespaceTokens("A ---> B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken, "---> should be recognized as ARROW")
        assertEquals("--->", arrowToken!!.second)
    }

    @Test
    fun testVeryLongArrow() {
        val tokens = nonWhitespaceTokens("A -----> B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken, "-----> should be recognized as ARROW")
        assertEquals("----->", arrowToken!!.second)
    }

    @Test
    fun testLongThickArrow() {
        val tokens = nonWhitespaceTokens("A ===> B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken, "===> should be recognized as ARROW")
        assertEquals("===>", arrowToken!!.second)
    }

    @Test
    fun testBidirectionalLongArrow() {
        val tokens = nonWhitespaceTokens("A <---> B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken, "<---> should be recognized as ARROW")
        assertEquals("<--->", arrowToken!!.second)
    }

    @Test
    fun testLongArrowDoubleHead() {
        val tokens = nonWhitespaceTokens("A --->> B")
        val arrowToken = tokens.find { it.first == MermaidTokenTypes.ARROW }
        assertNotNull(arrowToken, "--->> should be recognized as ARROW")
        assertEquals("--->>", arrowToken!!.second)
    }

    @Test
    fun testStateDiagramSnippet() {
        val input = """
            stateDiagram-v2
                state Moving {
                    slow --> fast
                }
        """.trimIndent()
        val tokens = nonWhitespaceTokens(input)
        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE, tokens[0].first)
        assertEquals("stateDiagram-v2", tokens[0].second)
        assertTrue(tokens.any { it.first == MermaidTokenTypes.KEYWORD && it.second == "state" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.BRACKET_OPEN && it.second == "{" })
        assertTrue(tokens.any { it.first == MermaidTokenTypes.BRACKET_CLOSE && it.second == "}" })
    }

    @Test
    fun testFrontmatter() {
        val input = "---\ntitle: My Diagram\n---\nflowchart LR"
        val tokens = nonWhitespaceTokens(input)
        // First --- is DIRECTIVE (opening frontmatter)
        assertEquals(MermaidTokenTypes.DIRECTIVE, tokens[0].first)
        assertEquals("---", tokens[0].second)
        // Frontmatter content is DIRECTIVE
        assertTrue(tokens.any { it.first == MermaidTokenTypes.DIRECTIVE && it.second == "title: My Diagram" })
        // Closing --- is DIRECTIVE
        val closingDash = tokens.filter { it.first == MermaidTokenTypes.DIRECTIVE && it.second == "---" }
        assertEquals(2, closingDash.size, "Both --- delimiters should be DIRECTIVE")
        // Diagram type follows frontmatter
        assertTrue(tokens.any { it.first == MermaidTokenTypes.DIAGRAM_TYPE && it.second == "flowchart" })
    }

    @Test
    fun testFrontmatterUnclosed() {
        val input = "---\ntitle: foo"
        val tokens = nonWhitespaceTokens(input)
        assertTrue(tokens.all { it.first == MermaidTokenTypes.DIRECTIVE },
            "Unclosed frontmatter: all content should be DIRECTIVE")
    }

    @Test
    fun testArrowWithoutSpacesIsTokenized() {
        // "Bob" matched in YYINITIAL, then the arrow and "Alice" are separate tokens in NORMAL:
        // TEXT cannot contain arrow characters, so glued arrows no longer swallow the next identifier.
        val tokens = nonWhitespaceTokens("Bob-->>Alice")
        assertEquals(3, tokens.size)
        assertEquals(MermaidTokenTypes.IDENTIFIER to "Bob", tokens[0])
        assertEquals(MermaidTokenTypes.ARROW to "-->>", tokens[1])
        assertEquals(MermaidTokenTypes.IDENTIFIER to "Alice", tokens[2])
    }

    @Test
    fun testArrowWithSpacesWorks() {
        val tokens = nonWhitespaceTokens("Bob -->> Alice")
        assertEquals(3, tokens.size)
        assertEquals(MermaidTokenTypes.IDENTIFIER, tokens[0].first)
        assertEquals(MermaidTokenTypes.ARROW, tokens[1].first)
        assertEquals("-->>", tokens[1].second)
        assertEquals(MermaidTokenTypes.IDENTIFIER, tokens[2].first)
    }

    @Test
    fun testOfIsIdentifier() {
        val tokens = nonWhitespaceTokens("    of")
        val token = tokens.find { it.second == "of" }
        assertNotNull(token)
        assertEquals(MermaidTokenTypes.IDENTIFIER, token!!.first,
            "'of' should be IDENTIFIER to avoid false keyword highlighting in text content")
    }

    @Test
    fun testAndIsIdentifier() {
        val tokens = nonWhitespaceTokens("    and")
        val token = tokens.find { it.second == "and" }
        assertNotNull(token)
        assertEquals(MermaidTokenTypes.IDENTIFIER, token!!.first,
            "'and' should be IDENTIFIER to avoid false keyword highlighting in text content")
    }

    @Test
    fun testOfInTitleNotKeyword() {
        val tokens = nonWhitespaceTokens("title History of Social Media")
        val ofToken = tokens.find { it.second == "of" }
        assertNotNull(ofToken)
        assertEquals(MermaidTokenTypes.IDENTIFIER, ofToken!!.first)
    }

    @Test
    fun testAndInTitleNotKeyword() {
        val tokens = nonWhitespaceTokens("title Reach and engagement")
        val andToken = tokens.find { it.second == "and" }
        assertNotNull(andToken)
        assertEquals(MermaidTokenTypes.IDENTIFIER, andToken!!.first)
    }

    @Test
    fun testCommaToken() {
        val tokens = nonWhitespaceTokens("5000, 6000")
        assertEquals(MermaidTokenTypes.NUMBER, tokens[0].first)
        assertEquals("5000", tokens[0].second)
        assertEquals(MermaidTokenTypes.COMMA, tokens[1].first)
        assertEquals(",", tokens[1].second)
        assertEquals(MermaidTokenTypes.NUMBER, tokens[2].first)
        assertEquals("6000", tokens[2].second)
    }

    @Test
    fun testCommaInXyChartData() {
        val tokens = nonWhitespaceTokens("bar [5000, 6000, 7500]")
        val numberTokens = tokens.filter { it.first == MermaidTokenTypes.NUMBER }
        assertEquals(3, numberTokens.size, "All numbers should be recognized individually")
        val commaTokens = tokens.filter { it.first == MermaidTokenTypes.COMMA }
        assertEquals(2, commaTokens.size)
    }

    @Test
    fun testSimpleMmdAllTokensColored() {
        val input = "flowchart LR\n    A[Start] --> B[End]"
        val tokens = nonWhitespaceTokens(input)

        assertEquals(MermaidTokenTypes.DIAGRAM_TYPE to "flowchart", tokens[0])
        assertEquals(MermaidTokenTypes.KEYWORD to "LR", tokens[1])
        assertEquals(MermaidTokenTypes.IDENTIFIER to "A", tokens[2])
        assertEquals(MermaidTokenTypes.BRACKET_OPEN to "[", tokens[3])
        assertEquals(MermaidTokenTypes.IDENTIFIER to "Start", tokens[4])
        assertEquals(MermaidTokenTypes.BRACKET_CLOSE to "]", tokens[5])
        assertEquals(MermaidTokenTypes.ARROW to "-->", tokens[6])
        assertEquals(MermaidTokenTypes.IDENTIFIER to "B", tokens[7])
        assertEquals(MermaidTokenTypes.BRACKET_OPEN to "[", tokens[8])
        assertEquals(MermaidTokenTypes.IDENTIFIER to "End", tokens[9])
        assertEquals(MermaidTokenTypes.BRACKET_CLOSE to "]", tokens[10])
        assertEquals(11, tokens.size)
    }

    // ── Glued arrows, `@` metadata and the SYMBOL fallback ─────────────

    private val ID = MermaidTokenTypes.IDENTIFIER
    private val AR = MermaidTokenTypes.ARROW
    private val SYM = MermaidTokenTypes.SYMBOL
    private val KW = MermaidTokenTypes.KEYWORD
    private val NUM = MermaidTokenTypes.NUMBER
    private val COLON = MermaidTokenTypes.COLON
    private val OPEN = MermaidTokenTypes.BRACKET_OPEN
    private val CLOSE = MermaidTokenTypes.BRACKET_CLOSE
    private val PIPE = MermaidTokenTypes.PIPE
    private val COMMA = MermaidTokenTypes.COMMA
    private val FORM_FEED = "\u000C"

    /** Compares the non-whitespace token list of [input] exactly, ignoring the leading diagram header. */
    private fun assertBodyTokens(input: String, vararg expected: Pair<IElementType, String>) {
        val tokens = nonWhitespaceTokens(input)
            .dropWhile { it.first == MermaidTokenTypes.DIAGRAM_TYPE || (it.first == KW && it.second in setOf("LR", "TD", "TB", "RL", "BT")) }
        assertEquals(expected.toList(), tokens, "Tokens for: $input")
    }

    @ParameterizedTest
    @ValueSource(strings = ["->>", "-->>", "->>+", "-->>-", "->", "-)", "--)", "-->", "--x"])
    fun testGluedSequenceArrows(arrow: String) {
        assertBodyTokens("sequenceDiagram\n    Alice${arrow}Bob: hi",
            ID to "Alice", AR to arrow, ID to "Bob", COLON to ":", ID to "hi")
    }

    @ParameterizedTest
    @ValueSource(strings = ["-->", "--->", "==>", "-.->", "--x", "--o", "<-->", "~~~", "---", "-->>"])
    fun testGluedFlowchartArrows(arrow: String) {
        assertBodyTokens("flowchart LR\n    A${arrow}B", ID to "A", AR to arrow, ID to "B")
    }

    // `o--` glued to the source (`Ao--B`) is inherently ambiguous with an identifier ending in `o`
    // and stays `Ao` + `--` (same family as the `-x` limitation); `A o-- B` works.
    @ParameterizedTest
    @ValueSource(strings = ["<|--", "--|>", "..>", "<..", "..|>", "<|..", "*--", "--*", "--o", "..", "--"])
    fun testGluedClassArrows(arrow: String) {
        assertBodyTokens("classDiagram\n    A${arrow}B", ID to "A", AR to arrow, ID to "B")
    }

    @Test
    fun testGluedErArrow() {
        assertBodyTokens("erDiagram\n    CUSTOMER||--o{ORDER : places",
            ID to "CUSTOMER", AR to "||--o{", ID to "ORDER", COLON to ":", ID to "places")
    }

    @Test
    fun testGluedStateArrow() {
        assertBodyTokens("stateDiagram-v2\n    [*] --> Still\n    Still-->Moving",
            OPEN to "[", SYM to "*", CLOSE to "]", AR to "-->", ID to "Still",
            ID to "Still", AR to "-->", ID to "Moving")
    }

    @Test
    fun testGluedLabeledFlowchartArrow() {
        assertBodyTokens("flowchart LR\n    A-->|yes|B",
            ID to "A", AR to "-->", PIPE to "|", ID to "yes", PIPE to "|", ID to "B")
    }

    @Test
    fun testDottedLinkWithText() {
        assertBodyTokens("flowchart LR\n    A-. text .->B",
            ID to "A", AR to "-.", ID to "text", AR to ".->", ID to "B")
    }

    @Test
    fun testOpenLinkWithText() {
        assertBodyTokens("flowchart LR\n    A-- text -->B",
            ID to "A", AR to "--", ID to "text", AR to "-->", ID to "B")
    }

    @Test
    fun testGluedCrossArrowIsKnownLimitation() {
        // `-x` glued to the target keeps matching HYPHEN_ID (excluding an `x`-initial tail would break
        // flowchart ids such as `pre-xfer`). `Alice -x Bob` and `Alice--xBob` work.
        assertBodyTokens("sequenceDiagram\n    Alice-xBob: x", ID to "Alice-xBob", COLON to ":", ID to "x")
        assertBodyTokens("sequenceDiagram\n    Alice -x Bob: x", ID to "Alice", AR to "-x", ID to "Bob", COLON to ":", ID to "x")
    }

    @Test
    fun testShapeMetadataAtLineStart() {
        assertBodyTokens("flowchart LR\n    U@{ shape: person }",
            ID to "U", SYM to "@", OPEN to "{", ID to "shape", COLON to ":", ID to "person", CLOSE to "}")
    }

    @Test
    fun testShapeMetadataMidLine() {
        assertBodyTokens("flowchart LR\n    A --> B@{ shape: person }",
            ID to "A", AR to "-->", ID to "B", SYM to "@", OPEN to "{", ID to "shape", COLON to ":", ID to "person", CLOSE to "}")
    }

    @Test
    fun testCollapsedSubgraphMetadata() {
        assertBodyTokens("flowchart LR\n    legacy@{ view: collapsed }",
            ID to "legacy", SYM to "@", OPEN to "{", ID to "view", COLON to ":", ID to "collapsed", CLOSE to "}")
    }

    @Test
    fun testEdgeIdIsSplitFromArrow() {
        assertBodyTokens("flowchart LR\n    A e1@--> B",
            ID to "A", ID to "e1", SYM to "@", AR to "-->", ID to "B")
    }

    @Test
    fun testZenumlStereotypeIsSingleSymbol() {
        assertBodyTokens("zenuml\n    @Actor Client", SYM to "@Actor", ID to "Client")
    }

    @Test
    fun testEmailInMessageText() {
        assertBodyTokens("sequenceDiagram\n    A->>B: mail me@x.com",
            ID to "A", AR to "->>", ID to "B", COLON to ":", ID to "mail", ID to "me", SYM to "@x", ID to ".com")
    }

    @Test
    fun testEqualsAndPostfixOperatorsAreSymbols() {
        assertBodyTokens("railroad-ebnf-beta\n    expression = term ;\n    Letter* ;\n    digit+ ;",
            ID to "expression", SYM to "=", ID to "term", MermaidTokenTypes.SEMICOLON to ";",
            ID to "Letter", SYM to "*", MermaidTokenTypes.SEMICOLON to ";",
            ID to "digit", SYM to "+", MermaidTokenTypes.SEMICOLON to ";")
    }

    @Test
    fun testHtmlBreakInsideLabel() {
        assertBodyTokens("flowchart LR\n    A[Line1<br/>Line2]",
            ID to "A", OPEN to "[", ID to "Line1", SYM to "<", ID to "br/", SYM to ">", ID to "Line2", CLOSE to "]")
    }

    @Test
    fun testClassGenericsUseSymbols() {
        assertBodyTokens("classDiagram\n    class Square~Shape~{",
            KW to "class", ID to "Square", SYM to "~", ID to "Shape", SYM to "~", OPEN to "{")
    }

    @Test
    fun testLoneHyphenAndSingleArrowInRequirement() {
        assertBodyTokens("requirementDiagram\n    test_entity - satisfies -> test_req",
            ID to "test_entity", SYM to "-", KW to "satisfies", AR to "->", ID to "test_req")
    }

    @Test
    fun testAmpersandIsSymbol() {
        assertBodyTokens("flowchart LR\n    A & B --> C", ID to "A", SYM to "&", ID to "B", AR to "-->", ID to "C")
    }

    @Test
    fun testSlashBetweenStringsIsSymbol() {
        val tokens = nonWhitespaceTokens("railroad-peg-beta\n    Keyword <- \"if\" / \"else\"")
        assertTrue(tokens.contains(AR to "<-"))
        assertTrue(tokens.contains(SYM to "/"))
        assertFalse(tokens.any { it.first == ID && it.second == "/" })
    }

    @Test
    fun testFormFeedIsSymbolNotBadCharacter() {
        // `[^]` fallback: under %unicode a plain `.` would leave a form feed unmatched (BAD_CHARACTER for the rest of the file)
        val tokens = tokenize("flowchart LR\n    A " + FORM_FEED + " B")
        assertFalse(tokens.any { it.first == TokenType.BAD_CHARACTER })
        assertTrue(tokens.contains(SYM to FORM_FEED))
    }

    @Test
    fun testAccentedHyphenatedIdentifierMidLine() {
        assertBodyTokens("flowchart LR\n    A --> Réseau-local", ID to "A", AR to "-->", ID to "Réseau-local")
    }

    @Test
    fun testAccentedIdentifierAtLineStart() {
        assertBodyTokens("flowchart LR\n    Réseau --> X", ID to "Réseau", AR to "-->", ID to "X")
    }

    @Test
    fun testNumberAtLineStart() {
        assertBodyTokens("timeline\n    2002 : LinkedIn", NUM to "2002", COLON to ":", ID to "LinkedIn")
    }

    @Test
    fun testSignedAndLeadingDotNumbers() {
        assertBodyTokens("xychart-beta\n    bar [2.3, 45, .98, -3.4]",
            KW to "bar", OPEN to "[", NUM to "2.3", COMMA to ",", NUM to "45", COMMA to ",",
            NUM to ".98", COMMA to ",", NUM to "-3.4", CLOSE to "]")
    }

    @Test
    fun testDigitRangeAndDatesStayIdentifiers() {
        assertBodyTokens("packet-beta\n    0-15: x", ID to "0-15", COLON to ":", ID to "x")
        assertBodyTokens("gantt\n    A task :a1, 2024-01-01, 30d",
            ID to "A", ID to "task", COLON to ":", ID to "a1", COMMA to ",", ID to "2024-01-01", COMMA to ",", ID to "30d")
    }

    @Test
    fun testDottedIdentifiersStayWhole() {
        assertBodyTokens("flowchart LR\n    v1.2.3 --> x.com", ID to "v1.2.3", AR to "-->", ID to "x.com")
        assertBodyTokens("zenuml\n    Client->OrderController.placeOrder()",
            ID to "Client", AR to "->", ID to "OrderController.placeOrder", OPEN to "(", CLOSE to ")")
    }

    @Test
    fun testHyphenatedKeywordsStillWinTies() {
        assertBodyTokens("quadrantChart\n    x-axis Low --> High\n    title Foo",
            KW to "x-axis", ID to "Low", AR to "-->", ID to "High", KW to "title", ID to "Foo")
        assertBodyTokens("gitGraph\n    cherry-pick id: \"x\"",
            KW to "cherry-pick", ID to "id", COLON to ":",
            MermaidTokenTypes.STRING_DOUBLE to "\"", MermaidTokenTypes.STRING_DOUBLE to "x", MermaidTokenTypes.STRING_DOUBLE to "\"")
    }

    @Test
    fun testStyleHashColorsStayIdentifiers() {
        assertBodyTokens("flowchart LR\n    style A fill:#f9f,stroke:#333",
            KW to "style", ID to "A", ID to "fill", COLON to ":", ID to "#f9f", COMMA to ",", ID to "stroke", COLON to ":", ID to "#333")
    }

    @Test
    fun testC4NamedAttribute() {
        val tokens = nonWhitespaceTokens("C4Context\n    Person(customer, \"Customer\", \$tags=\"x\")")
        assertTrue(tokens.contains(ID to "\$tags"))
        assertTrue(tokens.contains(SYM to "="))
    }
}
