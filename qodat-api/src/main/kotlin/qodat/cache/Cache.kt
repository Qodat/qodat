package qodat.cache

import qodat.cache.definition.*
import java.io.UnsupportedEncodingException

/**
 * This represents a cache plugin which can be used to define encoding/decoding operations.
 *
 * @param name the name of the cache
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
abstract class Cache(val name: String) {

    private val listeners = mutableListOf<CacheEventListener>()

    open fun encode(any: Any) : EncodeResult {
        throw UnsupportedEncodingException()
    }

    fun addListener(listener: CacheEventListener) {
        listeners += listener
    }

    fun removeListener(listener: CacheEventListener) {
        listeners -= listener
    }

    fun fire(event: CacheEvent){
        listeners.forEach {
            it.on(event)
        }
    }

    abstract fun getModelDefinition(id: String) : ModelDefinition

    abstract fun getAnimation(id: String) : AnimationDefinition

    abstract fun getNPCs() : Array<NPCDefinition>

    abstract fun getObjects() : Array<ObjectDefinition>

    abstract fun getItems() : Array<ItemDefinition>

    abstract fun getSpotAnimations() : Array<SpotAnimationDefinition>

    abstract fun getAnimationDefinitions() : Array<AnimationDefinition>

    abstract fun getAnimationSkeletonDefinition(frameHash: Int) : AnimationTransformationGroup

    abstract fun getFrameDefinition(frameHash: Int) : AnimationFrameLegacyDefinition?

    abstract fun getInterface(groupId: Int): Array<InterfaceDefinition>

    abstract fun getRootInterfaces(): Map<Int, List<InterfaceDefinition>>

    abstract fun getSprites(): Array<SpriteDefinition>

    abstract fun getSprite(groupId: Int, frameId: Int): SpriteDefinition

    abstract fun getTexture(id: Int): TextureDefinition

    open fun add(any: Any) {
        TODO("not implemented")
    }
}
