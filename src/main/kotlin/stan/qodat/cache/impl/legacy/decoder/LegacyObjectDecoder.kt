package stan.qodat.cache.impl.legacy.decoder

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyObjectDefinition
import java.util.*

/**
 * TODO: add documentation
 *
 * @author Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @version 1.0
 * @since 2019-01-10
 */
class LegacyObjectDecoder {

    fun load(id: Int, `is`: InputStream): LegacyObjectDefinition {
        val def = LegacyObjectBuilder()
        def.type = id
        def.setDefaults()
        while (true) {
            val opcode = `is`.readUnsignedByte()
            if (opcode == 0) break
            processOp317(opcode, def, `is`)
        }
        post(def)
        return LegacyObjectDefinition(
            name = def.name,
            modelIds = def.objectModels?.map { it.toString() }?.toTypedArray()?: emptyArray(),
            animationIds = arrayOf(def.animationID.toString()),
            findColor = def.recolorToFind,
            replaceColor = def.recolorToReplace
        )
    }

    private fun processOp317(
        opcode: Int,
        def: LegacyObjectBuilder,
        `is`: InputStream
    ) {
        when (opcode) {
            1 -> {
                processObjectTypesAndModels(def, `is`)
            }
            2 -> def.name = `is`.readStringOld()
            3 -> def.description = `is`.readStringOld()
            5 -> {
                val length = `is`.readUnsignedByte()
                if (length > 0) {
                    if (def.objectModels == null) {
                        processObjectModels(def, `is`, length)
                    } else {
                        `is`.skip(length * 2)
                    }
                }
            }
            14 -> def.sizeX = `is`.readUnsignedByte()
            15 -> def.sizeY = `is`.readUnsignedByte()
            17 -> def.solid = false
            18 -> def.blocksProjectile = false
            19 -> {
                def.interactiveType = `is`.readUnsignedByte()
                def.isInteractive = def.interactiveType == 1
            }
            21 -> def.groundContoured = true
            22 -> def.nonFlatShading = false
            23 -> def.occludes = true
            24 -> def.animationID = `is`.readUnsignedShort().let { if (it == 0xFFFF) -1 else it }
            28 -> def.decorDisplacement = `is`.readUnsignedByte()
            29 -> def.ambient = `is`.readByte()
            39 -> def.contrast = `is`.readByte()
            in 30..34 -> {
                if (def.actions == null)
                    def.actions = arrayOfNulls(5)
                def.actions!![opcode - 30] = `is`.readStringOld().let {
                    if (it.equals("Hidden", ignoreCase = true))
                        null
                    else
                        it
                }
            }
            40 -> {
                val length = `is`.readUnsignedByte()
                val recolorToFind = ShortArray(length)
                val recolorToReplace = ShortArray(length)
                for (index in 0 until length) {
                    recolorToFind[index] = `is`.readUnsignedShort().toShort()
                    recolorToReplace[index] = `is`.readUnsignedShort().toShort()
                }
                def.recolorToFind = recolorToFind
                def.recolorToReplace = recolorToReplace
            }
            60 -> def.minimapFunction = `is`.readUnsignedShort()
            62 -> def.rotated = true
            64 -> def.castsShadow = false
            65 -> def.modelSizeX = `is`.readUnsignedShort()
            66 -> def.modelSizeHeight = `is`.readUnsignedShort()
            67 -> def.modelSizeY = `is`.readUnsignedShort()
            68 -> def.mapSceneID = `is`.readUnsignedShort()
            69 -> def.surroundings = `is`.readByte().toInt()
            70 -> def.offsetX = `is`.readShort().toInt()
            71 -> def.offsetHeight = `is`.readShort().toInt()
            72 -> def.offsetY = `is`.readShort().toInt()
            73 -> def.groundObstructive = true
            74 -> def.ethereal = true
            75 -> def.supportedItems = `is`.readUnsignedByte()
            77 -> {
                readConfigBits(def, `is`)
                val length = `is`.readUnsignedByte()
                val configChangeDest = IntArray(length + 1)
                for (index in 0..length) {
                    configChangeDest[index] = `is`.readUnsignedShort()
                    if (0xFFFF == configChangeDest[index]) configChangeDest[index] = -1
                }
                def.configChangeDest = configChangeDest
            }
        }
    }

    private fun post(def: LegacyObjectBuilder) {
        if (def.interactiveType == -1) {
            def.isInteractive = def.objectModels != null && (def.objectTypes == null || def.objectTypes!![0] == 10)
            if (def.actions != null) def.isInteractive = true
        }
        if (def.supportedItems == -1) def.supportedItems = if (def.solid) 1 else 0
        if (def.ethereal) {
            def.solid = false
            def.blocksProjectile = false
        }
    }

