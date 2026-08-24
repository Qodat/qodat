package stan.qodat.cache

import stan.qodat.cache.impl.oldschool.loader.AnimationFrameCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnimationSkeletonIndexTest {

    @Test
    fun mapsLegacyAndMayaSequencesToSkeletons() {
        val sequences = listOf(
            AnimationSkeletonIndex.SequenceRef(10, -1, intArrayOf(0x00010002, 0x00030004)),
            AnimationSkeletonIndex.SequenceRef(11, 55, null),
            AnimationSkeletonIndex.SequenceRef(12, -1, intArrayOf()),
        )
        val mapped = AnimationSkeletonIndex.mapAnimationsToSkeletons(
            sequences,
            mapOf(0x00010002 to 8, 0x00030004 to 9),
            mapOf(55 to 8),
        )
        assertEquals(setOf(8, 9), mapped[10])
        assertEquals(setOf(8), mapped[11])
        assertEquals(emptySet(), mapped[12])
    }

    @Test
    fun expandsSiblingSkeletonsAndCollectsSharedAnims() {
        val signature = AnimationSkeletonIndex.SkeletonSignature(2, "1,2,3")
        val animationMap = AnimationSkeletonIndex.AnimationMap(
            skeletonIdsByAnimationId = mapOf(1 to setOf(10), 2 to setOf(11), 3 to setOf(12)),
            signatureBySkeletonId = mapOf(10 to signature, 11 to signature),
        )
        val expanded = AnimationSkeletonIndex.expandSkeletonIds(setOf(10), animationMap)
        assertEquals(setOf(10, 11), expanded)
        assertTrue(
            AnimationSkeletonIndex.animationIdsSharingSkeletons(
                expanded,
                animationMap.animationIdsBySkeletonId,
            ).contentEquals(intArrayOf(1, 2))
        )
    }

    @Test
    fun readsSkeletonIdsFromPayloads() {
        assertEquals(0x1234, AnimationSkeletonIndex.skeletonIdFromMayaAnimation(byteArrayOf(1, 0x12, 0x34)))
        assertNull(AnimationSkeletonIndex.skeletonIdFromMayaAnimation(byteArrayOf(1, 2)))
        val osrs = byteArrayOf(0x00, 0x2A)
        assertEquals(42, AnimationSkeletonIndex.skeletonIdFromLegacyFrame(osrs, 99))
        val nr317 = byteArrayOf(AnimationFrameCodec.NR_317_MAGIC, AnimationFrameCodec.NR_317_MAGIC)
        assertEquals(17, AnimationSkeletonIndex.skeletonIdFromLegacyFrame(nr317, 17))
    }

    @Test
    fun skeletonSignatureReadsLabelsAndMayaBoneCount() {
        val data = byteArrayOf(
            2,
            0, 0,
            2, 1,
            5, 1,
            3,
            0x00, 0x04,
        )
        val signature = AnimationSkeletonIndex.skeletonSignature(data)
        assertEquals(4, signature?.mayaBoneCount)
        assertEquals("1,3,5", signature?.labelKey)
        assertNull(AnimationSkeletonIndex.skeletonSignature(byteArrayOf()))
    }
}
