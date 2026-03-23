package com.alextdev.mermaidvisualizer.lang.completion

import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.alextdev.mermaidvisualizer.lang.psi.MermaidBlockContent
import com.alextdev.mermaidvisualizer.lang.psi.MermaidClassDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidDiagramBody
import com.alextdev.mermaidvisualizer.lang.psi.MermaidErDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidFlowchartDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidGenericDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidSequenceDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidStateDiagram
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

enum class MermaidDiagramKind(val keyword: String, val displayKey: String) {
    FLOWCHART("flowchart", "completion.mermaid.diagram.flowchart"),
    GRAPH("graph", "completion.mermaid.diagram.graph"),
    SEQUENCE("sequenceDiagram", "completion.mermaid.diagram.sequence"),
    CLASS("classDiagram", "completion.mermaid.diagram.class"),
    ER("erDiagram", "completion.mermaid.diagram.er"),
    STATE("stateDiagram-v2", "completion.mermaid.diagram.state"),
    STATE_V1("stateDiagram", "completion.mermaid.diagram.state"),
    GANTT("gantt", "completion.mermaid.diagram.gantt"),
    PIE("pie", "completion.mermaid.diagram.pie"),
    GIT_GRAPH("gitGraph", "completion.mermaid.diagram.gitGraph"),
    MINDMAP("mindmap", "completion.mermaid.diagram.mindmap"),
    TIMELINE("timeline", "completion.mermaid.diagram.timeline"),
    JOURNEY("journey", "completion.mermaid.diagram.journey"),
    SANKEY("sankey-beta", "completion.mermaid.diagram.sankey"),
    XY_CHART("xychart-beta", "completion.mermaid.diagram.xyChart"),
    QUADRANT("quadrantChart", "completion.mermaid.diagram.quadrant"),
    REQUIREMENT("requirementDiagram", "completion.mermaid.diagram.requirement"),
    C4_CONTEXT("C4Context", "completion.mermaid.diagram.c4"),
    C4_CONTAINER("C4Container", "completion.mermaid.diagram.c4"),
    C4_COMPONENT("C4Component", "completion.mermaid.diagram.c4"),
    C4_DYNAMIC("C4Dynamic", "completion.mermaid.diagram.c4"),
    C4_DEPLOYMENT("C4Deployment", "completion.mermaid.diagram.c4"),
    ZENUML("zenuml", "completion.mermaid.diagram.zenuml"),
    KANBAN("kanban", "completion.mermaid.diagram.kanban"),
    BLOCK("block-beta", "completion.mermaid.diagram.block"),
    PACKET("packet-beta", "completion.mermaid.diagram.packet"),
    ARCHITECTURE("architecture-beta", "completion.mermaid.diagram.architecture"),
    VENN("venn-beta", "completion.mermaid.diagram.venn"),
    ISHIKAWA("ishikawa-beta", "completion.mermaid.diagram.ishikawa"),
}

/**
 * Static completion catalogs for keywords, arrows, directions, and directives.
 * Pure data — no IDE dependencies beyond PSI tree navigation in [detectDiagramKind].
 */
object MermaidCompletionData {

    /** Top-5 diagram types shown in bold in the completion popup. */
    val POPULAR_TYPES: Set<MermaidDiagramKind> = setOf(
        MermaidDiagramKind.FLOWCHART,
        MermaidDiagramKind.SEQUENCE,
        MermaidDiagramKind.CLASS,
        MermaidDiagramKind.ER,
        MermaidDiagramKind.STATE,
    )

    /** Map from diagram type keyword text to [MermaidDiagramKind]. */
    private val KEYWORD_TO_KIND: Map<String, MermaidDiagramKind> by lazy {
        MermaidDiagramKind.entries.associateBy { it.keyword }
    }

    // ── Keyword sets per diagram type ──────────────────────────────────

    private val SHARED_KEYWORDS = setOf("accTitle", "accDescr")

    private val FLOWCHART_KEYWORDS = setOf(
        "subgraph", "direction", "style", "linkStyle", "classDef", "class",
        "click", "callback", "interpolate", "end",
    )

    private val SEQUENCE_KEYWORDS = setOf(
        "participant", "actor", "loop", "alt", "else", "opt", "par",
        "critical", "break", "rect", "note", "over", "left", "right",
        "activate", "deactivate", "autonumber", "link", "links",
        "create", "destroy", "box", "and", "end",
    )

    private val CLASS_KEYWORDS = setOf(
        "namespace", "annotation", "class", "end",
    )

    private val STATE_KEYWORDS = setOf(
        "state", "as", "note", "end",
    )

    private val ER_KEYWORDS = emptySet<String>()

