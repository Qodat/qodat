package stan.qodat.cache.impl.oldschool.loader

import qodat.cache.definition.AnimationFrameLegacyDefinition
import qodat.cache.definition.AnimationTransformationGroup
import stan.qodat.cache.impl.oldschool.definition.FrameDefinition
import stan.qodat.cache.impl.oldschool.definition.FramemapDefinition

/**
 * Frame / framemap decode that matches the NR 235 client:
 * OSRS format (RuneLite) plus the custom 317 path gated by magic bytes `0xF9 0xF9`.
 *
 * See `osrs.RSAnimation` / `osrs.RSFrames` / `osrs.RSFramesBase`.
 */
object AnimationFrameCodec {

    /** Same sentinel the NR client uses (`fileData[0] == -7 && fileData[1] == -7`). */
    const val NR_317_MAGIC: Byte = -7

    internal val EMPTY_INTS = IntArray(0)
    internal val EMPTY_INT_ARRAYS = emptyArray<IntArray>()

    private const val SCRATCH = 500
    private val scratchTls = ThreadLocal.withInitial { FrameScratch() }

    fun is317Frame(data: ByteArray): Boolean =
        data.size >= 2 && data[0] == NR_317_MAGIC && data[1] == NR_317_MAGIC

    fun is317Framemap(data: ByteArray): Boolean =
        data.size >= 2 &&
            data[data.lastIndex] == NR_317_MAGIC &&
            data[data.lastIndex - 1] == NR_317_MAGIC

    /**
     * Skeleton / framemap archive id for a frame file.
     * 317 frames do not store the id in the payload; the client uses the frame archive id.
     */
    fun framemapId(frameData: ByteArray, frameArchiveId: Int): Int =
        if (is317Frame(frameData)) frameArchiveId
        else (frameData[0].toInt() and 0xff shl 8) or (frameData[1].toInt() and 0xff)

    fun loadFramemap(id: Int, data: ByteArray): FramemapDefinition {
        val def = FramemapDefinition()
        def.id = id
        val cursor = scratchTls.get().masks
        cursor.reset(data)
        if (is317Framemap(data)) {
            decodeFramemap(def, cursor, shorts = true)
        } else {
            decodeFramemap(def, cursor, shorts = false)
        }
        return def
    }

    fun loadFrame(framemap: FramemapDefinition, id: Int, data: ByteArray): FrameDefinition {
        return if (is317Frame(data)) decode317Frame(framemap, id, data)
        else decodeOsrsFrame(framemap, id, data)
    }

    fun toDefinition(
        frame: FrameDefinition,
        transformGroup: AnimationTransformationGroup,
    ): AnimationFrameLegacyDefinition = object : AnimationFrameLegacyDefinition {
        override val transformationCount: Int = frame.translatorCount
        override val transformationGroupAccessIndices: IntArray = frame.indexFrameIds
        override val transformationDeltaX: IntArray = frame.translator_x
        override val transformationDeltaY: IntArray = frame.translator_y
        override val transformationDeltaZ: IntArray = frame.translator_z
        override val transformationGroup: AnimationTransformationGroup = transformGroup
    }

    fun transformationGroup(
        framemapId: Int,
        framemap: FramemapDefinition,
    ): AnimationTransformationGroup {
        if (framemap.id == framemapId) return framemap
        framemap.id = framemapId
        return framemap
    }

    private fun decodeFramemap(def: FramemapDefinition, cursor: DecodeCursor, shorts: Boolean) {
        val length = if (shorts) cursor.readUnsignedShort() else cursor.readUnsignedByte()
        def.length = length
        if (length == 0) {
            def.types = EMPTY_INTS
            def.frameMaps = EMPTY_INT_ARRAYS
            return
        }
        val types = IntArray(length)
        if (shorts) {
            for (i in 0 until length) types[i] = cursor.readUnsignedShort()
        } else {
            for (i in 0 until length) types[i] = cursor.readUnsignedByte()
        }
        def.types = types
        val maps = Array(length) {
            val n = if (shorts) cursor.readUnsignedShort() else cursor.readUnsignedByte()
            if (n == 0) EMPTY_INTS else IntArray(n)
        }
        for (i in 0 until length) {
            val group = maps[i]
            if (shorts) {
                for (j in group.indices) group[j] = cursor.readUnsignedShort()
            } else {
                for (j in group.indices) group[j] = cursor.readUnsignedByte()
            }
        }
        def.frameMaps = maps
    }

