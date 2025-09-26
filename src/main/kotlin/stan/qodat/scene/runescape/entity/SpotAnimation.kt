package stan.qodat.scene.runescape.entity

import qodat.cache.Cache
import qodat.cache.Encoder
import qodat.cache.definition.SpotAnimationDefinition
import stan.qodat.Properties
import stan.qodat.cache.impl.displee.DispleeCache
import stan.qodat.scene.runescape.animation.Animation

class SpotAnimation(
    cache: Cache = DispleeCache,
    definition: SpotAnimationDefinition,
    animationProvider: SpotAnimationDefinition.() -> Array<Animation>
) : AnimatedEntity<SpotAnimationDefinition>(cache, definition, animationProvider, labelPrefix = "spot_anim"), Encoder {

    override fun toString(): String = getName()

    override fun property() = Properties.selectedSpotAnimName
}
