package stan.qodat.cache.impl.legacy.decoder

import qodat.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyNpcDefinition

class LegacyNpcDecoder {

    fun load(id: Int, `is`: InputStream): LegacyNpcDefinition {

        val def = LegacyNpcBuilder()

        while (true) {

            val opcode = `is`.readUnsignedByte()
            if (opcode == 0)
                break

            this.decodeValues(opcode, def, `is`)
        }

        return LegacyNpcDefinition(
            name = def.name,
            modelIds = def.models?.map { it.toString() }?.toTypedArray()?: emptyArray(),
            animationIds = arrayOf(
                def.walkAnimation,
                def.stanceAnimation,
                def.rotate90LeftAnimation,
                def.rotate90RightAnimation,
                def.rotate180Animation,
            ).map { it.toString() }.toTypedArray(),
            findColor = def.recolorToFind,
            replaceColor = def.recolorToReplace
        )
    }

    private fun decodeValues(opcode: Int, def: LegacyNpcBuilder, stream: InputStream) {
        val length: Int
        var index: Int
        when (opcode) {
            1 -> {
                length = stream.readUnsignedByte()
                def.models = IntArray(length) {
                    stream.readUnsignedShort()
                }
            }
            2 -> def.name = stream.readStringOld()
            3 -> stream.readStringOld()
            12 -> def.tileSpacesOccupied = stream.readByte().toInt()
            13 -> def.stanceAnimation = stream.readUnsignedShort()
            14 -> def.walkAnimation = stream.readUnsignedShort()
            17 -> {
                def.walkAnimation = stream.readUnsignedShort()
                def.rotate180Animation = stream.readUnsignedShort()
                def.rotate90RightAnimation = stream.readUnsignedShort()
                def.rotate90LeftAnimation = stream.readUnsignedShort()
            }
            in 30..34 -> {
                def.options[opcode - 30] = stream.readStringOld()
                if (def.options[opcode - 30].equals("Hidden", ignoreCase = true))
                    def.options[opcode - 30] = null
            }
            40 -> {
                length = stream.readUnsignedByte()
                def.recolorToFind = ShortArray(length)
                def.recolorToReplace = ShortArray(length)
                index = 0
                while (index < length) {
                    def.recolorToFind!![index] = stream.readUnsignedShort().toShort()
                    def.recolorToReplace!![index] = stream.readUnsignedShort().toShort()
                    ++index
                }
            }
            60 -> {
                length = stream.readUnsignedByte()
                def.models_2 = IntArray(length) {
                    stream.readUnsignedShort()
                }

            }
            90, 91, 92 -> stream.readUnsignedShort()
            93 -> def.renderOnMinimap = false
            95 -> def.combatLevel = stream.readUnsignedShort()
            97 -> def.resizeX = stream.readUnsignedShort()
            98 -> def.resizeY = stream.readUnsignedShort()
            99 -> def.hasRenderPriority = true
            100 -> def.ambient = stream.readByte().toInt()
            101 -> def.contrast = stream.readByte() * 5
            102 -> def.headIcon = stream.readUnsignedShort()
            103 -> def.rotation = stream.readUnsignedShort()
            106 -> {
                def.varbitIndex = stream.readUnsignedShort()
                if (def.varbitIndex == 65535) def.varbitIndex = -1

                def.varpIndex = stream.readUnsignedShort()
                if (def.varpIndex == 65535) def.varpIndex = -1

                length = stream.readUnsignedByte()
                def.configs = IntArray(length + 1) {
                    stream.readUnsignedShort().let {
                        if(it == 65535)
                            -1
                        else it
                    }
                }
            }
            107 -> def.isClickable = false
            else -> Unit
        }
    }

    companion object {
        private class LegacyNpcBuilder {
            var recolorToFind: ShortArray? = null
            var rotation = 32
            var name = "null"
            var recolorToReplace: ShortArray? = null
            var models: IntArray? = null
            var models_2: IntArray? = null
            var stanceAnimation = -1
            var tileSpacesOccupied = 1
            var walkAnimation = -1
            var rotate90RightAnimation = -1
            var resizeX = 128
            var contrast = 0
            var rotate180Animation = -1
            var varbitIndex = -1
            var options = arrayOfNulls<String>(5)
            var renderOnMinimap = true
            var combatLevel = -1
            var rotate90LeftAnimation = -1
            var resizeY = 128
            var hasRenderPriority = false
            var ambient = 0
            var headIcon = -1
            var configs: IntArray? = null
            var varpIndex = -1
            var isClickable = true
        }
    }
}

