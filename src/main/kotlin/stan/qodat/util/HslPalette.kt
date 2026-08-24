package stan.qodat.util

import kotlin.math.pow

/**
 * The RuneScape colour palette.
 *
 * RuneScape stores colours as a 16 bit value packing 6 bits of hue, 3 bits of saturation and
 * 7 bits of lightness. The client never converts these on the fly; it pre-computes a table of
 * 65536 RGB values once and indexes it directly with the packed value, which is why the packed
 * value doubles as the palette index.
 *
 * Note that this is HSL (lightness), not HSB/HSV, and that the client applies a gamma curve
 * afterwards. Using a generic HSB conversion instead produces colours that are noticeably
 * over-saturated and too bright, especially in the mid tones where Gouraud shading spends most
 * of its range.
 *
 * Matches the OSRS client's precomputed HSL palette (6/3/7-bit hue/sat/light + gamma).
 */
object HslPalette {

    /**
     * The client exposes four brightness settings; this is the middle-of-the-road default.
     */
    const val DEFAULT_BRIGHTNESS = 0.8

    private const val SIZE = 65536

    private var brightness = DEFAULT_BRIGHTNESS
    private var palette: IntArray = build(DEFAULT_BRIGHTNESS)

    /**
     * Looks up the RGB value of a packed 16 bit HSL colour.
     */
    fun rgb(hsl: Int): Int = palette[hsl and 0xFFFF]

    fun setBrightness(value: Double) {
        if (value == brightness)
            return
        brightness = value
        palette = build(value)
    }

    /**
     * Finds the packed HSL value whose palette entry is closest to the given RGB colour.
     *
     * The palette is not analytically invertible because of the gamma curve, so this searches
     * for the nearest entry to guarantee that encoding and decoding round-trip.
     */
    // TODO(perf): O(65536) scan per colour. Build an inverse LUT or spatial index if recolor/export encode is hot.
    fun encode(red: Int, green: Int, blue: Int): Int {
        var best = 0
        var bestDistance = Int.MAX_VALUE
        for (index in 0 until SIZE) {
            val rgb = palette[index]
            val dr = (rgb shr 16 and 0xFF) - red
            val dg = (rgb shr 8 and 0xFF) - green
            val db = (rgb and 0xFF) - blue
            val distance = dr * dr + dg * dg + db * db
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
                if (distance == 0)
                    break
            }
        }
        return best
    }

    private fun build(brightness: Double): IntArray {
        val palette = IntArray(SIZE)
        var index = 0
        for (hueAndSaturation in 0 until 512) {
            val hue = (hueAndSaturation shr 3) / 64.0 + 0.0078125
            val saturation = (hueAndSaturation and 7) / 8.0 + 0.0625
            for (step in 0 until 128) {
                val lightness = step / 128.0
                var red = lightness
                var green = lightness
                var blue = lightness
                if (saturation != 0.0) {
                    val max = if (lightness < 0.5)
                        lightness * (1.0 + saturation)
                    else
                        lightness + saturation - lightness * saturation
                    val min = 2.0 * lightness - max
                    red = hueToChannel(min, max, wrapHue(hue + 1.0 / 3.0))
                    green = hueToChannel(min, max, hue)
                    blue = hueToChannel(min, max, wrapHue(hue - 1.0 / 3.0))
                }
                val rgb = brighten(
                    ((red * 256.0).toInt() shl 16) + ((green * 256.0).toInt() shl 8) + (blue * 256.0).toInt(),
                    brightness
                )
                // The client reserves 0 to mean "transparent" while rasterizing.
                palette[index++] = if (rgb == 0) 1 else rgb
            }
        }
        return palette
    }

    private fun wrapHue(hue: Double) = when {
        hue > 1.0 -> hue - 1.0
        hue < 0.0 -> hue + 1.0
        else -> hue
    }

    private fun hueToChannel(min: Double, max: Double, hue: Double) = when {
        6.0 * hue < 1.0 -> min + (max - min) * 6.0 * hue
        2.0 * hue < 1.0 -> max
        3.0 * hue < 2.0 -> min + (max - min) * (2.0 / 3.0 - hue) * 6.0
        else -> min
    }

    private fun brighten(rgb: Int, brightness: Double): Int {
        val red = ((rgb shr 16 and 0xFF) / 256.0).pow(brightness)
        val green = ((rgb shr 8 and 0xFF) / 256.0).pow(brightness)
        val blue = ((rgb and 0xFF) / 256.0).pow(brightness)
        return ((red * 256.0).toInt() shl 16) + ((green * 256.0).toInt() shl 8) + (blue * 256.0).toInt()
    }
}
