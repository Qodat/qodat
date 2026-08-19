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

    fun Color.encode(): Int = HslPalette.encode(
        (red * 255.0).toInt(),
        (green * 255.0).toInt(),
        (blue * 255.0).toInt()
    )

    fun hsbToColor(hsb: Short, alpha: Byte?) = hsbToColor(hsb.toInt(), alpha)

    fun hsbToColor(hsb: Int, alpha: Byte?): Color {
        val rgb = HslPalette.rgb(hsb)
        return Color.color(
            (rgb shr 16 and 0xFF) / 255.0,
            (rgb shr 8 and 0xFF) / 255.0,
            (rgb and 0xFF) / 255.0,
            opacityOf(alpha)
        )
    }

    /**
     * RuneScape stores face transparency inverted: 0 is fully opaque and 255 is fully
     * transparent (the client discards those faces entirely).
     */
    fun opacityOf(alpha: Byte?): Double {
        val transparency = alpha?.toUByte()?.toInt() ?: 0
        return 1.0 - transparency / 255.0
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

    fun hsbToRGB(hsb: Int): Int = HslPalette.rgb(hsb)
}