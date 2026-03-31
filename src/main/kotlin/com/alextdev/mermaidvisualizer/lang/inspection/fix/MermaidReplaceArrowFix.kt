package com.alextdev.mermaidvisualizer.lang.inspection.fix

import com.alextdev.mermaidvisualizer.MyMessageBundle

/**
 * Quick fix that replaces an invalid arrow with a valid [replacement] arrow.
 */
class MermaidReplaceArrowFix(
    replacement: String,
) : MermaidReplaceTextFix(replacement) {

    override fun getFamilyName(): String =
        MyMessageBundle.message("inspection.fix.replace.arrow.family")

    override fun getName(): String =
        MyMessageBundle.message("inspection.fix.replace.arrow", replacement)
}