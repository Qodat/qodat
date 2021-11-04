package stan.qodat.cache.impl.qodat

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import stan.qodat.Properties
import stan.qodat.cache.Cache
import stan.qodat.cache.definition.*
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.cache.impl.qodat.QodatCache.loadModels
import stan.qodat.cache.util.RSModelLoader
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
    private val animationFrames: MutableMap<Int, QodatAnimationFrameDefinition>
    private val models: MutableMap<String, QodatModelDefinition>

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
            .loadDefinitions<QodatAnimationFrameDefinition>("animation_frames")
            .associateBy { it.frameHash }
            .toMutableMap()
        models = qodatCacheDir.loadModels("models")
    }

    override fun encode(any: Any): File {
        if (any is Model) {
            val file = File.createTempFile(any.getName(), ".json")
            encodeModel(file, getQodatModelDefinition(any))
            return file
        }
        return super.encode(any)
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
                    definition, Properties.qodatCachePath.get()
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

    private inline fun<reified T> File.loadDefinitions(directoryName: String) = resolve(directoryName)
        .listFiles()
        ?.filterNotNull()
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