package stan.qodat.cache.impl.displee

import com.displee.cache.CacheLibrary
import com.displee.cache.ProgressListener
import net.runelite.cache.definitions.loaders.TextureLoader
import qodat.cache.Cache
import qodat.cache.definition.*
import qodat.cache.event.CacheReloadEvent
import qodat.cache.models.RSModelLoader
import stan.qodat.Properties
import stan.qodat.cache.impl.displee.anims.NpcAnimParser
import stan.qodat.cache.impl.displee.anims.ObjectAnimParser
import stan.qodat.cache.impl.displee.types.AnimManager
import stan.qodat.cache.impl.displee.types.InterfaceManager
import stan.qodat.cache.impl.displee.types.ItemManager
import stan.qodat.cache.impl.displee.types.NpcManager
import stan.qodat.cache.impl.displee.types.ObjectManager
import stan.qodat.cache.impl.displee.types.SpotAnimManager
import stan.qodat.cache.impl.displee.types.SpriteManager
import stan.qodat.cache.impl.oldschool.definition.RuneliteIntefaceDefinition
import stan.qodat.cache.impl.oldschool.definition.RuneliteSpriteDefinition
import stan.qodat.util.onInvalidation
import kotlin.io.path.absolutePathString

object DispleeCache : Cache("Displee") {

    lateinit var store: CacheLibrary

    lateinit var animLoader: AnimManager
    lateinit var spriteManager: SpriteManager
    lateinit var interfaceManager: InterfaceManager
    lateinit var npcManager: NpcManager
    lateinit var objectManager: ObjectManager
    lateinit var itemManager: ItemManager
    lateinit var spotAnimManager: SpotAnimManager

    lateinit var npcAnimParser: NpcAnimParser
    lateinit var objectAnimParser: ObjectAnimParser


    init {
        load()
        Properties.osrsCachePath.onInvalidation {
            load()
            fire(CacheReloadEvent(this@DispleeCache))
        }
    }

    private fun load() {
        store = CacheLibrary(
            path = Properties.osrsCachePath.get().absolutePathString(),
            clearDataAfterUpdate = false,
            listener = object : ProgressListener {
                override fun notify(progress: Double, message: String?) {
                    println("DispleeCache: $progress $message")
                }
            }
        )
        animLoader = AnimManager(store).apply(AnimManager::load)
        spriteManager = SpriteManager(store).apply(SpriteManager::load)
        interfaceManager = InterfaceManager(store).apply(InterfaceManager::load)
        npcManager = NpcManager(store).apply(NpcManager::load)
        objectManager = ObjectManager(store).apply(ObjectManager::load)
        itemManager = ItemManager(store).apply(ItemManager::load)
        spotAnimManager = SpotAnimManager(store).apply(SpotAnimManager::load)

        npcAnimParser = NpcAnimParser(store, npcManager)
        objectAnimParser = ObjectAnimParser(store, objectManager)
    }


    override fun getModelDefinition(id: String): ModelDefinition {
        val modelId = id.toIntOrNull() ?: throw IllegalArgumentException("Model id must be int-convertable $id")
        val modelData = store.data(7, modelId) ?: throw IllegalArgumentException("Model not found $id")
        return RSModelLoader().load(id, modelData)
    }

    override fun getAnimation(id: String): AnimationDefinition =
        animLoader.getSeq(id)

    override fun getNPCs(): Array<NPCDefinition> =
        npcManager.getNpcs()

    override fun getObjects(): Array<ObjectDefinition> =
        objectManager.getObjects()

    override fun getItems(): Array<ItemDefinition> =
        itemManager.getItems()

    override fun getSpotAnimations(): Array<SpotAnimationDefinition> =
        spotAnimManager.getSpotAnimations()

    override fun getAnimationDefinitions(): Array<AnimationDefinition> =
        animLoader.getSeqs()

    override fun getAnimationSkeletonDefinition(frameHash: Int): AnimationTransformationGroup =
        getFrameDefinition(frameHash)!!.transformationGroup

    override fun getFrameDefinition(frameHash: Int): AnimationFrameLegacyDefinition? =
        animLoader.getFrameDef(frameHash)

    override fun getInterface(groupId: Int): Array<InterfaceDefinition> =
        interfaceManager
            .getIntefaceGroup(groupId)
            ?.mapNotNull { it?.let(::RuneliteIntefaceDefinition) }
            ?.toTypedArray()
            ?:emptyArray()

    override fun getRootInterfaces(): Map<Int, List<InterfaceDefinition>> =
        interfaceManager
            .getInterfaces()
            .filterNotNull()
            .flatMap { components -> components.mapNotNull { it?.let(::RuneliteIntefaceDefinition) } }
            .groupBy { it.id.shr(16) }

    override fun getSprites(): Array<SpriteDefinition> =
        spriteManager.getSprites().map { RuneliteSpriteDefinition(it) }.toTypedArray()

    override fun getSprite(groupId: Int, frameId: Int): SpriteDefinition =
        RuneliteSpriteDefinition(spriteManager.findSprite(groupId, frameId)?: throw IllegalArgumentException("Sprite not found $groupId:$frameId"))

    override fun getTexture(id: Int): TextureDefinition {
        val textureData = store.data(9, 0, id) ?: throw IllegalArgumentException("Texture not found $id")
        val texture = TextureLoader().load(id, textureData)
        texture.method2680(1.0, 128) { spriteId, frameId ->
            spriteManager.findSprite(spriteId, frameId)
        }
        return object : TextureDefinition {
            override var id: Int = id
            override val fileIds: IntArray = texture.fileIds!!
            override var pixels: IntArray = texture.getPixels()
        }
    }

    internal fun getFileId(hexString: String): Int =
        Integer.parseInt(hexString.substring(0, hexString.length - 4), 16)

    internal fun getFrameId(hexString: String) =
        Integer.parseInt(hexString.substring(hexString.length - 4), 16)
}