    private val GANTT_KEYWORDS = setOf(
        "title", "section", "dateFormat", "axisFormat", "tickInterval",
        "excludes", "includes", "todayMarker", "weekday",
    )

    private val PIE_KEYWORDS = setOf("title", "showData")

    private val GIT_GRAPH_KEYWORDS = setOf(
        "branch", "checkout", "merge", "commit", "cherry-pick", "tag", "order",
    )

    private val MINDMAP_KEYWORDS = setOf("root")

    private val QUADRANT_KEYWORDS = setOf(
        "title", "x-axis", "y-axis",
        "quadrant-1", "quadrant-2", "quadrant-3", "quadrant-4",
    )

    private val XY_CHART_KEYWORDS = setOf(
        "title", "x-axis", "y-axis", "bar", "line",
    )

    private val BLOCK_KEYWORDS = setOf("columns", "block", "space", "end")

    private val ARCHITECTURE_KEYWORDS = setOf("group", "service", "junction")

    private val REQUIREMENT_KEYWORDS = setOf(
        "element", "requirement", "functionalRequirement",
        "interfaceRequirement", "performanceRequirement", "designConstraint",
        "verifymethod", "docRef", "satisfies", "traces", "derives",
        "refines", "verifies", "copies",
    )

    private val C4_KEYWORDS = setOf(
        "Person", "Person_Ext", "System", "System_Ext", "SystemDb", "SystemQueue",
        "Container", "Container_Ext", "ContainerDb", "ContainerQueue",
        "Component", "Component_Ext", "ComponentDb", "ComponentQueue",
        "Boundary", "Enterprise_Boundary", "System_Boundary", "Container_Boundary",
        "Deployment_Node", "Node", "Node_L", "Node_R",
        "Rel", "Rel_U", "Rel_D", "Rel_L", "Rel_R", "Rel_Back", "BiRel",
        "UpdateLayoutConfig", "UpdateRelStyle", "UpdateElementStyle",
    )

    private val VENN_KEYWORDS = setOf("set", "union")

    private val TIMELINE_KEYWORDS = setOf("title", "section")

    private val JOURNEY_KEYWORDS = setOf("title", "section")

    private val KANBAN_KEYWORDS = setOf("title", "section")

    private val KEYWORDS_BY_KIND: Map<MermaidDiagramKind, Set<String>> by lazy {
        val map = mapOf(
            MermaidDiagramKind.FLOWCHART to FLOWCHART_KEYWORDS,
            MermaidDiagramKind.GRAPH to FLOWCHART_KEYWORDS,
            MermaidDiagramKind.SEQUENCE to SEQUENCE_KEYWORDS,
            MermaidDiagramKind.CLASS to CLASS_KEYWORDS,
            MermaidDiagramKind.ER to ER_KEYWORDS,
            MermaidDiagramKind.STATE to STATE_KEYWORDS,
            MermaidDiagramKind.STATE_V1 to STATE_KEYWORDS,
            MermaidDiagramKind.GANTT to GANTT_KEYWORDS,
            MermaidDiagramKind.PIE to PIE_KEYWORDS,
            MermaidDiagramKind.GIT_GRAPH to GIT_GRAPH_KEYWORDS,
            MermaidDiagramKind.MINDMAP to MINDMAP_KEYWORDS,
            MermaidDiagramKind.TIMELINE to TIMELINE_KEYWORDS,
            MermaidDiagramKind.JOURNEY to JOURNEY_KEYWORDS,
            MermaidDiagramKind.SANKEY to emptySet(),
            MermaidDiagramKind.XY_CHART to XY_CHART_KEYWORDS,
            MermaidDiagramKind.QUADRANT to QUADRANT_KEYWORDS,
            MermaidDiagramKind.REQUIREMENT to REQUIREMENT_KEYWORDS,
            MermaidDiagramKind.C4_CONTEXT to C4_KEYWORDS,
            MermaidDiagramKind.C4_CONTAINER to C4_KEYWORDS,
            MermaidDiagramKind.C4_COMPONENT to C4_KEYWORDS,
            MermaidDiagramKind.C4_DYNAMIC to C4_KEYWORDS,
            MermaidDiagramKind.C4_DEPLOYMENT to C4_KEYWORDS,
            MermaidDiagramKind.ZENUML to emptySet(),
            MermaidDiagramKind.KANBAN to KANBAN_KEYWORDS,
            MermaidDiagramKind.BLOCK to BLOCK_KEYWORDS,
            MermaidDiagramKind.PACKET to emptySet(),
            MermaidDiagramKind.ARCHITECTURE to ARCHITECTURE_KEYWORDS,
            MermaidDiagramKind.VENN to VENN_KEYWORDS,
            MermaidDiagramKind.ISHIKAWA to emptySet(),
        )
        val missing = MermaidDiagramKind.entries.toSet() - map.keys
        check(missing.isEmpty()) { "KEYWORDS_BY_KIND missing entries for: $missing" }
        map
    }

