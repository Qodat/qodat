package stan.export

import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.runescape.animation.Animation

fun main() {

    val animationDefinitions = OldschoolCacheRuneLite.getAnimationDefinitions()
    val animationDefinition = OldschoolCacheRuneLite.getAnimation("13")
    val animation = Animation(label = "bloeep", animationDefinition, OldschoolCacheRuneLite).apply {
        idProperty.set(13)
    }

    AnimationExporter().encode(animation)
}