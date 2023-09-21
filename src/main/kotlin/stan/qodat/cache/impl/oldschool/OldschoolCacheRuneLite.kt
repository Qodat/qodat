package stan.qodat.cache.impl.oldschool

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import net.runelite.cache.*
import net.runelite.cache.definitions.loaders.SpotAnimLoader
import net.runelite.cache.fs.Index
import net.runelite.cache.fs.Store
import qodat.cache.Cache
import qodat.cache.definition.*
import qodat.cache.event.CacheReloadEvent
import qodat.cache.models.RSModelLoader
import stan.qodat.Properties
import stan.qodat.cache.impl.oldschool.definition.FrameDefinition
import stan.qodat.cache.impl.oldschool.definition.FrameMapDefinition
import stan.qodat.cache.impl.oldschool.definition.RuneliteIntefaceDefinition
import stan.qodat.cache.impl.oldschool.definition.RuneliteSpriteDefinition
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader206
import stan.qodat.util.onInvalidation
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   29/01/2021
 */
object OldschoolCacheRuneLite : Cache("LIVE") {

    internal var store = Store(Properties.osrsCachePath.get().toFile())

    lateinit var npcManager: NpcManager
    lateinit var itemManager: ItemManager
    lateinit var objectManager: ObjectManager
    lateinit var textureManager: TextureManager
    lateinit var interfaceManager: InterfaceManager
    lateinit var spriteManager: SpriteManager

    lateinit var frameIndex: Index
    lateinit var framemapIndex: Index
    lateinit var frames: HashMap<Int, Map<Int, AnimationFrameDefinition>>
    lateinit var frameMaps: HashMap<Int, Pair<FrameMapDefinition, AnimationTransformationGroup>>

    private lateinit var animations : Array<AnimationDefinition>
    private lateinit var spotAnimations : Array<SpotAnimationDefinition>

    private val gson = GsonBuilder().create()
    private val intArrayType = object: TypeToken<IntArray>() {}.type

    init {
        load()
        Properties.osrsCachePath.onInvalidation {
            store = Store(get().toFile())
            load()
            fire(CacheReloadEvent(this@OldschoolCacheRuneLite))
        }
    }

    private fun load() {
        store.load()
        frameIndex = store.getIndex(IndexType.ANIMATIONS)
        framemapIndex = store.getIndex(IndexType.SKELETONS)
        frames = HashMap<Int, Map<Int, AnimationFrameDefinition>>()
        frameMaps = HashMap<Int, Pair<FrameMapDefinition, AnimationTransformationGroup>>()
        npcManager = NpcManager(store)
        npcManager.load()
        itemManager = ItemManager(store)
        itemManager.load()
        objectManager = ObjectManager(store)
        objectManager.load()
        textureManager = TextureManager(store)
        interfaceManager = InterfaceManager(store)
        interfaceManager.load()
        spriteManager = SpriteManager(store)
        spriteManager.load()
        textureManager = TextureManager(store)
        textureManager.load()
    }

    override fun getStore(): Store {
        return store
    }

    override fun getTexture(id: Int): TextureDefinition {
        val def = textureManager.findTexture(id)
        def.method2680(1.0, 128) { spriteId, frameId ->
            spriteManager.findSprite(spriteId, frameId)
        }
        return object : TextureDefinition {
            override var id: Int = def.id
            override val fileIds: IntArray = def.fileIds!!
            override var pixels: IntArray = def.getPixels()
        }
    }
    fun getModelData(id: String) : ByteArray {
        val modelId = id.toIntOrNull()?:throw IllegalArgumentException("Model id must be int-convertable $id")
        val modelIndex = store.getIndex(IndexType.MODELS)
        val archive = modelIndex.getArchive(modelId)
        return archive.decompress(store.storage.loadArchive(archive))
    }

    override fun getModelDefinition(id: String): ModelDefinition {
        val modelId = id.toIntOrNull()?:throw IllegalArgumentException("Model id must be int-convertable $id")
        val modelIndex = store.getIndex(IndexType.MODELS)
        val archive = modelIndex.getArchive(modelId)
        return RSModelLoader().load(id, archive.decompress(store.storage.loadArchive(archive)))
    }

