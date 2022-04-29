package mqo

import javafx.scene.paint.Color

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   2019-07-18
 * @version 1.0
 */
class MQOMaterial(line: MQOLine) {

    var r = 0.0
    var g = 0.0
    var b = 0.0
    var a = 0.0

    init {
        var i = 0
        while (i < line.countStrings()) {

            val word = line[i++]

            if(!word.startsWith(COL_PREFIX))
                continue

            r = word.substringAfter(COL_PREFIX).toDouble()
            g = line[i++].toDouble()
            b = line[i++].toDouble()
            a = line[i++].dropLast(1).toDouble()
        }
    }

    fun toColor() = Color.color(r, g, b)!!

    companion object {
        private const val COL_PREFIX = "col("
    }
}