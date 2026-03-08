package com.alextdev.mermaidvisualizer.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import java.io.IOException
import java.util.Base64
import javax.swing.JComponent

private val LOG = Logger.getInstance("MermaidPreviewPanel")

private val BASE64_ENCODER: Base64.Encoder = Base64.getEncoder()

internal class MermaidPreviewPanel(parentDisposable: Disposable) {

    private val browser: JBCefBrowser = JBCefBrowser()
    private var pageLoaded = false
    private var pendingRender: (() -> Unit)? = null
    private val scrollQuery: JBCefJSQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private var scrollCallback: ((Double) -> Unit)? = null

    val component: JComponent get() = browser.component

    init {
        Disposer.register(parentDisposable, browser)
        Disposer.register(parentDisposable, scrollQuery)

        scrollQuery.addHandler { fractionStr ->
            val fraction = fractionStr.toDoubleOrNull()
            if (fraction != null) {
                ApplicationManager.getApplication().invokeLater {
                    scrollCallback?.invoke(fraction)
                }
            } else {
                LOG.warn("Received unparseable scroll fraction from JS bridge: '$fractionStr'")
            }
            null
        }

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(cefBrowser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain == true) {
                    if (httpStatusCode != 200 && httpStatusCode != 0) {
                        LOG.warn("JCEF page load completed with status $httpStatusCode")
                    }
                    ApplicationManager.getApplication().invokeLater {
                        if (browser.isDisposed) return@invokeLater
                        pageLoaded = true
                        injectScrollBridge()
                        pendingRender?.invoke()
                        pendingRender = null
                    }
                }
            }

            override fun onLoadError(
                cefBrowser: CefBrowser?,
                frame: CefFrame?,
                errorCode: CefLoadHandler.ErrorCode?,
                errorText: String?,
                failedUrl: String?,
            ) {
                LOG.warn("JCEF page load failed: errorCode=$errorCode, errorText=$errorText, url=$failedUrl")
                ApplicationManager.getApplication().invokeLater {
                    if (browser.isDisposed) return@invokeLater
                    pendingRender = null
                }
            }
        }, browser.cefBrowser)

        browser.loadHTML(cachedHtml)
    }

    fun render(source: String, isDark: Boolean, forceThemeRefresh: Boolean = false) {
        if (browser.isDisposed) return
        val encoded = BASE64_ENCODER.encodeToString(source.toByteArray(Charsets.UTF_8))
        val themeClass = if (isDark) "dark-theme" else ""
        val js = """
            document.body.className='$themeClass';
            window.renderDiagram('$encoded',$forceThemeRefresh).catch(function(e) {
                console.error('[MermaidVisualizer] renderDiagram failed: ' + e.message);
                var c = document.getElementById('mermaid-container');
                if (c) window.__showError(c, 'Render error: ' + e.message, null, document.body.classList.contains('dark-theme'));
            });
        """.trimIndent()

        val execute = { browser.cefBrowser.executeJavaScript(js, "mermaid-preview", 0) }

        if (pageLoaded) {
            execute()
        } else {
            pendingRender = execute
        }
    }

    fun setScrollCallback(callback: (Double) -> Unit) {
        scrollCallback = callback
    }

    fun scrollToFraction(fraction: Double) {
        if (!pageLoaded || browser.isDisposed) return
        val clamped = fraction.coerceIn(0.0, 1.0)
        browser.cefBrowser.executeJavaScript(
            "window.__scrollPreviewTo($clamped);",
            "mermaid-scroll",
            0,
        )
    }

    private fun injectScrollBridge() {
        if (browser.isDisposed) return
        val injection = scrollQuery.inject("fraction")
        val js = "window.__mermaidScrollBridge = function(fraction) { $injection };"
        browser.cefBrowser.executeJavaScript(js, "mermaid-scroll-bridge", 0)
    }

    companion object {
        fun isAvailable(): Boolean = JBCefApp.isSupported()

        private val cachedHtml: String by lazy { buildHtml() }

        private fun buildHtml(): String {
            val mermaidJs = loadResource("web/mermaid.min.js")
            val standaloneJs = loadResource("web/mermaid-standalone.js")
            val standaloneCss = loadResource("web/mermaid-standalone.css")

            return buildString(mermaidJs.length + standaloneJs.length + standaloneCss.length + 256) {
                append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                append("<style>")
                append(standaloneCss)
                append("</style>")
                append("</head><body>")
                append("<div id=\"mermaid-container\"></div>")
                append("<script>")
                append(mermaidJs)
                append("</script>")
                append("<script>")
                append(standaloneJs)
                append("</script>")
                append("</body></html>")
            }
        }

        private fun loadResource(path: String): String {
            val stream = MermaidPreviewPanel::class.java.classLoader.getResourceAsStream(path)
                ?: throw IllegalStateException("Required resource not found: $path — plugin installation may be corrupted")
            return try {
                stream.use { it.reader(Charsets.UTF_8).readText() }
            } catch (e: IOException) {
                throw IllegalStateException("Failed to load required resource: $path", e)
            }
        }
    }
}
