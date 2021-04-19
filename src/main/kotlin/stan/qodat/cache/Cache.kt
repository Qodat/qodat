package stan.qodat.cache

import stan.qodat.cache.definition.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
abstract class Cache(name: String) {

    abstract fun getModel(id: Int) : ModelDefinition

    abstract fun getNPCs() : Array<NPCDefinition>

    abstract fun getObjects() : Array<ObjectDefinition>

    abstract fun getItems() : Array<ItemDefinition>

    abstract fun getAnimationDefinitions() : Array<AnimationDefinition>

    abstract fun getAnimationSkeletonDefinition(frameHash: Int) : AnimationSkeletonDefinition

    abstract fun getFrameDefinition(frameHash: Int) : AnimationFrameDefinition?
}