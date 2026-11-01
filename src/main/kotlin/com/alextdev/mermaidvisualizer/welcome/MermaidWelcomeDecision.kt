package com.alextdev.mermaidvisualizer.welcome

/** Pure once-per-version rules of the welcome / what's new page. */
internal object MermaidWelcomeDecision {

    /** True when [current] is a real version that differs from the last handled one (first install included). */
    fun isNewVersion(lastShown: String?, current: String?): Boolean {
        val version = current?.trim()
        return !version.isNullOrEmpty() && version != lastShown?.trim()
    }

    /** True when the page must be opened: a new version and the page is enabled in the settings. */
    fun shouldShow(lastShown: String?, current: String?, enabled: Boolean): Boolean =
        enabled && isNewVersion(lastShown, current)
}

/** What the startup activity does for a project opening. */
internal sealed interface WelcomeOutcome {
    /** Nothing to do: same version as last time, unknown version, or another project already took it. */
    data object Skip : WelcomeOutcome

    /** The new version was recorded but the page is disabled: nothing is opened. */
    data object RecordOnly : WelcomeOutcome

    /** The new version was recorded by this caller: open the page with the changes since [previousShown]. */
    data class Show(val version: String, val previousShown: String?) : WelcomeOutcome
}

/**
 * Applies the once-per-version rules to [state]. The version is recorded even when the page is disabled,
 * so that enabling it later does not show a stale page. Only the caller whose [MermaidWelcomeState.markShown]
 * succeeds gets [WelcomeOutcome.Show].
 */
internal fun recordWelcomeForVersion(state: MermaidWelcomeState, current: String?, enabled: Boolean): WelcomeOutcome {
    val previous = state.lastShownVersion
    if (!MermaidWelcomeDecision.isNewVersion(previous, current)) return WelcomeOutcome.Skip
    val version = current?.trim() ?: return WelcomeOutcome.Skip
    if (!state.markShown(version)) return WelcomeOutcome.Skip
    return if (MermaidWelcomeDecision.shouldShow(previous, version, enabled)) {
        WelcomeOutcome.Show(version, previous)
    } else {
        WelcomeOutcome.RecordOnly
    }
}
