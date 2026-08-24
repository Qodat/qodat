package stan.qodat.scene.runescape.entity

import qodat.cache.Cache
import qodat.cache.Encoder
import qodat.cache.definition.NPCDefinition
import stan.qodat.Properties
import stan.qodat.cache.impl.displee.DispleeCache
import stan.qodat.scene.runescape.animation.Animation

class NPC(
    cache: Cache = DispleeCache,
    definition: NPCDefinition,
    resolveAnimations: (Array<String>) -> Array<Animation>
) : AnimatedEntity<NPCDefinition>(cache, definition, resolveAnimations, "npc"), Encoder {

    override fun toString(): String = getName()

    override fun property() = Properties.selectedNpcName
}
