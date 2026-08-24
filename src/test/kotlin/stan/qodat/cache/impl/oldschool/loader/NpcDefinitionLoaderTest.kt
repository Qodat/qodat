package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.OutputBuffer
import net.runelite.cache.definitions.loaders.NpcLoader as RuneLiteNpcLoader
import stan.qodat.cache.NpcPrimaryAnimations
import stan.qodat.cache.impl.oldschool.definition.NpcDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NpcDefinitionLoaderTest {

    @Test
    fun decodesModelsNameAnimsAndRecolors() {
        val npc = NpcLoader().load(394, oldestPayload())
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
        assertTrue(npc.modelIds.contentEquals(arrayOf("100", "101")))
        assertTrue(npc.primaryAnimationIds.contentEquals(arrayOf("20", "21", "23", "24", "22")))
    }

    @Test
    fun defaultFootprintScalesWithSize() {
        val npc = NpcLoader().load(1, terminator())
        assertEquals("null", npc.name)
        assertEquals((0.4F * 128).toInt(), npc.footprintSize)
        assertTrue(npc.modelIds.isEmpty())
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
        assertFalse(NpcLoader().configureForRevision(REV_210_NPC_ARCHIVE_REV - 1).rev210HeadIcons)
        assertTrue(NpcLoader().configureForRevision(REV_210_NPC_ARCHIVE_REV).rev210HeadIcons)
    }

    @Test
    fun intModelOpcodeReplacesShortModels() {
        val npc = NpcLoader().load(3, midIntModelPayload())
        assertTrue(npc.models.contentEquals(intArrayOf(900_001)))
        assertTrue(npc.chatheadModels.contentEquals(intArrayOf(900_002)))
    }

    @Test
    fun newerDecoderReadsOlderShortModelBytes() {
        val fromOldest = NpcLoader().configureForRevision(REV_210_NPC_ARCHIVE_REV - 1).load(4, oldestPayload())
        val fromNewestTable = NpcLoader().configureForRevision(REV_210_NPC_ARCHIVE_REV).load(4, oldestPayload())

        assertTrue(fromOldest.models.contentEquals(fromNewestTable.models))
        assertEquals("Guard", fromNewestTable.name)
        assertEquals(20, fromNewestTable.standingAnimation)
        assertEquals(42, fromNewestTable.combatLevel)
        assertTrue(fromNewestTable.recolorToFind.contentEquals(shortArrayOf(0x1111)))
    }

    @Test
    fun headIconOpcodeUsesShortFormBelowRev210Gate() {
        val npc = NpcLoader().configureForRevision(REV_210_NPC_ARCHIVE_REV - 1).load(5, pre210HeadIconPayload())
        assertTrue(npc.headIconArchiveIds.contentEquals(intArrayOf(-1)))
        assertTrue(npc.headIconSpriteIndex.contentEquals(shortArrayOf(5)))
    }

    @Test
    fun headIconOpcodeUsesBitfieldAtRev210Gate() {
        val npc = NpcLoader().configureForRevision(REV_210_NPC_ARCHIVE_REV).load(6, rev210HeadIconPayload())
        assertTrue(npc.headIconArchiveIds.contentEquals(intArrayOf(12)))
        assertTrue(npc.headIconSpriteIndex.contentEquals(shortArrayOf(3)))
    }

    @Test
    fun decodesNewestIntModelsRunCrawlAndExtras() {
        val npc = NpcLoader().load(7, newestPayload())
        assertTrue(npc.models.contentEquals(intArrayOf(1_000_042)))
        assertEquals(30, npc.runAnimation)
        assertEquals(31, npc.runRotate180Animation)
        assertEquals(32, npc.runRotateLeftAnimation)
        assertEquals(33, npc.runRotateRightAnimation)
        assertEquals(40, npc.crawlAnimation)
        assertEquals(50, npc.height)
        assertEquals(2, npc.renderPriority)
        assertTrue(npc.isFollower)
        assertEquals("Talk-to", npc.actions[0])
        assertEquals(77, npc.params!![0x010203])
        assertEquals("param", npc.params!![0x0A0B0C])
    }

    @Test
    fun unknownOpcodeIsIgnoredWithoutShiftingFollowingFields() {
        val bytes = OutputBuffer(16).apply {
            writeByte(8)
            writeByte(2)
            writeString("Kept")
            writeByte(0)
        }.array()

        val npc = NpcLoader().load(8, bytes)
        assertEquals("Kept", npc.name)
    }

    @Test
    fun encodeLatestRoundTripsIntModelsAndExtras() {
        val original = NpcLoader().load(9, newestPayload())
        val encoded = NpcLoader().encode(original, NpcEncodeFormat.LATEST)
        val decoded = NpcLoader().load(9, encoded)

        assertTrue(original.models.contentEquals(decoded.models))
        assertEquals(original.runAnimation, decoded.runAnimation)
        assertEquals(original.crawlAnimation, decoded.crawlAnimation)
        assertEquals(original.height, decoded.height)
        assertEquals(original.renderPriority, decoded.renderPriority)
        assertEquals(original.isFollower, decoded.isFollower)
        assertEquals(original.actions[0], decoded.actions[0])
        assertEquals(original.params!![0x010203], decoded.params!![0x010203])
        assertEquals(original.params!![0x0A0B0C], decoded.params!![0x0A0B0C])
    }

    @Test
    fun encodeShortModelOmitsIntRangeOpsAndUsesOpcode1() {
        val original = NpcDefinition(10).apply {
            name = "Guard"
            models = intArrayOf(100, 101)
            standingAnimation = 20
            walkingAnimation = 21
            rotate180Animation = 22
            rotateLeftAnimation = 23
            rotateRightAnimation = 24
        }
        val encoded = NpcLoader().encode(original, NpcEncodeFormat.SHORT_MODEL)
        val decoded = NpcLoader().load(10, encoded)

        assertTrue(decoded.models.contentEquals(intArrayOf(100, 101)))
        assertEquals("Guard", decoded.name)
        assertEquals(20, decoded.standingAnimation)
        assertEquals(21, decoded.walkingAnimation)
    }

    @Test
    fun encodeShortModelRejectsIntRangeIds() {
        val def = NpcDefinition(11).apply { models = intArrayOf(900_001) }
        assertFailsWith<IllegalArgumentException> {
            NpcLoader().encode(def, NpcEncodeFormat.SHORT_MODEL)
        }
    }

    @Test
    fun matchesRuneLiteOnOldestGateAndNewestPayloads() {
        assertMatchesRuneLite(14, oldestPayload())
        assertMatchesRuneLite(15, midIntModelPayload())
        assertMatchesRuneLite(16, newestPayload())
        assertMatchesRuneLite(17, pre210HeadIconPayload(), revision = REV_210_NPC_ARCHIVE_REV - 1)
        assertMatchesRuneLite(18, rev210HeadIconPayload(), revision = REV_210_NPC_ARCHIVE_REV)
        assertMatchesRuneLite(19, rev210HeadIconPayload(), revision = REV_210_NPC_ARCHIVE_REV + 1)
    }

    private fun assertMatchesRuneLite(id: Int, bytes: ByteArray, revision: Int? = null) {
        val rlLoader = RuneLiteNpcLoader()
        val oursLoader = NpcLoader()
        if (revision != null) {
            rlLoader.configureForRevision(revision)
            oursLoader.configureForRevision(revision)
        }
        val rl = rlLoader.load(id, bytes)
        val ours = oursLoader.load(id, bytes)
        assertEquals(rl.id, ours.id)
        assertEquals(rl.name, ours.name)
        assertEquals(rl.size, ours.size)
        assertTrue(intArraysEqual(rl.models, ours.models))
        assertTrue(intArraysEqual(rl.chatheadModels, ours.chatheadModels))
        assertEquals(rl.standingAnimation, ours.standingAnimation)
        assertEquals(rl.walkingAnimation, ours.walkingAnimation)
        assertEquals(rl.rotate180Animation, ours.rotate180Animation)
        assertEquals(rl.rotateLeftAnimation, ours.rotateLeftAnimation)
        assertEquals(rl.rotateRightAnimation, ours.rotateRightAnimation)
        assertEquals(rl.runAnimation, ours.runAnimation)
        assertEquals(rl.runRotate180Animation, ours.runRotate180Animation)
        assertEquals(rl.crawlAnimation, ours.crawlAnimation)
        assertEquals(rl.combatLevel, ours.combatLevel)
        assertEquals(rl.footprintSize, ours.footprintSize)
        assertEquals(rl.height, ours.height)
        assertEquals(rl.renderPriority, ours.renderPriority)
        assertEquals(rl.isFollower, ours.isFollower)
        assertTrue(shortArraysEqual(rl.recolorToFind, ours.recolorToFind))
        assertTrue(shortArraysEqual(rl.recolorToReplace, ours.recolorToReplace))
        assertTrue(intArraysEqual(rl.headIconArchiveIds, ours.headIconArchiveIds))
        assertTrue(shortArraysEqual(rl.headIconSpriteIndex, ours.headIconSpriteIndex))
        assertEquals(rl.params?.get(0x010203), ours.params?.get(0x010203))
        assertEquals(rl.params?.get(0x0A0B0C), ours.params?.get(0x0A0B0C))
    }

    private fun oldestPayload(): ByteArray =
        OutputBuffer(16).apply {
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

    private fun midIntModelPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(61)
            writeByte(1)
            writeInt(900_001)
            writeByte(62)
            writeByte(1)
            writeInt(900_002)
            writeByte(0)
        }.array()

    private fun pre210HeadIconPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(102)
            writeShort(5)
            writeByte(0)
        }.array()

    private fun rev210HeadIconPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(102)
            writeByte(1)
            writeBigSmart(12)
            writeUnsignedShortSmartMinusOne(3)
            writeByte(0)
        }.array()

    private fun newestPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(61)
            writeByte(1)
            writeInt(1_000_042)
            writeByte(2)
            writeString("Dragon")
            writeByte(30)
            writeString("Talk-to")
            writeByte(111)
            writeByte(114)
            writeShort(30)
            writeByte(115)
            writeShort(30)
            writeShort(31)
            writeShort(32)
            writeShort(33)
            writeByte(116)
            writeShort(40)
            writeByte(122)
            writeByte(124)
            writeShort(50)
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

    private fun terminator(): ByteArray =
        OutputBuffer(16).apply { writeByte(0) }.array()

    private fun OutputBuffer.writeUnsignedShortSmartMinusOne(value: Int) {
        val encoded = value + 1
        if (encoded in 0 until 128) writeByte(encoded) else writeShort(value + 0x8001)
    }

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
