package stan.qodat.cache

import net.runelite.cache.definitions.NpcDefinition
import kotlin.test.Test
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
}
