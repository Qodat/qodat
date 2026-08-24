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
    private val resolveAnimations: (Array<String>) -> Array<Animation>,
    labelPrefix: String? = null,
) : Entity<D>(cache, definition, labelPrefix), Transformable, GroupableTransformable {

    private lateinit var animations: Array<Animation>
    private var selectedAnimationProp: SimpleObjectProperty<Animation>? = null

    val selectedAnimation: SimpleObjectProperty<Animation>
        get() = selectedAnimationProp ?: SimpleObjectProperty<Animation>().also { selectedAnimationProp = it }

    fun getAnimations(): Array<Animation> {
        if (!this::animations.isInitialized) {
            val loaded = resolveAnimations(definition.animationIds)
            // Animations are loaded asynchronously; don't cache a miss if ids
            // exist but haven't been resolved yet.
            if (loaded.size >= definition.animationIds.size)
                animations = loaded
            return loaded
        }
        return animations
    }

    fun getPrimaryAnimations(): Array<Animation> {
        val ids = definition.primaryAnimationIds
        if (ids.isEmpty())
            return emptyArray()
        return resolveAnimations(ids)
    }

    fun extraAnimationCount(): Int =
        (definition.animationIds.size - definition.primaryAnimationIds.size).coerceAtLeast(0)

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
