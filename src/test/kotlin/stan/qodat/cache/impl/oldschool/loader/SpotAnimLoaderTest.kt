package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.OutputBuffer
import net.runelite.cache.definitions.loaders.SpotAnimLoader as RuneLiteSpotAnimLoader
import java.util.OptionalInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpotAnimLoaderTest {

    @Test
    fun emptyPayloadKeepsIdAndDefaults() {
        val def = SpotAnimLoader().load(14, terminator())

        assertEquals(OptionalInt.of(14), def.getOptionalId())
        assertEquals("14", def.name)
        assertEquals(0, def.modelId)
        assertEquals(-1, def.animationId)
        assertEquals(128, def.resizeX)
        assertEquals(128, def.resizeY)
        assertEquals(0, def.rotation)
        assertEquals(0, def.ambient)
        assertEquals(0, def.contrast)
        assertNull(def.debugName)
        assertTrue(def.modelIds.contentEquals(arrayOf("0")))
        assertTrue(def.animationIds.contentEquals(arrayOf("-1")))
    }

    @Test
    fun decodesOldestShortModelPayload() {
        val def = SpotAnimLoader().load(1, oldestPayload())

        assertEquals(99, def.modelId)
        assertEquals(3, def.animationId)
        assertEquals(200, def.resizeX)
        assertEquals(150, def.resizeY)
        assertEquals(512, def.rotation)
        assertEquals(40, def.ambient)
        assertEquals(20, def.contrast)
        assertNull(def.debugName)
        assertTrue(def.recolorToFind!!.contentEquals(shortArrayOf(0x1234)))
        assertTrue(def.recolorToReplace!!.contentEquals(shortArrayOf(0x5678.toShort())))
        assertTrue(def.modelIds.contentEquals(arrayOf("99")))
        assertTrue(def.animationIds.contentEquals(arrayOf("3")))
        assertTrue(def.findColor!!.contentEquals(shortArrayOf(0x1234)))
        assertTrue(def.replaceColor!!.contentEquals(shortArrayOf(0x5678.toShort())))
    }

    @Test
    fun decodesMidIntModelWithoutDebugName() {
        val def = SpotAnimLoader().load(2, midIntModelPayload())

        assertEquals(900_001, def.modelId)
        assertEquals(44, def.animationId)
        assertNull(def.debugName)
        assertTrue(def.modelIds.contentEquals(arrayOf("900001")))
    }

    @Test
    fun decodesNewestIntModelDebugNameAndRetexture() {
        val def = SpotAnimLoader().load(3, newestPayload())

        assertEquals(1_000_042, def.modelId)
        assertEquals(8, def.animationId)
        assertEquals("fire_blast", def.debugName)
        assertTrue(def.textureToFind!!.contentEquals(shortArrayOf(10)))
        assertTrue(def.textureToReplace!!.contentEquals(shortArrayOf(20)))
    }

    @Test
    fun newerDecoderReadsOlderShortModelBytes() {
        val fromOldest = SpotAnimLoader().load(4, oldestPayload())
        val fromNewestTable = SpotAnimLoader().load(4, oldestPayload())

        assertEquals(fromOldest.modelId, fromNewestTable.modelId)
        assertEquals(99, fromNewestTable.modelId)
        assertEquals(3, fromNewestTable.animationId)
        assertEquals(200, fromNewestTable.resizeX)
        assertNull(fromNewestTable.debugName)
    }

    @Test
    fun unknownOpcodeIsIgnoredWithoutShiftingFollowingFields() {
        val bytes = OutputBuffer(16).apply {
            writeByte(10)
            writeByte(2)
            writeShort(5)
            writeByte(0)
        }.array()

        val def = SpotAnimLoader().load(5, bytes)
        assertEquals(5, def.animationId)
        assertEquals(0, def.modelId)
    }

    @Test
    fun encodeLatestRoundTripsIntModelAndDebugName() {
        val original = SpotAnimLoader().load(6, newestPayload())
        val encoded = SpotAnimLoader().encode(original, SpotAnimEncodeFormat.LATEST)
        val decoded = SpotAnimLoader().load(6, encoded)

        assertEquals(original.modelId, decoded.modelId)
        assertEquals(original.animationId, decoded.animationId)
        assertEquals(original.debugName, decoded.debugName)
        assertTrue(original.textureToFind.contentEquals(decoded.textureToFind))
        assertTrue(original.textureToReplace.contentEquals(decoded.textureToReplace))
    }

    @Test
    fun encodeShortModelOmitsDebugNameAndUsesOpcode1() {
        val original = SpotAnimDefinitionFilled()
        val encoded = SpotAnimLoader().encode(original, SpotAnimEncodeFormat.SHORT_MODEL)
        val decoded = SpotAnimLoader().load(7, encoded)

        assertEquals(99, decoded.modelId)
        assertEquals(3, decoded.animationId)
        assertNull(decoded.debugName)
        assertTrue(decoded.recolorToFind!!.contentEquals(shortArrayOf(7)))
        assertTrue(decoded.recolorToReplace!!.contentEquals(shortArrayOf(8)))
    }

    @Test
    fun encodeShortModelRejectsIntRangeIds() {
        val def = stan.qodat.cache.impl.oldschool.definition.SpotAnimDefinition(8).apply {
            modelId = 900_001
        }
        assertFailsWith<IllegalArgumentException> {
            SpotAnimLoader().encode(def, SpotAnimEncodeFormat.SHORT_MODEL)
        }
    }

    @Test
    fun matchesRuneLiteOnOldestAndNewestPayloads() {
        assertMatchesRuneLite(14, oldestPayload())
        assertMatchesRuneLite(15, midIntModelPayload())
        assertMatchesRuneLite(16, newestPayload())
    }

    private fun assertMatchesRuneLite(id: Int, bytes: ByteArray) {
        val rl = RuneLiteSpotAnimLoader().load(id, bytes)
        val ours = SpotAnimLoader().load(id, bytes)
        assertEquals(rl.id, ours.id)
        assertEquals(rl.modelId, ours.modelId)
        assertEquals(rl.animationId, ours.animationId)
        assertEquals(rl.resizeX, ours.resizeX)
        assertEquals(rl.resizeY, ours.resizeY)
        assertEquals(rl.rotaton, ours.rotation)
        assertEquals(rl.ambient, ours.ambient)
        assertEquals(rl.contrast, ours.contrast)
        assertEquals(rl.debugName, ours.debugName)
        assertTrue(shortArraysEqual(rl.recolorToFind, ours.recolorToFind))
        assertTrue(shortArraysEqual(rl.recolorToReplace, ours.recolorToReplace))
        assertTrue(shortArraysEqual(rl.textureToFind, ours.textureToFind))
        assertTrue(shortArraysEqual(rl.textureToReplace, ours.textureToReplace))
    }

    private fun SpotAnimDefinitionFilled() =
        stan.qodat.cache.impl.oldschool.definition.SpotAnimDefinition(7).apply {
            modelId = 99
            animationId = 3
            debugName = "should_not_encode"
            recolorToFind = shortArrayOf(7)
            recolorToReplace = shortArrayOf(8)
        }

    private fun oldestPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(1)
            writeShort(99)
            writeByte(2)
            writeShort(3)
            writeByte(4)
            writeShort(200)
            writeByte(5)
            writeShort(150)
            writeByte(6)
            writeShort(512)
            writeByte(7)
            writeByte(40)
            writeByte(8)
            writeByte(20)
            writeByte(40)
            writeByte(1)
            writeShort(0x1234)
            writeShort(0x5678)
            writeByte(0)
        }.array()

    private fun midIntModelPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(3)
            writeInt(900_001)
            writeByte(2)
            writeShort(44)
            writeByte(0)
        }.array()

    private fun newestPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(3)
            writeInt(1_000_042)
            writeByte(2)
            writeShort(8)
            writeByte(9)
            writeString("fire_blast")
            writeByte(41)
            writeByte(1)
            writeShort(10)
            writeShort(20)
            writeByte(0)
        }.array()

    private fun terminator(): ByteArray =
        OutputBuffer(16).apply { writeByte(0) }.array()

    private fun shortArraysEqual(left: ShortArray?, right: ShortArray?): Boolean {
        if (left == null && right == null) return true
        if (left == null || right == null) return false
        return left.contentEquals(right)
    }
}
