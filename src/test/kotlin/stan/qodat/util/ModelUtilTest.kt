package stan.qodat.util

import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelUtilTest {

    @Test
    fun opacityTreatsNullAndZeroAsFullyOpaque() {
        assertEquals(1.0, ModelUtil.opacityOf(null))
        assertEquals(1.0, ModelUtil.opacityOf(0))
    }

    @Test
    fun opacityInvertsUnsignedTransparency() {
        assertEquals(0.0, ModelUtil.opacityOf((-1).toByte()))
        assertEquals(1.0 - 1.0 / 255.0, ModelUtil.opacityOf(1), 1e-12)
        assertEquals(1.0 - 128.0 / 255.0, ModelUtil.opacityOf(0x80.toByte()), 1e-12)
    }

    @Test
    fun hsbToColorAppliesOpacityAndPaletteRgb() {
        val hsb = 20000
        val rgb = HslPalette.rgb(hsb)
        val color = ModelUtil.hsbToColor(hsb, 0x80.toByte())
        assertEquals((rgb shr 16 and 0xFF) / 255.0, color.red, 1e-6)
        assertEquals((rgb shr 8 and 0xFF) / 255.0, color.green, 1e-6)
        assertEquals((rgb and 0xFF) / 255.0, color.blue, 1e-6)
        assertEquals(ModelUtil.opacityOf(0x80.toByte()), color.opacity, 1e-6)
        assertEquals(color, ModelUtil.hsbToColor(hsb.toShort(), 0x80.toByte()))
    }

    @Test
    fun hsbToColorNullAlphaIsOpaque() {
        assertEquals(1.0, ModelUtil.hsbToColor(1, null).opacity)
    }

    @Test
    fun getShadeZeroIsBlackAndOneKeepsChannels() {
        val source = java.awt.Color(200, 100, 50)
        val black = ModelUtil.getShade(source, 0.0)
        assertEquals(0, black.red)
        assertEquals(0, black.green)
        assertEquals(0, black.blue)

        val same = ModelUtil.getShade(source, 1.0)
        assertEquals(200, same.red)
        assertEquals(100, same.green)
        assertEquals(50, same.blue)
    }

    @Test
    fun getShadeDarkensWithoutCrossingChannels() {
        val source = java.awt.Color(200, 100, 50)
        val shaded = ModelUtil.getShade(source, 0.25)
        assertTrue(shaded.red < source.red)
        assertTrue(shaded.green < source.green)
        assertTrue(shaded.blue < source.blue)
        assertTrue(shaded.red > shaded.green)
        assertTrue(shaded.green > shaded.blue)
    }

    @Test
    fun encodeUsesEightBitChannels() {
        val encoded = with(ModelUtil) { Color.color(1.0, 0.0, 0.0).encode() }
        assertEquals(
            HslPalette.encode(255, 0, 0),
            encoded
        )
    }
}
