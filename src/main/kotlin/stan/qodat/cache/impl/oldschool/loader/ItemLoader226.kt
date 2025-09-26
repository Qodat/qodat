package stan.qodat.cache.impl.oldschool.loader

import stan.qodat.cache.impl.oldschool.definition.ItemDefinition226
import stan.qodat.cache.util.InputStream

class ItemLoader226 {

    fun load(id: Int, b: ByteArray): ItemDefinition226 {
        val def = ItemDefinition226(id)
        val `is` = InputStream(b)
        while (true) {
            val opcode = `is`.readUnsignedByte()
            if (opcode == 0) {
                break
            }
//            println("decoding[$id]: Found opcode $opcode")
            def.decodeValues(opcode, `is`)
        }
        return def
    }

    private fun ItemDefinition226.decodeValues(opcode: Int, stream: InputStream) {
        when(opcode) {
            1 -> inventoryModel = stream.readUnsignedShort()
            2 -> name = stream.readString()
            3 -> examineText = stream.readString()
            4 -> zoom2d = stream.readUnsignedShort()
            5 -> xan2d = stream.readUnsignedShort()
            6 -> yan2d = stream.readUnsignedShort()
            7 -> xOffset2d = stream.readUnsignedShort()
            8 -> yOffset2d = stream.readUnsignedShort()
            9 -> unknown1 = stream.readString()
            11 -> stackable = 1
            12 -> cost = stream.readInt()
            13 -> wearPos1 = stream.readUnsignedByte()
            14 -> wearPos2 = stream.readUnsignedByte()
            16 -> members = true
            23 -> {
                maleModel0 = stream.readUnsignedShort()
                maleOffset = stream.readUnsignedByte()
            }
            24 -> maleModel1 = stream.readUnsignedShort()
            25 -> {
                femaleModel0 = stream.readUnsignedShort()
                femaleOffset = stream.readUnsignedByte()
            }
            26 -> femaleModel1 = stream.readUnsignedShort()
            27 -> wearPos3 = stream.readUnsignedByte()
            in 30..34 -> {
                if (options == null) options = arrayOfNulls(5)
                options!![opcode - 30] = stream.readString()
                if (options!![opcode - 30].equals("Hidden", ignoreCase = true)) {
                    options!![opcode - 30] = null
                }
            }
            in 35..39 -> {
                if (interfaceOptions == null) interfaceOptions = arrayOfNulls(5)
                interfaceOptions!![opcode - 35] = stream.readString()
            }
            40 -> {
                val length = stream.readUnsignedByte()
                recolorToFind = ShortArray(length)
                recolorToReplace = ShortArray(length)
                repeat(length) {
                    recolorToFind!![it] = stream.readUnsignedShort().toShort()
                    recolorToReplace!![it] = stream.readUnsignedShort().toShort()
                }
            }
            41 -> {
                val length = stream.readUnsignedByte()
                retextureToFind = ShortArray(length)
                retextureToReplace = ShortArray(length)
                repeat(length) {
                    retextureToFind!![it] = stream.readUnsignedShort().toShort()
                    retextureToReplace!![it] = stream.readUnsignedShort().toShort()
                }
            }
            42 -> shiftClickDropIndex = stream.readByte().toInt()
            43 -> {
                val opId = stream.readUnsignedByte()
                if (subOps == null) subOps = arrayOfNulls(5)
                val valid = opId >= 0 && opId < 5
                if (valid && subOps!![opId] == null) {
                    subOps!![opId] = arrayOfNulls(20)
                }
                while (true) {
                    val subopId = stream.readUnsignedByte() - 1
                    if (subopId == -1) {
                        break
                    }
                    val op = stream.readString()
                    if (valid && subopId >= 0 && subopId < 20) {
                        subOps!![opId]!![subopId] = op
                    }
                }
            }
            65 -> isTradeable = true
            75 -> weight = stream.readShort().toInt()
            78 -> maleModel2 = stream.readUnsignedShort()
            79 -> femaleModel2 = stream.readUnsignedShort()
            90 -> maleHeadModel = stream.readUnsignedShort()
            91 -> femaleHeadModel = stream.readUnsignedShort()
            92 -> maleHeadModel2 = stream.readUnsignedShort()
            93 -> femaleHeadModel2 = stream.readUnsignedShort()
            94 -> category = stream.readUnsignedShort()
            95 -> zan2d = stream.readUnsignedShort()
            97 -> notedId = stream.readUnsignedShort()
            98 -> notedTemplateId = stream.readUnsignedShort()
            in 100..109 -> {
                if (countObj == null) countObj = IntArray(10)
                if (countCo == null) countCo = IntArray(10)
                countObj!![opcode - 100] = stream.readUnsignedShort()
                countCo!![opcode - 100] = stream.readUnsignedShort()
            }
            110 -> resizeX = stream.readUnsignedShort()
            111 -> resizeY = stream.readUnsignedShort()
            112 -> resizeZ = stream.readUnsignedShort()
            113 -> ambient = stream.readByte().toInt()
            114 -> contrast = stream.readByte().toInt()
            115 -> team = stream.readUnsignedByte()
            139 -> boughtId = stream.readUnsignedShort()
            140 -> boughtTemplateId = stream.readUnsignedShort()
            148 -> placeholderId = stream.readUnsignedShort()
            149 -> placeholderTemplateId = stream.readUnsignedShort()
            249 -> {
                val length = stream.readUnsignedByte()
                params = HashMap(length)
                repeat(length) {
                    val isString = stream.readUnsignedByte().toInt() == 1
                    val key = stream.read24BitInt()
                    val value: Any = if (isString) {
                        stream.readString()
                    } else {
                        stream.readInt()
                    }
                    params!![key] = value
                }
            }
        }
    }
}