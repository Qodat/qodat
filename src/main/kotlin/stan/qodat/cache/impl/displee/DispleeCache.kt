package stan.qodat.cache.impl.displee

import com.displee.cache.CacheLibrary
import com.displee.cache.ProgressListener
import net.runelite.cache.definitions.loaders.TextureLoader
import org.slf4j.LoggerFactory
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
import java.util.AbstractList
import kotlin.io.path.absolutePathString
import kotlin.system.measureTimeMillis

object DispleeCache : Cache("Displee") {

    private val logger = LoggerFactory.getLogger(DispleeCache::class.java)

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

    private val storeLock = Any()

    init {
        Properties.osrsCachePath.onInvalidation {
            reloadFromSource()
            fire(CacheReloadEvent(this@DispleeCache))
        }
    }

    override fun reloadFromSource() {
        openStore()
    }

    internal fun ensureReady() = ensureStore()

    private fun ensureStore() {
        if (!::store.isInitialized || store.closed) {
            openStore()
        }
    }

    private fun openStore() {
        synchronized(storeLock) {
            if (::store.isInitialized) {
                try {
                    if (!store.closed)
                        store.close()
                } catch (_: Exception) {
                }
            }
            val path = Properties.osrsCachePath.get().absolutePathString()
            val elapsed = measureTimeMillis {
                store = CacheLibrary(
                    path = path,
                    clearDataAfterUpdate = false,
                    listener = object : ProgressListener {
                        override fun notify(progress: Double, message: String?) {
                            logger.debug("DispleeCache: {} {}", progress, message)
                        }
                    }
                )
                animLoader = AnimManager(store)
                spriteManager = SpriteManager(store)
                interfaceManager = InterfaceManager(store)
                npcManager = NpcManager(store)
                objectManager = ObjectManager(store)
                itemManager = ItemManager(store)
                spotAnimManager = SpotAnimManager(store)

                npcAnimParser = NpcAnimParser(store, npcManager)
                objectAnimParser = ObjectAnimParser(store, objectManager)
            }
            logger.debug("Opened cache store in {}ms ({})", elapsed, path)
        }
    }

    override fun getModelDefinition(id: String): ModelDefinition {
        ensureStore()
        val modelId = id.toIntOrNull() ?: throw IllegalArgumentException("Model id must be int-convertable $id")
        val modelData = store.data(7, modelId) ?: throw IllegalArgumentException("Model not found $id")
        return RSModelLoader().load(id, modelData)
    }

    override fun getAnimation(id: String): AnimationDefinition {
        ensureStore()
        return animLoader.getSeq(id)
    }

    override fun getNPCs(): Array<NPCDefinition> {
        ensureStore()
        lateinit var result: Array<NPCDefinition>
        val elapsed = measureTimeMillis { result = npcManager.getNpcs() }
        logger.debug("NPC list ready: {} entries in {}ms", result.size, elapsed)
        return result
    }

    override fun getObjects(): Array<ObjectDefinition> {
        ensureStore()
        lateinit var result: Array<ObjectDefinition>
        val elapsed = measureTimeMillis { result = objectManager.getObjects() }
        logger.debug("Object list ready: {} entries in {}ms", result.size, elapsed)
        return result
    }

    override fun getItems(): Array<ItemDefinition> {
        ensureStore()
        lateinit var result: Array<ItemDefinition>
        val elapsed = measureTimeMillis { result = itemManager.getItems() }
        logger.debug("Item list ready: {} entries in {}ms", result.size, elapsed)
        return result
    }

    override fun getSpotAnimations(): Array<SpotAnimationDefinition> {
        ensureStore()
        lateinit var result: Array<SpotAnimationDefinition>
        val elapsed = measureTimeMillis { result = spotAnimManager.getSpotAnimations() }
        logger.debug("SpotAnimation list ready: {} entries in {}ms", result.size, elapsed)
        return result
    }

    override fun getAnimationDefinitions(): Array<AnimationDefinition> {
        ensureStore()
        lateinit var result: Array<AnimationDefinition>
        val elapsed = measureTimeMillis { result = animLoader.getSeqs() }
        logger.debug("Animation list ready: {} entries in {}ms", result.size, elapsed)
        return result
    }

    override fun getAnimationSkeletonDefinition(frameHash: Int): AnimationTransformationGroup =
        getFrameDefinition(frameHash)!!.transformationGroup

    override fun getFrameDefinition(frameHash: Int): AnimationFrameLegacyDefinition? {
        ensureStore()
        return animLoader.getFrameDef(frameHash)
    }

    override fun getInterface(groupId: Int): Array<InterfaceDefinition> {
        ensureStore()
        return interfaceManager
            .getIntefaceGroup(groupId)
            ?.mapNotNull { it?.let(::RuneliteIntefaceDefinition) }
            ?.toTypedArray()
            ?: emptyArray()
    }

    override fun getRootInterfaces(): Map<Int, List<InterfaceDefinition>> {
        ensureStore()
        lateinit var result: Map<Int, List<InterfaceDefinition>>
        val elapsed = measureTimeMillis {
            val raw = interfaceManager.getInterfaces()
            val groups = LinkedHashMap<Int, List<InterfaceDefinition>>()
            for (groupId in raw.indices) {
                val components = raw[groupId] ?: continue
                if (components.all { it == null }) continue
                groups[groupId] = LazyInterfaceList(components)
            }
            result = groups
        }
        logger.debug("Interface groups ready: {} groups in {}ms", result.size, elapsed)
        return result
    }

    override fun getSprites(): Array<SpriteDefinition> {
        ensureStore()
        lateinit var result: Array<SpriteDefinition>
        val elapsed = measureTimeMillis {
            result = spriteManager.getSprites().map { RuneliteSpriteDefinition(it) }.toTypedArray()
        }
        logger.debug("Sprite list ready: {} entries in {}ms", result.size, elapsed)
        return result
    }

    override fun getSprite(groupId: Int, frameId: Int): SpriteDefinition {
        ensureStore()
        return RuneliteSpriteDefinition(
            spriteManager.findSprite(groupId, frameId)
                ?: throw IllegalArgumentException("Sprite not found $groupId:$frameId")
        )
    }

    override fun getTexture(id: Int): TextureDefinition {
        ensureStore()
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

    private class LazyInterfaceList(
        private val components: Array<net.runelite.cache.definitions.InterfaceDefinition?>
    ) : AbstractList<InterfaceDefinition>() {
        private val mapped: List<InterfaceDefinition> by lazy {
            components.mapNotNull { it?.let(::RuneliteIntefaceDefinition) }
        }

        override val size: Int
            get() = mapped.size

        override fun get(index: Int): InterfaceDefinition = mapped[index]
    }
}
