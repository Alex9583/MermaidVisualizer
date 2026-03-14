package com.alextdev.mermaidvisualizer

import com.google.gson.JsonSyntaxException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.util.Base64

class MermaidExportUtilsTest {

    @Test
    fun `ExportPayload parses valid JSON with both fields`() {
        val json = """{"svg":"c3Zn","png":"cG5n"}"""
        val payload = ExportPayload.parse(json)
        assertEquals("c3Zn", payload.svgB64)
        assertEquals("cG5n", payload.pngB64)
    }

    @Test
    fun `ExportPayload treats missing png as null`() {
        val json = """{"svg":"c3Zn"}"""
        val payload = ExportPayload.parse(json)
        assertEquals("c3Zn", payload.svgB64)
        assertNull(payload.pngB64)
    }

    @Test
    fun `ExportPayload treats empty png as null`() {
        val json = """{"svg":"c3Zn","png":""}"""
        val payload = ExportPayload.parse(json)
        assertEquals("c3Zn", payload.svgB64)
        assertNull(payload.pngB64)
    }

    @Test
    fun `ExportPayload rejects empty svgB64 via constructor`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExportPayload("", "cG5n")
        }
    }

    @Test
    fun `ExportPayload throws on missing svg field`() {
        val json = """{"png":"cG5n"}"""
        assertThrows(IllegalArgumentException::class.java) {
            ExportPayload.parse(json)
        }
    }

    @Test
    fun `ExportPayload throws on empty JSON object`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExportPayload.parse("{}")
        }
    }

    @Test
    fun `ExportPayload throws on invalid JSON`() {
        assertThrows(JsonSyntaxException::class.java) {
            ExportPayload.parse("not json")
        }
    }

    @Test
    fun `ExportPayload preserves base64 content accurately`() {
        val svgContent = "<svg xmlns=\"http://www.w3.org/2000/svg\"><text>Hello</text></svg>"
        val svgB64 = Base64.getEncoder().encodeToString(svgContent.toByteArray(Charsets.UTF_8))
        val json = """{"svg":"$svgB64","png":""}"""
        val payload = ExportPayload.parse(json)
        val decoded = String(Base64.getDecoder().decode(payload.svgB64), Charsets.UTF_8)
        assertEquals(svgContent, decoded)
    }

    @Test
    fun `ImageTransferable supports image flavor only`() {
        val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val transferable = ImageTransferable(img)
        assertTrue(transferable.isDataFlavorSupported(DataFlavor.imageFlavor))
        assertFalse(transferable.isDataFlavorSupported(DataFlavor.stringFlavor))
        assertEquals(1, transferable.transferDataFlavors.size)
        assertEquals(DataFlavor.imageFlavor, transferable.transferDataFlavors[0])
        assertSame(img, transferable.getTransferData(DataFlavor.imageFlavor))
    }

    @Test
    fun `ImageTransferable throws on unsupported flavor`() {
        val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val transferable = ImageTransferable(img)
        assertThrows(UnsupportedFlavorException::class.java) {
            transferable.getTransferData(DataFlavor.stringFlavor)
        }
    }
}
