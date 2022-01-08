package stan.qodat.util

import com.sun.javafx.util.Utils
import javafx.scene.paint.Color

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   2019-07-10
 * @version 1.0
 */
object ModelUtil {

    fun Color.encode(): Int {
        val hsb = java.awt.Color.RGBtoHSB(
            (red*255.0).toInt(),
            (green*255.0).toInt(),
            (blue*255.0).toInt(),
            null
        )
        return hsb[0].times(63).toInt().shl(10) +
                hsb[1].times(7).toInt().shl(7) +
                hsb[2].times(127).toInt()
    }

    fun hsbToColor(hsb: Short, alpha: Byte?) = hsbToColor(hsb.toInt(), alpha)

    fun hsbToColor(hsb: Int, alpha: Byte?): Color {

        var transparency = alpha?.toUByte()?.toDouble()
        if(transparency == null || transparency <= 0)
            transparency = 255.0

        val hue = (hsb shr 10) and 0x3f
        val sat = (hsb shr 7) and 0x07
        val bri = (hsb and 0x7f)
        val awtCol = java.awt.Color.getHSBColor(hue.toFloat() / 63, sat.toFloat() / 7, bri.toFloat() / 127)
        val r = awtCol.red / 255.0
        val g = awtCol.green / 255.0
        val b = awtCol.blue / 255.0
        return Color.color(r, g, b, transparency / 255.0)
    }

    fun getShade(color: java.awt.Color, shade: Double): java.awt.Color {
        val redLinear = Math.pow(color.red.toDouble(), 2.4) * shade
        val greenLinear = Math.pow(color.green.toDouble(), 2.4) * shade
        val blueLinear = Math.pow(color.blue.toDouble(), 2.4) * shade

        val red = Math.pow(redLinear, 1 / 2.4).toInt()
        val green = Math.pow(greenLinear, 1 / 2.4).toInt()
        val blue = Math.pow(blueLinear, 1 / 2.4).toInt()

        return java.awt.Color(red, green, blue)
    }

    fun hsbToRGB(hsb: Int): Int {
        val h = hsb shr 10 and 0x3f
        val s = hsb shr 7 and 0x07
        val b = hsb and 0x7f
        return java.awt.Color.HSBtoRGB(h.toFloat() / 63, s.toFloat() / 7, b.toFloat() / 127)
    }
}