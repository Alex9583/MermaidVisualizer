package com.alextdev.mermaidvisualizer.settings

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

internal class MermaidSettingsConfigurable : Configurable {

    private var themeSelection: MermaidTheme? = MermaidTheme.AUTO
    private var lookSelection: MermaidLook? = MermaidLook.CLASSIC
    private var fontFamilySelection: MermaidFontFamily? = MermaidFontFamily.DEFAULT
    private var maxTextSizeText: String = DEFAULT_MAX_TEXT_SIZE.toString()
    private var debounceMsText: String = DEFAULT_DEBOUNCE_MS.toString()

    private var dialogPanel: DialogPanel? = null

    override fun getDisplayName(): String = MyMessageBundle.message("settings.mermaid.displayName")

    override fun createComponent(): JComponent {
        val settings = service<MermaidSettings>()
        loadFromState(settings.state)

        val p = panel {
            group(MyMessageBundle.message("settings.mermaid.group.rendering")) {
                row(MyMessageBundle.message("settings.mermaid.theme.label")) {
                    comboBox(MermaidTheme.entries)
                        .bindItem(::themeSelection)
                        .applyToComponent {
                            renderer = SimpleListCellRenderer.create("") { theme: MermaidTheme ->
                                MyMessageBundle.message(theme.displayKey)
                            }
                        }
                }
                row(MyMessageBundle.message("settings.mermaid.look.label")) {
                    comboBox(MermaidLook.entries)
                        .bindItem(::lookSelection)
                        .applyToComponent {
                            renderer = SimpleListCellRenderer.create("") { look: MermaidLook ->
                                MyMessageBundle.message(look.displayKey)
                            }
                        }
                }
                row(MyMessageBundle.message("settings.mermaid.fontFamily.label")) {
                    comboBox(MermaidFontFamily.entries)
                        .bindItem(::fontFamilySelection)
                        .applyToComponent {
                            renderer = SimpleListCellRenderer.create("") { font: MermaidFontFamily ->
                                MyMessageBundle.message(font.displayKey)
                            }
                        }
                }
                row(MyMessageBundle.message("settings.mermaid.maxTextSize.label")) {
                    textField()
                        .bindText(::maxTextSizeText)
                        .columns(10)
                        .validationOnInput {
                            val value = it.text.trim().toIntOrNull()
                            when {
                                value == null -> error(MyMessageBundle.message("settings.mermaid.validation.notANumber"))
                                value !in MIN_MAX_TEXT_SIZE..MAX_MAX_TEXT_SIZE ->
                                    warning(MyMessageBundle.message("settings.mermaid.maxTextSize.outOfRange"))
                                else -> null
                            }
                        }
                        .comment(MyMessageBundle.message("settings.mermaid.maxTextSize.comment"))
                }
                row(MyMessageBundle.message("settings.mermaid.debounce.label")) {
                    textField()
                        .bindText(::debounceMsText)
                        .columns(6)
                        .validationOnInput {
                            val value = it.text.trim().toLongOrNull()
                            when {
                                value == null -> error(MyMessageBundle.message("settings.mermaid.validation.notANumber"))
                                value !in MIN_DEBOUNCE_MS..MAX_DEBOUNCE_MS ->
                                    warning(MyMessageBundle.message("settings.mermaid.debounce.outOfRange"))
                                else -> null
                            }
                        }
                        .comment(MyMessageBundle.message("settings.mermaid.debounce.comment"))
                }
            }
        }
        dialogPanel = p
        return p
    }

    override fun isModified(): Boolean {
        val settings = service<MermaidSettings>()
        val state = settings.state
        dialogPanel?.apply()
        return (themeSelection ?: MermaidTheme.AUTO) != state.theme ||
            (lookSelection ?: MermaidLook.CLASSIC) != state.look ||
            (fontFamilySelection ?: MermaidFontFamily.DEFAULT) != state.fontFamily ||
            parseMaxTextSize() != state.maxTextSize ||
            parseDebounceMs() != state.debounceMs
    }

    override fun apply() {
        dialogPanel?.apply()
        val settings = service<MermaidSettings>()
        val oldState = settings.state.copy()
        val newState = MermaidSettings.State(
            theme = themeSelection ?: MermaidTheme.AUTO,
            look = lookSelection ?: MermaidLook.CLASSIC,
            fontFamily = fontFamilySelection ?: MermaidFontFamily.DEFAULT,
            maxTextSize = parseMaxTextSize(),
            debounceMs = parseDebounceMs(),
        )
        settings.loadState(newState)
        maxTextSizeText = settings.state.maxTextSize.toString()
        debounceMsText = settings.state.debounceMs.toString()
        if (settings.state != oldState) {
            ApplicationManager.getApplication().messageBus
                .syncPublisher(MERMAID_SETTINGS_TOPIC)
                .settingsChanged()
        }
    }

    override fun reset() {
        val settings = service<MermaidSettings>()
        loadFromState(settings.state)
        dialogPanel?.reset()
    }

    private fun loadFromState(state: MermaidSettings.State) {
        themeSelection = state.theme
        lookSelection = state.look
        fontFamilySelection = state.fontFamily
        maxTextSizeText = state.maxTextSize.toString()
        debounceMsText = state.debounceMs.toString()
    }

    private fun parseMaxTextSize(): Int {
        val raw = maxTextSizeText.trim().toIntOrNull() ?: DEFAULT_MAX_TEXT_SIZE
        return clampMaxTextSize(raw)
    }

    private fun parseDebounceMs(): Long {
        val raw = debounceMsText.trim().toLongOrNull() ?: DEFAULT_DEBOUNCE_MS
        return clampDebounceMs(raw)
    }
}
