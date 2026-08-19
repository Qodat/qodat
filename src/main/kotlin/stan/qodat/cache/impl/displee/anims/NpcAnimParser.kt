package stan.qodat.cache.impl.displee.anims

import com.displee.cache.CacheLibrary
import net.runelite.cache.definitions.NpcDefinition
import stan.qodat.Properties
import stan.qodat.cache.AnimationSkeletonIndex
import stan.qodat.cache.NpcPrimaryAnimations
import stan.qodat.cache.impl.displee.types.NpcManager

class NpcAnimParser(
    cacheLibrary: CacheLibrary,
    private val npcManager: NpcManager
) : AnimParser(cacheLibrary) {

    override fun matchAnimationsToSkeletons(animationMap: AnimationSkeletonIndex.AnimationMap) {
        npcManager.load()
        val npcs = npcManager.npcs
        val entities = npcs.values.mapNotNull { npc ->
            val ids = NpcPrimaryAnimations.intIds(npc)
            if (ids.isEmpty()) null else npc.id to ids
        }
        val npcsByModel = HashMap<Int, MutableList<Int>>()
        for (npc in npcs.values) {
            for (modelId in npc.models ?: continue) {
                npcsByModel.getOrPut(modelId) { ArrayList() }.add(npc.id)
            }
        }
        AnimationSkeletonIndex.writeMatchesForEntities(
            outputDir = Properties.osrsCachePath.get().resolve("npc_anims"),
            entities = entities,
            skeletonIdsByAnimationId = animationMap.skeletonIdsByAnimationId,
            animationMap = animationMap,
            expandReferences = { npcId, ownRefs ->
                expandRelatedAnimationRefs(npcId, ownRefs, npcs, npcsByModel, animationMap)
            },
        ) { done, total ->
            report("Matched npc ($done / $total)", done.toDouble(), total.toDouble())
        }
    }

    private fun expandRelatedAnimationRefs(
        npcId: Int,
        ownRefs: IntArray,
        npcs: Map<Int, NpcDefinition>,
        npcsByModel: Map<Int, List<Int>>,
        animationMap: AnimationSkeletonIndex.AnimationMap,
    ): IntArray {
        val npc = npcs[npcId] ?: return ownRefs
        val ownAreMaya = ownRefs.any { it in animationMap.mayaAnimationIds }
        val expanded = ownRefs.toMutableSet()
        for (modelId in npc.models ?: return ownRefs) {
            for (otherId in npcsByModel[modelId].orEmpty()) {
                if (otherId == npcId) continue
                val other = npcs[otherId] ?: continue
                for (animationId in NpcPrimaryAnimations.intIds(other)) {
                    val relatedIsMaya = animationId in animationMap.mayaAnimationIds
                    if (relatedIsMaya == ownAreMaya) expanded.add(animationId)
                }
            }
        }
        return expanded.toIntArray()
    }
}
