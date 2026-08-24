package stan.qodat.cache

import net.runelite.cache.definitions.NpcDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NpcPrimaryAnimationsTest {

    @Test
    fun keepsPositiveDistinctStanceIds() {
        val npc = NpcDefinition(1).apply {
            standingAnimation = 10
            walkingAnimation = 11
            idleRotateLeftAnimation = 10
            idleRotateRightAnimation = -1
            rotateLeftAnimation = 0
            rotateRightAnimation = 12
            rotate180Animation = 13
            runAnimation = 11
        }
        assertTrue(NpcPrimaryAnimations.intIds(npc).contentEquals(intArrayOf(10, 11, 12, 13)))
        assertTrue(NpcPrimaryAnimations.ids(npc).contentEquals(arrayOf("10", "11", "12", "13")))
    }

    @Test
    fun labelsCollapseSharedFamilyRoles() {
        val npc = NpcDefinition(1).apply {
            standingAnimation = 10
            walkingAnimation = 11
            idleRotateLeftAnimation = 10
            idleRotateRightAnimation = -1
            rotateLeftAnimation = 0
            rotateRightAnimation = 12
            rotate180Animation = 13
            runAnimation = 11
        }
        val labels = NpcPrimaryAnimations.labels(npc)
        assertEquals("Idle", labels["10"])
        assertEquals("Walk · Run", labels["11"])
        assertEquals("Rotate right", labels["12"])
        assertEquals("Rotate 180", labels["13"])
    }

    @Test
    fun labelsKeepRotateOnlyWhenFamilyHeadIsMissing() {
        val npc = NpcDefinition(2).apply {
            rotateLeftAnimation = 20
            rotateRightAnimation = 20
            runRotate180Animation = 21
        }
        val labels = NpcPrimaryAnimations.labels(npc)
        assertEquals("Rotate left · Rotate right", labels["20"])
        assertEquals("Run rotate 180", labels["21"])
    }

    @Test
    fun legacyLabelsFollowDecoderOrder() {
        val labels = NpcPrimaryAnimations.legacyLabels(arrayOf("8", "9", "12", "11", "10"))
        assertEquals("Walk", labels["8"])
        assertEquals("Idle", labels["9"])
        assertEquals("Rotate left", labels["12"])
        assertEquals("Rotate right", labels["11"])
        assertEquals("Rotate 180", labels["10"])
    }

    @Test
    fun legacyLabelsSkipUnsetIds() {
        val labels = NpcPrimaryAnimations.legacyLabels(arrayOf("-1", "9", "-1", "-1", "-1"))
        assertEquals(mapOf("9" to "Idle"), labels)
    }
}
