package stan.qodat.cache.impl.displee.anims

import com.displee.cache.CacheLibrary
import stan.qodat.Properties
import stan.qodat.cache.AnimationSkeletonIndex
import stan.qodat.cache.impl.displee.types.ObjectManager

class ObjectAnimParser(
    cacheLibrary: CacheLibrary,
    private val objectManager: ObjectManager
) : AnimParser(cacheLibrary) {

    override fun matchAnimationsToSkeletons(animationMap: AnimationSkeletonIndex.AnimationMap) {
        objectManager.load()
        val entities = objectManager.objects.mapNotNull { (id, definition) ->
            val animationId = definition.animationIds.firstOrNull()?.toIntOrNull()?.takeIf { it > 0 }
                ?: return@mapNotNull null
            id to intArrayOf(animationId)
        }
        AnimationSkeletonIndex.writeMatchesForEntities(
            outputDir = Properties.osrsCachePath.get().resolve("object_anims"),
            entities = entities,
            skeletonIdsByAnimationId = animationMap.skeletonIdsByAnimationId,
            animationMap = animationMap,
        ) { done, total ->
            report("Matched object ($done / $total)", done.toDouble(), total.toDouble())
        }
    }
}
