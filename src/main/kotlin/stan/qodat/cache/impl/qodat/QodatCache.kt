package stan.qodat.cache.impl.qodat

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import qodat.cache.definition.*
import stan.qodat.Properties
import qodat.cache.Cache
import qodat.cache.EncodeResult
import qodat.cache.models.RSModelLoader
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.entity.NPC
import stan.qodat.scene.runescape.model.Model
import java.io.File
import java.io.UnsupportedEncodingException
import kotlin.io.path.outputStream

@ExperimentalSerializationApi
object QodatCache : Cache("qodat") {

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val npcs: MutableList<QodatNpcDefinition>
    private val items: MutableList<QodatItemDefinition>
    private val objects: MutableList<QodatObjectDefinition>
    private val animations: MutableMap<String, QodatAnimationDefinition>
    private val animationSkeletons: MutableList<QodatAnimationDefinition>
    private val animationFrames: MutableMap<Int, List<QodatAnimationFrameDefinition>>
    private val models: MutableMap<String, QodatModelDefinition>

    @JvmStatic
    fun main(args: Array<String>) {

//        val frameArchiveId = 2
//        for (frameIndex in 0..15){
//            val hash = ((frameArchiveId and  0xFFFF) shl 16) or (frameIndex and 0xFFFF)
//            val hexString = Integer.toHexString(hash)
//            assert(getFileId(hexString) == frameArchiveId)
//            assert(getFrameId(hexString) == frameIndex)
//            println("${hash},")
//        }
//        Paths.get("/Users/stanvanderbend/IdeaProjects/kotlin-qodat/exports/animation_frames/json/2")
//            .toFile()
//            .loadDefinitions<QodatAnimationFrameDefinition>("animation_frames/2")
//            .forEach {
//                it.frameHash
//            }
    }
    internal fun getFileId(hexString: String): Int {
        return Integer.parseInt(hexString.substring(0, hexString.length - 4), 16)
    }

    internal fun getFrameId(hexString: String): Int {
        return Integer.parseInt(hexString.substring(hexString.length - 4), 16)
    }

    init {
        val qodatCachePath = Properties.qodatCachePath.get()
        val qodatCacheDir = qodatCachePath.toFile()
        if (!qodatCacheDir.exists())
            qodatCacheDir.mkdir()

        npcs = qodatCacheDir.loadDefinitions("npcs")
        items = qodatCacheDir.loadDefinitions("items")
        objects = qodatCacheDir.loadDefinitions("objects")
        animations = qodatCacheDir
            .loadDefinitions<QodatAnimationDefinition>("animations")
            .associateBy { it.id }
            .toMutableMap()
        animationSkeletons = qodatCacheDir.loadDefinitions("animation_skeletons")
        animationFrames = qodatCacheDir
            .resolve("animation_frames")
            .listFiles()
            ?.associate {
                it.nameWithoutExtension.toInt() to it.loadDefinitions<QodatAnimationFrameDefinition>("animation_frames")
            }
            ?.toMutableMap()
            ?: mutableMapOf()

        models = qodatCacheDir.loadModels("models")
    }

    override fun encode(any: Any): EncodeResult {
        if (any is Model) {
            val saveDir = Properties.defaultExportsPath.get().resolve("model").resolve("json").toFile().apply {
                if (!parentFile.exists())
                    parentFile.mkdir()
                if (!exists())
                    mkdir()
            }
            val file = saveDir.resolve("${any.getName()}.json").apply {
                if (!exists())
                    createNewFile()
            }
            encodeModel(file, getQodatModelDefinition(any))
            return EncodeResult(file)
        } else if (any is Animation) {
            val animationSaveDir = Properties.defaultExportsPath.get().resolve("animation").resolve("json").toFile().apply {
                if (!parentFile.exists())
                    parentFile.mkdirs()
                if (!exists())
                    mkdir()
            }
            val animationFile = animationSaveDir.resolve("${any.getName()}.json").apply {
                if (!exists())
                    createNewFile()
            }
            val animationFrameSaveDir = Properties.defaultExportsPath.get().resolve("animation_frames").resolve("json").toFile().apply {
                if (!parentFile.exists())
                    parentFile.mkdirs()
                if (!exists())
                    mkdir()
            }
            val animationFrameSkeletonSaveDir = Properties.defaultExportsPath.get().resolve("animation_skeletons").resolve("json").toFile().apply {
                if (!parentFile.exists())
                    parentFile.mkdirs()
                if (!exists())
                    mkdir()
            }
            val frameList = any.getFrameList()
            val frameArchiveId = any.exportFrameArchiveId.get()
//            animationFrameSaveDir.listFiles()
//                ?.mapNotNull { it.nameWithoutExtension.toIntOrNull() }
//                ?.maxOrNull()?:1
            val animationDefinition = QodatAnimationDefinition(
                id = any.getName(),
                frameHashes = IntArray(frameList.size) { index ->
                    ((frameArchiveId and  0xFFFF) shl 16) or (index and 0xFFFF)
                },
                frameLengths = IntArray(frameList.size) { index ->
                    val frame = frameList[index]
                    frame.getLength().toInt()
                },
                loopOffset = any.loopOffsetProperty.get(),
                leftHandItem = any.leftHandItemProperty.get(),
                rightHandItem = any.rightHandItemProperty.get(),
            )

            val animationSkeletons = frameList.map {
                val group = it.definition!!.transformationGroup
                QodatAnimationSkeletonDefinition(
                    group.id,
                    group.transformationTypes,
                    group.targetVertexGroupsIndices
                )
            }
            val animationFrameDefinitions = frameList.mapIndexed { index, it ->
                QodatAnimationFrameDefinition(
                    ((frameArchiveId and  0xFFFF) shl 16) or (index and 0xFFFF),
                    it.getTransformationCount(),
                    it.transformationList.map { it.groupIndexProperty.get() }.toIntArray(),
                    it.transformationList.map { it.getDeltaX() }.toIntArray(),
                    it.transformationList.map { it.getDeltaY() }.toIntArray(),
                    it.transformationList.map { it.getDeltaZ() }.toIntArray(),
                    animationSkeletons[index]
                )
            }
            for ((index, animationFrameDefinition) in animationFrameDefinitions.withIndex()) {
                val animationFrameFile = animationFrameSaveDir.resolve(frameArchiveId.toString()).resolve("${index}.json").apply {
                    if (!parentFile.exists())
                        parentFile.mkdir()
                }
                json.encodeToStream(animationFrameDefinition, animationFrameFile.outputStream())
            }
            for (animationSkeletonDefinition in animationSkeletons) {
                val animationSkeletonFile = animationFrameSkeletonSaveDir.resolve("${animationSkeletonDefinition.id}.json")
                json.encodeToStream(animationSkeletonDefinition, animationSkeletonFile.outputStream())
            }
            json.encodeToStream(animationDefinition, animationFile.outputStream())
            return EncodeResult(animationFile)
        }
        return super.encode(any)
    }