    override fun getAnimation(id: String): AnimationDefinition {
        return getAnimationDefinitions().find { it.id == id }!!
    }

    val animIdsCache = ConcurrentHashMap<Int, Array<String>>()

    override fun getNPCs(): Array<NPCDefinition> {
        val npcAnimsDir = Properties.osrsCachePath.get().resolve("npc_anims").toFile()
        if (!npcAnimsDir.exists()){
            println("Did not find npc_anims dir, creating...")
            return emptyArray()
        }

        val animatedNpcs = runBlocking {
            npcManager.npcs
                .filter { it.models != null && it.models.isNotEmpty() }
                .map { npc ->
                    GlobalScope.async(Dispatchers.IO) {
                        object : NPCDefinition {
                            override fun getOptionalId() = OptionalInt.of(npc.id)
                            override val name = npc.name.ifBlank { "null" }
                            override val modelIds = npc.models.map { it.toString() }.toTypedArray()
                            override val animationIds = try {
                                println("${npc.name} ${npc.standingAnimation}")
                                animIdsCache.getOrPut(npc.standingAnimation) {
                                    npcAnimsDir
                                        .resolve("${npc.id}.json")
                                        .bufferedReader()
                                        .use {gson.fromJson<IntArray>(it, intArrayType).map { it.toString() }.toTypedArray() }
                                }
                            } catch (ignored: Exception) {
                                System.err.println("Failed to load anim data for npc ${npc.name} ${npc.standingAnimation}")
                                emptyArray()
                            }
                            override val findColor = npc.recolorToFind
                            override val replaceColor = npc.recolorToReplace
                        }
                    }
                }.awaitAll()
        }

        return animatedNpcs.toTypedArray()
    }

    override fun getObjects(): Array<ObjectDefinition> {
        return objectManager.objects.map {
            object : ObjectDefinition {
                override fun getOptionalId() = OptionalInt.of(it.id)
                override val name = it.name
                override val modelIds = it.objectModels?.map { it.toString() }?.toTypedArray()?: emptyArray()
                override val animationIds = if (it.animationID == -1)
                    emptyArray()
                else
                    arrayOf(it.animationID.toString())
                override val findColor = it.recolorToFind
                override val replaceColor = it.recolorToReplace
            }
        }.toTypedArray()
    }

    override fun getItems(): Array<ItemDefinition> {
        return itemManager.items.map {
            object : ItemDefinition {
                override fun getOptionalId() = OptionalInt.of(it.id)
                override val name = it.name
                override val modelIds = arrayOf(it.inventoryModel.toString())
                override val findColor = it.colorFind
                override val replaceColor = it.colorReplace
            }
        }.toTypedArray()
    }

    override fun getSpotAnimations(): Array<SpotAnimationDefinition> {
        if (!this::spotAnimations.isInitialized) {
            val storage = store.storage
            val index = store.getIndex(IndexType.CONFIGS)
            val spotAnimArchive = index.getArchive(ConfigType.SPOTANIM.id)
            val spotAnimArchiveData = storage.loadArchive(spotAnimArchive)
            val spotAnimArchiveFiles = spotAnimArchive.getFiles(spotAnimArchiveData)
            spotAnimations = spotAnimArchiveFiles.files.map {
                val spotAnim = SpotAnimLoader().load(it.fileId, it.contents)!!
                return@map object : SpotAnimationDefinition {
                    override fun getOptionalId() = OptionalInt.of(spotAnim.id)
                    override val name: String = spotAnim.id.toString()
                    override val modelIds: Array<String> = arrayOf(spotAnim.getModelId().toString())
                    override val findColor: ShortArray? = spotAnim.recolorToFind
                    override val replaceColor: ShortArray? = spotAnim.recolorToReplace
                    override val animationIds: Array<String> = arrayOf(spotAnim.animationId.toString())
                }
            }.toTypedArray()
        }
        return spotAnimations
    }

