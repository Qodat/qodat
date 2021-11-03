package stan.qodat.cache

import stan.qodat.cache.definition.*
import stan.qodat.scene.runescape.entity.NPC
import java.io.File
import java.io.UnsupportedEncodingException

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
abstract class Cache(val name: String) {

    open fun encode(any: Any) : File {
        throw UnsupportedEncodingException()
    }

    abstract fun getModelDefinition(id: String) : ModelDefinition

    abstract fun getAnimation(id: String) : AnimationDefinition

    abstract fun getNPCs() : Array<NPCDefinition>

    abstract fun getObjects() : Array<ObjectDefinition>

    abstract fun getItems() : Array<ItemDefinition>

    abstract fun getAnimationDefinitions() : Array<AnimationDefinition>

    abstract fun getAnimationSkeletonDefinition(frameHash: Int) : AnimationSkeletonDefinition

    abstract fun getFrameDefinition(frameHash: Int) : AnimationFrameDefinition?

    open fun add(any: Any) {
        TODO("not implemented")
    }
}