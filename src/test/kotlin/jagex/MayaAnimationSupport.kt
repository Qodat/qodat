package jagex

import com.displee.cache.CacheLibrary
import com.displee.cache.index.Index
import com.displee.cache.index.archive.Archive
import java.io.RandomAccessFile
import java.nio.file.Files

/**
 * Synthetic Maya payloads and a stub [Index] so [MayaAnimation] can be loaded
 * without a real cache or JavaFX [stan.qodat.scene.runescape.model.Model].
 */
internal object MayaAnimationSupport {

    const val ANIMATION_ID = 0
    const val FRAME_GROUP_ID = 1

    const val TYPE_SECONDARY = 1
    const val TYPE_TRANSFORMATION = 4
    const val FLAG_CHANNEL_0 = 1
    const val FLAG_CHANNEL_TRANSLATE_X = 4

    fun identitySkeleton(
        transformTypes: IntArray = intArrayOf(5),
        labels: Array<IntArray> = arrayOf(intArrayOf(0)),
        boneCount: Int = 1,
    ): ByteArray = Bytes().apply {
        u8(transformTypes.size)
        transformTypes.forEach { u8(it) }
        labels.forEach { label ->
            u8(label.size)
            label.forEach { u8(it) }
        }
        if (boneCount > 0) {
            u16(boneCount)
            u8(1)
            repeat(boneCount) {
                i16(-1)
                identityMatrix()
                f32(0f)
                f32(0f)
                f32(0f)
            }
        }
    }.toByteArray()

    fun skeletonWithoutMayaBones(
        transformTypes: IntArray = intArrayOf(1),
        labels: Array<IntArray> = arrayOf(intArrayOf(0)),
    ): ByteArray = Bytes().apply {
        u8(transformTypes.size)
        transformTypes.forEach { u8(it) }
        labels.forEach { label ->
            u8(label.size)
            label.forEach { u8(it) }
        }
    }.toByteArray()

    fun animationBytes(
        frameGroupId: Int = FRAME_GROUP_ID,
        totalDuration: Int = 0,
        curves: List<MayaCurveSpec> = emptyList(),
        version: Int = 0,
    ): ByteArray = Bytes().apply {
        u8(version)
        u16(frameGroupId)
        u16(0)
        u16(0)
        u8(totalDuration)
        u16(curves.size)
        curves.forEach { curve ->
            u8(curve.type)
            shortSmart(curve.trackIndex)
            u8(curve.flag)
            writeCurve(curve)
        }
    }.toByteArray()

    fun constantCurve(
        value: Float,
        frameNumber: Int = 0,
        type: Int,
        trackIndex: Int,
        flag: Int,
    ) = MayaCurveSpec(type, trackIndex, flag, listOf(frameNumber to value))

    fun linearCurve(value: Float, frameNumber: Int = 0): MayaAnimationFrame {
        val frame = MayaAnimationFrame()
        frame.read(Buffer(curvePayload(listOf(frameNumber to value))), 0)
        frame.initialiseKeyFrames()
        return frame
    }

    fun load(
        index: Index,
        skeletonData: ByteArray,
        animationData: ByteArray = animationBytes(),
        animationId: Int = ANIMATION_ID,
        frameGroupId: Int = FRAME_GROUP_ID,
    ): MayaAnimation {
        pack(index, animationId shr 16 and 0xFFFF, animationId and 0xFFFF, animationData)
        pack(index, frameGroupId, 0, skeletonData)
        val loaded = MayaAnimation.load(index, index, animationId, false)
            ?: error("MayaAnimation.load returned null")
        loaded.awaitLoaded(5_000)
        return loaded
    }

    fun pack(index: Index, archiveId: Int, fileId: Int, data: ByteArray?) {
        (index as StubMayaIndex).pack(archiveId, fileId, data)
    }

    fun <T> withStubIndex(block: (Index) -> T): T {
        val dir = Files.createTempDirectory("maya-anim-")
        var library: CacheLibrary? = null
        var raf: RandomAccessFile? = null
        try {
            Files.createFile(dir.resolve("main_file_cache.dat2"))
            Files.createFile(dir.resolve("main_file_cache.idx255"))
            library = CacheLibrary(dir.toString())
            raf = RandomAccessFile(dir.resolve("dummy.idx").toFile(), "rw")
            return block(StubMayaIndex(library, raf))
        } finally {
            raf?.close()
            library?.close()
            dir.toFile().deleteRecursively()
        }
    }

    data class MayaCurveSpec(
        val type: Int,
        val trackIndex: Int,
        val flag: Int,
        val keyframes: List<Pair<Int, Float>>,
    )

    private fun Bytes.writeCurve(curve: MayaCurveSpec) {
        writeBytes(curvePayload(curve.keyframes))
    }

    private fun curvePayload(keyframes: List<Pair<Int, Float>>): ByteArray = Bytes().apply {
        u16(keyframes.size)
        u8(0)
        u8(0)
        u8(0)
        u8(0)
        keyframes.forEach { (frame, value) ->
            i16(frame)
            f32(value)
            f32(0f)
            f32(0f)
            f32(0f)
            f32(0f)
        }
    }.toByteArray()

    private class Bytes {
        private val bytes = ArrayList<Byte>()

        fun u8(value: Int) {
            bytes.add((value and 0xFF).toByte())
        }

        fun u16(value: Int) {
            bytes.add((value shr 8 and 0xFF).toByte())
            bytes.add((value and 0xFF).toByte())
        }

        fun i16(value: Int) = u16(value and 0xFFFF)

        fun f32(value: Float) {
            val bits = value.toBits()
            bytes.add((bits shr 24 and 0xFF).toByte())
            bytes.add((bits shr 16 and 0xFF).toByte())
            bytes.add((bits shr 8 and 0xFF).toByte())
            bytes.add((bits and 0xFF).toByte())
        }

        fun shortSmart(value: Int) {
            require(value in -64..63) { "shortSmart test helper only covers the single-byte range" }
            u8(value + 64)
        }

        fun identityMatrix() {
            val identity = floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            )
            identity.forEach { f32(it) }
        }

        fun writeBytes(data: ByteArray) {
            data.forEach { bytes.add(it) }
        }

        fun toByteArray() = bytes.toByteArray()
    }

    private class StubMayaIndex(
        origin: CacheLibrary,
        raf: RandomAccessFile,
    ) : Index(origin, 256, raf) {
        private val packed = LinkedHashMap<Int, Archive>()

        fun pack(archiveId: Int, fileId: Int, data: ByteArray?) {
            val archive = packed.getOrPut(archiveId) { Archive(archiveId) }
            if (data == null) {
                archive.files[fileId] = com.displee.cache.index.archive.file.File(fileId, null)
            } else {
                archive.add(fileId, data)
            }
        }

        override fun archive(id: Int, xtea: IntArray?, direct: Boolean): Archive? = packed[id]
    }
}
