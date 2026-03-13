package com.alextdev.mermaidvisualizer.markdown

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO

class MermaidMarkdownExportHandlerTest {

    @Test
    fun `base64 SVG decoding roundtrip`() {
        val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect width=\"10\" height=\"10\"/></svg>"
        val b64 = Base64.getEncoder().encodeToString(svg.toByteArray(Charsets.UTF_8))
        val decoded = String(Base64.getDecoder().decode(b64), Charsets.UTF_8)
        assertEquals(svg, decoded)
    }

    @Test
    fun `base64 PNG decoding produces valid image`() {
        val img = BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)
        val baos = java.io.ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        val b64 = Base64.getEncoder().encodeToString(baos.toByteArray())
        val decoded = Base64.getDecoder().decode(b64)
        val result = ImageIO.read(ByteArrayInputStream(decoded))
        assertNotNull(result)
        assertEquals(2, result.width)
        assertEquals(2, result.height)
    }

    @Test
    fun `message tag constants match JS contract`() {
        assertEquals("mermaid/copy-svg", TAG_COPY_SVG)
        assertEquals("mermaid/copy-png", TAG_COPY_PNG)
        assertEquals("mermaid/save", TAG_SAVE)
    }
}
