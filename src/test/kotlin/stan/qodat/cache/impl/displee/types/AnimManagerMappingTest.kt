package stan.qodat.cache.impl.displee.types

import net.runelite.cache.definitions.FrameDefinition
import net.runelite.cache.definitions.FramemapDefinition
import qodat.cache.definition.AnimationDefinition
import qodat.cache.definition.AnimationMayaDefinition
import stan.qodat.cache.impl.displee.CacheIdPackingTest
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition206
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition226
import stan.qodat.cache.impl.oldschool.loader.AnimationFrameCodec
import stan.qodat.cache.impl.oldschool.loader.Sound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimManagerMappingTest {

    @Test
    fun getSeqRejectsNonIntegerIds() {
        val error = assertFailsWith<IllegalArgumentException> { getSeq(emptyMap(), "walk") }
        assertEquals("Animation id must be int-convertable walk", error.message)
    }

    @Test
    fun getSeqThrowsWhenSequenceIsMissing() {
        val error = assertFailsWith<IllegalArgumentException> { getSeq(emptyMap(), "12") }
        assertEquals("Animation not found 12", error.message)
    }

    @Test
    fun getSeqsIsEmptyWhenNothingWasLoaded() {
        assertTrue(getSeqs(emptyMap()).isEmpty())
    }

    @Test
    fun mapsLegacySequenceAndTreatsNullFramesAsEmpty() {
        val sequence = SequenceDefinition226("8").apply {
            frameIDs = null
            frameLenghts = null
            frameStep = 4
            leftHandItem = 10
            rightHandItem = 11
            animMayaId = -1
        }
        val mapped = mapSeq(sequence)
        assertFalse(mapped is AnimationMayaDefinition)
        assertEquals("8", mapped.id)
        assertTrue(mapped.frameHashes.isEmpty())
        assertTrue(mapped.frameLengths.isEmpty())
        assertEquals(4, mapped.loopOffset)
        assertEquals(10, mapped.leftHandItem)
        assertEquals(11, mapped.rightHandItem)
    }

    @Test
    fun mapsMayaSequenceAndDropsNullSounds() {
        val keep = Sound(3, 4, 5, 2, 1)
        val sequence = SequenceDefinition226("9").apply {
            frameIDs = intArrayOf(0x00010002)
            frameLenghts = intArrayOf(5)
            frameStep = 1
            leftHandItem = -1
            rightHandItem = -1
            animMayaId = 77
            animMayaStart = 2
            animMayaEnd = 8
            animMayaMasks = booleanArrayOf(true, false)
            sounds = mapOf(0 to keep, 1 to null)
        }
        val mapped = mapSeq(sequence) as AnimationMayaDefinition
        assertEquals(77, mapped.animMayaID)
        assertEquals(2, mapped.animMayaStart)
        assertEquals(8, mapped.animMayaEnd)
        assertTrue(mapped.animMayaMasks.contentEquals(booleanArrayOf(true, false)))
        assertEquals(setOf(0), mapped.animMayaFrameSounds.keys)
        val sound = mapped.animMayaFrameSounds.getValue(0)
        assertEquals(3, sound.id)
        assertEquals(2, sound.loops)
        assertEquals(4, sound.location)
        assertEquals(1, sound.retain)
        assertEquals(5, sound.weight)
    }

    @Test
    fun fallback206MappingKeepsFrameTables() {
        val sequence = SequenceDefinition206("3").apply {
            frameIDs = intArrayOf(1, 2)
            frameLenghts = intArrayOf(6, 7)
            frameStep = 0
            leftHandItem = 8
            rightHandItem = 9
        }
        val mapped = mapFallback206(sequence)
        assertEquals("3", mapped.id)
        assertTrue(mapped.frameHashes.contentEquals(intArrayOf(1, 2)))
        assertTrue(mapped.frameLengths.contentEquals(intArrayOf(6, 7)))
        assertEquals(0, mapped.loopOffset)
        assertEquals(8, mapped.leftHandItem)
        assertEquals(9, mapped.rightHandItem)
    }

    @Test
    fun getFrameDefSplitsPackedHashLikeAnimManager() {
        val hash = CacheIdPackingTest.packQodatFrameHash(0x10, 0x03)
        val hex = Integer.toHexString(hash)
        assertEquals(0x10, CacheIdPackingTest.getFileId(hex))
        assertEquals(0x03, CacheIdPackingTest.getFrameId(hex))
    }

    @Test
    fun frameCodecMapsFrameAndFramemapFields() {
        val framemap = FramemapDefinition().apply {
            types = intArrayOf(0, 5)
            frameMaps = arrayOf(intArrayOf(1, 2), intArrayOf(3))
        }
        val group = AnimationFrameCodec.transformationGroup(17, framemap)
        assertEquals(17, group.id)
        assertTrue(group.transformationTypes.contentEquals(intArrayOf(0, 5)))
        assertTrue(group.targetVertexGroupsIndices[1].contentEquals(intArrayOf(3)))

        val frame = FrameDefinition().apply {
            translatorCount = 2
            indexFrameIds = intArrayOf(0, 1)
            translator_x = intArrayOf(10, 20)
            translator_y = intArrayOf(0, 30)
            translator_z = intArrayOf(0, 40)
        }
        val mapped = AnimationFrameCodec.toDefinition(frame, group)
        assertEquals(2, mapped.transformationCount)
        assertTrue(mapped.transformationGroupAccessIndices.contentEquals(intArrayOf(0, 1)))
        assertTrue(mapped.transformationDeltaX.contentEquals(intArrayOf(10, 20)))
        assertTrue(mapped.transformationDeltaY.contentEquals(intArrayOf(0, 30)))
        assertTrue(mapped.transformationDeltaZ.contentEquals(intArrayOf(0, 40)))
        assertEquals(group, mapped.transformationGroup)
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
                    override val animMayaFrameSounds =
                        sequence.sounds?.entries
                            ?.filter { it.value != null }
                            ?.associate { it.key to it.value!!.toRuneliteSound() }
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
