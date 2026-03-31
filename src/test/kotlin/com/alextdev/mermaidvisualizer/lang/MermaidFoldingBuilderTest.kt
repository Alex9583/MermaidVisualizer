package com.alextdev.mermaidvisualizer.lang

import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidFoldingBuilderTest : BasePlatformTestCase() {

    private val builder = MermaidFoldingBuilder()

    private fun getFoldRegions(text: String): Array<FoldingDescriptor> {
        val psi = myFixture.configureByText("test.mmd", text)
        val document = myFixture.editor.document
        return builder.buildFoldRegions(psi, document, false)
    }

    // ── Block folding ──────────────────────────────────────────────────

    fun testSubgraphFolds() {
        val regions = getFoldRegions(
            "flowchart LR\n    subgraph Payment\n        A --> B\n    end"
        )
        assertEquals(1, regions.size)
    }

    fun testSequenceLoopFolds() {
        val regions = getFoldRegions(
            "sequenceDiagram\n    loop Every minute\n        A->>B: check\n    end"
        )
        assertEquals(1, regions.size)
    }

    fun testAltElseFoldsAsOneRegion() {
        val regions = getFoldRegions(
            "sequenceDiagram\n    alt success\n        A->>B: ok\n    else failure\n        A->>B: error\n    end"
        )
        assertEquals(1, regions.size)
    }

    fun testNestedBlocksFold() {
        val regions = getFoldRegions(
            "flowchart LR\n    subgraph outer\n        subgraph inner\n            A --> B\n        end\n    end"
        )
        assertEquals(2, regions.size)
    }

    fun testDeeplyNestedBlocks() {
        val regions = getFoldRegions(
            "flowchart LR\n    subgraph a\n        subgraph b\n            subgraph c\n                A --> B\n            end\n        end\n    end"
        )
        assertEquals(3, regions.size)
    }

    fun testMultipleBlockTypes() {
        val regions = getFoldRegions(
            "sequenceDiagram\n" +
                "    opt optional\n        A->>B: x\n    end\n" +
                "    par parallel\n        A->>B: y\n    end\n" +
                "    critical section\n        A->>B: z\n    end"
        )
        assertEquals(3, regions.size)
    }

    fun testSingleLineBlockNoFold() {
        val regions = getFoldRegions(
            "flowchart LR\n    subgraph test end"
        )
        // keyword and end on same line — no fold
        assertEquals(0, regions.size)
    }

    fun testUnclosedBlockNoFold() {
        val regions = getFoldRegions(
            "flowchart LR\n    subgraph test\n        A --> B"
        )
        assertEquals(0, regions.size)
    }

    fun testBlockPlaceholder() {
        val regions = getFoldRegions(
            "flowchart LR\n    subgraph Payment\n        A --> B\n    end"
        )
        assertEquals("...", builder.getPlaceholderText(regions[0].element))
    }

    // ── Comment folding ────────────────────────────────────────────────

    fun testTwoConsecutiveCommentsFold() {
        val regions = getFoldRegions(
            "flowchart LR\n    %% first comment\n    %% second comment\n    A --> B"
        )
        assertEquals(1, regions.size)
    }

    fun testThreeConsecutiveCommentsFold() {
        val regions = getFoldRegions(
            "flowchart LR\n    %% line 1\n    %% line 2\n    %% line 3\n    A --> B"
        )
        // 3 consecutive comments = 1 fold region, not 2
        assertEquals(1, regions.size)
    }

    fun testSingleCommentNoFold() {
        val regions = getFoldRegions(
            "flowchart LR\n    %% just one comment\n    A --> B"
        )
        assertEquals(0, regions.size)
    }

    fun testNonAdjacentCommentsNoFold() {
        val regions = getFoldRegions(
            "flowchart LR\n    %% comment 1\n    A --> B\n    %% comment 2"
        )
        assertEquals(0, regions.size)
    }

    fun testCommentPlaceholder() {
        val regions = getFoldRegions(
            "flowchart LR\n    %% first\n    %% second\n    A --> B"
        )
        assertEquals("%% ...", builder.getPlaceholderText(regions[0].element))
    }

    // ── Mixed ──────────────────────────────────────────────────────────

    fun testBlocksAndCommentsIndependent() {
        val regions = getFoldRegions(
            "flowchart LR\n" +
                "    %% comment 1\n" +
                "    %% comment 2\n" +
                "    subgraph Payment\n" +
                "        A --> B\n" +
                "    end"
        )
        assertEquals(2, regions.size)
    }

    fun testIsCollapsedByDefault() {
        val regions = getFoldRegions(
            "flowchart LR\n    subgraph Payment\n        A --> B\n    end"
        )
        assertFalse(builder.isCollapsedByDefault(regions[0].element))
    }
}
