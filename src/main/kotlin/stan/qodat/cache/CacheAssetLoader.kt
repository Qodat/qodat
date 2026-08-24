package stan.qodat.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import qodat.cache.Cache
import qodat.cache.definition.AnimationDefinition
import qodat.cache.definition.AnimationMayaDefinition
import qodat.cache.definition.EntityDefinition
import qodat.cache.definition.InterfaceDefinition
import qodat.cache.definition.SpriteDefinition
import stan.qodat.Properties
import stan.qodat.cache.impl.displee.DispleeCache
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationLegacy
import stan.qodat.scene.runescape.animation.AnimationMaya
import stan.qodat.scene.runescape.entity.*
import stan.qodat.scene.runescape.ui.InterfaceGroup
import stan.qodat.scene.runescape.ui.Sprite
import stan.qodat.task.BackgroundTasks
import kotlin.coroutines.coroutineContext
import kotlin.system.measureNanoTime

class CacheAssetLoader(
    private val cache: Cache,
    private val resolveAnimations: (Array<String>) -> Array<Animation>
) {

    suspend fun loadAll(
        selectedFirst: String?,
        onProgress: (String) -> Unit = {},
        onNpcs: (List<NPC>) -> Unit,
        onObjects: (List<Object>) -> Unit,
        onItems: (List<Item>) -> Unit,
        onSpotAnims: (List<SpotAnimation>) -> Unit,
        onSprites: (List<Sprite>) -> Unit,
        onInterfaces: (List<InterfaceGroup>) -> Unit,
        onAnimations: (List<Animation>) -> Unit,
    ) {
        maybeSubmitMissingAnimParsers()
        val present = cache.populatedListKinds()
        val jobs = linkedMapOf(
            Cache.LIST_NPC to suspend { applyOnFx(loadNpcsNow(), onNpcs) },
            Cache.LIST_OBJECT to suspend { applyOnFx(loadObjectsNow(), onObjects) },
            Cache.LIST_ITEM to suspend { applyOnFx(loadItemsNow(), onItems) },
            Cache.LIST_SPOT_ANIM to suspend { applyOnFx(loadSpotAnimsNow(), onSpotAnims) },
            Cache.LIST_SPRITES to suspend { applyOnFx(loadSpritesNow(), onSprites) },
            Cache.LIST_INTERFACES to suspend { applyOnFx(loadInterfacesNow(), onInterfaces) },
            Cache.LIST_ANIMATIONS to suspend { applyOnFx(loadAnimationsNow(), onAnimations) },
        )
        val empties = linkedMapOf(
            Cache.LIST_NPC to suspend { applyOnFx(emptyList(), onNpcs) },
            Cache.LIST_OBJECT to suspend { applyOnFx(emptyList(), onObjects) },
            Cache.LIST_ITEM to suspend { applyOnFx(emptyList(), onItems) },
            Cache.LIST_SPOT_ANIM to suspend { applyOnFx(emptyList(), onSpotAnims) },
            Cache.LIST_SPRITES to suspend { applyOnFx(emptyList(), onSprites) },
            Cache.LIST_INTERFACES to suspend { applyOnFx(emptyList(), onInterfaces) },
            Cache.LIST_ANIMATIONS to suspend { applyOnFx(emptyList(), onAnimations) },
        )
        val order = buildList {
            if (selectedFirst != null && selectedFirst in jobs) add(selectedFirst)
            jobs.keys.filter { it != selectedFirst }.forEach { add(it) }
        }
        for (name in order) {
            coroutineContext.ensureActive()
            if (name !in present) {
                logger.debug("Skipping empty {} list for cache {}", name, cache.name)
                empties.getValue(name).invoke()
                continue
            }
            onProgress("Loading $name from cache ${cache.name}")
            jobs.getValue(name).invoke()
        }
    }

    fun loadAnimations(onCompleted: (List<Animation>) -> Unit) {
        launchListLoad("Animations") { applyOnFx(loadAnimationsNow(), onCompleted) }
    }

    fun loadItems(onCompleted: (List<Item>) -> Unit) {
        launchListLoad("Item") { applyOnFx(loadItemsNow(), onCompleted) }
    }

    fun loadSpotAnims(onCompleted: (List<SpotAnimation>) -> Unit) {
        launchListLoad("SpotAnim") { applyOnFx(loadSpotAnimsNow(), onCompleted) }
    }

    fun loadSprites(onCompleted: (List<Sprite>) -> Unit) {
        launchListLoad("Sprites") { applyOnFx(loadSpritesNow(), onCompleted) }
    }

    fun loadInterfaces(onCompleted: (List<InterfaceGroup>) -> Unit) {
        launchListLoad("Interfaces") { applyOnFx(loadInterfacesNow(), onCompleted) }
    }

    fun loadObjects(onCompleted: (List<Object>) -> Unit) {
        maybeSubmitObjectAnimParser()
        launchListLoad("Object") { applyOnFx(loadObjectsNow(), onCompleted) }
    }

    fun loadNpcs(onCompleted: (List<NPC>) -> Unit) {
        maybeSubmitNpcAnimParser()
        launchListLoad("NPC") { applyOnFx(loadNpcsNow(), onCompleted) }
    }

    private fun launchListLoad(name: String, block: suspend () -> Unit) {
        BackgroundTasks.launch(addProgressIndicator = true, title = "Loading $name from cache ${cache.name}") {
            block()
        }
    }

    private suspend fun maybeSubmitMissingAnimParsers() {
        coroutineContext.ensureActive()
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

    private suspend fun <T> applyOnFx(value: T, onCompleted: (T) -> Unit) {
        withContext(Dispatchers.JavaFx) { onCompleted(value) }
    }

    private suspend fun loadNpcsNow(): List<NPC> = loadEntities(
        definitions = { cache.getNPCs() },
        mapper = { NPC(cache, this, resolveAnimations) }
    )

    private suspend fun loadObjectsNow(): List<Object> = loadEntities(
        definitions = { cache.getObjects() },
        mapper = { Object(cache, this, resolveAnimations) }
    )

    private suspend fun loadItemsNow(): List<Item> = loadEntities(
        definitions = { cache.getItems() },
        mapper = { Item(cache, this) }
    )

    private suspend fun loadSpotAnimsNow(): List<SpotAnimation> = loadEntities(
        definitions = { cache.getSpotAnimations() },
        mapper = { SpotAnimation(cache, this, resolveAnimations) }
    )

    private suspend fun loadAnimationsNow(): List<Animation> {
        val animationDefinitions: Array<AnimationDefinition>
        val decodeNs = measureNanoTime {
            animationDefinitions = withContext(Dispatchers.IO) { cache.getAnimationDefinitions() }
        }
        val animations: List<Animation>
        val wrapNs = measureNanoTime {
            animations = withContext(Dispatchers.Default) {
                CacheListWrap.mapNotNullIndexedCancellable(animationDefinitions) { i, definition ->
                    try {
                        val animationId = definition.id.toIntOrNull() ?: i
                        when {
                            definition is AnimationMayaDefinition ->
                                AnimationMaya(definition.id, definition, cache, animationId)
                            definition.frameHashes.isNotEmpty() ->
                                AnimationLegacy(definition.id, definition, cache, animationId)
                            else -> null
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }
        }
        logger.debug(
            "Decoded {} animations in {}ms, wrapped {} in {}ms",
            animationDefinitions.size, decodeNs / 1_000_000,
            animations.size, wrapNs / 1_000_000
        )
        return animations
    }

    private suspend fun loadSpritesNow(): List<Sprite> {
        val definitions: Array<SpriteDefinition>
        val decodeNs = measureNanoTime {
            definitions = withContext(Dispatchers.IO) { cache.getSprites() }
        }
        val values: List<Sprite>
        val wrapNs = measureNanoTime {
            values = withContext(Dispatchers.Default) {
                coroutineContext.ensureActive()
                definitions.map { Sprite(it, cache = cache) }
            }
        }
        logger.debug(
            "Decoded {} sprites in {}ms, wrapped {} in {}ms",
            definitions.size, decodeNs / 1_000_000,
            values.size, wrapNs / 1_000_000
        )
        println("Loaded ${values.size} Sprite")
        return values
    }

    private suspend fun loadInterfacesNow(): List<InterfaceGroup> {
        val groups: Map<Int, List<InterfaceDefinition>>
        val decodeNs = measureNanoTime {
            groups = withContext(Dispatchers.IO) { cache.getRootInterfaces() }
        }
        val values: List<InterfaceGroup>
        val wrapNs = measureNanoTime {
            values = withContext(Dispatchers.Default) {
                coroutineContext.ensureActive()
                groups.map { (id, definitions) -> InterfaceGroup(cache, id, definitions) }
            }
        }
        logger.debug(
            "Decoded {} interface groups in {}ms, wrapped {} in {}ms",
            groups.size, decodeNs / 1_000_000,
            values.size, wrapNs / 1_000_000
        )
        println("Loaded ${values.size} InterfaceGroup")
        return values
    }

    private suspend inline fun <D : EntityDefinition, reified T : Entity<D>> loadEntities(
        crossinline definitions: () -> Array<D>,
        crossinline mapper: D.() -> T,
    ): List<T> {
        val name = T::class.simpleName
        val loadedDefinitions: Array<D>
        val decodeNs = measureNanoTime {
            loadedDefinitions = withContext(Dispatchers.IO) { definitions() }
        }
        if (loadedDefinitions.isEmpty() && cache is DispleeCache) {
            throw IllegalStateException("Displee cache returned 0 $name definitions")
        }
        val showNulls = Properties.showNullNamedEntities.get()
        val loaded: List<T>
        val wrapNs = measureNanoTime {
            loaded = withContext(Dispatchers.Default) {
                CacheListWrap.mapNotNullCancellable(loadedDefinitions) { definition ->
                    val nullName = definition.name.isBlank() || definition.name == "null"
                    if (definition.modelIds.isNotEmpty() && (!nullName || showNulls))
                        mapper(definition)
                    else
                        null
                }
            }
        }
        logger.debug(
            "Decoded {} {} in {}ms, wrapped {} in {}ms",
            loadedDefinitions.size, name, decodeNs / 1_000_000,
            loaded.size, wrapNs / 1_000_000
        )
        println("Loaded ${loaded.size} $name")
        return loaded
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CacheAssetLoader::class.java)
    }
}
