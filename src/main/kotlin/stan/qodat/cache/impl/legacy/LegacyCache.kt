package stan.qodat.cache.impl.legacy

import qodat.cache.definition.*
import stan.qodat.Properties
import qodat.cache.Cache
import stan.qodat.cache.impl.legacy.storage.*

object LegacyCache : Cache("Legacy") {

    init {
        val path = Properties.legacyCachePath.get()
        LegacyNpcStorage.load(path)
        LegacyObjectStorage.load(path)
        LegacyAnimationStorage.load(path)
        LegacyBodyKitStorage.load(path)
    }

    override fun getAnimation(id: String) = LegacyAnimationStorage[id.toInt()]

    override fun getNPCs(): Array<NPCDefinition> = Array(LegacyNpcStorage.npcCount) {
        LegacyNpcStorage[it]
    }

    override fun getObjects(): Array<ObjectDefinition> = Array(LegacyObjectStorage.objectCount) {
        LegacyObjectStorage[it]!!
    }
    override fun getItems(): Array<ItemDefinition> = emptyArray()

    override fun getAnimationDefinitions(): Array<AnimationDefinition> = Array(LegacyAnimationStorage.animations.size) {
        LegacyAnimationStorage[it]
    }

    override fun getAnimationSkeletonDefinition(frameHash: Int) = LegacyFrameStorage.getSkeleton(
        Properties.legacyCachePath.get(),
        frameHash
    )!!

    override fun getFrameDefinition(frameHash: Int) = LegacyFrameStorage.decode(
        Properties.legacyCachePath.get(),
        frameHash
    )!!

    override fun getInterface(groupId: Int): Array<InterfaceDefinition> {
        return emptyArray()
    }

    override fun getRootInterfaces(): Map<Int, List<InterfaceDefinition>> {
        return emptyMap()
    }

    override fun getSprite(groupId: Int, frameId: Int): SpriteDefinition {
        TODO("Not yet implemented")
    }

    override fun getTexture(id: Int): TextureDefinition {
        TODO("Not yet implemented")
    }

    override fun getModelDefinition(id: String) = LegacyModelStorage.getModel(
        Properties.legacyCachePath.get(),
        id
    )
}