package com.alextdev.mermaidvisualizer

import com.google.gson.JsonParser
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO

private val LOG = Logger.getInstance("MermaidExport")

private const val NOTIFICATION_GROUP_ID = "Mermaid Visualizer"

internal fun notifyMermaid(project: Project?, message: String, type: NotificationType) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup(NOTIFICATION_GROUP_ID)
        .createNotification(message, type)
        .notify(project)
}

internal fun copySvgToClipboard(b64: String, project: Project?) {
    if (b64.isEmpty()) {
        LOG.warn("SVG extraction returned empty data")
        notifyMermaid(project, MyMessageBundle.message("markdown.export.copy.failed"), NotificationType.ERROR)
        return
    }
    try {
        val svgBytes = Base64.getDecoder().decode(b64)
        val svg = String(svgBytes, Charsets.UTF_8)
        CopyPasteManager.getInstance().setContents(StringSelection(svg))
        notifyMermaid(project, MyMessageBundle.message("markdown.export.copy.svg.success"), NotificationType.INFORMATION)
    } catch (e: IllegalArgumentException) {
        LOG.error("Failed to copy SVG: invalid base64 (length=${b64.length})", e)
        notifyMermaid(project, MyMessageBundle.message("markdown.export.copy.failed"), NotificationType.ERROR)
    } catch (e: Exception) {
        LOG.error("Failed to copy SVG", e)
        notifyMermaid(project, MyMessageBundle.message("markdown.export.copy.failed"), NotificationType.ERROR)
    }
}

internal fun copyPngToClipboard(b64: String, project: Project?) {
    if (b64.isEmpty()) {
        LOG.warn("PNG extraction returned empty data")
        notifyMermaid(project, MyMessageBundle.message("markdown.export.copy.failed"), NotificationType.ERROR)
        return
    }
    ApplicationManager.getApplication().executeOnPooledThread {
        try {
            val pngBytes = Base64.getDecoder().decode(b64)
            val image = ImageIO.read(ByteArrayInputStream(pngBytes))
                ?: throw IllegalStateException("Failed to decode PNG image from ${pngBytes.size} bytes")
            ApplicationManager.getApplication().invokeLater {
                CopyPasteManager.getInstance().setContents(ImageTransferable(image))
                notifyMermaid(project, MyMessageBundle.message("markdown.export.copy.png.success"), NotificationType.INFORMATION)
            }
        } catch (e: Exception) {
            LOG.error("Failed to copy PNG", e)
            ApplicationManager.getApplication().invokeLater {
                notifyMermaid(project, MyMessageBundle.message("markdown.export.copy.failed"), NotificationType.ERROR)
            }
        }
    }
}

internal data class ExportPayload(val svgB64: String, val pngB64: String?) {
    init {
        require(svgB64.isNotEmpty()) { "svgB64 must not be empty" }
        require(pngB64 == null || pngB64.isNotEmpty()) { "pngB64 must be null or non-empty" }
    }

    companion object {
        fun parse(jsonData: String): ExportPayload {
            val jsonObj = JsonParser.parseString(jsonData).asJsonObject
            val svg = jsonObj.get("svg")?.asString
                ?: throw IllegalArgumentException("Save payload missing 'svg' field")
            val png = jsonObj.get("png")?.asString?.ifEmpty { null }
            return ExportPayload(svg, png)
        }
    }
}

private fun promptSaveDestination(project: Project?): File? {
    val descriptor = FileSaverDescriptor(
        MyMessageBundle.message("action.export.dialog.title"),
        MyMessageBundle.message("action.export.dialog.description"),
    ).apply {
        withExtensionFilter(
            MyMessageBundle.message("action.export.filter.label"),
            "png", "svg",
        )
    }
    val wrapper = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
    return wrapper.save(null as VirtualFile?, "diagram")?.file
}

private fun decodePayloadBytes(payload: ExportPayload, isPng: Boolean): ByteArray {
    val b64 = if (isPng) requireNotNull(payload.pngB64) { "PNG data is null" } else payload.svgB64
    return Base64.getDecoder().decode(b64)
}

private fun writeDiagramFile(outputFile: File, payload: ExportPayload, isPng: Boolean, project: Project?) {
    ApplicationManager.getApplication().executeOnPooledThread {
        try {
            val bytes = decodePayloadBytes(payload, isPng)
            if (bytes.isEmpty()) {
                LOG.warn("Decoded payload bytes are empty for ${outputFile.name}")
                ApplicationManager.getApplication().invokeLater {
                    notifyMermaid(project, MyMessageBundle.message("markdown.export.save.failed"), NotificationType.ERROR)
                }
                return@executeOnPooledThread
            }
            outputFile.writeBytes(bytes)
            ApplicationManager.getApplication().invokeLater {
                notifyMermaid(
                    project,
                    MyMessageBundle.message("markdown.export.save.success", outputFile.name),
                    NotificationType.INFORMATION,
                )
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outputFile)
            }
        } catch (e: Exception) {
            LOG.error("Failed to save diagram to ${outputFile.name}", e)
            ApplicationManager.getApplication().invokeLater {
                notifyMermaid(project, MyMessageBundle.message("markdown.export.save.failed"), NotificationType.ERROR)
            }
        }
    }
}

internal fun saveDiagramToFile(jsonData: String, project: Project?) {
    try {
        val payload = ExportPayload.parse(jsonData)
        val outputFile = promptSaveDestination(project) ?: return
        val isPng = outputFile.extension.equals("png", ignoreCase = true)

        if (isPng && payload.pngB64 == null) {
            LOG.error("PNG extraction returned empty data for save")
            notifyMermaid(project, MyMessageBundle.message("markdown.export.save.failed"), NotificationType.ERROR)
            return
        }

        writeDiagramFile(outputFile, payload, isPng, project)
    } catch (e: IllegalArgumentException) {
        LOG.error("Failed to parse save request", e)
        notifyMermaid(project, MyMessageBundle.message("markdown.export.save.failed"), NotificationType.ERROR)
    } catch (e: Exception) {
        LOG.error("Failed to handle save request", e)
        notifyMermaid(project, MyMessageBundle.message("markdown.export.save.failed"), NotificationType.ERROR)
    }
}

internal class ImageTransferable(private val image: BufferedImage) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor
    override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
        return image
    }
}
