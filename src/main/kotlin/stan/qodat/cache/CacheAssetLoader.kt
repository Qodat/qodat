package stan.qodat.cache

import javafx.application.Platform
import javafx.concurrent.Task
import org.slf4j.LoggerFactory
import qodat.cache.Cache
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
    private val resolveAnimations: (Array<String>) -> Array<Animation>
) {

    fun loadAll(
        selectedFirst: String?,
        onNpcs: (List<NPC>) -> Unit,
        onObjects: (List<Object>) -> Unit,
        onItems: (List<Item>) -> Unit,
        onSpotAnims: (List<SpotAnimation>) -> Unit,
        onSprites: (List<Sprite>) -> Unit,
        onInterfaces: (List<InterfaceGroup>) -> Unit,
        onAnimations: (List<Animation>) -> Unit,
    ) {
        maybeSubmitMissingAnimParsers()
        BackgroundTasks.submit(addProgressIndicator = true, object : Task<Unit>() {
            init {
                updateTitle("Loading ${cache.name} cache lists")
            }

            override fun call() {
                val jobs = linkedMapOf(
                    "NPC" to { applyOnFx(loadNpcsNow(), onNpcs) },
                    "Object" to { applyOnFx(loadObjectsNow(), onObjects) },
                    "Item" to { applyOnFx(loadItemsNow(), onItems) },
                    "SpotAnim" to { applyOnFx(loadSpotAnimsNow(), onSpotAnims) },
                    "Sprites" to { applyOnFx(loadSpritesNow(), onSprites) },
                    "Interfaces" to { applyOnFx(loadInterfacesNow(), onInterfaces) },
                    "Animations" to { applyOnFx(loadAnimationsNow(), onAnimations) },
                )
                val order = buildList {
                    if (selectedFirst != null && selectedFirst in jobs) add(selectedFirst)
                    jobs.keys.filter { it != selectedFirst }.forEach { add(it) }
                }
                for (name in order) {
                    updateTitle("Loading $name from cache ${cache.name}")
                    updateMessage("Loading $name")
                    jobs.getValue(name).invoke()
                }
            }
        })
    }

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
        maybeSubmitObjectAnimParser()
        BackgroundTasks.submit(addProgressIndicator = true, createObjectLoadTask(cache, onCompleted))
    }

    fun loadNpcs(onCompleted: (List<NPC>) -> Unit) {
        maybeSubmitNpcAnimParser()
        BackgroundTasks.submit(addProgressIndicator = true, createNPCLoadTask(cache, onCompleted))
    }

    private fun maybeSubmitMissingAnimParsers() {
        maybeSubmitNpcAnimParser()
        maybeSubmitObjectAnimParser()
    }

    private fun maybeSubmitNpcAnimParser() {
        if (cache !is DispleeCache) return
        val npcAnimsDir = Properties.osrsCachePath.get().resolve("npc_anims").toFile()
        if (!npcAnimsDir.exists() || npcAnimsDir.listFiles().isNullOrEmpty()) {
            println("Did not find npc_anims dir, creating...")
            npcAnimsDir.mkdirs()
            cache.ensureReady()
            BackgroundTasks.submit(addProgressIndicator = true, cache.npcAnimParser)
        }
    }

    private fun maybeSubmitObjectAnimParser() {
        if (cache !is DispleeCache) return
        val objectAnimsDir = Properties.osrsCachePath.get().resolve("object_anims").toFile()
        if (!objectAnimsDir.exists() || objectAnimsDir.listFiles().isNullOrEmpty()) {
            println("Did not find object_anims dir, creating...")
            objectAnimsDir.mkdirs()
            cache.ensureReady()
            BackgroundTasks.submit(addProgressIndicator = true, cache.objectAnimParser)
        }
    }

    private fun <T> applyOnFx(value: T, onCompleted: (T) -> Unit) {
        Platform.runLater { onCompleted(value) }
    }

    private fun loadNpcsNow(): List<NPC> = loadEntities(
        definitions = { cache.getNPCs() },
        mapper = { NPC(cache, this, resolveAnimations) }
    )

    private fun loadObjectsNow(): List<Object> = loadEntities(
        definitions = { cache.getObjects() },
        mapper = { Object(cache, this, resolveAnimations) }
    )

    private fun loadItemsNow(): List<Item> = loadEntities(
        definitions = { cache.getItems() },
        mapper = { Item(cache, this) }
    )

    private fun loadSpotAnimsNow(): List<SpotAnimation> = loadEntities(
        definitions = { cache.getSpotAnimations() },
        mapper = { SpotAnimation(cache, this, resolveAnimations) }
    )

    private fun loadAnimationsNow(): List<Animation> {
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
                CacheParallel.nextProgressStep(progressCounter, total, updateFrequency)
            }
        }
        val values = animations.filterNotNull()
        logger.debug("Wrapped {} animations in {}ms", values.size, elapsed)
        return values
    }

    private fun loadSpritesNow(): List<Sprite> {
        lateinit var values: List<Sprite>
        val elapsed = measureTimeMillis {
            val definitions = cache.getSprites()
            values = definitions.mapNotNull { definition ->
                if (definition.width > 0 && definition.height > 0) Sprite(definition) else null
            }
        }
        logger.debug("Wrapped {} sprites in {}ms", values.size, elapsed)
        println("Loaded ${values.size} Sprite")
        return values
    }

    private fun loadInterfacesNow(): List<InterfaceGroup> {
        lateinit var values: List<InterfaceGroup>
        val elapsed = measureTimeMillis {
            val groups = cache.getRootInterfaces()
            values = groups.map { (id, definitions) -> InterfaceGroup(cache, id, definitions) }
        }
        logger.debug("Wrapped {} interface groups in {}ms", values.size, elapsed)
        println("Loaded ${values.size} InterfaceGroup")
        return values
    }

    private fun createLoadAnimationsTask(@Suppress("UNUSED_PARAMETER") cache: Cache, onCompleted: (List<Animation>) -> Unit) = object : Task<Void?>() {
        override fun call(): Void? {
            applyOnFx(loadAnimationsNow(), onCompleted)
            return null
        }
    }

    private fun createObjectLoadTask(cache: Cache, onCompleted: (List<Object>) -> Unit) = createLoadTask(
        definitions = { cache.getObjects() },
        mapper = { Object(cache, this, resolveAnimations) }
    ) { Platform.runLater { onCompleted(this) } }

    private fun createNPCLoadTask(cache: Cache, onCompleted: (List<NPC>) -> Unit) = createLoadTask(
        definitions = { cache.getNPCs() },
        mapper = { NPC(cache, this, resolveAnimations) }
    ) { Platform.runLater { onCompleted(this) } }

    private fun createSpotAnimsLoadTask(cache: Cache, onCompleted: (List<SpotAnimation>) -> Unit) = createLoadTask(
        definitions = { cache.getSpotAnimations() },
        mapper = { SpotAnimation(cache, this, resolveAnimations) }
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
            applyOnFx(loadSpritesNow(), onCompleted)
        }
    }

    private fun createInterfacesLoadTask(cache: Cache, onCompleted: (List<InterfaceGroup>) -> Unit) = object : Task<Unit>() {
        init {
            updateTitle("Loading interfaces from cache ${cache.name}")
        }

        override fun call() {
            applyOnFx(loadInterfacesNow(), onCompleted)
        }
    }

    private inline fun <D : EntityDefinition, reified T : Entity<D>> loadEntities(
        crossinline definitions: () -> Array<D>,
        crossinline mapper: D.() -> T,
    ): List<T> {
        val name = T::class.simpleName
        val loadedDefinitions = definitions()
        if (loadedDefinitions.isEmpty() && cache is DispleeCache) {
            throw IllegalStateException("Displee cache returned 0 $name definitions")
        }
        val progressCounter = AtomicInteger()
        val total = loadedDefinitions.size
        val updateFrequency = (total / 500).coerceAtLeast(1)
        val showNulls = Properties.showNullNamedEntities.get()
        lateinit var loaded: List<T>
        val elapsed = measureTimeMillis {
            loaded = Arrays.stream(loadedDefinitions).parallel().map { definition ->
                CacheParallel.nextProgressStep(progressCounter, total, updateFrequency)
                val nullName = definition.name.isBlank() || definition.name == "null"
                if (definition.modelIds.isNotEmpty() && (!nullName || showNulls))
                    mapper(definition)
                else
                    null
            }.toArray { arrayOfNulls<T>(it) }.filterNotNull()
        }
        logger.debug("Wrapped {} {} in {}ms", loaded.size, name, elapsed)
        println("Loaded ${loaded.size} $name")
        return loaded
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
                onLoaded(loadEntities(definitions, mapper))
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CacheAssetLoader::class.java)
    }
}
