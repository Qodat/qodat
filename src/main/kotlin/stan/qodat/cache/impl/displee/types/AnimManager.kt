package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import net.runelite.cache.definitions.FramemapDefinition
import qodat.cache.definition.AnimationDefinition
import qodat.cache.definition.AnimationFrameLegacyDefinition
import qodat.cache.definition.AnimationMayaDefinition
import qodat.cache.definition.AnimationSound
import qodat.cache.definition.AnimationTransformationGroup
import stan.qodat.cache.impl.displee.DispleeCache.getFileId
import stan.qodat.cache.impl.displee.DispleeCache.getFrameId
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition206
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition226
import stan.qodat.cache.impl.oldschool.loader.AnimationFrameCodec
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader206
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader226

class AnimManager(
    private val cacheLibrary: CacheLibrary
) {

    private val seqs = mutableMapOf<Int, AnimationDefinition>()
    private val frames = mutableMapOf<Int, Map<Int, AnimationFrameLegacyDefinition>>()
    private val frameMaps = mutableMapOf<Int, Pair<FramemapDefinition, AnimationTransformationGroup>>()
    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return
        val archive = cacheLibrary.index(2).archive(12)!!
        val revision = archive.revision
        // TODO(perf): decodes every sequence sequentially; each file is independent
        archive.files.forEach { (fileId, file) ->
            val data = file.data ?: return@forEach
            try {
                seqs[fileId] = loadSeq(revision, fileId, data)
            } catch (_: Exception) {
            }
        }
        loaded = true
    }

    fun getSeq(id: String): AnimationDefinition {
        load()
        return getSeq(seqs, id)
    }

    fun getSeqs(): Array<AnimationDefinition> {
        load()
        return getSeqs(seqs)
    }

    fun getFrameDef(frameHash: Int): AnimationFrameLegacyDefinition? {
        val hexString = Integer.toHexString(frameHash)
        val frameArchiveId = getFileId(hexString)
        val frameArchiveFileId = getFrameId(hexString)
        // TODO(perf): first hash in an archive decodes every frame file; consider lazy per-file decode
        return frames.getOrPut(frameArchiveId) {
            val frameArchive = cacheLibrary.index(0).archive(frameArchiveId)!!
            frameArchive.files().associate { file ->
                val frameContents = file.data ?: error("Frame data null")
                val frameMapArchiveId = AnimationFrameCodec.framemapId(frameContents, frameArchiveId)
                val (frameMapDefinition, transformGroup) = frameMaps.getOrPut(frameMapArchiveId) {
                    val frameMapContents = cacheLibrary.data(1, frameMapArchiveId)!!
                    val frameMapDefinition = AnimationFrameCodec.loadFramemap(frameMapArchiveId, frameMapContents)
                    frameMapDefinition to AnimationFrameCodec.transformationGroup(frameMapArchiveId, frameMapDefinition)
                }
                val frame = AnimationFrameCodec.loadFrame(frameMapDefinition, file.id, frameContents)
                file.id to AnimationFrameCodec.toDefinition(frame, transformGroup)
            }
        }[frameArchiveFileId]
    }


    private fun loadSeq(
        revision: Int,
        seqId: Int,
        seqData: ByteArray,
    ): AnimationDefinition = try {
        // TODO(perf): SequenceLoader226 is constructed per sequence; reuse one loader per revision
        val sequence = SequenceLoader226().apply {
            configureForRevision(revision)
        }.load(seqId, seqData)
        mapSeq(sequence)
    } catch (_: Exception) {
        mapFallback206(SequenceLoader206().load(seqId, seqData))
    }

    companion object {
        internal fun getSeq(seqs: Map<Int, AnimationDefinition>, id: String): AnimationDefinition {
            val seqId = id.toIntOrNull()
                ?: throw IllegalArgumentException("Animation id must be int-convertable $id")
            return seqs[seqId] ?: throw IllegalArgumentException("Animation not found $id")
        }

        internal fun getSeqs(seqs: Map<Int, AnimationDefinition>): Array<AnimationDefinition> =
            seqs.values.toTypedArray()

        internal fun mapSeq(sequence: SequenceDefinition226): AnimationDefinition =
            if (sequence.animMayaId >= 0)
                object : AnimationMayaDefinition {
                    override val id: String = sequence.id
                    override val frameHashes: IntArray = sequence.frameIDs ?: IntArray(0)
                    override val frameLengths: IntArray = sequence.frameLenghts ?: IntArray(0)
                    override val loopOffset: Int = sequence.frameStep
                    override val leftHandItem: Int = sequence.leftHandItem
                    override val rightHandItem: Int = sequence.rightHandItem
                    override val animMayaID: Int = sequence.animMayaId
                    override val animMayaFrameSounds: Map<Int, AnimationSound> =
                        sequence.sounds?.entries
                            ?.mapNotNull { (frame, sound) -> sound?.let { frame to it } }
                            ?.toMap()
                            ?: emptyMap()
                    override val animMayaStart: Int = sequence.animMayaStart
                    override val animMayaEnd: Int = sequence.animMayaEnd
                    override val animMayaMasks: BooleanArray = sequence.animMayaMasks ?: BooleanArray(0)
                }
            else object : AnimationDefinition {
                override val id: String = sequence.id
                override val frameHashes: IntArray = sequence.frameIDs ?: IntArray(0)
                override val frameLengths: IntArray = sequence.frameLenghts ?: IntArray(0)
                override val loopOffset: Int = sequence.frameStep
                override val leftHandItem: Int = sequence.leftHandItem
                override val rightHandItem: Int = sequence.rightHandItem
            }

        internal fun mapFallback206(sequence: SequenceDefinition206): AnimationDefinition =
            object : AnimationDefinition {
                override val id: String = sequence.id
                override val frameHashes: IntArray = sequence.frameIDs ?: IntArray(0)
                override val frameLengths: IntArray = sequence.frameLenghts ?: IntArray(0)
                override val loopOffset: Int = sequence.frameStep
                override val leftHandItem: Int = sequence.leftHandItem
                override val rightHandItem: Int = sequence.rightHandItem
            }
    }
}
