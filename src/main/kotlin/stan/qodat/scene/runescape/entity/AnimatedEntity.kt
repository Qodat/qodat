package stan.qodat.scene.runescape.entity

import javafx.beans.property.SimpleObjectProperty
import qodat.cache.Cache
import qodat.cache.definition.AnimatedEntityDefinition
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.transform.GroupableTransformable
import stan.qodat.scene.transform.Transformable

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
abstract class AnimatedEntity<D : AnimatedEntityDefinition>(
    cache: Cache,
    definition: D,
    private val animationProvider: D.() -> Array<Animation>
) : Entity<D>(cache, definition), Transformable, GroupableTransformable {

    private lateinit var animations: Array<Animation>
//    private lateinit var skeletons: Map<Int, AnimationSkeleton>

    val selectedAnimation = SimpleObjectProperty<Animation>()

//    fun getSkeletons(): Map<Int, AnimationSkeleton> {
//        if (!this::skeletons.isInitialized) {
//            val map = HashMap<Int, AnimationSkeleton>()
//            for (animation in getAnimations()) {
//                for ((id, skeleton) in animation.getSkeletons()){
//                    if (!map.containsKey(id)){
//                        map[id] = skeleton
//                    }
//                }
//            }
//            skeletons = map
//        }
//        return skeletons
//    }

    fun getAnimations(): Array<Animation> {
//        if (!this::animations.isInitialized)
//            animations = animationProvider.invoke(definition)
        return animationProvider.invoke(definition)
    }

    override fun animate(index: Int) {

        val animation = selectedAnimation.get()?:return
        val frame = animation.getFrameList().getOrNull(index) ?:return

        animate(frame)
    }

    override fun animate(frame: AnimationFrame) {
        for (model in getModels())
            model.animate(frame)
    }
}