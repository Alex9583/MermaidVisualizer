package com.alextdev.mermaidvisualizer.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

internal const val DEFAULT_MAX_TEXT_SIZE = 100_000
internal const val DEFAULT_DEBOUNCE_MS = 300L
internal const val DEFAULT_MAX_HEIGHT_PERCENT = 60

internal const val MIN_MAX_TEXT_SIZE = 1_000
internal const val MAX_MAX_TEXT_SIZE = 10_000_000
internal const val MIN_DEBOUNCE_MS = 0L
internal const val MAX_DEBOUNCE_MS = 5_000L
internal const val MIN_MAX_HEIGHT_PERCENT = 20
internal const val MAX_MAX_HEIGHT_PERCENT = 100

internal const val DEFAULT_BACKGROUND_COLOR = "#FFFFFF"
internal const val DEFAULT_LINE_COLOR = "#888888"

internal fun clampMaxTextSize(value: Int): Int = value.coerceIn(MIN_MAX_TEXT_SIZE, MAX_MAX_TEXT_SIZE)
internal fun clampDebounceMs(value: Long): Long = value.coerceIn(MIN_DEBOUNCE_MS, MAX_DEBOUNCE_MS)
internal fun clampMaxHeightPercent(value: Int): Int = value.coerceIn(MIN_MAX_HEIGHT_PERCENT, MAX_MAX_HEIGHT_PERCENT)

private val HEX_COLOR_REGEX = Regex("^#[0-9a-fA-F]{6}$")

internal fun normalizeHexColor(value: String, fallback: String): String =
    if (HEX_COLOR_REGEX.matches(value)) value.uppercase() else fallback

private fun jsonEscape(s: String): String = s
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

@State(name = "MermaidVisualizerSettings", storages = [Storage("MermaidVisualizer.xml")])
@Service(Service.Level.APP)
internal class MermaidSettings : PersistentStateComponent<MermaidSettings.State> {

    data class State(
        var theme: MermaidTheme = MermaidTheme.AUTO,
        var look: MermaidLook = MermaidLook.CLASSIC,
        var layout: MermaidLayout = MermaidLayout.DEFAULT,
        var fontFamily: MermaidFontFamily = MermaidFontFamily.DEFAULT,
        var maxTextSize: Int = DEFAULT_MAX_TEXT_SIZE,
        var debounceMs: Long = DEFAULT_DEBOUNCE_MS,
        var maxHeightPercent: Int = DEFAULT_MAX_HEIGHT_PERCENT,
        var overrideBackgroundColor: Boolean = false,
        var backgroundColor: String = DEFAULT_BACKGROUND_COLOR,
        var overrideLineColor: Boolean = false,
        var lineColor: String = DEFAULT_LINE_COLOR,
    ) {
        init {
            maxTextSize = maxTextSize.coerceIn(MIN_MAX_TEXT_SIZE, MAX_MAX_TEXT_SIZE)
            debounceMs = debounceMs.coerceIn(MIN_DEBOUNCE_MS, MAX_DEBOUNCE_MS)
            maxHeightPercent = maxHeightPercent.coerceIn(MIN_MAX_HEIGHT_PERCENT, MAX_MAX_HEIGHT_PERCENT)
            backgroundColor = normalizeHexColor(backgroundColor, DEFAULT_BACKGROUND_COLOR)
            lineColor = normalizeHexColor(lineColor, DEFAULT_LINE_COLOR)
        }
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state.copy(
            maxTextSize = state.maxTextSize.coerceIn(MIN_MAX_TEXT_SIZE, MAX_MAX_TEXT_SIZE),
            debounceMs = state.debounceMs.coerceIn(MIN_DEBOUNCE_MS, MAX_DEBOUNCE_MS),
            maxHeightPercent = state.maxHeightPercent.coerceIn(MIN_MAX_HEIGHT_PERCENT, MAX_MAX_HEIGHT_PERCENT),
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
        append('"')
        val layout = myState.layout
        if (layout.jsValue != null) {
            append(",\"layout\":\"")
            append(jsonEscape(layout.jsValue))
            append('"')
        }
        append(",\"maxTextSize\":")
        append(myState.maxTextSize)
        append(",\"maxHeightPercent\":")
        append(myState.maxHeightPercent)
        val font = myState.fontFamily
        if (font.cssValue != null) {
            append(",\"fontFamily\":\"")
            append(jsonEscape(font.cssValue))
            append('"')
        }
        if (myState.overrideBackgroundColor) {
            append(",\"backgroundColor\":\"")
            append(normalizeHexColor(myState.backgroundColor, DEFAULT_BACKGROUND_COLOR))
            append('"')
        }
        if (myState.overrideLineColor) {
            append(",\"lineColor\":\"")
            append(normalizeHexColor(myState.lineColor, DEFAULT_LINE_COLOR))
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
    NEO("neo", "settings.mermaid.theme.neo"),
    NEO_DARK("neo-dark", "settings.mermaid.theme.neoDark"),
    REDUX("redux", "settings.mermaid.theme.redux"),
    REDUX_DARK("redux-dark", "settings.mermaid.theme.reduxDark"),
    REDUX_COLOR("redux-color", "settings.mermaid.theme.reduxColor"),
    REDUX_DARK_COLOR("redux-dark-color", "settings.mermaid.theme.reduxDarkColor"),
}

/**
 * Layout engine passed to `mermaid.initialize({ layout })`. [DEFAULT] sends nothing, so Mermaid's own
 * default applies (ELK since Mermaid 12; mindmap keeps cose-bilkent, swimlane keeps its lane layout).
 * A `layout` set in a diagram's front matter always wins over this setting.
 */
enum class MermaidLayout(val jsValue: String?, val displayKey: String) {
    DEFAULT(null, "settings.mermaid.layout.default"),
    DAGRE("dagre", "settings.mermaid.layout.dagre"),
    ELK("elk", "settings.mermaid.layout.elk"),
}

enum class MermaidLook(val jsValue: String, val displayKey: String) {
    CLASSIC("classic", "settings.mermaid.look.classic"),
    HAND_DRAWN("handDrawn", "settings.mermaid.look.handDrawn"),
    NEO("neo", "settings.mermaid.look.neo"),
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
