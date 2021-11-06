package stan.qodat.cache.impl.legacy.decoder

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyKitDefinition

/**
 * TODO: add documentation
 *
 * @author Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @version 1.0
 * @since 2019-01-10
 */
class LegacyKitDecoder {

    fun load(id: Int, `is`: InputStream): LegacyKitDefinition {

        var bodyPartId = 0
        var nonSelectable: Boolean = false
        var modelIds : Array<String>? = null
        val recolorToFind = ShortArray(9) { -1 }
        val recolorToReplace = ShortArray(9) { -1 }
        val models = IntArray(9) { -1 } // idk what this is for

        while (true) {
            val opcode = `is`.readUnsignedByte()
            if (opcode == 0)
                break

            when (opcode) {
                1 -> bodyPartId = `is`.readUnsignedByte()
                2 -> {
                    val length = `is`.readUnsignedByte()
                    modelIds = Array(length) {
                        `is`.readUnsignedShort().toString()
                    }
                }
                3 -> nonSelectable = true
                in 40..49 -> recolorToReplace[opcode - 40] = `is`.readUnsignedShort().toShort()
                in 50..59 -> recolorToFind[opcode - 50] = `is`.readUnsignedShort().toShort()
                in 60..69 -> models[opcode - 60] = `is`.readShort().toInt()
            }
        }

        return LegacyKitDefinition(
            name = id.toString(),
            bodyPartId = bodyPartId,
            modelIds = modelIds!!,
            findColor = recolorToFind,
            replaceColor = recolorToReplace
        )
    }

}
