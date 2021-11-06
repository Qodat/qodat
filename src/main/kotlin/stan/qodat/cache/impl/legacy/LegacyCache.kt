package stan.qodat.cache.impl.legacy

import stan.qodat.Properties
import stan.qodat.cache.Cache
import stan.qodat.cache.definition.*
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

    override fun getModelDefinition(id: String) = LegacyModelStorage.getModel(
        Properties.legacyCachePath.get(),
        id
    )
}