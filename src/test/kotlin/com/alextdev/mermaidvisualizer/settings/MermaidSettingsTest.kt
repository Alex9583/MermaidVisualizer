package com.alextdev.mermaidvisualizer.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MermaidSettingsTest {

    private lateinit var settings: MermaidSettings

    @BeforeEach
    fun setUp() {
        settings = MermaidSettings()
    }

    @Test
    fun `default state has expected values`() {
        val state = settings.state
        assertEquals(MermaidTheme.AUTO, state.theme)
        assertEquals(MermaidLook.CLASSIC, state.look)
        assertEquals(MermaidFontFamily.DEFAULT, state.fontFamily)
        assertEquals(DEFAULT_MAX_TEXT_SIZE, state.maxTextSize)
        assertEquals(DEFAULT_DEBOUNCE_MS, state.debounceMs)
    }

    @Test
    fun `resolveJsTheme with Auto returns dark for dark IDE`() {
        assertEquals("dark", settings.resolveJsTheme(isDark = true))
    }

    @Test
    fun `resolveJsTheme with Auto returns default for light IDE`() {
        assertEquals("default", settings.resolveJsTheme(isDark = false))
    }

    @Test
    fun `resolveJsTheme with explicit theme ignores isDark`() {
        settings.loadState(MermaidSettings.State(theme = MermaidTheme.FOREST))
        assertEquals("forest", settings.resolveJsTheme(isDark = true))
        assertEquals("forest", settings.resolveJsTheme(isDark = false))
    }

    @Test
    fun `resolveJsTheme for all explicit themes`() {
        for (theme in MermaidTheme.entries) {
            settings.loadState(MermaidSettings.State(theme = theme))
            if (theme == MermaidTheme.AUTO) {
                assertEquals("dark", settings.resolveJsTheme(isDark = true))
                assertEquals("default", settings.resolveJsTheme(isDark = false))
            } else {
                assertEquals(theme.jsValue, settings.resolveJsTheme(isDark = true))
                assertEquals(theme.jsValue, settings.resolveJsTheme(isDark = false))
            }
        }
    }

    @Test
    fun `toJsConfigJson with defaults omits theme and fontFamily`() {
        val json = settings.toJsConfigJson()
        assertFalse(json.contains("\"theme\""), "Auto theme should not emit theme field")
        assertFalse(json.contains("\"fontFamily\""), "Default fontFamily should be omitted")
        assertTrue(json.contains("\"look\":\"classic\""))
        assertTrue(json.contains("\"maxTextSize\":100000"))
    }

    @Test
    fun `toJsConfigJson with explicit theme includes theme`() {
        settings.loadState(MermaidSettings.State(theme = MermaidTheme.FOREST))
        val json = settings.toJsConfigJson()
        assertTrue(json.contains("\"theme\":\"forest\""))
    }

    @Test
    fun `toJsConfigJson with fontFamily includes it`() {
        settings.loadState(MermaidSettings.State(fontFamily = MermaidFontFamily.ARIAL))
        val json = settings.toJsConfigJson()
        assertTrue(json.contains("\"fontFamily\":\"Arial\""))
    }

    @Test
    fun `toJsConfigJson with hand-drawn look`() {
        settings.loadState(MermaidSettings.State(look = MermaidLook.HAND_DRAWN))
        val json = settings.toJsConfigJson()
        assertTrue(json.contains("\"look\":\"handDrawn\""))
    }

    @Test
    fun `toJsConfigJson with multi-word font uses correct cssValue`() {
        settings.loadState(MermaidSettings.State(fontFamily = MermaidFontFamily.COMIC_SANS_MS))
        val json = settings.toJsConfigJson()
        assertTrue(json.contains("\"fontFamily\":\"Comic Sans MS\""))
    }

    @Test
    fun `loadState clamps maxTextSize below minimum`() {
        settings.loadState(MermaidSettings.State(maxTextSize = 500))
        assertEquals(1000, settings.state.maxTextSize)
    }

    @Test
    fun `loadState clamps maxTextSize above maximum`() {
        settings.loadState(MermaidSettings.State(maxTextSize = 20_000_000))
        assertEquals(10_000_000, settings.state.maxTextSize)
    }

    @Test
    fun `loadState clamps debounceMs below minimum`() {
        settings.loadState(MermaidSettings.State(debounceMs = -100))
        assertEquals(0, settings.state.debounceMs)
    }

    @Test
    fun `loadState clamps debounceMs above maximum`() {
        settings.loadState(MermaidSettings.State(debounceMs = 10_000))
        assertEquals(5000, settings.state.debounceMs)
    }

    @Test
    fun `MermaidFontFamily enum has correct cssValues`() {
        assertNull(MermaidFontFamily.DEFAULT.cssValue)
        assertEquals("Arial", MermaidFontFamily.ARIAL.cssValue)
        assertEquals("Comic Sans MS", MermaidFontFamily.COMIC_SANS_MS.cssValue)
        assertEquals("Courier New", MermaidFontFamily.COURIER_NEW.cssValue)
        assertEquals("Georgia", MermaidFontFamily.GEORGIA.cssValue)
        assertEquals("Helvetica", MermaidFontFamily.HELVETICA.cssValue)
        assertEquals("Trebuchet MS", MermaidFontFamily.TREBUCHET_MS.cssValue)
        assertEquals("Verdana", MermaidFontFamily.VERDANA.cssValue)
    }

    @Test
    fun `state round-trip preserves values`() {
        val original = MermaidSettings.State(
            theme = MermaidTheme.NEUTRAL,
            look = MermaidLook.HAND_DRAWN,
            fontFamily = MermaidFontFamily.GEORGIA,
            maxTextSize = 50_000,
            debounceMs = 500,
        )
        settings.loadState(original)
        val loaded = settings.state
        assertEquals(MermaidTheme.NEUTRAL, loaded.theme)
        assertEquals(MermaidLook.HAND_DRAWN, loaded.look)
        assertEquals(MermaidFontFamily.GEORGIA, loaded.fontFamily)
        assertEquals(50_000, loaded.maxTextSize)
        assertEquals(500L, loaded.debounceMs)
    }

    @Test
    fun `MermaidTheme enum has correct jsValues`() {
        assertEquals(null, MermaidTheme.AUTO.jsValue)
        assertEquals("default", MermaidTheme.DEFAULT.jsValue)
        assertEquals("dark", MermaidTheme.DARK.jsValue)
        assertEquals("forest", MermaidTheme.FOREST.jsValue)
        assertEquals("neutral", MermaidTheme.NEUTRAL.jsValue)
    }

    @Test
    fun `MermaidLook enum has correct jsValues`() {
        assertEquals("classic", MermaidLook.CLASSIC.jsValue)
        assertEquals("handDrawn", MermaidLook.HAND_DRAWN.jsValue)
    }
}
