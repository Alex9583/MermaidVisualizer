package com.alextdev.mermaidvisualizer.lang.inspection.fix

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind

/**
 * Quick fix that replaces an unknown diagram type keyword with [suggestion].
 */
class MermaidSuggestDiagramTypeFix(
    private val wrongType: String,
    suggestion: String,
) : MermaidReplaceTextFix(suggestion) {

    override fun getFamilyName(): String =
        MyMessageBundle.message("inspection.fix.replace.diagram.type.family")

    override fun getName(): String =
        MyMessageBundle.message("inspection.fix.replace.diagram.type", wrongType, replacement)

    companion object {
        private val ALL_KEYWORDS = MermaidDiagramKind.entries.map { it.keyword }.distinct()

        /**
         * Returns up to [limit] closest diagram type keywords to [wrongType],
         * sorted by edit distance (ascending).
         */
        fun suggestClosest(wrongType: String, limit: Int = 3): List<String> {
            return ALL_KEYWORDS
                .map { it to editDistance(wrongType.lowercase(), it.lowercase()) }
                .sortedBy { it.second }
                .take(limit)
                .map { it.first }
        }

        fun editDistance(a: String, b: String): Int {
            val m = a.length
            val n = b.length
            val dp = Array(m + 1) { IntArray(n + 1) }
            for (i in 0..m) dp[i][0] = i
            for (j in 0..n) dp[0][j] = j
            for (i in 1..m) {
                for (j in 1..n) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                }
            }
            return dp[m][n]
        }
    }
}