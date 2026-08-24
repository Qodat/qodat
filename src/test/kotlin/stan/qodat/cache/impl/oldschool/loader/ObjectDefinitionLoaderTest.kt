package stan.qodat.cache.impl.oldschool.loader

import net.runelite.cache.definitions.loaders.ObjectLoader
import net.runelite.cache.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObjectDefinitionLoaderTest {

    @Test
    fun decodesModelsNameAnimationAndRecolors() {
        val bytes = OutputStream().apply {
            writeByte(1)
            writeByte(2)
            writeShort(50)
            writeByte(10)
            writeShort(51)
            writeByte(11)
            writeByte(2)
            writeString("Door")
            writeByte(24)
            writeShort(88)
            writeByte(40)
            writeByte(1)
            writeShort(0x0102)
            writeShort(0x0304)
            writeByte(14)
            writeByte(2)
            writeByte(15)
            writeByte(3)
            writeByte(0)
        }.flip()

        val obj = ObjectLoader().load(1738, bytes)
        assertEquals(1738, obj.id)
        assertEquals("Door", obj.name)
        assertTrue(obj.objectModels.contentEquals(intArrayOf(50, 51)))
        assertTrue(obj.objectTypes.contentEquals(intArrayOf(10, 11)))
        assertEquals(88, obj.animationID)
        assertEquals(2, obj.sizeX)
        assertEquals(3, obj.sizeY)
        assertTrue(obj.recolorToFind.contentEquals(shortArrayOf(0x0102)))
        assertTrue(obj.recolorToReplace.contentEquals(shortArrayOf(0x0304)))
    }

    @Test
    fun unsetAnimationSentinelBecomesMinusOne() {
        val bytes = OutputStream().apply {
            writeByte(24)
            writeShort(0xFFFF)
            writeByte(0)
        }.flip()

        assertEquals(-1, ObjectLoader().load(1, bytes).animationID)
    }

    @Test
    fun modelOnlyOpcodeClearsTypes() {
        val bytes = OutputStream().apply {
            writeByte(5)
            writeByte(1)
            writeShort(9)
            writeByte(0)
        }.flip()

        val obj = ObjectLoader().load(2, bytes)
        assertTrue(obj.objectModels.contentEquals(intArrayOf(9)))
        assertNull(obj.objectTypes)
        assertEquals(1, obj.wallOrDoor)
    }

    @Test
    fun clearedInteractTypeDisablesSupportedItems() {
        val bytes = OutputStream().apply {
            writeByte(17)
            writeByte(0)
        }.flip()

        val obj = ObjectLoader().load(3, bytes)
        assertEquals(0, obj.interactType)
        assertFalse(obj.isBlocksProjectile)
        assertEquals(0, obj.supportsItems)
    }

    @Test
    fun intModelOpcodeAndRevisionConfigStillLoad() {
        val bytes = OutputStream().apply {
            writeByte(7)
            writeByte(1)
            writeInt(800_002)
            writeByte(0)
        }.flip()

        val obj = ObjectLoader().configureForRevision(1000).load(4, bytes)
        assertTrue(obj.objectModels.contentEquals(intArrayOf(800_002)))
        assertNull(obj.objectTypes)
    }
}
