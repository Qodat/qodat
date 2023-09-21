package stan.qodat.cache

import javafx.application.Platform
import javafx.concurrent.Task
import qodat.cache.Cache
import qodat.cache.definition.AnimatedEntityDefinition
import qodat.cache.definition.EntityDefinition
import stan.qodat.Properties
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.entity.*
import stan.qodat.task.BackgroundTasks
import stan.qodat.util.createNpcAnimsJsonDir
import stan.qodat.util.createObjectAnimsJsonDir
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

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

    fun loadObjects(onCompleted: (List<Object>) -> Unit) {
        if (cache is OldschoolCacheRuneLite) {
            val objectAnimsDir = Properties.osrsCachePath.get().resolve("object_anims").toFile()
            if (!objectAnimsDir.exists()) {
                println("Did not find object_anims dir, creating...")
                objectAnimsDir.mkdir()
                val task = createObjectAnimsJsonDir(
                    cache = cache,
                    objectManager = OldschoolCacheRuneLite.objectManager
                )
                task.setOnSucceeded {
                    BackgroundTasks.submit(addProgressIndicator = true, createObjectLoadTask(cache, onCompleted))
                }
                BackgroundTasks.submit(addProgressIndicator = true, task)
                return
            }
        }
        BackgroundTasks.submit(addProgressIndicator = true, createObjectLoadTask(cache, onCompleted))
    }

    fun loadNpcs(onCompleted: (List<NPC>) -> Unit) {
        if (cache is OldschoolCacheRuneLite) {
            val npcAnimsDir = Properties.osrsCachePath.get().resolve("npc_anims").toFile()
            if (!npcAnimsDir.exists()) {
                println("Did not find npc_anims dir, creating...")
                npcAnimsDir.mkdir()
                val task = createNpcAnimsJsonDir(
                    cache = cache,
                    npcManager = OldschoolCacheRuneLite.npcManager
                )
                task.setOnSucceeded {
                    BackgroundTasks.submit(addProgressIndicator = true, createNPCLoadTask(cache, onCompleted))
                }
                BackgroundTasks.submit(addProgressIndicator = true, task)
                return
            }
        }
        BackgroundTasks.submit(addProgressIndicator = true, createNPCLoadTask(cache, onCompleted))
    }

    private fun createLoadAnimationsTask(cache: Cache, onCompleted: (List<Animation>) -> Unit) = object : Task<Void?>() {
        override fun call(): Void? {
            val animationDefinitions = cache.getAnimationDefinitions()
            val animations = ArrayList<Animation>()
            for ((i, definition) in animationDefinitions.withIndex()) {
                try {
                    if (definition.frameHashes.isNotEmpty())
                        animations += Animation("$i", definition, cache).apply {
                            this.idProperty.set(i)
                        }
                    else if (definition.skeletalAnimationId > 0) {
                        animations += Animation("$i (Skeletal)", definition, cache).apply {
                            this.idProperty.set(i)
                        }
                    }
                    updateProgress((100.0 * i.div(animationDefinitions.size)), 100.0)
                    updateMessage("Loading animation (${i + 1} / ${animationDefinitions.size})")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Platform.runLater {
                onCompleted(animations)
            }
            return null
        }
    }
    private fun createObjectLoadTask(cache: Cache, onCompleted: (List<Object>) -> Unit) = createLoadTask(
        definitions = cache.getObjects(),
        mapper = { Object(cache, this, animationLoader) }
    ) { Platform.runLater { onCompleted(this) } }

    private fun createNPCLoadTask(cache: Cache, onCompleted: (List<NPC>) -> Unit) = createLoadTask(
        definitions = cache.getNPCs(),
        mapper = { NPC(cache, this, animationLoader) }
    ) { Platform.runLater { onCompleted(this) } }

    private fun createSpotAnimsLoadTask(cache: Cache, onCompleted: (List<SpotAnimation>) -> Unit) = createLoadTask(
        definitions = cache.getSpotAnimations(),
        mapper = { SpotAnimation(cache, this, animationLoader) }
    ) { Platform.runLater { onCompleted(this) } }

    private fun createItemsLoadTask(cache: Cache, onCompleted: (List<Item>) -> Unit) = createLoadTask(
        definitions = cache.getItems(),
        mapper = { Item(cache, this) })
    { Platform.runLater { onCompleted(this) } }

    private inline fun<D : EntityDefinition, reified T : Entity<D>> createLoadTask(
        definitions: Array<D>,
        crossinline mapper: D.() -> T,
        crossinline onLoaded: List<T>.() -> Unit
    ) : Task<Unit> {
        val progressCounter = AtomicInteger()
        val total = definitions.size
        val updateFrequency = (total / 500).coerceAtLeast(1)
        val name = T::class.simpleName
        return object : Task<Unit>() {
            init {
                updateTitle("Loading $total $name from cache ${cache.name}")
            }
            override fun call() {
                val values = Stream.of(*definitions).parallel().map {
                    val count = progressCounter.incrementAndGet()
                    if (count % updateFrequency == 0) {
                        Platform.runLater {
                            updateProgress(count.toLong(), total.toLong())
                            updateMessage("Loading $name ($count / $total)")
                        }
                    }
                    val nullName = it.name.isBlank() || it.name == "null"
                    val showNulls = Properties.showNullNamedEntities.get()
                    if (it.modelIds.isNotEmpty() && (!nullName || showNulls))
                        mapper(it)
                    else
                        null
                }.toArray { arrayOfNulls<T>(it) }.filterNotNull()
                println("Loaded ${values.size} $name")
                onLoaded(values)
            }
        }
    }

}