    /** Returns context-specific keywords + shared keywords for a diagram kind. */
    fun keywordsFor(kind: MermaidDiagramKind): Set<String> {
        val specific = KEYWORDS_BY_KIND[kind] ?: emptySet()
        return specific + SHARED_KEYWORDS
    }

    // ── Block keywords (mirror MermaidParserUtil) ──────────────────────

    private val FLOWCHART_BLOCK_KEYWORDS = setOf("subgraph")
    private val SEQUENCE_BLOCK_KEYWORDS = setOf(
        "loop", "alt", "opt", "par", "critical", "break", "rect", "box",
    )
    private val CLASS_BLOCK_KEYWORDS = setOf("namespace")

    /** Block keywords that open a block...end structure for a given diagram kind. */
    fun blockKeywordsFor(kind: MermaidDiagramKind): Set<String> = when (kind) {
        MermaidDiagramKind.FLOWCHART, MermaidDiagramKind.GRAPH -> FLOWCHART_BLOCK_KEYWORDS
        MermaidDiagramKind.SEQUENCE -> SEQUENCE_BLOCK_KEYWORDS
        MermaidDiagramKind.CLASS -> CLASS_BLOCK_KEYWORDS
        MermaidDiagramKind.BLOCK -> setOf("block")
        else -> emptySet()
    }

    /** Divider keywords valid inside blocks for a given diagram kind. */
    fun dividerKeywordsFor(kind: MermaidDiagramKind): Set<String> = when (kind) {
        MermaidDiagramKind.SEQUENCE -> setOf("else", "and")
        else -> emptySet()
    }

    // ── Arrow sets per diagram type ────────────────────────────────────

    data class ArrowEntry(val arrow: String, val descriptionKey: String)

    private val FLOWCHART_ARROWS = listOf(
        ArrowEntry("-->", "completion.mermaid.arrow.solidArrow"),
        ArrowEntry("--->", "completion.mermaid.arrow.longSolidArrow"),
        ArrowEntry("==>", "completion.mermaid.arrow.thickArrow"),
        ArrowEntry("-.->", "completion.mermaid.arrow.dottedArrow"),
        ArrowEntry("--x", "completion.mermaid.arrow.crossEnd"),
        ArrowEntry("--o", "completion.mermaid.arrow.circleEnd"),
        ArrowEntry("-->|text|", "completion.mermaid.arrow.labeledArrow"),
        ArrowEntry("<-->", "completion.mermaid.arrow.bidirectional"),
        ArrowEntry("~~~", "completion.mermaid.arrow.invisible"),
    )

    private val SEQUENCE_ARROWS = listOf(
        ArrowEntry("->>", "completion.mermaid.arrow.solidAsync"),
        ArrowEntry("-->>", "completion.mermaid.arrow.dottedAsync"),
        ArrowEntry("->", "completion.mermaid.arrow.solidNoArrow"),
        ArrowEntry("-->", "completion.mermaid.arrow.dottedNoArrow"),
        ArrowEntry("-x", "completion.mermaid.arrow.solidCross"),
        ArrowEntry("--x", "completion.mermaid.arrow.dottedCross"),
        ArrowEntry("-)", "completion.mermaid.arrow.solidOpen"),
        ArrowEntry("--)", "completion.mermaid.arrow.dottedOpen"),
    )

    private val CLASS_ARROWS = listOf(
        ArrowEntry("<|--", "completion.mermaid.arrow.inheritance"),
        ArrowEntry("--|>", "completion.mermaid.arrow.inheritance"),
        ArrowEntry("*--", "completion.mermaid.arrow.composition"),
        ArrowEntry("o--", "completion.mermaid.arrow.aggregation"),
        ArrowEntry("-->", "completion.mermaid.arrow.association"),
        ArrowEntry("..>", "completion.mermaid.arrow.dependency"),
        ArrowEntry("<..", "completion.mermaid.arrow.dependency"),
        ArrowEntry("..|>", "completion.mermaid.arrow.realization"),
        ArrowEntry("--", "completion.mermaid.arrow.link"),
        ArrowEntry("..", "completion.mermaid.arrow.dottedLink"),
    )