    /**
     * Official OSRS frame: mask stream then smart-delta stream.
     * Copied from RuneLite [net.runelite.cache.definitions.loaders.FrameLoader].
     */
    private fun decodeOsrsFrame(framemap: FramemapDefinition, id: Int, data: ByteArray): FrameDefinition {
        val scratch = scratchTls.get()
        val masks = scratch.masks
        val values = scratch.values
        masks.reset(data)
        values.reset(data)
        val def = FrameDefinition()
        def.id = id
        def.framemap = framemap

        masks.readUnsignedShort()
        val length = masks.readUnsignedByte()
        values.offset = 3 + length

        val indexFrameIds = scratch.ids
        val translatorX = scratch.x
        val translatorY = scratch.y
        val translatorZ = scratch.z
        val types = def.framemap.types

        var lastI = -1
        var index = 0
        for (i in 0 until length) {
            val mask = masks.readUnsignedByte()
            if (mask <= 0) continue

            index = insertType0Predecessor(
                types, i, lastI, index,
                indexFrameIds, translatorX, translatorY, translatorZ,
            )

            indexFrameIds[index] = i
            var fallback = 0
            if (types[i] == 3) {
                fallback = 128
            }
            translatorX[index] = if (mask and 1 != 0) values.readSmart() else fallback
            translatorY[index] = if (mask and 2 != 0) values.readSmart() else fallback
            translatorZ[index] = if (mask and 4 != 0) values.readSmart() else fallback
            lastI = i
            ++index
            if (types[i] == 5) {
                def.showing = true
            }
        }

        if (values.offset != data.size) {
            throw RuntimeException("OSRS frame $id leftover bytes: offset=${values.offset} length=${data.size}")
        }

        copyScratch(def, index, indexFrameIds, translatorX, translatorY, translatorZ)
        return def
    }

    /**
     * NR 317 frame: magic, then a single sequential stream (mask + optional shorts per transform).
     * Deltas use [read317Short] (`RSBuffer.getShort2`).
     */
    private fun decode317Frame(framemap: FramemapDefinition, id: Int, data: ByteArray): FrameDefinition {
        val def = FrameDefinition()
        def.id = id
        def.framemap = framemap

        var offset = 2
        val length = data[offset].toInt() and 0xff
        offset++

        val scratch = scratchTls.get()
        val indexFrameIds = scratch.ids
        val translatorX = scratch.x
        val translatorY = scratch.y
        val translatorZ = scratch.z
        val types = def.framemap.types

        var lastI = -1
        var index = 0
        for (i in 0 until length) {
            val mask = data[offset].toInt() and 0xff
            offset++
            if (mask <= 0) continue

            index = insertType0Predecessor(
                types, i, lastI, index,
                indexFrameIds, translatorX, translatorY, translatorZ,
            )

            indexFrameIds[index] = i
            var fallback = 0
            if (types[i] == 3) {
                fallback = 128
            }
            if (mask and 1 != 0) {
                translatorX[index] = read317Short(data, offset)
                offset += 2
            } else {
                translatorX[index] = fallback
            }
            if (mask and 2 != 0) {
                translatorY[index] = read317Short(data, offset)
                offset += 2
            } else {
                translatorY[index] = fallback
            }
            if (mask and 4 != 0) {
                translatorZ[index] = read317Short(data, offset)
                offset += 2
            } else {
                translatorZ[index] = fallback
            }
            lastI = i
            ++index
            if (types[i] == 5) {
                def.showing = true
            }
        }

        copyScratch(def, index, indexFrameIds, translatorX, translatorY, translatorZ)
        return def
    }

    /**
     * `RSBuffer.getShort2`: signed 16-bit with the NR client's `65537` wrap
     * (`i > 32767` then `i -= 65537`).
     */
    internal fun read317Short(data: ByteArray, offset: Int): Int {
        var value = ((data[offset].toInt() and 255) shl 8) + (data[offset + 1].toInt() and 255)
        if (value > 32767) {
            value -= 65537
        }
        return value
    }

    private fun insertType0Predecessor(
        types: IntArray,
        i: Int,
        lastI: Int,
        index: Int,
        indexFrameIds: IntArray,
        translatorX: IntArray,
        translatorY: IntArray,
        translatorZ: IntArray,
    ): Int {
        if (types[i] == 0) return index
        for (prev in i - 1 downTo lastI + 1) {
            if (types[prev] == 0) {
                indexFrameIds[index] = prev
                translatorX[index] = 0
                translatorY[index] = 0
                translatorZ[index] = 0
                return index + 1
            }
        }
        return index
    }

    private fun copyScratch(
        def: FrameDefinition,
        index: Int,
        indexFrameIds: IntArray,
        translatorX: IntArray,
        translatorY: IntArray,
        translatorZ: IntArray,
    ) {
        def.translatorCount = index
        if (index == 0) {
            def.indexFrameIds = EMPTY_INTS
            def.translator_x = EMPTY_INTS
            def.translator_y = EMPTY_INTS
            def.translator_z = EMPTY_INTS
            return
        }
        def.indexFrameIds = indexFrameIds.copyOf(index)
        def.translator_x = translatorX.copyOf(index)
        def.translator_y = translatorY.copyOf(index)
        def.translator_z = translatorZ.copyOf(index)
    }

    private class FrameScratch {
        val masks = DecodeCursor(ByteArray(0))
        val values = DecodeCursor(ByteArray(0))
        val ids = IntArray(SCRATCH)
        val x = IntArray(SCRATCH)
        val y = IntArray(SCRATCH)
        val z = IntArray(SCRATCH)
    }
}
