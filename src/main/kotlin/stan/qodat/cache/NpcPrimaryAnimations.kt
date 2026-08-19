package stan.qodat.cache

import net.runelite.cache.definitions.NpcDefinition

/**
 * Stance / locomotion ids that belong in the first animation list.
 * Skeleton-matched extras from `npc_anims/{id}.json` stay lazy.
 */
object NpcPrimaryAnimations {

    fun intIds(npc: NpcDefinition): IntArray =
        intArrayOf(
            npc.standingAnimation,
            npc.walkingAnimation,
            npc.idleRotateLeftAnimation,
            npc.idleRotateRightAnimation,
            npc.rotateLeftAnimation,
            npc.rotateRightAnimation,
            npc.rotate180Animation,
            npc.runAnimation,
            npc.runRotate180Animation,
            npc.runRotateLeftAnimation,
            npc.runRotateRightAnimation,
            npc.crawlAnimation,
            npc.crawlRotate180Animation,
            npc.crawlRotateLeftAnimation,
            npc.crawlRotateRightAnimation,
        ).filter { it > 0 }.distinct().toIntArray()

    fun ids(npc: NpcDefinition): Array<String> =
        intIds(npc).map { it.toString() }.toTypedArray()
}
