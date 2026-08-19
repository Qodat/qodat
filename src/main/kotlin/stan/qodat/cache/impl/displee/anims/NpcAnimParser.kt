package stan.qodat.cache.impl.displee.anims

import com.displee.cache.CacheLibrary
import stan.qodat.Properties
import stan.qodat.cache.AnimationSkeletonIndex
import stan.qodat.cache.NpcPrimaryAnimations
import stan.qodat.cache.impl.displee.types.NpcManager

class NpcAnimParser(
    cacheLibrary: CacheLibrary,
    private val npcManager: NpcManager
) : AnimParser(cacheLibrary) {

    override fun matchAnimationsToSkeletons(skeletonIdsByAnimationId: Map<Int, Set<Int>>) {
        npcManager.load()
        val entities = npcManager.npcs.values.mapNotNull { npc ->
            val ids = NpcPrimaryAnimations.intIds(npc)
            if (ids.isEmpty()) null else npc.id to ids
        }
        AnimationSkeletonIndex.writeMatchesForEntities(
            outputDir = Properties.osrsCachePath.get().resolve("npc_anims"),
            entities = entities,
            skeletonIdsByAnimationId = skeletonIdsByAnimationId,
        ) { done, total ->
            report("Matched npc ($done / $total)", done.toDouble(), total.toDouble())
        }
    }
}
