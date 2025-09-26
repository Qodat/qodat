package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import net.runelite.cache.definitions.FramemapDefinition
import net.runelite.cache.definitions.SequenceDefinition
import net.runelite.cache.definitions.loaders.FrameLoader
import net.runelite.cache.definitions.loaders.FramemapLoader
import net.runelite.cache.definitions.loaders.SequenceLoader
import qodat.cache.definition.AnimationDefinition
import qodat.cache.definition.AnimationFrameLegacyDefinition
import qodat.cache.definition.AnimationMayaDefinition
import qodat.cache.definition.AnimationTransformationGroup
import stan.qodat.cache.impl.displee.DispleeCache.getFileId
import stan.qodat.cache.impl.displee.DispleeCache.getFrameId
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader206
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader226

class AnimManager(
    private val cacheLibrary: CacheLibrary
) {

    private val seqs = mutableMapOf<Int, AnimationDefinition>()
    private val frames = mutableMapOf<Int, Map<Int, AnimationFrameLegacyDefinition>>()
    private val frameMaps = mutableMapOf<Int, Pair<FramemapDefinition, AnimationTransformationGroup>>()

    fun load() {
        val archive = cacheLibrary.index(2).archive(12)!!
        val revision = archive.revision
        archive.files.forEach { (fileId, file) ->
            seqs[fileId] = loadSeq(revision, fileId, file.data?:error("Frame data null"))
        }
    }

    fun getSeq(id: String): AnimationDefinition {
        val seqId = id.toIntOrNull() ?: throw IllegalArgumentException("Animation id must be int-convertable $id")
        return seqs[seqId] ?: throw IllegalArgumentException("Animation not found $id")
    }

    fun getSeqs(): Array<AnimationDefinition> =
        seqs.values.toTypedArray()

    fun getFrameDef(frameHash: Int): AnimationFrameLegacyDefinition? {
        val hexString = Integer.toHexString(frameHash)
        val frameArchiveId = getFileId(hexString)
        val frameArchiveFileId = getFrameId(hexString)
        return frames.getOrPut(frameArchiveId) {
            val frameArchive = cacheLibrary.index(0).archive(frameArchiveId)!!
            frameArchive.files().associate { file ->
                val frameContents = file.data ?: error("Frame data null")
                val frameMapArchiveId = frameContents[0].toInt() and 0xff shl 8 or (frameContents[1].toInt() and 0xff)
                val (frameMapDefinition, transformGroup) = frameMaps.getOrPut(frameMapArchiveId) {
                    val frameMapContents = cacheLibrary.data(1, frameMapArchiveId)!!
                    val frameMapDefinition = FramemapLoader().load(frameMapArchiveId, frameMapContents)
                    frameMapDefinition to object : AnimationTransformationGroup {
                        override val id: Int = frameMapArchiveId
                        override val transformationTypes: IntArray = frameMapDefinition.types
                        override val targetVertexGroupsIndices: Array<IntArray> = frameMapDefinition.frameMaps
                    }
                }
                val frame = FrameLoader().load(frameMapDefinition, file.id, frameContents)
                file.id to object : AnimationFrameLegacyDefinition {
                    override val transformationCount: Int = frame.translatorCount
                    override val transformationGroupAccessIndices: IntArray = frame.indexFrameIds
                    override val transformationDeltaX: IntArray = frame.translator_x
                    override val transformationDeltaY: IntArray = frame.translator_y
                    override val transformationDeltaZ: IntArray = frame.translator_z
                    override val transformationGroup: AnimationTransformationGroup = transformGroup
                }
            }
        }[frameArchiveFileId]
    }


    private fun loadSeq(
        revision: Int,
        seqId: Int,
        seqData: ByteArray,
    ): AnimationDefinition = try {
        val sequence = SequenceLoader226().apply {
            configureForRevision(revision)
        }.load(seqId, seqData)
        if (sequence.animMayaId >= 0)
            object : AnimationMayaDefinition {
                override val id: String = seqId.toString()
                override val frameHashes: IntArray = sequence.frameIDs ?: IntArray(0)
                override val frameLengths: IntArray = sequence.frameLenghts ?: IntArray(0)
                override val loopOffset: Int = sequence.frameStep
                override val leftHandItem: Int = sequence.leftHandItem
                override val rightHandItem: Int = sequence.rightHandItem
                override val animMayaID: Int = sequence.animMayaId
                override val animMayaFrameSounds: Map<Int, SequenceDefinition.Sound> =
                    sequence.sounds?.entries?.filter { it.value != null }?.associate { it.key to it.value?.toRuneliteSound()!! } ?: emptyMap()
                override val animMayaStart: Int = sequence.animMayaStart
                override val animMayaEnd: Int = sequence.animMayaEnd
                override val animMayaMasks: BooleanArray = sequence.animMayaMasks ?: BooleanArray(0)
            }
        else object : AnimationDefinition {
            override val id: String = seqId.toString()
            override val frameHashes: IntArray = sequence.frameIDs ?: IntArray(0)
            override val frameLengths: IntArray = sequence.frameLenghts ?: IntArray(0)
            override val loopOffset: Int = sequence.frameStep
            override val leftHandItem: Int = sequence.leftHandItem
            override val rightHandItem: Int = sequence.rightHandItem
        }
    } catch (e: Exception) {
        e.printStackTrace()
        val sequence = SequenceLoader206().load(seqId, seqData)
        object : AnimationDefinition {
            override val id: String = seqId.toString()
            override val frameHashes: IntArray = sequence.frameIDs ?: IntArray(0)
            override val frameLengths: IntArray = sequence.frameLenghts ?: IntArray(0)
            override val loopOffset: Int = sequence.frameStep
            override val leftHandItem: Int = sequence.leftHandItem
            override val rightHandItem: Int = sequence.rightHandItem
        }
    }
}