    override fun getAnimationDefinitions(): Array<AnimationDefinition> {
        if (!this::animations.isInitialized) {
            val storage = store.storage
            val index = store.getIndex(IndexType.CONFIGS)

            val seqArchive = index.getArchive(ConfigType.SEQUENCE.id)
            val seqArchiveData = storage.loadArchive(seqArchive)
            val seqArchiveFiles = seqArchive.getFiles(seqArchiveData)

            animations = seqArchiveFiles.files.map {
                val sequence = SequenceLoader206().load(it.fileId, it.contents)
                return@map object : AnimationDefinition {
                    override val id: String = it.fileId.toString()
                    override val frameHashes: IntArray = sequence.frameIDs?: IntArray(0)
                    override val frameLengths: IntArray = sequence.frameLenghts?: IntArray(0)
                    override val loopOffset: Int = sequence.frameStep
                    override val leftHandItem: Int = sequence.leftHandItem
                    override val rightHandItem: Int = sequence.rightHandItem
                    override val skeletalAnimationId: Int = sequence.skeletalAnimationId
                }
            }.toTypedArray()
        }
        return animations
    }

    override fun getFrameDefinition(frameHash: Int): AnimationFrameDefinition? {

        val storage = store.storage
        val hexString = Integer.toHexString(frameHash)

        val frameArchiveId = getFileId(hexString)
        val frameArchiveFileId = getFrameId(hexString)

        return frames.getOrPut(frameArchiveId) {
            val frameArchive = frameIndex.getArchive(frameArchiveId)!!
            val frameArchiveContents = storage.loadArchive(frameArchive)
            val frameArchiveFiles = frameArchive.getFiles(frameArchiveContents)!!
            val files = frameArchiveFiles.files
            Array(files.size) {
                val file = files[it]
                val frameContents = file.contents
                val frameMapArchiveId = frameContents[0].toInt() and 0xff shl 8 or (frameContents[1].toInt() and 0xff)
                val (frameMapDefinition, transformGroup) = frameMaps.getOrPut(frameMapArchiveId) {
                    val frameMapArchive = framemapIndex.getArchive(frameMapArchiveId)
                    val frameMapContents = frameMapArchive.decompress(storage.loadArchive(frameMapArchive))
                    val frameMapDefinition = FrameMapDefinition(frameMapArchive.archiveId, frameMapContents)
                    frameMapDefinition to object : AnimationTransformationGroup {
                        override val id: Int = frameMapArchiveId
                        override val transformationTypes: IntArray = frameMapDefinition.types
                        override val targetVertexGroupsIndices: Array<IntArray> = frameMapDefinition.frameMaps
                    }
                }
                val frame = FrameDefinition(frameMapDefinition, file.fileId, frameContents)
                file.fileId to object : AnimationFrameDefinition {
                    override val transformationCount: Int = frame.translatorCount
                    override val transformationGroupAccessIndices: IntArray = frame.indexFrameIds
                    override val transformationDeltaX: IntArray = frame.translator_x
                    override val transformationDeltaY: IntArray = frame.translator_y
                    override val transformationDeltaZ: IntArray = frame.translator_z
                    override val transformationGroup: AnimationTransformationGroup = transformGroup
                    override val framemapArchiveIndex: Int = frame.framemapArchiveIndex
                }
            }.toMap()
        }[frameArchiveFileId]
    }

    override fun getInterface(groupId: Int): Array<InterfaceDefinition> =
        interfaceManager
            .getIntefaceGroup(groupId)
            .map { RuneliteIntefaceDefinition(it) }
            .toTypedArray()

    override fun getRootInterfaces(): Map<Int, List<InterfaceDefinition>> =
        interfaceManager.interfaces
            .flatten()
            .map { RuneliteIntefaceDefinition(it) }
            .groupBy { it.id.shr(16) }

    override fun getSprites(): Array<SpriteDefinition> =
        spriteManager.sprites.map { RuneliteSpriteDefinition(it) }.toTypedArray()

    override fun getSprite(groupId: Int, frameId: Int): SpriteDefinition =
        RuneliteSpriteDefinition(spriteManager.findSprite(groupId, frameId))

    override fun getAnimationSkeletonDefinition(frameHash: Int): AnimationTransformationGroup=
        getFrameDefinition(frameHash)!!.transformationGroup

    internal fun getFileId(hexString: String): Int =
        Integer.parseInt(hexString.substring(0, hexString.length - 4), 16)

    internal fun getFrameId(hexString: String) =
        Integer.parseInt(hexString.substring(hexString.length - 4), 16)

}
