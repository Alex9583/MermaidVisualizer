package com.alextdev.mermaidvisualizer.editor

import com.alextdev.mermaidvisualizer.copyPngToClipboard
import com.alextdev.mermaidvisualizer.copySvgToClipboard
import com.alextdev.mermaidvisualizer.saveDiagramToFile
import com.alextdev.mermaidvisualizer.settings.MermaidSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
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

internal class MermaidPreviewPanel(
    private val project: Project?,
    parentDisposable: Disposable,
) {

    private val browser: JBCefBrowser = JBCefBrowser()
    private var pageLoaded = false
    private var pendingRender: (() -> Unit)? = null
    private val scrollQuery: JBCefJSQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private val copySvgQuery: JBCefJSQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private val copyPngQuery: JBCefJSQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private val saveQuery: JBCefJSQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private val errorQuery: JBCefJSQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private var errorCallback: ((String) -> Unit)? = null

    val component: JComponent get() = browser.component

    init {
        Disposer.register(parentDisposable, browser)
        Disposer.register(parentDisposable, scrollQuery)
        Disposer.register(parentDisposable, copySvgQuery)
        Disposer.register(parentDisposable, copyPngQuery)
        Disposer.register(parentDisposable, saveQuery)
        Disposer.register(parentDisposable, errorQuery)

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

        copySvgQuery.addHandler { b64 ->
            ApplicationManager.getApplication().invokeLater { copySvgToClipboard(b64, project) }
            null
        }

        copyPngQuery.addHandler { b64 ->
            ApplicationManager.getApplication().invokeLater { copyPngToClipboard(b64, project) }
            null
        }

        saveQuery.addHandler { jsonData ->
            ApplicationManager.getApplication().invokeLater { saveDiagramToFile(jsonData, project) }
            null
        }

        errorQuery.addHandler { payload ->
            ApplicationManager.getApplication().invokeLater {
                if (!browser.isDisposed) {
                    errorCallback?.invoke(payload)
                }
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
                        injectExportBridges()
                        injectErrorBridge()
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
                if (errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED) return
                LOG.error("JCEF page load failed: errorCode=$errorCode, errorText=$errorText, url=$failedUrl")
                ApplicationManager.getApplication().invokeLater {
                    if (browser.isDisposed) return@invokeLater
                    pendingRender = null
                    val errorMsg = (errorText ?: "Unknown error")
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n")
                        .replace("\r", "")
                    browser.cefBrowser.executeJavaScript(
                        """
                        document.body.style.cssText = 'display:flex;align-items:center;justify-content:center;height:100vh;margin:0;font-family:sans-serif;color:#999';
                        document.body.textContent = 'Preview failed to load: $errorMsg';
                        """.trimIndent(),
                        "mermaid-load-error",
                        0,
                    )
                }
            }
        }, browser.cefBrowser)

        browser.loadHTML(cachedHtml)
    }

    private var scrollCallback: ((Double) -> Unit)? = null

    fun render(source: String, isDark: Boolean, forceThemeRefresh: Boolean = false, generation: Long = 0L) {
        if (browser.isDisposed) return
        val encoded = BASE64_ENCODER.encodeToString(source.toByteArray(Charsets.UTF_8))
        val themeClass = if (isDark) "dark-theme" else ""
        val configJson = service<MermaidSettings>().toJsConfigJson()
        val js = """
            window.__MERMAID_CONFIG=$configJson;
            document.body.className='$themeClass';
            window.renderDiagram('$encoded',$forceThemeRefresh,$generation).catch(function(e) {
                console.error('[MermaidVisualizer] renderDiagram failed: ' + e.message);
                const c = document.getElementById('mermaid-container');
                if (c) window.__showError(c, 'Render error: ' + e.message, null, document.body.classList.contains('dark-theme'));
                if (typeof window.__mermaidErrorBridge === 'function') {
                    try { window.__mermaidErrorBridge(JSON.stringify({status:'error',message:e.message||String(e),line:null,column:null,gen:$generation})); } catch(_) {}
                }
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

    fun setErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
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

    private fun injectErrorBridge() {
        if (browser.isDisposed) return
        val injection = errorQuery.inject("payload")
        val js = "window.__mermaidErrorBridge = function(payload) { $injection };"
        browser.cefBrowser.executeJavaScript(js, "mermaid-error-bridge", 0)
    }

    private fun injectExportBridges() {
        if (browser.isDisposed) return
        val copySvgInjection = copySvgQuery.inject("b64")
        val copyPngInjection = copyPngQuery.inject("b64")
        val saveInjection = saveQuery.inject("payload")
        val js = """
            window.__copySvgBridge = function(b64) { $copySvgInjection };
            window.__copyPngBridge = function(b64) { $copyPngInjection };
            window.__saveBridge = function(payload) { $saveInjection };
        """.trimIndent()
        browser.cefBrowser.executeJavaScript(js, "mermaid-export-bridge", 0)
    }

    companion object {
        fun isAvailable(): Boolean = JBCefApp.isSupported()

        private val cachedHtml: String by lazy { buildHtml() }

        private fun buildHtml(): String {
            val mermaidJs = loadResource("web/mermaid.min.js")
            val zoomJs = loadResource("web/mermaid-zoom.js")
            val coreJs = loadResource("web/mermaid-core.js")
            val standaloneJs = loadResource("web/mermaid-standalone.js")
            val standaloneCss = loadResource("web/mermaid-standalone.css")
            val shadowCss = loadResource("web/mermaid-shadow.css")

            return buildString(mermaidJs.length + zoomJs.length + coreJs.length + standaloneJs.length + standaloneCss.length + shadowCss.length + 512) {
                append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">")
                append("<style>")
                append(standaloneCss)
                append("</style>")
                append("<style id=\"mermaid-shadow-styles\">")
                append(shadowCss)
                append("</style>")
                append("</head><body>")
                append("<div id=\"mermaid-container\"></div>")
                append("<script>")
                append(mermaidJs)
                append("</script>")
                append("<script>")
                append(coreJs)
                append("</script>")
                append("<script>")
                append(zoomJs)
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
