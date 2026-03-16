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

    private fun tokenTypes(input: String): List<IElementType> = tokenize(input).map { it.first }

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
        "venn-beta", "ishikawa-beta"
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
    fun testKeywordEnd() {
        val tokens = nonWhitespaceTokens("    end")
        val endToken = tokens.find { it.second == "end" }
        assertNotNull(endToken)
        assertEquals(MermaidTokenTypes.KEYWORD, endToken!!.first)
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
        "subgraph", "end", "direction", "style", "linkStyle", "classDef", "class",
        "click", "callback", "interpolate",
        // Sequence
        "participant", "actor", "loop", "alt", "else", "opt", "par", "and",
        "critical", "break", "rect", "note", "over", "left", "right", "of",
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
        "group", "service", "junction",
        // Venn
        "set", "union",
        // Requirement
        "element", "requirement", "functionalRequirement", "interfaceRequirement",
        "performanceRequirement", "designConstraint",
        "verifymethod", "docRef",
        "satisfies", "traces", "derives", "refines", "verifies", "copies",
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
}