    override fun getTexture(id: Int): TextureDefinition {
        TODO("Not yet implemented")
    }

    private fun encodeModel(file: File, modelDefinition: QodatModelDefinition) {
        json.encodeToStream(modelDefinition, file.outputStream())
    }

    private fun getQodatModelDefinition(any: Model) : QodatModelDefinition {
        return any.modelDefinition.let {
            if (it is QodatModelDefinition)
                it
            else
                QodatModelDefinition.create(any.modelDefinition)
        }
    }

    override fun add(any: Any) {
        when (any) {
            is Model -> {
                val modelName = any.getName()
                val modelDefinition = getQodatModelDefinition(any)
                models[modelName] = modelDefinition
                val modelFile = Properties.qodatCachePath.get().resolve("models/$modelName.json").toFile()
                if (!modelFile.parentFile.exists())
                    modelFile.parentFile.mkdir()
                encodeModel(modelFile, modelDefinition)
            }
            is NPC -> {
                val definition = any.definition
                if (definition !is QodatNpcDefinition)
                    throw IllegalArgumentException("Can only serialize QodatNpcDefinitions not $definition")

                npcs.add(definition)
                json.encodeToStream(
                    definition,
                    Properties.qodatCachePath.get()
                        .resolve("npcs/" + definition.name + ".json")
                        .outputStream()
                )
            }
        }
    }

    override fun getModelDefinition(id: String): ModelDefinition {
        return models[id]!!
    }

    override fun getAnimation(id: String) = OldschoolCacheRuneLite.getAnimation(id)

    override fun getNPCs(): Array<NPCDefinition> = npcs.toTypedArray()

    override fun getObjects(): Array<ObjectDefinition> = objects.toTypedArray()

    override fun getItems(): Array<ItemDefinition> = items.toTypedArray()

    override fun getAnimationDefinitions(): Array<AnimationDefinition> {
        return OldschoolCacheRuneLite.getAnimationDefinitions()
    }

    override fun getAnimationSkeletonDefinition(frameHash: Int): AnimationSkeletonDefinition {
        return OldschoolCacheRuneLite.getAnimationSkeletonDefinition(frameHash)
    }

    override fun getFrameDefinition(frameHash: Int): AnimationFrameDefinition? {
        return OldschoolCacheRuneLite.getFrameDefinition(frameHash)
    }

    override fun getInterface(groupId: Int): Array<InterfaceDefinition> {
        return emptyArray()
    }

    override fun getRootInterfaces(): Map<Int, List<InterfaceDefinition>> {
        return emptyMap()
    }

    override fun getSprite(groupId: Int, frameId: Int): SpriteDefinition {
        TODO("Not yet implemented")
    }

    private inline fun<reified T> File.loadDefinitions(directoryName: String) = resolve(directoryName)
        .listFiles()
        ?.filterNotNull()
        ?.filter { it.extension == "json" }
        ?.map { json.decodeFromStream<T>(it.inputStream()) }
        ?.toMutableList()
        ?: mutableListOf()

    private fun File.loadModels(directoryName: String) = resolve(directoryName)
        .listFiles()
        ?.filterNotNull()
        ?.map {
            when (it.extension) {
                "json" -> json.decodeFromStream(it.inputStream())
                "model", "dat" -> QodatModelDefinition.create(RSModelLoader().load(it.name, it.readBytes()))
                else -> throw UnsupportedEncodingException("Can only read .json, .model, .dat files.")
            }
        }
        ?.associateBy { it.getName() }
        ?.toMutableMap()
        ?: mutableMapOf()
}