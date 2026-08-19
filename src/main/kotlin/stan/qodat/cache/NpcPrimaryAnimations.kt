package stan.qodat.cache

import net.runelite.cache.definitions.NpcDefinition

/**
 * Stance / locomotion ids that belong in the first animation list.
 * Skeleton-matched extras from `npc_anims/{id}.json` stay lazy.
 */
object NpcPrimaryAnimations {

    fun ids(npc: NpcDefinition): Array<String> =
        listOf(
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
        ).filter { it > 0 }.map { it.toString() }.distinct().toTypedArray()
}
