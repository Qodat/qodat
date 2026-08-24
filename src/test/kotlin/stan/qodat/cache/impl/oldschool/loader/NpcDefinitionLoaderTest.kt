package stan.qodat.cache.impl.oldschool.loader

import net.runelite.cache.definitions.loaders.NpcLoader
import com.displee.io.impl.OutputBuffer
import stan.qodat.cache.NpcPrimaryAnimations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NpcDefinitionLoaderTest {

    @Test
    fun decodesModelsNameAnimsAndRecolors() {
        val bytes = OutputBuffer(16).apply {
            writeByte(1)
            writeByte(2)
            writeShort(100)
            writeShort(101)
            writeByte(2)
            writeString("Guard")
            writeByte(12)
            writeByte(2)
            writeByte(13)
            writeShort(20)
            writeByte(17)
            writeShort(21)
            writeShort(22)
            writeShort(23)
            writeShort(24)
            writeByte(40)
            writeByte(1)
            writeShort(0x1111)
            writeShort(0x2222)
            writeByte(95)
            writeShort(42)
            writeByte(0)
        }.array()

        val npc = NpcLoader().load(394, bytes)
        assertEquals(394, npc.id)
        assertEquals("Guard", npc.name)
        assertTrue(npc.models.contentEquals(intArrayOf(100, 101)))
        assertEquals(2, npc.size)
        assertEquals(20, npc.standingAnimation)
        assertEquals(21, npc.walkingAnimation)
        assertEquals(22, npc.rotate180Animation)
        assertEquals(23, npc.rotateLeftAnimation)
        assertEquals(24, npc.rotateRightAnimation)
        assertEquals(42, npc.combatLevel)
        assertTrue(npc.recolorToFind.contentEquals(shortArrayOf(0x1111)))
        assertTrue(npc.recolorToReplace.contentEquals(shortArrayOf(0x2222)))
        assertEquals((0.4F * 2 * 128).toInt(), npc.footprintSize)
        assertTrue(NpcPrimaryAnimations.intIds(npc).contentEquals(intArrayOf(20, 21, 23, 24, 22)))
    }

    @Test
    fun defaultFootprintScalesWithSize() {
        val bytes = OutputBuffer(16).apply { writeByte(0) }.array()
        val npc = NpcLoader().load(1, bytes)
        assertEquals("null", npc.name)
        assertEquals((0.4F * 128).toInt(), npc.footprintSize)
    }

    @Test
    fun revisionSwitchStillDecodesName() {
        val bytes = OutputBuffer(16).apply {
            writeByte(2)
            writeString("Man")
            writeByte(0)
        }.array()

        val npc = NpcLoader().configureForRevision(1000).load(2, bytes)
        assertEquals("Man", npc.name)
    }

    @Test
    fun intModelOpcodeReplacesShortModels() {
        val bytes = OutputBuffer(16).apply {
            writeByte(61)
            writeByte(1)
            writeInt(900_001)
            writeByte(0)
        }.array()

        val npc = NpcLoader().load(3, bytes)
        assertTrue(npc.models.contentEquals(intArrayOf(900_001)))
    }
}
