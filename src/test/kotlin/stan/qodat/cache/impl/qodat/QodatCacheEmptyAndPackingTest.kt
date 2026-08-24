package stan.qodat.cache.impl.qodat

import qodat.cache.definition.InterfaceDefinition
import qodat.cache.definition.SpotAnimationDefinition
import qodat.cache.definition.SpriteDefinition
import stan.qodat.cache.impl.displee.CacheIdPackingTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QodatCacheEmptyAndPackingTest {

    @Test
    fun emptyQodatCacheHasNoListKinds() {
        assertTrue(qodatPopulatedKinds(0, 0, 0, 0).isEmpty())
        assertEquals(setOf(qodat.cache.Cache.LIST_NPC), qodatPopulatedKinds(1, 0, 0, 0))
        assertEquals(
            setOf(qodat.cache.Cache.LIST_ITEM, qodat.cache.Cache.LIST_ANIMATIONS),
            qodatPopulatedKinds(0, 0, 2, 3),
        )
    }

    @Test
    fun emptyCollectionsMatchQodatCacheStubs() {
        assertTrue(getInterface(0).isEmpty())
        assertTrue(getRootInterfaces().isEmpty())
        assertTrue(getSprites().isEmpty())
        assertTrue(getSpotAnimations().isEmpty())
    }

    @Test
    fun encodePacksFrameArchiveAndIndexIntoHashes() {
        val frameArchiveId = 42
        val hashes = IntArray(3) { index ->
            CacheIdPackingTest.packQodatFrameHash(frameArchiveId, index)
        }
        assertEquals(0x002A_0000, hashes[0])
        assertEquals(0x002A_0001, hashes[1])
        assertEquals(0x002A_0002, hashes[2])
        hashes.forEachIndexed { index, hash ->
            val hex = Integer.toHexString(hash)
            assertEquals(frameArchiveId, CacheIdPackingTest.getFileId(hex))
            assertEquals(index, CacheIdPackingTest.getFrameId(hex))
        }
    }

    @Test
    fun qodatDefinitionsHoldMappedIdentity() {
        val npc = QodatNpcDefinition("Guard", arrayOf("100"), arrayOf("20"))
        val item = QodatItemDefinition("Whip", arrayOf("321"))
        val obj = QodatObjectDefinition("Door", arrayOf("50"), arrayOf("88"))
        val anim = QodatAnimationDefinition("9", intArrayOf(1), intArrayOf(5), 0, -1, -1)
        assertEquals("Guard", npc.name)
        assertEquals("Whip", item.name)
        assertEquals("Door", obj.name)
        assertEquals("9", anim.id)
        assertTrue(npc.modelIds.contentEquals(arrayOf("100")))
        assertTrue(item.modelIds.contentEquals(arrayOf("321")))
        assertTrue(obj.animationIds.contentEquals(arrayOf("88")))
    }

    companion object {
        @Suppress("UNUSED_PARAMETER")
        internal fun getInterface(groupId: Int): Array<InterfaceDefinition> = emptyArray()

        internal fun getRootInterfaces(): Map<Int, List<InterfaceDefinition>> = emptyMap()

        internal fun getSprites(): Array<SpriteDefinition> = emptyArray()

        internal fun getSpotAnimations(): Array<SpotAnimationDefinition> = emptyArray()
    }
}