    private fun processObjectTypesAndModels(
        def: LegacyObjectBuilder,
        `is`: InputStream
    ) {
        val length = `is`.readUnsignedByte()
        if (length > 0) {
            val objectTypes = IntArray(length)
            val objectModels = IntArray(length)
            for (index in 0 until length) {
                objectModels[index] = `is`.readUnsignedShort()
                objectTypes[index] = `is`.readUnsignedByte()
            }
            def.objectTypes = objectTypes
            def.objectModels = objectModels
        }
    }

    private fun processObjectModels(
        def: LegacyObjectBuilder,
        `is`: InputStream,
        length: Int
    ) {
        def.objectTypes = null
        val objectModels = IntArray(length)
        for (index in 0 until length) objectModels[index] = `is`.readUnsignedShort()
        def.objectModels = objectModels
    }

    private fun readConfigChangeDest(
        `is`: InputStream,
        length: Int,
        configChangeDest: IntArray
    ) {
        for (index in 0..length) {
            configChangeDest[index] = `is`.readUnsignedShort()
            if (0xFFFF == configChangeDest[index]) configChangeDest[index] = -1
        }
    }

    private fun readConfigBits(def: LegacyObjectBuilder, `is`: InputStream) {
        var varpID = `is`.readUnsignedShort()
        if (varpID == 0xFFFF) varpID = -1
        def.varbitID = varpID
        var configId = `is`.readUnsignedShort()
        if (configId == 0xFFFF) configId = -1
        def.varpID = configId
    }
    fun InputStream.readStringOld(): String {
        val start = offset
        while (true) {
            if (readByte().toInt() == 10)
                break
        }
        return String(array, start, offset - start - 1)
    }

    private companion object {

        private class LegacyObjectBuilder {
            var groundObstructive = false
            var ambient: Byte = 0.toByte()
            var offsetX = 0
            var name: String = "null"
            var modelSizeY = 0
            var contrast: Byte = 0
            var sizeX = 0
            var offsetHeight = 0
            var minimapFunction = 0
            var recolorToReplace: ShortArray? = null
            var modelSizeX = 0
            var varpID = 0
            var rotated = false
            var type = 0
            var blocksProjectile = true
            var mapSceneID = 0
            var configChangeDest: IntArray?= null
            var supportedItems = 0
            var sizeY = 0
            var groundContoured = false
            var occludes = false
            var ethereal = false
            var solid = false
            var surroundings = 0
            var nonFlatShading = false
            var modelSizeHeight = 0
            var objectModels: IntArray? = null
            var varbitID = 0
            var decorDisplacement = 0
            var objectTypes: IntArray? = null
            var description: String? = null
            var isInteractive = false
            var castsShadow = false
            var animationID = 0
            var offsetY = 0
            var recolorToFind: ShortArray? = null
            var actions: Array<String?>? = arrayOfNulls(5)
            var interactiveType = -1
            var interactType = 2
            var retextureToFind: ShortArray? = null
            var textureToReplace: ShortArray? = null
            var anInt2110 = 0
            var anInt2083 = 0
            var anInt2112 = 0
            var anInt2113 = 0
            var anIntArray2084: IntArray? = null
            var anInt2105 = 0
            var mapAreaId = 0
            var params: Map<Int, Any>? = null

            fun setDefaults() {
                objectModels = null
                objectTypes = null
                name = "null"
                description = null
                recolorToFind = null
                recolorToReplace = null
                sizeX = 1
                sizeY = 1
                solid = true
                blocksProjectile = true
                isInteractive = false
                groundContoured = false
                nonFlatShading = false
                occludes = false
                animationID = -1
                decorDisplacement = 16
                ambient = 0
                contrast = 0
                actions = null
                minimapFunction = -1
                mapSceneID = -1
                rotated = false
                castsShadow = true
                modelSizeX = 128
                modelSizeHeight = 128
                modelSizeY = 128
                surroundings = 0
                offsetX = 0
                offsetHeight = 0
                offsetY = 0
                groundObstructive = false
                ethereal = false
                supportedItems = -1
                varbitID = -1
                varpID = -1
                configChangeDest = null
            }

            override fun toString(): String {
                return "ObjectDefinition{" +
                        "name='" + name + '\'' +
                        ", occludes=" + occludes +
                        ", ethereal=" + ethereal +
                        ", solid=" + solid +
                        ", objectModels=" + Arrays.toString(objectModels) +
                        ", animationID=" + animationID +
                        '}'
            }
        }
    }
}