    private val ER_ARROWS = listOf(
        ArrowEntry("||--o{", "completion.mermaid.arrow.oneToMany"),
        ArrowEntry("||--|{", "completion.mermaid.arrow.oneToManyMandatory"),
        ArrowEntry("}o--||", "completion.mermaid.arrow.manyToOne"),
        ArrowEntry("}|--||", "completion.mermaid.arrow.manyToOneMandatory"),
        ArrowEntry("||--||", "completion.mermaid.arrow.oneToOne"),
        ArrowEntry("}o--o{", "completion.mermaid.arrow.manyToMany"),
    )

    private val STATE_ARROWS = listOf(
        ArrowEntry("-->", "completion.mermaid.arrow.transition"),
    )

    private val ARROWS_BY_KIND: Map<MermaidDiagramKind, List<ArrowEntry>> by lazy {
        buildMap {
            put(MermaidDiagramKind.FLOWCHART, FLOWCHART_ARROWS)
            put(MermaidDiagramKind.GRAPH, FLOWCHART_ARROWS)
            put(MermaidDiagramKind.SEQUENCE, SEQUENCE_ARROWS)
            put(MermaidDiagramKind.CLASS, CLASS_ARROWS)
            put(MermaidDiagramKind.ER, ER_ARROWS)
            put(MermaidDiagramKind.STATE, STATE_ARROWS)
            put(MermaidDiagramKind.STATE_V1, STATE_ARROWS)
        }
    }

    /** Returns arrows valid for a given diagram kind, or empty list if none. */
    fun arrowsFor(kind: MermaidDiagramKind): List<ArrowEntry> =
        ARROWS_BY_KIND[kind] ?: emptyList()

    // ── Flowchart directions ───────────────────────────────────────────

    data class DirectionEntry(val code: String, val descriptionKey: String)

    val FLOWCHART_DIRECTIONS = listOf(
        DirectionEntry("LR", "completion.mermaid.direction.lr"),
        DirectionEntry("RL", "completion.mermaid.direction.rl"),
        DirectionEntry("TD", "completion.mermaid.direction.td"),
        DirectionEntry("TB", "completion.mermaid.direction.tb"),
        DirectionEntry("BT", "completion.mermaid.direction.bt"),
    )

    // ── Directive config ───────────────────────────────────────────────

    val DIRECTIVE_CONFIG_KEYS = listOf("theme", "look", "fontFamily", "maxTextSize")
    val DIRECTIVE_THEME_VALUES = listOf("default", "dark", "forest", "neutral")
    val DIRECTIVE_LOOK_VALUES = listOf("classic", "handDrawn")

    // ── Context detection ──────────────────────────────────────────────

    /**
     * Detects the [MermaidDiagramKind] at the given PSI position by walking up the tree.
     * Returns `null` if the position is outside any diagram (file-level).
     */
    fun detectDiagramKind(position: PsiElement): MermaidDiagramKind? {
        val diagramNode = PsiTreeUtil.getParentOfType(
            position,
            MermaidFlowchartDiagram::class.java,
            MermaidSequenceDiagram::class.java,
            MermaidClassDiagram::class.java,
            MermaidErDiagram::class.java,
            MermaidStateDiagram::class.java,
            MermaidGenericDiagram::class.java,
        )
        return when (diagramNode) {
            is MermaidFlowchartDiagram -> {
                val typeText = diagramNode.node
                    .findChildByType(MermaidTokenTypes.DIAGRAM_TYPE)?.text
                if (typeText == "graph") MermaidDiagramKind.GRAPH else MermaidDiagramKind.FLOWCHART
            }
            is MermaidSequenceDiagram -> MermaidDiagramKind.SEQUENCE
            is MermaidClassDiagram -> MermaidDiagramKind.CLASS
            is MermaidErDiagram -> MermaidDiagramKind.ER
            is MermaidStateDiagram -> {
                val typeText = diagramNode.node
                    .findChildByType(MermaidTokenTypes.DIAGRAM_TYPE)?.text
                if (typeText == "stateDiagram") MermaidDiagramKind.STATE_V1 else MermaidDiagramKind.STATE
            }
            is MermaidGenericDiagram -> resolveGenericKind(diagramNode)
            else -> null
        }
    }

    private fun resolveGenericKind(diagram: MermaidGenericDiagram): MermaidDiagramKind? {
        val typeText = diagram.node
            .findChildByType(MermaidTokenTypes.DIAGRAM_TYPE)?.text
            ?: return null
        return KEYWORD_TO_KIND[typeText]
    }

    /** Returns `true` if the position is inside a [MermaidDiagramBody] or [MermaidBlockContent]. */
    fun isInsideDiagramBody(position: PsiElement): Boolean =
        PsiTreeUtil.getParentOfType(
            position,
            MermaidDiagramBody::class.java,
            MermaidBlockContent::class.java,
        ) != null
}
