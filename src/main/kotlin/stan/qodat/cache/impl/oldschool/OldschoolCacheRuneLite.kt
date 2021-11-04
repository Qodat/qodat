package stan.qodat.cache.impl.oldschool

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.runelite.cache.*
import net.runelite.cache.definitions.FramemapDefinition
import net.runelite.cache.definitions.NpcDefinition
import net.runelite.cache.definitions.loaders.FrameLoader
import net.runelite.cache.definitions.loaders.FramemapLoader
import net.runelite.cache.definitions.loaders.SequenceLoader
import net.runelite.cache.fs.Index
import net.runelite.cache.fs.Store
import stan.qodat.Properties
import stan.qodat.cache.Cache
import stan.qodat.cache.definition.*
import stan.qodat.cache.util.RSModelLoader

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   29/01/2021
 */
object OldschoolCacheRuneLite : Cache("LIVE") {

    internal var store = Store(Properties.osrsCachePath.get().toFile())

    var npcManager: NpcManager
    private var itemManager: ItemManager
    var objectManager: ObjectManager
    var textureManager: TextureManager

    private val frameIndex: Index
    private val framemapIndex: Index
    private val frames = HashMap<Int, Map<Int, AnimationFrameDefinition>>()
    private val frameMaps = HashMap<Int, Pair<FramemapDefinition, AnimationSkeletonDefinition>>()

    private lateinit var animations : Array<AnimationDefinition>


    init {
        store.load()
        frameIndex = store.getIndex(IndexType.FRAMES)
        framemapIndex = store.getIndex(IndexType.FRAMEMAPS)
        npcManager = NpcManager(store)
        npcManager.load()
        itemManager = ItemManager(store)
        itemManager.load()
        objectManager = ObjectManager(store)
        objectManager.load()
        textureManager = TextureManager(store)
    }

    private val gson = GsonBuilder().create()
    private val intArrayType = object: TypeToken<IntArray>() {}.type

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
        return animations.find { it.id == id }!!
    }

    override fun getNPCs(): Array<NPCDefinition> {
        val npcAnimsDir = Properties.osrsCachePath.get().resolve("npc_anims").toFile()
        if (!npcAnimsDir.exists()){
            println("Did not find npc_anims dir, creating...")
            return emptyArray()
        }

        val npcsByName = HashMap<String, NpcDefinition>()
        for (npc in npcManager.npcs) {
            if (npc.name.isEmpty() || npc.models == null || npc.models.isEmpty())
                continue
            npcsByName[npc.name] = npc
        }

        val animatedNpcs = ArrayList<NPCDefinition>()
        for (file in npcAnimsDir.listFiles()!!){
            val name = file.nameWithoutExtension
            val npc = npcsByName[name]?:continue
            val animsReader = file.bufferedReader()
            val anims = gson.fromJson<IntArray>(animsReader, intArrayType)
            animsReader.close()
            animatedNpcs.add(object : NPCDefinition {
                override val name: String
                    get() = npc.name
                override val modelIds: Array<String>
                    get() = npc.models.map { it.toString() }.toTypedArray()
                override val animationIds: Array<String>
                    get() = anims.map { it.toString() }.toTypedArray()
            })
        }
        return animatedNpcs.toTypedArray()
    }

    override fun getObjects(): Array<ObjectDefinition> {
        return objectManager.objects.map {
            object : ObjectDefinition {
                override val name: String
                    get() = it.name
                override val modelIds: Array<String>
                    get() = it.objectModels?.map { it.toString() }?.toTypedArray()?: emptyArray()
                override val animationIds: Array<String>
                    get() = if (it.animationID == -1)
                        emptyArray()
                    else
                        arrayOf(it.animationID.toString())
            }
        }.toTypedArray()
    }

    override fun getItems(): Array<ItemDefinition> {
        return itemManager.items.map {
            object : ItemDefinition {
                override val name: String
                    get() = it.name
                override val modelIds: Array<String>
                    get() = arrayOf(it.inventoryModel.toString())
            }
        }.toTypedArray()
    }

    override fun getAnimationDefinitions(): Array<AnimationDefinition> {
        if (!this::animations.isInitialized) {
            val storage = store.storage
            val index = store.getIndex(IndexType.CONFIGS)

            val seqArchive = index.getArchive(ConfigType.SEQUENCE.id)
            val seqArchiveData = storage.loadArchive(seqArchive)
            val seqArchiveFiles = seqArchive.getFiles(seqArchiveData)

            animations = seqArchiveFiles.files.map {
                val sequence = SequenceLoader().load(it.fileId, it.contents)!!
                return@map object : AnimationDefinition {
                    override val id: String
                        get() = it.fileId.toString()
                    override val frameHashes: IntArray
                        get() = sequence.frameIDs?: IntArray(0)
                    override val frameLengths: IntArray
                        get() = sequence.frameLenghts?: IntArray(0)
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
                    val frameMapDefinition = FramemapLoader().load(frameMapArchive.archiveId, frameMapContents)
                    frameMapDefinition to object : AnimationSkeletonDefinition {
                        override val id: Int
                            get() = frameMapArchiveId
                        override val transformationTypes: IntArray
                            get() = frameMapDefinition.types
                        override val targetVertexGroupsIndices: Array<IntArray>
                            get() = frameMapDefinition.frameMaps
                    }
                }
                val frame = FrameLoader().load(frameMapDefinition, file.fileId, frameContents)
                file.fileId to object : AnimationFrameDefinition {
                    override val transformationCount: Int
                        get() = frame.translatorCount
                    override val transformationGroupAccessIndices: IntArray
                        get() = frame.indexFrameIds
                    override val transformationDeltaX: IntArray
                        get() = frame.translator_x
                    override val transformationDeltaY: IntArray
                        get() = frame.translator_y
                    override val transformationDeltaZ: IntArray
                        get() = frame.translator_z
                    override val transformationGroup: AnimationSkeletonDefinition
                        get() = transformGroup
                }
            }.toMap()
        }[frameArchiveFileId]
    }

    override fun getAnimationSkeletonDefinition(frameHash: Int): AnimationSkeletonDefinition {
        return getFrameDefinition(frameHash)!!.transformationGroup
    }

    internal fun getFileId(hexString: String): Int {
        return Integer.parseInt(hexString.substring(0, hexString.length - 4), 16)
    }

    internal fun getFrameId(hexString: String): Int {
        return Integer.parseInt(hexString.substring(hexString.length - 4), 16)
    }

}