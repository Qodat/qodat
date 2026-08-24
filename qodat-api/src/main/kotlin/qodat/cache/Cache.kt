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

    /**
     * Discard in-memory definitions and re-read them from the backing cache source.
     * Implementations must not fire [qodat.cache.event.CacheReloadEvent]; callers notify listeners after reload.
     */
    open fun reloadFromSource() {
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

    /**
     * Every frame in sprite archive [groupId]. Default walks [getSprite] from
     * frame 0 until a miss; caches that decode one archive should override.
     */
    open fun getSpriteArchive(groupId: Int): Array<SpriteDefinition> {
        val frames = ArrayList<SpriteDefinition>()
        var frame = 0
        while (frame <= 256) {
            val sprite = runCatching { getSprite(groupId, frame) }.getOrNull() ?: break
            frames.add(sprite)
            frame++
        }
        return frames.toTypedArray()
    }

    abstract fun getTexture(id: Int): TextureDefinition

    open fun add(any: Any) {
        TODO("not implemented")
    }

    /**
     * List-load kinds this cache actually has. [CacheAssetLoader] skips the
     * rest so an empty qodat cache does not walk NPC/Object/Item/… no-ops
     * or re-wrap another cache's animations.
     */
    open fun populatedListKinds(): Set<String> = ALL_LIST_KINDS

    companion object {
        const val LIST_NPC = "NPC"
        const val LIST_OBJECT = "Object"
        const val LIST_ITEM = "Item"
        const val LIST_SPOT_ANIM = "SpotAnim"
        const val LIST_SPRITES = "Sprites"
        const val LIST_INTERFACES = "Interfaces"
        const val LIST_ANIMATIONS = "Animations"
        val ALL_LIST_KINDS: Set<String> = linkedSetOf(
            LIST_NPC, LIST_OBJECT, LIST_ITEM, LIST_SPOT_ANIM,
            LIST_SPRITES, LIST_INTERFACES, LIST_ANIMATIONS,
        )
    }
}
