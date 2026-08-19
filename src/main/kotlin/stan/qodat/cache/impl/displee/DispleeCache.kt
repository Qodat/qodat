package stan.qodat.cache.impl.displee

import com.displee.cache.CacheLibrary
import com.displee.cache.ProgressListener
import javafx.application.Platform
import javafx.concurrent.Task
import net.runelite.cache.definitions.loaders.TextureLoader
import org.slf4j.LoggerFactory
import qodat.cache.Cache
import qodat.cache.definition.*
import qodat.cache.event.CacheReloadEvent
import qodat.cache.models.RSModelLoader
import stan.qodat.Properties
import stan.qodat.cache.ModelDefinitionCache
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
import java.util.concurrent.locks.ReentrantLock
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

    private val storeLock = ReentrantLock()

    init {
        Properties.osrsCachePath.onInvalidation {
            reloadFromSource()
            fire(CacheReloadEvent(this@DispleeCache))
        }
    }

    override fun reloadFromSource() {
        storeLock.lock()
        try {
            closeStore()
            openStore()
        } finally {
            storeLock.unlock()
        }
    }

    internal fun ensureReady() {
        storeLock.lock()
        try {
            if (!isOpen()) openStore()
        } finally {
            storeLock.unlock()
        }
    }

    fun createAnimationRescanTask(onFinished: () -> Unit = {}): Task<Unit> {
        storeLock.lock()
        try {
            if (!isOpen()) openStore()
            resetAnimScanDirectories()
            val npcParser = NpcAnimParser(store, npcManager)
            val objectParser = ObjectAnimParser(store, objectManager)
            npcAnimParser = npcParser
            objectAnimParser = objectParser
            return object : Task<Unit>() {
                init {
                    updateTitle("Rescanning animations")
                }

                override fun call() {
                    try {
                        val sink: (String, Double, Double) -> Unit = { message, work, total ->
                            updateMessage(message)
                            if (total > 0.0) updateProgress(work, total)
                        }
                        updateMessage("Scanning NPC animations...")
                        npcParser.executeScan(sink)
                        updateMessage("Scanning object animations...")
                        objectParser.executeScan(sink)
                        Platform.runLater {
                            fire(CacheReloadEvent(this@DispleeCache))
                            onFinished()
                        }
                    } catch (e: Exception) {
                        Platform.runLater(onFinished)
                        throw e
                    }
                }
            }
        } finally {
            storeLock.unlock()
        }
    }

    private fun resetAnimScanDirectories() {
        val root = Properties.osrsCachePath.get()
        for (name in listOf("npc_anims", "object_anims")) {
            val dir = root.resolve(name).toFile()
            if (dir.exists()) dir.deleteRecursively()
            dir.mkdirs()
        }
    }

    private fun isOpen(): Boolean = ::store.isInitialized && !store.closed

    private fun closeStore() {
        if (!::store.isInitialized) return
        try {
            if (!store.closed) store.close()
        } catch (_: Exception) {
        }
    }

    private fun openStore() {
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
        logger.info("Opened cache store in {}ms ({})", elapsed, path)
    }

    private inline fun <T> withOpenStore(block: () -> T): T {
        storeLock.lock()
        try {
            if (!isOpen()) openStore()
            return block()
        } finally {
            storeLock.unlock()
        }
    }

    override fun getModelDefinition(id: String): ModelDefinition =
        ModelDefinitionCache.getOrLoad(name, id) {
            withOpenStore {
                val modelId = id.toIntOrNull() ?: throw IllegalArgumentException("Model id must be int-convertable $id")
                val modelData = readModelData(modelId)
                    ?: throw IllegalArgumentException("Model not found $id")
                RSModelLoader().load(id, modelData)
            }
        }

    private fun readModelData(modelId: Int): ByteArray? {
        repeat(2) {
            store.data(7, modelId)?.let { return it }
            val archive = store.index(7).archive(modelId) ?: return null
            archive.files().firstOrNull { it.data != null }?.data?.let { return it }
        }
        return null
    }

    override fun getAnimation(id: String): AnimationDefinition = withOpenStore {
        animLoader.getSeq(id)
    }

    override fun getNPCs(): Array<NPCDefinition> = withOpenStore {
        timed("NPC", expectedMin = 1) { npcManager.getNpcs() }
    }

    override fun getObjects(): Array<ObjectDefinition> = withOpenStore {
        timed("Object", expectedMin = 1) { objectManager.getObjects() }
    }

    override fun getItems(): Array<ItemDefinition> = withOpenStore {
        timed("Item", expectedMin = 1) { itemManager.getItems() }
    }

    override fun getSpotAnimations(): Array<SpotAnimationDefinition> = withOpenStore {
        timed("SpotAnimation", expectedMin = 1) { spotAnimManager.getSpotAnimations() }
    }

    override fun getAnimationDefinitions(): Array<AnimationDefinition> = withOpenStore {
        timed("Animation", expectedMin = 1) { animLoader.getSeqs() }
    }

    override fun getAnimationSkeletonDefinition(frameHash: Int): AnimationTransformationGroup =
        getFrameDefinition(frameHash)!!.transformationGroup

    override fun getFrameDefinition(frameHash: Int): AnimationFrameLegacyDefinition? = withOpenStore {
        animLoader.getFrameDef(frameHash)
    }

    override fun getInterface(groupId: Int): Array<InterfaceDefinition> = withOpenStore {
        interfaceManager
            .getIntefaceGroup(groupId)
            ?.mapNotNull { it?.let(::RuneliteIntefaceDefinition) }
            ?.toTypedArray()
            ?: emptyArray()
    }

    override fun getRootInterfaces(): Map<Int, List<InterfaceDefinition>> = withOpenStore {
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
        if (result.isEmpty()) {
            throw IllegalStateException("Displee cache returned 0 interface groups")
        }
        logger.debug("Interface groups ready: {} groups in {}ms", result.size, elapsed)
        result
    }

    override fun getSprites(): Array<SpriteDefinition> = withOpenStore {
        timed("Sprite", expectedMin = 1) {
            spriteManager.getSprites().map { RuneliteSpriteDefinition(it) }.toTypedArray()
        }
    }

    override fun getSprite(groupId: Int, frameId: Int): SpriteDefinition = withOpenStore {
        RuneliteSpriteDefinition(
            spriteManager.findSprite(groupId, frameId)
                ?: throw IllegalArgumentException("Sprite not found $groupId:$frameId")
        )
    }

    override fun getTexture(id: Int): TextureDefinition = withOpenStore {
        val textureData = store.data(9, 0, id) ?: throw IllegalArgumentException("Texture not found $id")
        val texture = TextureLoader().load(id, textureData)
        texture.method2680(1.0, 128) { spriteId, frameId ->
            spriteManager.findSprite(spriteId, frameId)
        }
        object : TextureDefinition {
            override var id: Int = id
            override val fileIds: IntArray = texture.fileIds!!
            override var pixels: IntArray = texture.getPixels()
        }
    }

    private inline fun <T> timed(kind: String, expectedMin: Int, block: () -> Array<T>): Array<T> {
        lateinit var result: Array<T>
        val elapsed = measureTimeMillis { result = block() }
        if (result.size < expectedMin) {
            throw IllegalStateException("Displee cache returned ${result.size} $kind entries")
        }
        logger.debug("{} list ready: {} entries in {}ms", kind, result.size, elapsed)
        return result
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
