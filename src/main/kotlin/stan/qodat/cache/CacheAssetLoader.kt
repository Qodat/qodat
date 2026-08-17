package stan.qodat.cache

import javafx.application.Platform
import javafx.concurrent.Task
import org.slf4j.LoggerFactory
import qodat.cache.Cache
import qodat.cache.definition.AnimatedEntityDefinition
import qodat.cache.definition.AnimationMayaDefinition
import qodat.cache.definition.EntityDefinition
import stan.qodat.Properties
import stan.qodat.cache.impl.displee.DispleeCache
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationLegacy
import stan.qodat.scene.runescape.animation.AnimationMaya
import stan.qodat.scene.runescape.entity.*
import stan.qodat.scene.runescape.ui.InterfaceGroup
import stan.qodat.scene.runescape.ui.Sprite
import stan.qodat.task.BackgroundTasks
import java.util.Arrays
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class CacheAssetLoader(
    private val cache: Cache,
    private val animationLoader: (AnimatedEntityDefinition) -> Array<Animation>
) {

    fun loadAnimations(onCompleted: (List<Animation>) -> Unit) {
        BackgroundTasks.submit(addProgressIndicator = true, createLoadAnimationsTask(cache, onCompleted))
    }

    fun loadItems(onCompleted: (List<Item>) -> Unit) {
        BackgroundTasks.submit(addProgressIndicator = true, createItemsLoadTask(cache, onCompleted))
    }

    fun loadSpotAnims(onCompleted: (List<SpotAnimation>) -> Unit) {
        BackgroundTasks.submit(addProgressIndicator = true, createSpotAnimsLoadTask(cache, onCompleted))
    }

    fun loadSprites(onCompleted: (List<Sprite>) -> Unit) {
        BackgroundTasks.submit(addProgressIndicator = true, createSpritesLoadTask(cache, onCompleted))
    }

    fun loadInterfaces(onCompleted: (List<InterfaceGroup>) -> Unit) {
        BackgroundTasks.submit(addProgressIndicator = true, createInterfacesLoadTask(cache, onCompleted))
    }

    fun loadObjects(onCompleted: (List<Object>) -> Unit) {
        if (cache is DispleeCache) {
            val objectAnimsDir = Properties.osrsCachePath.get().resolve("object_anims").toFile()
            if (!objectAnimsDir.exists()) {
                println("Did not find object_anims dir, creating...")
                objectAnimsDir.mkdir()
                cache.ensureReady()
                BackgroundTasks.submit(addProgressIndicator = true, cache.objectAnimParser)
            }
        }
        BackgroundTasks.submit(addProgressIndicator = true, createObjectLoadTask(cache, onCompleted))
    }

    fun loadNpcs(onCompleted: (List<NPC>) -> Unit) {
        if (cache is DispleeCache) {
            val npcAnimsDir = Properties.osrsCachePath.get().resolve("npc_anims").toFile()
            if (!npcAnimsDir.exists()) {
                println("Did not find npc_anims dir, creating...")
                npcAnimsDir.mkdir()
                cache.ensureReady()
                BackgroundTasks.submit(addProgressIndicator = true, cache.npcAnimParser)
            }
        }
        BackgroundTasks.submit(addProgressIndicator = true, createNPCLoadTask(cache, onCompleted))
    }

    private fun createLoadAnimationsTask(cache: Cache, onCompleted: (List<Animation>) -> Unit) = object : Task<Void?>() {
        override fun call(): Void? {
            val animationDefinitions = cache.getAnimationDefinitions()
            val total = animationDefinitions.size
            val progressCounter = AtomicInteger()
            val updateFrequency = (total / 500).coerceAtLeast(1)
            val animations = arrayOfNulls<Animation>(total)
            val elapsed = measureTimeMillis {
                CacheParallel.forEachIndexed(total) { i ->
                    val definition = animationDefinitions[i]
                    try {
                        val animationId = definition.id.toIntOrNull() ?: i
                        animations[i] = when {
                            definition is AnimationMayaDefinition -> AnimationMaya(definition.id, definition, cache).apply {
                                this.idProperty.set(animationId)
                            }
                            definition.frameHashes.isNotEmpty() -> AnimationLegacy(definition.id, definition, cache).apply {
                                this.idProperty.set(animationId)
                            }
                            else -> null
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    CacheParallel.nextProgressStep(progressCounter, total, updateFrequency)?.let { count ->
                        updateProgress(count.toLong(), total.toLong())
                        updateMessage("Loading animation ($count / $total)")
                    }
                }
            }
            val values = animations.filterNotNull()
            logger.debug("Wrapped {} animations in {}ms", values.size, elapsed)
            Platform.runLater {
                onCompleted(values)
            }
            return null
        }
    }

    private fun createObjectLoadTask(cache: Cache, onCompleted: (List<Object>) -> Unit) = createLoadTask(
        definitions = { cache.getObjects() },
        mapper = { Object(cache, this, animationLoader) }
    ) { Platform.runLater { onCompleted(this) } }

    private fun createNPCLoadTask(cache: Cache, onCompleted: (List<NPC>) -> Unit) = createLoadTask(
        definitions = { cache.getNPCs() },
        mapper = { NPC(cache, this, animationLoader) }
    ) { Platform.runLater { onCompleted(this) } }

    private fun createSpotAnimsLoadTask(cache: Cache, onCompleted: (List<SpotAnimation>) -> Unit) = createLoadTask(
        definitions = { cache.getSpotAnimations() },
        mapper = { SpotAnimation(cache, this, animationLoader) }
    ) { Platform.runLater { onCompleted(this) } }

    private fun createItemsLoadTask(cache: Cache, onCompleted: (List<Item>) -> Unit) = createLoadTask(
        definitions = { cache.getItems() },
        mapper = { Item(cache, this) }
    ) { Platform.runLater { onCompleted(this) } }

    private fun createSpritesLoadTask(cache: Cache, onCompleted: (List<Sprite>) -> Unit) = object : Task<Unit>() {
        init {
            updateTitle("Loading sprites from cache ${cache.name}")
        }

        override fun call() {
            lateinit var values: List<Sprite>
            val elapsed = measureTimeMillis {
                val definitions = cache.getSprites()
                val total = definitions.size
                updateProgress(0, total.toLong())
                values = definitions.mapNotNull { definition ->
                    if (definition.width > 0 && definition.height > 0) Sprite(definition) else null
                }
                updateProgress(total.toLong(), total.toLong())
                updateMessage("Loading Sprite (${values.size} / $total)")
            }
            logger.debug("Wrapped {} sprites in {}ms", values.size, elapsed)
            println("Loaded ${values.size} Sprite")
            Platform.runLater { onCompleted(values) }
        }
    }

    private fun createInterfacesLoadTask(cache: Cache, onCompleted: (List<InterfaceGroup>) -> Unit) = object : Task<Unit>() {
        init {
            updateTitle("Loading interfaces from cache ${cache.name}")
        }

        override fun call() {
            lateinit var values: List<InterfaceGroup>
            val elapsed = measureTimeMillis {
                val groups = cache.getRootInterfaces()
                val total = groups.size
                updateProgress(0, total.toLong())
                values = groups.map { (id, definitions) -> InterfaceGroup(cache, id, definitions) }
                updateProgress(total.toLong(), total.toLong())
                updateMessage("Loading Interface ($total / $total)")
            }
            logger.debug("Wrapped {} interface groups in {}ms", values.size, elapsed)
            println("Loaded ${values.size} InterfaceGroup")
            Platform.runLater { onCompleted(values) }
        }
    }

    private inline fun <D : EntityDefinition, reified T : Entity<D>> createLoadTask(
        crossinline definitions: () -> Array<D>,
        crossinline mapper: D.() -> T,
        crossinline onLoaded: List<T>.() -> Unit
    ): Task<Unit> {
        val name = T::class.simpleName
        return object : Task<Unit>() {
            init {
                updateTitle("Loading $name from cache ${cache.name}")
            }

            override fun call() {
                val loadedDefinitions = definitions()
                val progressCounter = AtomicInteger()
                val total = loadedDefinitions.size
                val updateFrequency = (total / 500).coerceAtLeast(1)
                updateTitle("Loading $total $name from cache ${cache.name}")
                val showNulls = Properties.showNullNamedEntities.get()
                lateinit var loaded: List<T>
                val elapsed = measureTimeMillis {
                    loaded = Arrays.stream(loadedDefinitions).parallel().map { definition ->
                        CacheParallel.nextProgressStep(progressCounter, total, updateFrequency)?.let { count ->
                            updateProgress(count.toLong(), total.toLong())
                            updateMessage("Loading $name ($count / $total)")
                        }
                        val nullName = definition.name.isBlank() || definition.name == "null"
                        if (definition.modelIds.isNotEmpty() && (!nullName || showNulls))
                            mapper(definition)
                        else
                            null
                    }.toArray { arrayOfNulls<T>(it) }.filterNotNull()
                }
                logger.debug("Wrapped {} {} in {}ms", loaded.size, name, elapsed)
                println("Loaded ${loaded.size} $name")
                onLoaded(loaded)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CacheAssetLoader::class.java)
    }
}
