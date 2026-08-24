package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import com.displee.cache.index.archive.Archive
import qodat.cache.definition.AnimationDefinition
import qodat.cache.definition.AnimationFrameLegacyDefinition
import qodat.cache.definition.AnimationMayaDefinition
import qodat.cache.definition.AnimationSound
import qodat.cache.definition.AnimationTransformationGroup
import stan.qodat.cache.BoundedLruCache
import stan.qodat.cache.impl.oldschool.definition.FramemapDefinition
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition206
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition226
import stan.qodat.cache.impl.oldschool.loader.AnimationFrameCodec
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader206
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader226

class AnimManager(
    private val cacheLibrary: CacheLibrary
) {

    private val seqs = mutableMapOf<Int, AnimationDefinition>()
    private val frames = BoundedLruCache<Long, AnimationFrameLegacyDefinition?>(MAX_DECODED_FRAMES)
    private val frameMaps = BoundedLruCache<Int, FramemapDefinition>(MAX_FRAMEMAPS)
    private val frameArchives = BoundedLruCache<Int, Archive>(MAX_FRAME_ARCHIVES)
    private var seqArray: Array<AnimationDefinition>? = null
    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return
        val archive = cacheLibrary.index(2).archive(12)!!
        val revision = archive.revision
        val loader226 = SequenceLoader226().apply { configureForRevision(revision) }
        val loader206 = SequenceLoader206()
        archive.files.forEach { (fileId, file) ->
            val data = file.data ?: return@forEach
            try {
                seqs[fileId] = loadSeq(loader226, loader206, fileId, data)
            } catch (_: Exception) {
            }
        }
        seqArray = seqs.values.toTypedArray()
        loaded = true
    }

    fun getSeq(id: String): AnimationDefinition {
        load()
        return getSeq(seqs, id)
    }

    fun getSeqs(): Array<AnimationDefinition> {
        load()
        return seqArray ?: getSeqs(seqs).also { seqArray = it }
    }

    fun getFrameDef(frameHash: Int): AnimationFrameLegacyDefinition? {
        val archiveId = archiveId(frameHash)
        val fileId = fileId(frameHash)
        return frames.getOrLoad(frameKey(archiveId, fileId)) {
            decodeFrame(archiveId, fileId)
        }
    }

    fun getFramemap(frameHash: Int): AnimationTransformationGroup? {
        val archiveId = archiveId(frameHash)
        val fileId = fileId(frameHash)
        val data = frameBytes(archiveId, fileId) ?: return null
        val framemapId = AnimationFrameCodec.framemapId(data, archiveId)
        return framemap(framemapId)
    }

    private fun decodeFrame(archiveId: Int, fileId: Int): AnimationFrameLegacyDefinition? {
        val data = frameBytes(archiveId, fileId) ?: return null
        val framemapId = AnimationFrameCodec.framemapId(data, archiveId)
        val framemap = framemap(framemapId) ?: return null
        return AnimationFrameCodec.loadFrame(framemap, fileId, data)
    }

    private fun framemap(framemapId: Int): FramemapDefinition? {
        frameMaps[framemapId]?.let { return it }
        val contents = cacheLibrary.data(1, framemapId) ?: return null
        return AnimationFrameCodec.loadFramemap(framemapId, contents).also {
            frameMaps.put(framemapId, it)
        }
    }

    private fun frameBytes(archiveId: Int, fileId: Int): ByteArray? {
        frameArchives[archiveId]?.let { return it.file(fileId)?.data }
        val opened = try {
            cacheLibrary.index(0).archive(archiveId)
        } catch (_: Exception) {
            null
        } ?: return null
        frameArchives.put(archiveId, opened)
        return opened.file(fileId)?.data
    }

    private fun loadSeq(
        loader226: SequenceLoader226,
        loader206: SequenceLoader206,
        seqId: Int,
        seqData: ByteArray,
    ): AnimationDefinition = try {
        mapSeq(loader226.load(seqId, seqData))
    } catch (_: Exception) {
        mapFallback206(loader206.load(seqId, seqData))
    }

    companion object {
        internal const val MAX_DECODED_FRAMES = 2048
        internal const val MAX_FRAMEMAPS = 256
        internal const val MAX_FRAME_ARCHIVES = 32

        internal fun archiveId(frameHash: Int): Int = frameHash ushr 16

        internal fun fileId(frameHash: Int): Int = frameHash and 0xFFFF

        internal fun frameKey(archiveId: Int, fileId: Int): Long =
            (archiveId.toLong() shl 16) or (fileId.toLong() and 0xFFFF)

        internal fun getSeq(seqs: Map<Int, AnimationDefinition>, id: String): AnimationDefinition {
            val seqId = id.toIntOrNull()
                ?: throw IllegalArgumentException("Animation id must be int-convertable $id")
            return seqs[seqId] ?: throw IllegalArgumentException("Animation not found $id")
        }

        internal fun getSeqs(seqs: Map<Int, AnimationDefinition>): Array<AnimationDefinition> =
            seqs.values.toTypedArray()

        internal fun mapSeq(sequence: SequenceDefinition226): AnimationDefinition {
            if (sequence.frameIDs == null) sequence.frameIDs = EMPTY_INTS
            if (sequence.frameLenghts == null) sequence.frameLenghts = EMPTY_INTS
            if (sequence.animMayaId >= 0) return MappedMayaSeq(sequence)
            return sequence
        }

        internal fun mapFallback206(sequence: SequenceDefinition206): AnimationDefinition {
            if (sequence.frameIDs == null) sequence.frameIDs = EMPTY_INTS
            if (sequence.frameLenghts == null) sequence.frameLenghts = EMPTY_INTS
            return sequence
        }

        internal fun compactSounds(sounds: Map<Int, AnimationSound?>?): Map<Int, AnimationSound> {
            if (sounds.isNullOrEmpty()) return emptyMap()
            var out: HashMap<Int, AnimationSound>? = null
            for ((frame, sound) in sounds) {
                if (sound == null) continue
                val map = out ?: HashMap<Int, AnimationSound>(sounds.size).also { out = it }
                map[frame] = sound
            }
            return out ?: emptyMap()
        }

        internal val EMPTY_INTS = IntArray(0)
        internal val EMPTY_BOOLS = BooleanArray(0)
    }

    internal class MappedMayaSeq(
        private val sequence: SequenceDefinition226,
    ) : AnimationMayaDefinition {
        override val id: String = sequence.id
        override val frameHashes: IntArray = sequence.frameIDs ?: EMPTY_INTS
        override val frameLengths: IntArray = sequence.frameLenghts ?: EMPTY_INTS
        override val loopOffset: Int = sequence.frameStep
        override val leftHandItem: Int = sequence.leftHandItem
        override val rightHandItem: Int = sequence.rightHandItem
        override val animMayaID: Int = sequence.animMayaId
        override val animMayaFrameSounds: Map<Int, AnimationSound> = compactSounds(sequence.sounds)
        override val animMayaStart: Int = sequence.animMayaStart
        override val animMayaEnd: Int = sequence.animMayaEnd
        override val animMayaMasks: BooleanArray = sequence.animMayaMasks ?: EMPTY_BOOLS
    }
}
