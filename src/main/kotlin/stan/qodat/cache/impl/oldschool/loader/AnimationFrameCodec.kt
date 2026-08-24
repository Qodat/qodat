package stan.qodat.cache.impl.oldschool.loader

import net.runelite.cache.definitions.FrameDefinition
import net.runelite.cache.definitions.FramemapDefinition
import com.displee.io.impl.InputBuffer
import qodat.cache.definition.AnimationFrameLegacyDefinition
import qodat.cache.definition.AnimationTransformationGroup

/**
 * Frame / framemap decode that matches the NR 235 client:
 * OSRS format (RuneLite) plus the custom 317 path gated by magic bytes `0xF9 0xF9`.
 *
 * See `osrs.RSAnimation` / `osrs.RSFrames` / `osrs.RSFramesBase`.
 */
object AnimationFrameCodec {

    /** Same sentinel the NR client uses (`fileData[0] == -7 && fileData[1] == -7`). */
    const val NR_317_MAGIC: Byte = -7

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
        if (is317Framemap(data)) {
            decodeFramemap(def, data) { readUnsignedShort() }
        } else {
            decodeFramemap(def, data) { readUnsignedByte() }
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
    ): AnimationTransformationGroup = object : AnimationTransformationGroup {
        override val id: Int = framemapId
        override val transformationTypes: IntArray = framemap.types
        override val targetVertexGroupsIndices: Array<IntArray> = framemap.frameMaps
    }

    private fun decodeFramemap(def: FramemapDefinition, data: ByteArray, readValue: InputBuffer.() -> Int) {
        val input = InputBuffer(data)
        def.length = input.readValue()
        def.types = IntArray(def.length) { input.readValue() }
        def.frameMaps = Array(def.length) { IntArray(input.readValue()) }
        for (i in 0 until def.length) {
            for (j in def.frameMaps[i].indices) {
                def.frameMaps[i][j] = input.readValue()
            }
        }
    }

    /**
     * Official OSRS frame: mask stream then smart-delta stream.
     * Copied from RuneLite [net.runelite.cache.definitions.loaders.FrameLoader].
     */
    private fun decodeOsrsFrame(framemap: FramemapDefinition, id: Int, data: ByteArray): FrameDefinition {
        val def = FrameDefinition()
        val input = InputBuffer(data)
        val values = InputBuffer(data)
        def.id = id
        def.framemap = framemap

        input.readUnsignedShort()
        val length = input.readUnsignedByte()
        values.offset += 3 + length

        // TODO(perf): four IntArray(500) scratch buffers are allocated per frame
        val indexFrameIds = IntArray(500)
        val translatorX = IntArray(500)
        val translatorY = IntArray(500)
        val translatorZ = IntArray(500)

        var lastI = -1
        var index = 0
        for (i in 0 until length) {
            val mask = input.readUnsignedByte()
            if (mask <= 0) continue

            index = insertType0Predecessor(
                def.framemap.types, i, lastI, index,
                indexFrameIds, translatorX, translatorY, translatorZ,
            )

            indexFrameIds[index] = i
            var fallback = 0
            if (def.framemap.types[i] == 3) {
                fallback = 128
            }
            translatorX[index] = if (mask and 1 != 0) values.readSmart() else fallback
            translatorY[index] = if (mask and 2 != 0) values.readSmart() else fallback
            translatorZ[index] = if (mask and 4 != 0) values.readSmart() else fallback
            lastI = i
            ++index
            if (def.framemap.types[i] == 5) {
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

        // TODO(perf): four IntArray(500) scratch buffers are allocated per frame
        val indexFrameIds = IntArray(500)
        val translatorX = IntArray(500)
        val translatorY = IntArray(500)
        val translatorZ = IntArray(500)

        var lastI = -1
        var index = 0
        for (i in 0 until length) {
            val mask = data[offset].toInt() and 0xff
            offset++
            if (mask <= 0) continue

            index = insertType0Predecessor(
                def.framemap.types, i, lastI, index,
                indexFrameIds, translatorX, translatorY, translatorZ,
            )

            indexFrameIds[index] = i
            var fallback = 0
            if (def.framemap.types[i] == 3) {
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
            if (def.framemap.types[i] == 5) {
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
        def.indexFrameIds = IntArray(index)
        def.translator_x = IntArray(index)
        def.translator_y = IntArray(index)
        def.translator_z = IntArray(index)
        for (i in 0 until index) {
            def.indexFrameIds[i] = indexFrameIds[i]
            def.translator_x[i] = translatorX[i]
            def.translator_y[i] = translatorY[i]
            def.translator_z[i] = translatorZ[i]
        }
    }
}
