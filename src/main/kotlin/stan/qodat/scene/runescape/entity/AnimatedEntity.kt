package stan.qodat.scene.runescape.entity

import stan.qodat.Qodat
import stan.qodat.cache.definition.AnimatedEntityDefinition
import stan.qodat.cache.Cache
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.AnimationSkeleton
import stan.qodat.scene.transform.Transformable
import stan.qodat.util.getAnimation

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
open class AnimatedEntity<D : AnimatedEntityDefinition>(
    cache: Cache,
    private val definition: D
) : Entity<D>(cache, definition), Transformable {

    private lateinit var animations: Array<Animation>
    private lateinit var skeletons: Map<Int, AnimationSkeleton>

    fun getSkeletons(): Map<Int, AnimationSkeleton> {
        if (!this::skeletons.isInitialized) {
            val map = HashMap<Int, AnimationSkeleton>()
            for (animation in getAnimations()) {
                for ((id, skeleton) in animation.getSkeletons()){
                    if (!map.containsKey(id)){
                        map[id] = skeleton
                    }
                }
            }
            skeletons = map
        }
        return skeletons
    }

    fun getAnimations(): Array<Animation> {
        if (!this::animations.isInitialized) {
            animations = definition.animationIds
                .map { Qodat.getAnimation(it) }
                .filterNotNull()
                .toTypedArray()
        }
        return animations
    }

    override fun animate(frame: AnimationFrame) {
        for (model in getModels())
            model.animate(frame)
    }
}