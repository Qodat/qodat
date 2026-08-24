package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.OutputBuffer
import net.runelite.cache.definitions.loaders.ObjectLoader as RuneLiteObjectLoader
import stan.qodat.cache.impl.oldschool.definition.ObjectDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObjectDefinitionLoaderTest {

    @Test
    fun decodesModelsNameAnimationAndRecolors() {
        val obj = ObjectLoader().load(1738, oldestPayload())
        assertEquals(1738, obj.id)
        assertEquals("Door", obj.name)
        assertTrue(obj.objectModels.contentEquals(intArrayOf(50, 51)))
        assertTrue(obj.objectTypes.contentEquals(intArrayOf(10, 11)))
        assertEquals(88, obj.animationId)
        assertEquals(2, obj.sizeX)
        assertEquals(3, obj.sizeY)
        assertTrue(obj.recolorToFind.contentEquals(shortArrayOf(0x0102)))
        assertTrue(obj.recolorToReplace.contentEquals(shortArrayOf(0x0304)))
        assertTrue(obj.modelIds.contentEquals(arrayOf("50", "51")))
        assertTrue(obj.animationIds.contentEquals(arrayOf("88")))
    }

    @Test
    fun unsetAnimationSentinelBecomesMinusOne() {
        val bytes = OutputBuffer(16).apply {
            writeByte(24)
            writeShort(0xFFFF)
            writeByte(0)
        }.array()

        val obj = ObjectLoader().load(1, bytes)
        assertEquals(-1, obj.animationId)
        assertTrue(obj.animationIds.isEmpty())
    }

    @Test
    fun modelOnlyOpcodeClearsTypes() {
        val bytes = OutputBuffer(16).apply {
            writeByte(5)
            writeByte(1)
            writeShort(9)
            writeByte(0)
        }.array()

        val obj = ObjectLoader().load(2, bytes)
        assertTrue(obj.objectModels.contentEquals(intArrayOf(9)))
        assertNull(obj.objectTypes)
        assertEquals(1, obj.wallOrDoor)
    }

    @Test
    fun clearedInteractTypeDisablesSupportedItems() {
        val bytes = OutputBuffer(16).apply {
            writeByte(17)
            writeByte(0)
        }.array()

        val obj = ObjectLoader().load(3, bytes)
        assertEquals(0, obj.interactType)
        assertFalse(obj.blocksProjectile)
        assertEquals(0, obj.supportsItems)
    }

    @Test
    fun intModelOpcodeAndRevisionConfigStillLoad() {
        val bytes = OutputBuffer(16).apply {
            writeByte(7)
            writeByte(1)
            writeInt(800_002)
            writeByte(0)
        }.array()

        val obj = ObjectLoader().configureForRevision(1000).load(4, bytes)
        assertTrue(obj.objectModels.contentEquals(intArrayOf(800_002)))
        assertNull(obj.objectTypes)
    }

    @Test
    fun revisionSwitchGatesAmbientSoundRetain() {
        assertFalse(ObjectLoader().configureForRevision(REV_220_OBJ_ARCHIVE_REV - 1).rev220SoundData)
        assertTrue(ObjectLoader().configureForRevision(REV_220_OBJ_ARCHIVE_REV).rev220SoundData)
        assertTrue(ObjectLoader().configureForRevision(REV_220_OBJ_ARCHIVE_REV + 1).rev220SoundData)
    }

    @Test
    fun ambientSoundOmitsRetainBelowRev220Gate() {
        val obj = ObjectLoader().configureForRevision(REV_220_OBJ_ARCHIVE_REV - 1).load(5, pre220SoundPayload())
        assertEquals(100, obj.ambientSoundId)
        assertEquals(5, obj.ambientSoundDistance)
        assertEquals(0, obj.ambientSoundRetain)
    }

    @Test
    fun ambientSoundReadsRetainAtRev220Gate() {
        val atGate = ObjectLoader().configureForRevision(REV_220_OBJ_ARCHIVE_REV).load(6, rev220SoundPayload())
        val aboveGate = ObjectLoader().configureForRevision(REV_220_OBJ_ARCHIVE_REV + 1).load(6, rev220SoundPayload())
        assertEquals(100, atGate.ambientSoundId)
        assertEquals(5, atGate.ambientSoundDistance)
        assertEquals(7, atGate.ambientSoundRetain)
        assertEquals(7, aboveGate.ambientSoundRetain)
    }

    @Test
    fun newerDecoderReadsOlderShortModelBytes() {
        val fromOldest = ObjectLoader().configureForRevision(REV_220_OBJ_ARCHIVE_REV - 1).load(7, oldestPayload())
        val fromNewestTable = ObjectLoader().configureForRevision(REV_220_OBJ_ARCHIVE_REV).load(7, oldestPayload())

        assertTrue(fromOldest.objectModels.contentEquals(fromNewestTable.objectModels))
        assertTrue(fromOldest.objectTypes.contentEquals(fromNewestTable.objectTypes))
        assertEquals("Door", fromNewestTable.name)
        assertEquals(88, fromNewestTable.animationId)
        assertTrue(fromNewestTable.recolorToFind.contentEquals(shortArrayOf(0x0102)))
    }

    @Test
    fun decodesNewestIntModelsSoundsAndExtras() {
        val obj = ObjectLoader().load(8, newestPayload())
        assertTrue(obj.objectModels.contentEquals(intArrayOf(800_002)))
        assertTrue(obj.objectTypes.contentEquals(intArrayOf(10)))
        assertEquals("Portal", obj.name)
        assertEquals(99, obj.animationId)
        assertEquals(12, obj.ambientSoundId)
        assertEquals(4, obj.ambientSoundDistance)
        assertEquals(3, obj.ambientSoundRetain)
        assertTrue(obj.randomizeAnimStart)
        assertEquals("Open", obj.actions[0])
        assertEquals(77, obj.params!![0x010203])
        assertEquals("param", obj.params!![0x0A0B0C])
    }

    @Test
    fun unknownOpcodeIsIgnoredWithoutShiftingFollowingFields() {
        val bytes = OutputBuffer(16).apply {
            writeByte(8)
            writeByte(2)
            writeString("Kept")
            writeByte(0)
        }.array()

        val obj = ObjectLoader().load(9, bytes)
        assertEquals("Kept", obj.name)
    }

    @Test
    fun encodeLatestRoundTripsIntModelsAndExtras() {
        val original = ObjectLoader().load(10, newestPayload())
        val encoded = ObjectLoader().encode(original, ObjectEncodeFormat.LATEST)
        val decoded = ObjectLoader().load(10, encoded)

        assertTrue(original.objectModels.contentEquals(decoded.objectModels))
        assertTrue(original.objectTypes.contentEquals(decoded.objectTypes))
        assertEquals(original.name, decoded.name)
        assertEquals(original.animationId, decoded.animationId)
        assertEquals(original.ambientSoundId, decoded.ambientSoundId)
        assertEquals(original.ambientSoundRetain, decoded.ambientSoundRetain)
        assertEquals(original.randomizeAnimStart, decoded.randomizeAnimStart)
        assertEquals(original.actions[0], decoded.actions[0])
        assertEquals(original.params!![0x010203], decoded.params!![0x010203])
        assertEquals(original.params!![0x0A0B0C], decoded.params!![0x0A0B0C])
    }

    @Test
    fun encodeShortModelOmitsIntRangeOpsAndUsesOpcode1() {
        val original = ObjectDefinition(11).apply {
            name = "Door"
            objectModels = intArrayOf(50, 51)
            objectTypes = intArrayOf(10, 11)
            animationId = 88
        }
        val encoded = ObjectLoader().encode(original, ObjectEncodeFormat.SHORT_MODEL)
        val decoded = ObjectLoader().load(11, encoded)

        assertTrue(decoded.objectModels.contentEquals(intArrayOf(50, 51)))
        assertTrue(decoded.objectTypes.contentEquals(intArrayOf(10, 11)))
        assertEquals("Door", decoded.name)
        assertEquals(88, decoded.animationId)
    }

    @Test
    fun encodeShortModelRejectsIntRangeIds() {
        val def = ObjectDefinition(12).apply { objectModels = intArrayOf(800_002) }
        assertFailsWith<IllegalArgumentException> {
            ObjectLoader().encode(def, ObjectEncodeFormat.SHORT_MODEL)
        }
    }

    @Test
    fun matchesRuneLiteOnOldestGateAndNewestPayloads() {
        assertMatchesRuneLite(14, oldestPayload())
        assertMatchesRuneLite(15, midIntModelPayload())
        assertMatchesRuneLite(16, newestPayload())
        assertMatchesRuneLite(17, pre220SoundPayload(), revision = REV_220_OBJ_ARCHIVE_REV - 1)
        assertMatchesRuneLite(18, rev220SoundPayload(), revision = REV_220_OBJ_ARCHIVE_REV)
        assertMatchesRuneLite(19, rev220SoundPayload(), revision = REV_220_OBJ_ARCHIVE_REV + 1)
    }

    private fun assertMatchesRuneLite(id: Int, bytes: ByteArray, revision: Int? = null) {
        val rlLoader = RuneLiteObjectLoader()
        val oursLoader = ObjectLoader()
        if (revision != null) {
            rlLoader.configureForRevision(revision)
            oursLoader.configureForRevision(revision)
        }
        val rl = rlLoader.load(id, bytes)
        val ours = oursLoader.load(id, bytes)
        assertEquals(rl.id, ours.id)
        assertEquals(rl.name, ours.name)
        assertTrue(intArraysEqual(rl.objectModels, ours.objectModels))
        assertTrue(intArraysEqual(rl.objectTypes, ours.objectTypes))
        assertEquals(rl.animationID, ours.animationId)
        assertEquals(rl.sizeX, ours.sizeX)
        assertEquals(rl.sizeY, ours.sizeY)
        assertEquals(rl.wallOrDoor, ours.wallOrDoor)
        assertEquals(rl.interactType, ours.interactType)
        assertEquals(rl.isBlocksProjectile, ours.blocksProjectile)
        assertEquals(rl.supportsItems, ours.supportsItems)
        assertEquals(rl.ambientSoundId, ours.ambientSoundId)
        assertEquals(rl.ambientSoundDistance, ours.ambientSoundDistance)
        assertEquals(rl.ambientSoundRetain, ours.ambientSoundRetain)
        assertEquals(rl.isRandomizeAnimStart, ours.randomizeAnimStart)
        assertTrue(shortArraysEqual(rl.recolorToFind, ours.recolorToFind))
        assertTrue(shortArraysEqual(rl.recolorToReplace, ours.recolorToReplace))
        assertEquals(rl.params?.get(0x010203), ours.params?.get(0x010203))
        assertEquals(rl.params?.get(0x0A0B0C), ours.params?.get(0x0A0B0C))
    }

    private fun oldestPayload(): ByteArray =
        OutputBuffer(16).apply {
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
        }.array()

    private fun midIntModelPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(7)
            writeByte(1)
            writeInt(800_002)
            writeByte(0)
        }.array()

    private fun pre220SoundPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(78)
            writeShort(100)
            writeByte(5)
            writeByte(0)
        }.array()

    private fun rev220SoundPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(78)
            writeShort(100)
            writeByte(5)
            writeByte(7)
            writeByte(0)
        }.array()

    private fun newestPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(6)
            writeByte(1)
            writeInt(800_002)
            writeByte(10)
            writeByte(2)
            writeString("Portal")
            writeByte(24)
            writeShort(99)
            writeByte(30)
            writeString("Open")
            writeByte(78)
            writeShort(12)
            writeByte(4)
            writeByte(3)
            writeByte(89)
            writeByte(249)
            writeByte(2)
            writeByte(0)
            write24BitInt(0x010203)
            writeInt(77)
            writeByte(1)
            write24BitInt(0x0A0B0C)
            writeString("param")
            writeByte(0)
        }.array()

    private fun intArraysEqual(left: IntArray?, right: IntArray?): Boolean {
        if (left == null && right == null) return true
        if (left == null || right == null) return false
        return left.contentEquals(right)
    }

    private fun shortArraysEqual(left: ShortArray?, right: ShortArray?): Boolean {
        if (left == null && right == null) return true
        if (left == null || right == null) return false
        return left.contentEquals(right)
    }
}
