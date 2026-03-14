package com.alextdev.mermaidvisualizer.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

internal const val DEFAULT_MAX_TEXT_SIZE = 100_000
internal const val DEFAULT_DEBOUNCE_MS = 300L

internal const val MIN_MAX_TEXT_SIZE = 1_000
internal const val MAX_MAX_TEXT_SIZE = 10_000_000
internal const val MIN_DEBOUNCE_MS = 0L
internal const val MAX_DEBOUNCE_MS = 5_000L

internal fun clampMaxTextSize(value: Int): Int = value.coerceIn(MIN_MAX_TEXT_SIZE, MAX_MAX_TEXT_SIZE)
internal fun clampDebounceMs(value: Long): Long = value.coerceIn(MIN_DEBOUNCE_MS, MAX_DEBOUNCE_MS)

private fun jsonEscape(s: String): String = s
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

@State(name = "MermaidVisualizerSettings", storages = [Storage("MermaidVisualizer.xml")])
@Service(Service.Level.APP)
internal class MermaidSettings : PersistentStateComponent<MermaidSettings.State> {

    data class State(
        var theme: MermaidTheme = MermaidTheme.AUTO,
        var look: MermaidLook = MermaidLook.CLASSIC,
        var fontFamily: MermaidFontFamily = MermaidFontFamily.DEFAULT,
        var maxTextSize: Int = DEFAULT_MAX_TEXT_SIZE,
        var debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    ) {
        init {
            maxTextSize = maxTextSize.coerceIn(MIN_MAX_TEXT_SIZE, MAX_MAX_TEXT_SIZE)
            debounceMs = debounceMs.coerceIn(MIN_DEBOUNCE_MS, MAX_DEBOUNCE_MS)
        }
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state.copy(
            maxTextSize = state.maxTextSize.coerceIn(MIN_MAX_TEXT_SIZE, MAX_MAX_TEXT_SIZE),
            debounceMs = state.debounceMs.coerceIn(MIN_DEBOUNCE_MS, MAX_DEBOUNCE_MS),
        )
    }

    fun resolveJsTheme(isDark: Boolean): String =
        myState.theme.jsValue ?: if (isDark) "dark" else "default"

    fun toJsConfigJson(): String = buildString {
        append('{')
        val theme = myState.theme
        if (theme.jsValue != null) {
            append("\"theme\":\"")
            append(jsonEscape(theme.jsValue))
            append("\",")
        }
        append("\"look\":\"")
        append(jsonEscape(myState.look.jsValue))
        append("\",\"maxTextSize\":")
        append(myState.maxTextSize)
        val font = myState.fontFamily
        if (font.cssValue != null) {
            append(",\"fontFamily\":\"")
            append(jsonEscape(font.cssValue))
            append('"')
        }
        append('}')
    }
}

enum class MermaidTheme(val jsValue: String?, val displayKey: String) {
    AUTO(null, "settings.mermaid.theme.auto"),
    DEFAULT("default", "settings.mermaid.theme.default"),
    DARK("dark", "settings.mermaid.theme.dark"),
    FOREST("forest", "settings.mermaid.theme.forest"),
    NEUTRAL("neutral", "settings.mermaid.theme.neutral"),
}

enum class MermaidLook(val jsValue: String, val displayKey: String) {
    CLASSIC("classic", "settings.mermaid.look.classic"),
    HAND_DRAWN("handDrawn", "settings.mermaid.look.handDrawn"),
}

enum class MermaidFontFamily(val cssValue: String?, val displayKey: String) {
    DEFAULT(null, "settings.mermaid.fontFamily.default"),
    ARIAL("Arial", "settings.mermaid.fontFamily.arial"),
    COMIC_SANS_MS("Comic Sans MS", "settings.mermaid.fontFamily.comicSansMs"),
    COURIER_NEW("Courier New", "settings.mermaid.fontFamily.courierNew"),
    GEORGIA("Georgia", "settings.mermaid.fontFamily.georgia"),
    HELVETICA("Helvetica", "settings.mermaid.fontFamily.helvetica"),
    TREBUCHET_MS("Trebuchet MS", "settings.mermaid.fontFamily.trebuchetMs"),
    VERDANA("Verdana", "settings.mermaid.fontFamily.verdana"),
}
