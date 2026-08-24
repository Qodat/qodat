package stan.qodat.cache.impl.oldschool.loader

import net.runelite.cache.io.OutputStream
import java.util.OptionalInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemLoader226Test {

    @Test
    fun emptyPayloadKeepsIdAndDefaults() {
        val def = ItemLoader226().load(4151, terminator())

        assertEquals(OptionalInt.of(4151), def.getOptionalId())
        assertEquals("4151", def.name)
        assertEquals(-1, def.inventoryModel)
        assertEquals(2000, def.zoom2d)
        assertFalse(def.members)
        assertFalse(def.isTradeable)
    }

    @Test
    fun decodesCoreFieldsThroughRuneLiteInputStream() {
        val bytes = OutputStream().apply {
            writeByte(1)
            writeShort(321)
            writeByte(2)
            writeString("Abyssal whip")
            writeByte(3)
            writeString("A weapon from the abyss.")
            writeByte(4)
            writeShort(840)
            writeByte(5)
            writeShort(280)
            writeByte(6)
            writeShort(90)
            writeByte(12)
            writeInt(120_001)
            writeByte(16)
            writeByte(65)
            writeByte(94)
            writeShort(14)
            writeByte(95)
            writeShort(7)
            writeByte(0)
        }.flip()

        val def = ItemLoader226().load(4151, bytes)
        assertEquals("Abyssal whip", def.name)
        assertEquals("A weapon from the abyss.", def.examineText)
        assertEquals(321, def.inventoryModel)
        assertTrue(def.modelIds.contentEquals(arrayOf("321")))
        assertEquals(840, def.zoom2d)
        assertEquals(280, def.xan2d)
        assertEquals(90, def.yan2d)
        assertEquals(120_001, def.cost)
        assertTrue(def.members)
        assertTrue(def.isTradeable)
        assertEquals(14, def.category)
        assertEquals(7, def.zan2d)
    }

    @Test
    fun signed2dOffsetsWrapPast32767() {
        val bytes = OutputStream().apply {
            writeByte(7)
            writeShort(0xFFFF)
            writeByte(8)
            writeShort(0x8000)
            writeByte(0)
        }.flip()

        val def = ItemLoader226().load(1, bytes)
        assertEquals(-1, def.xOffset2d)
        assertEquals(-32768, def.yOffset2d)
    }

    @Test
    fun hiddenGroundOptionIsCleared() {
        val bytes = OutputStream().apply {
            writeByte(30)
            writeString("Hidden")
            writeByte(31)
            writeString("Wield")
            writeByte(35)
            writeString("Drop")
            writeByte(0)
        }.flip()

        val def = ItemLoader226().load(2, bytes)
        assertNull(def.options!![0])
        assertEquals("Wield", def.options!![1])
        assertEquals("Drop", def.interfaceOptions!![0])
    }

    @Test
    fun recolorRetextureAndStackVariants() {
        val bytes = OutputStream().apply {
            writeByte(40)
            writeByte(1)
            writeShort(0x1234)
            writeShort(0x5678)
            writeByte(41)
            writeByte(1)
            writeShort(10)
            writeShort(20)
            writeByte(100)
            writeShort(4152)
            writeShort(5)
            writeByte(0)
        }.flip()

        val def = ItemLoader226().load(3, bytes)
        assertTrue(def.recolorToFind!!.contentEquals(shortArrayOf(0x1234)))
        assertTrue(def.recolorToReplace!!.contentEquals(shortArrayOf(0x5678.toShort())))
        assertTrue(def.retextureToFind!!.contentEquals(shortArrayOf(10)))
        assertTrue(def.retextureToReplace!!.contentEquals(shortArrayOf(20)))
        assertEquals(4152, def.countObj!![0])
        assertEquals(5, def.countCo!![0])
    }

    @Test
    fun stackablePostZerosWeight() {
        val bytes = OutputStream().apply {
            writeByte(75)
            writeShort(250)
            writeByte(11)
            writeByte(0)
        }.flip()

        val def = ItemLoader226().load(4, bytes)
        assertEquals(1, def.stackable)
        assertEquals(0, def.weight)
    }

    @Test
    fun skipOpcodesDoNotShiftFollowingFields() {
        val bytes = OutputStream().apply {
            writeByte(200)
            writeByte(1)
            writeByte(2)
            writeString("sub")
            writeByte(201)
            writeByte(3)
            writeShort(4)
            writeShort(5)
            writeInt(6)
            writeInt(7)
            writeString("cond")
            writeByte(202)
            writeByte(8)
            writeShort(9)
            writeShort(10)
            writeShort(11)
            writeInt(12)
            writeInt(13)
            writeString("cond-sub")
            writeByte(2)
            writeString("After skips")
            writeByte(0)
        }.flip()

        val def = ItemLoader226().load(5, bytes)
        assertEquals("After skips", def.name)
    }

    @Test
    fun decodesIntModelIdsParamsAndNotes() {
        val bytes = OutputStream().apply {
            writeByte(44)
            writeInt(1_000_042)
            writeByte(97)
            writeShort(4160)
            writeByte(98)
            writeShort(799)
            writeByte(249)
            writeByte(2)
            writeByte(0)
            write24BitInt(0x010203)
            writeInt(77)
            writeByte(1)
            write24BitInt(0x0A0B0C)
            writeString("param")
            writeByte(0)
        }.flip()

        val def = ItemLoader226().load(6, bytes)
        assertEquals(1_000_042, def.inventoryModel)
        assertEquals(4160, def.notedId)
        assertEquals(799, def.notedTemplateId)
        assertEquals(77, def.params!![0x010203])
        assertEquals("param", def.params!![0x0A0B0C])
    }

    @Test
    fun decodesWornModelsAndSubOps() {
        val bytes = OutputStream().apply {
            writeByte(23)
            writeShort(11)
            writeByte(4)
            writeByte(25)
            writeShort(12)
            writeByte(5)
            writeByte(43)
            writeByte(1)
            writeByte(1)
            writeString("Extra")
            writeByte(0)
            writeByte(0)
        }.flip()

        val def = ItemLoader226().load(7, bytes)
        assertEquals(11, def.maleModel0)
        assertEquals(4, def.maleOffset)
        assertEquals(12, def.femaleModel0)
        assertEquals(5, def.femaleOffset)
        assertEquals("Extra", def.subOps!![1]!![0])
    }

    private fun terminator(): ByteArray =
        OutputStream().apply { writeByte(0) }.flip()
}
