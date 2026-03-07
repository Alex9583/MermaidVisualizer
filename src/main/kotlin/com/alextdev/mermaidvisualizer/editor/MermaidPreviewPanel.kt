package com.alextdev.mermaidvisualizer.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
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

    val component: JComponent get() = browser.component

    init {
        Disposer.register(parentDisposable, browser)

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(cefBrowser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain == true) {
                    if (httpStatusCode != 200 && httpStatusCode != 0) {
                        LOG.warn("JCEF page load completed with status $httpStatusCode")
                    }
                    ApplicationManager.getApplication().invokeLater {
                        if (browser.isDisposed) return@invokeLater
                        pageLoaded = true
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
                LOG.error("JCEF page load failed: errorCode=$errorCode, errorText=$errorText, url=$failedUrl")
                ApplicationManager.getApplication().invokeLater {
                    if (browser.isDisposed) return@invokeLater
                    pageLoaded = true
                    pendingRender = null
                }
            }
        }, browser.cefBrowser)

        browser.loadHTML(cachedHtml)
    }

    fun render(source: String, isDark: Boolean, forceThemeRefresh: Boolean = false) {
        val encoded = BASE64_ENCODER.encodeToString(source.toByteArray(Charsets.UTF_8))
        val themeClass = if (isDark) "dark-theme" else ""
        val js = """
            try {
                document.body.className='$themeClass';
                window.renderDiagram('$encoded',$forceThemeRefresh);
            } catch(e) {
                console.error('[MermaidVisualizer] renderDiagram failed: ' + e.message);
                var c = document.getElementById('mermaid-container');
                if (c) c.textContent = 'Render error: ' + e.message;
            }
        """.trimIndent()

        if (pageLoaded) {
            browser.cefBrowser.executeJavaScript(js, "mermaid-preview", 0)
        } else {
            // Only keep the latest pending render
            pendingRender = { browser.cefBrowser.executeJavaScript(js, "mermaid-preview", 0) }
        }
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
