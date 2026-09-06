package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.lang.psi.MermaidBlock
import com.alextdev.mermaidvisualizer.lang.psi.MermaidBlockDivider
import com.alextdev.mermaidvisualizer.lang.psi.MermaidClassDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidDiagramBody
import com.alextdev.mermaidvisualizer.lang.psi.MermaidErDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidFlowchartDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidFlowchartDirection
import com.alextdev.mermaidvisualizer.lang.psi.MermaidGenericDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidSequenceDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidStateDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidStatement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidParserTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testData"

    private fun parseText(text: String): MermaidFile {
        val psiFile = myFixture.configureByText("test.mmd", text)
        assertInstanceOf(psiFile, MermaidFile::class.java)
        return psiFile as MermaidFile
    }

    private inline fun <reified T : com.intellij.psi.PsiElement> findFirst(file: MermaidFile): T? =
        PsiTreeUtil.findChildOfType(file, T::class.java)

    private inline fun <reified T : com.intellij.psi.PsiElement> findAll(file: MermaidFile): Collection<T> =
        PsiTreeUtil.findChildrenOfType(file, T::class.java)

    fun testEmptyFile() {
        val file = parseText("")
        assertNull(findFirst<MermaidDiagram>(file))
    }

    fun testFlowchartDiagram() {
        val file = parseText("flowchart LR\n    A --> B")
        val diagram = findFirst<MermaidFlowchartDiagram>(file)
        assertNotNull(diagram)
        val direction = findFirst<MermaidFlowchartDirection>(file)
        assertNotNull(direction)
        assertEquals("LR", direction!!.text)
    }

    fun testFlowchartWithoutDirection() {
        val file = parseText("flowchart\n    A --> B")
        val diagram = findFirst<MermaidFlowchartDiagram>(file)
        assertNotNull(diagram)
        val direction = findFirst<MermaidFlowchartDirection>(file)
        assertNull(direction)
    }

    fun testGraphAlias() {
        val file = parseText("graph TD\n    A --> B")
        val diagram = findFirst<MermaidFlowchartDiagram>(file)
        assertNotNull(diagram)
    }

    fun testSequenceDiagram() {
        val file = parseText("sequenceDiagram\n    Alice->>Bob: Hello")
        val diagram = findFirst<MermaidSequenceDiagram>(file)
        assertNotNull(diagram)
    }

    fun testClassDiagram() {
        val file = parseText("classDiagram\n    Animal <|-- Duck")
        val diagram = findFirst<MermaidClassDiagram>(file)
        assertNotNull(diagram)
    }

    fun testErDiagram() {
        val file = parseText("erDiagram\n    CUSTOMER ||--o{ ORDER : places")
        val diagram = findFirst<MermaidErDiagram>(file)
        assertNotNull(diagram)
    }

    fun testGenericDiagramGantt() {
        val file = parseText("gantt\n    title A Gantt Diagram")
        val diagram = findFirst<MermaidGenericDiagram>(file)
        assertNotNull(diagram)
    }

    fun testGenericDiagramPie() {
        val file = parseText("pie\n    \"Dogs\" : 386")
        val diagram = findFirst<MermaidGenericDiagram>(file)
        assertNotNull(diagram)
    }

    fun testGenericDiagramMindmap() {
        val file = parseText("mindmap\n    root((mindmap))")
        val diagram = findFirst<MermaidGenericDiagram>(file)
        assertNotNull(diagram)
    }

    fun testSubgraphBlock() {
        val file = parseText("flowchart LR\n    subgraph test\n        A --> B\n    end")
        val block = findFirst<MermaidBlock>(file)
        assertNotNull(block)
    }

    fun testNestedSubgraphs() {
        val file = parseText(
            "flowchart LR\n" +
            "    subgraph outer\n" +
            "        subgraph inner\n" +
            "            A --> B\n" +
            "        end\n" +
            "    end"
        )
        val blocks = findAll<MermaidBlock>(file)
        assertEquals(2, blocks.size)
    }

    fun testSequenceLoopBlock() {
        val file = parseText("sequenceDiagram\n    loop Every minute\n        A->>B: ping\n    end")
        val block = findFirst<MermaidBlock>(file)
        assertNotNull(block)
    }

    fun testSequenceAltElseEnd() {
        val file = parseText(
            "sequenceDiagram\n" +
            "    alt success\n" +
            "        A->>B: ok\n" +
            "    else failure\n" +
            "        A->>B: error\n" +
            "    end"
        )
        val block = findFirst<MermaidBlock>(file)
        assertNotNull(block)
        val divider = findFirst<MermaidBlockDivider>(file)
        assertNotNull(divider)
    }

    fun testUnclosedBlock() {
        val file = parseText("flowchart LR\n    subgraph test\n        A --> B")
        val block = findFirst<MermaidBlock>(file)
        assertNotNull(block)
        assertNotNull(file.node)
    }

    fun testDirectiveBeforeDiagram() {
        val file = parseText("%%{init: {'theme': 'dark'}}%%\nflowchart LR\n    A --> B")
        val diagram = findFirst<MermaidFlowchartDiagram>(file)
        assertNotNull(diagram)
    }

    fun testCommentInBody() {
        val file = parseText("flowchart LR\n    %% this is a comment\n    A --> B")
        val diagram = findFirst<MermaidFlowchartDiagram>(file)
        assertNotNull(diagram)
        val body = findFirst<MermaidDiagramBody>(file)
        assertNotNull(body)
    }

    fun testStatements() {
        val file = parseText("flowchart LR\n    A --> B\n    C --> D")
        val statements = findAll<MermaidStatement>(file)
        assertTrue(statements.isNotEmpty())
    }

    fun testOrphanEnd() {
        val file = parseText("flowchart LR\n    A --> B\nend")
        assertNotNull(file.node)
    }

    // --- State diagram ---

    fun testStateDiagram() {
        val file = parseText("stateDiagram-v2\n    [*] --> Still\n    Still --> Moving")
        val diagram = findFirst<MermaidStateDiagram>(file)
        assertNotNull(diagram)
    }

    fun testStateDiagramCompositeState() {
        val file = parseText(
            "stateDiagram-v2\n" +
            "    state Moving {\n" +
            "        [*] --> slow\n" +
            "        slow --> fast\n" +
            "    }"
        )
        val diagram = findFirst<MermaidStateDiagram>(file)
        assertNotNull(diagram)
        // Composite states use { }, not end — should NOT create MermaidBlock
        val blocks = findAll<MermaidBlock>(file)
        assertTrue("state { ... } should not produce MermaidBlock (uses braces, not end)", blocks.isEmpty())
    }

    // --- Generic diagram types ---

    fun testGenericDiagramC4() {
        val file = parseText(
            "C4Context\n" +
            "    Person(customer, \"Customer\", \"A customer\")\n" +
            "    System(banking, \"Banking System\", \"Core banking\")\n" +
            "    Rel(customer, banking, \"Uses\")"
        )
        val diagram = findFirst<MermaidGenericDiagram>(file)
        assertNotNull(diagram)
    }

    fun testGenericDiagramTimeline() {
        val file = parseText("timeline\n    title History\n    2002 : LinkedIn\n    2004 : Facebook")
        val diagram = findFirst<MermaidGenericDiagram>(file)
        assertNotNull(diagram)
    }

    fun testGenericDiagramGitGraph() {
        val file = parseText(
            "gitGraph\n" +
            "    commit\n" +
            "    branch develop\n" +
            "    checkout develop\n" +
            "    commit\n" +
            "    checkout main\n" +
            "    merge develop"
        )
        val diagram = findFirst<MermaidGenericDiagram>(file)
        assertNotNull(diagram)
    }

    // --- Block keywords exhaustive ---

    fun testSequenceOptBlock() {
        val file = parseText("sequenceDiagram\n    opt If available\n        A->>B: request\n    end")
        val block = findFirst<MermaidBlock>(file)
        assertNotNull(block)
    }

    fun testSequenceParAndBlock() {
        val file = parseText(
            "sequenceDiagram\n" +
            "    par Service A\n" +
            "        A->>B: request\n" +
            "    and Service B\n" +
            "        A->>C: request\n" +
            "    end"
        )
        val block = findFirst<MermaidBlock>(file)
        assertNotNull(block)
        val divider = findFirst<MermaidBlockDivider>(file)
        assertNotNull(divider)
    }

    fun testSequenceCriticalBlock() {
        val file = parseText("sequenceDiagram\n    critical Establish connection\n        A->>B: connect\n    end")
        val block = findFirst<MermaidBlock>(file)
        assertNotNull(block)
    }

    fun testSequenceBoxBlock() {
        val file = parseText("sequenceDiagram\n    box Blue Group\n        participant A\n        participant B\n    end")
        val block = findFirst<MermaidBlock>(file)
        assertNotNull(block)
    }

    fun testSequenceRectBlock() {
        val file = parseText("sequenceDiagram\n    rect rgb(200, 150, 255)\n        A->>B: request\n    end")
        val block = findFirst<MermaidBlock>(file)
        assertNotNull(block)
    }

    fun testClassNamespaceBlock() {
        val file = parseText(
            "classDiagram\n" +
            "    namespace Animals {\n" +
            "        class Duck\n" +
            "        class Fish\n" +
            "    }"
        )
        val diagram = findFirst<MermaidClassDiagram>(file)
        assertNotNull(diagram)
        assertNoErrors(file)
        assertTrue("namespace must not open an end-terminated block",
            findAll<MermaidBlock>(file).isEmpty())
    }

    // --- Block edge cases ---

    fun testMultipleDividers() {
        val file = parseText(
            "sequenceDiagram\n" +
            "    alt case A\n" +
            "        A->>B: a\n" +
            "    else case B\n" +
            "        A->>B: b\n" +
            "    else case C\n" +
            "        A->>B: c\n" +
            "    end"
        )
        val dividers = findAll<MermaidBlockDivider>(file)
        assertEquals("alt...else...else...end should have 2 dividers", 2, dividers.size)
    }

    fun testNestedBlocksWithDividers() {
        val file = parseText(
            "sequenceDiagram\n" +
            "    alt success\n" +
            "        loop retry\n" +
            "            A->>B: attempt\n" +
            "        end\n" +
            "    else failure\n" +
            "        A->>B: error\n" +
            "    end"
        )
        val blocks = findAll<MermaidBlock>(file)
        assertEquals("alt containing loop = 2 blocks", 2, blocks.size)
        val dividers = findAll<MermaidBlockDivider>(file)
        assertEquals("expected 1 divider", 1, dividers.size)
    }

    fun testDeepNesting() {
        val file = parseText(
            "flowchart LR\n" +
            "    subgraph L1\n" +
            "        subgraph L2\n" +
            "            subgraph L3\n" +
            "                A --> B\n" +
            "            end\n" +
            "        end\n" +
            "    end"
        )
        val blocks = findAll<MermaidBlock>(file)
        assertEquals("3 levels of nested subgraphs", 3, blocks.size)
    }

    fun testMultipleDiagramsInFile() {
        val file = parseText(
            "flowchart LR\n" +
            "    A --> B\n" +
            "sequenceDiagram\n" +
            "    Alice->>Bob: Hello"
        )
        val flowchart = findFirst<MermaidFlowchartDiagram>(file)
        val sequence = findFirst<MermaidSequenceDiagram>(file)
        assertNotNull(flowchart)
        assertNotNull(sequence)
    }

    // --- File extensions ---

    fun testParseMermaidExtensionFile() {
        val psiFile = myFixture.configureByFile("simple.mermaid")
        assertInstanceOf(psiFile, MermaidFile::class.java)
        val diagram = PsiTreeUtil.findChildOfType(psiFile, MermaidFlowchartDiagram::class.java)
        assertNotNull(diagram)
    }

    // --- Keyword 'and'/'else' in generic diagrams (not dividers) ---

    fun testMindmapAndKeyword() {
        val file = parseText(
            "mindmap\n" +
            "  root((mindmap))\n" +
            "    Tools\n" +
            "      Pen and paper\n" +
            "      Mermaid"
        )
        val diagram = findFirst<MermaidGenericDiagram>(file)
        assertNotNull(diagram)
        // 'and' should be part of a statement, not a divider error
        val dividers = findAll<MermaidBlockDivider>(file)
        assertTrue("'and' in mindmap should not create dividers", dividers.isEmpty())
    }

    fun testQuadrantAndKeyword() {
        val file = parseText(
            "quadrantChart\n" +
            "    title Reach and engagement\n" +
            "    x-axis Low Reach --> High Reach"
        )
        val diagram = findFirst<MermaidGenericDiagram>(file)
        assertNotNull(diagram)
        val dividers = findAll<MermaidBlockDivider>(file)
        assertTrue("'and' in quadrant should not create dividers", dividers.isEmpty())
    }

    private fun assertNoErrors(file: MermaidFile) {
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue("expected no parse errors, got: ${errors.map { it.errorDescription }}", errors.isEmpty())
    }

    fun testSequenceMessageContainingAnd() {
        val file = parseText(
            "sequenceDiagram\n" +
            "    participant Unit\n" +
            "    participant Service\n" +
            "    Unit->>Service: Start licence check and verify"
        )
        assertNotNull(findFirst<MermaidSequenceDiagram>(file))
        assertNoErrors(file)
        assertTrue("'and' in message text should not create a divider",
            findAll<MermaidBlockDivider>(file).isEmpty())
    }

    fun testSequenceMessageContainingElse() {
        val file = parseText("sequenceDiagram\n    A->>B: either this else that")
        assertNoErrors(file)
        assertTrue("'else' in message text should not create a divider",
            findAll<MermaidBlockDivider>(file).isEmpty())
    }

    fun testSequenceMessageContainingBlockKeyword() {
        val file = parseText("sequenceDiagram\n    A->>B: retry the loop again")
        assertNoErrors(file)
        assertTrue("'loop' in message text should not open a block",
            findAll<MermaidBlock>(file).isEmpty())
    }

    fun testSequenceMessageAndInsideParBlock() {
        val file = parseText(
            "sequenceDiagram\n" +
            "    par Branch one\n" +
            "        A->>B: check and verify\n" +
            "    and Branch two\n" +
            "        A->>C: request\n" +
            "    end"
        )
        assertNoErrors(file)
        assertEquals("only the line-start 'and' is a divider",
            1, findAll<MermaidBlockDivider>(file).size)
    }

    fun testSequenceLineStartAndOutsideBlockIsStillAnError() {
        val file = parseText(
            "sequenceDiagram\n" +
            "    A->>B: hello\n" +
            "    and orphan branch"
        )
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertFalse("'and' at line start outside par should stay an error (matches Mermaid.js)",
            errors.isEmpty())
    }

    // --- Diagram context per generic type (no stale context, no phantom blocks) ---

    fun testGenericDiagramAfterSequenceDoesNotInheritSequenceContext() {
        val file = parseText(
            "sequenceDiagram\n" +
            "    A->>B: hi\n" +
            "mindmap\n" +
            "  root((r))\n" +
            "    and more ideas"
        )
        assertNoErrors(file)
        assertTrue("'and' in a mindmap after a sequenceDiagram must not become a divider",
            findAll<MermaidBlockDivider>(file).isEmpty())
    }

    fun testMindmapLineStartBlockKeywordIsContent() {
        val file = parseText(
            "mindmap\n" +
            "  root((day))\n" +
            "    break time\n" +
            "    box of ideas\n" +
            "    lunch"
        )
        assertNoErrors(file)
        assertTrue("content diagrams have no end-terminated blocks",
            findAll<MermaidBlock>(file).isEmpty())
    }

    fun testBlockBetaCompositeBlock() {
        val file = parseText(
            "block-beta\n" +
            "    columns 3\n" +
            "    block:group1:2\n" +
            "        A B\n" +
            "    end\n" +
            "    C"
        )
        assertNoErrors(file)
        assertEquals("block-beta composite block must still parse as a block",
            1, findAll<MermaidBlock>(file).size)
    }

    fun testSwimlaneSubgraphBlock() {
        val file = parseText(
            "swimlane-beta LR\n" +
            "    subgraph Lane1\n" +
            "        A --> B\n" +
            "    end"
        )
        assertNoErrors(file)
        assertEquals("swimlane-beta supports subgraph blocks like flowchart",
            1, findAll<MermaidBlock>(file).size)
    }

    // --- ER subgraphs (Mermaid 11.17.0+) ---

    fun testErSubgraphBlock() {
        val file = parseText(
            "erDiagram\n" +
            "    subgraph title1\n" +
            "        A1 ||--|| A2 : links\n" +
            "    end\n" +
            "    title1 ||--|| A2 : links"
        )
        assertNotNull(findFirst<MermaidErDiagram>(file))
        assertNoErrors(file)
        assertEquals("erDiagram supports subgraph blocks since Mermaid 11.17",
            1, findAll<MermaidBlock>(file).size)
    }

    fun testErNestedSubgraphsWithEntityAttributes() {
        val file = parseText(
            "erDiagram\n" +
            "    subgraph title2\n" +
            "        CAR ||--o{ NAMED-DRIVER : allows\n" +
            "        subgraph title3\n" +
            "            PERSON\n" +
            "            PERSON {\n" +
            "                string firstName\n" +
            "                int age\n" +
            "            }\n" +
            "        end\n" +
            "    end"
        )
        assertNoErrors(file)
        assertEquals("nested ER subgraphs with attribute braces inside",
            2, findAll<MermaidBlock>(file).size)
    }

    fun testErOrphanEndIsStillAnError() {
        val file = parseText(
            "erDiagram\n" +
            "    A1 ||--|| A2 : links\n" +
            "    end"
        )
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertFalse("'end' without an opening subgraph should stay an error", errors.isEmpty())
    }
}
