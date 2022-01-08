package stan.qodat.cache

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.concurrent.Task
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.definition.EntityDefinition
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.controller.AnimationController
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.entity.Item
import stan.qodat.scene.runescape.entity.NPC
import stan.qodat.scene.runescape.entity.Object
import stan.qodat.util.createNpcAnimsJsonDir
import stan.qodat.util.createObjectAnimsJsonDir
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

class CacheAssetLoader(
    private val cache: Cache,
    private val animationController: AnimationController
) {

    fun loadAnimations(onCompleted: (List<Animation>) -> Unit) {
        Qodat.mainController.executeBackgroundTasks(createLoadAnimationsTask(cache, onCompleted))
    }

    fun loadItems(onCompleted: (List<Item>) -> Unit) {
        Qodat.mainController.executeBackgroundTasks(createItemsLoadTask(cache, onCompleted))
    }

    fun loadObjects(onCompleted: (List<Object>) -> Unit) {
        if (cache is OldschoolCacheRuneLite) {
            val objectAnimsDir = Properties.osrsCachePath.get().resolve("object_anims").toFile()
            if (!objectAnimsDir.exists()) {
                println("Did not find object_anims dir, creating...")
                objectAnimsDir.mkdir()
                val task = createObjectAnimsJsonDir(
                    store = OldschoolCacheRuneLite.store,
                    objectManager = OldschoolCacheRuneLite.objectManager
                )
                task.setOnSucceeded {
                    Qodat.mainController.executeBackgroundTasks(createObjectLoadTask(cache, onCompleted))
                }
                Qodat.mainController.executeBackgroundTasks(task)
                return
            }
        }
        Qodat.mainController.executeBackgroundTasks(createObjectLoadTask(cache, onCompleted))
    }

    fun loadNpcs(onCompleted: (List<NPC>) -> Unit) {
        if (cache is OldschoolCacheRuneLite) {
            val npcAnimsDir = Properties.osrsCachePath.get().resolve("npc_anims").toFile()
            if (!npcAnimsDir.exists()) {
                println("Did not find npc_anims dir, creating...")
                npcAnimsDir.mkdir()
                val task = createNpcAnimsJsonDir(
                    store = OldschoolCacheRuneLite.store,
                    npcManager = OldschoolCacheRuneLite.npcManager
                )
                task.setOnSucceeded {
                    Qodat.mainController.executeBackgroundTasks(createNPCLoadTask(cache, onCompleted))
                }
                Qodat.mainController.executeBackgroundTasks(task)
                return
            }
        }
        Qodat.mainController.executeBackgroundTasks(createNPCLoadTask(cache, onCompleted))
    }

    private fun createLoadAnimationsTask(cache: Cache, onCompleted: (List<Animation>) -> Unit) = object : Task<Void?>() {
        override fun call(): Void? {
            val animationDefinitions = cache.getAnimationDefinitions()
            val animations = ArrayList<Animation>()
            for ((i, definition) in animationDefinitions.withIndex()) {
                try {
                    if (definition.frameHashes.isNotEmpty())
                        animations += Animation("$i", definition, cache)
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
        mapper = { Object(cache, this, animationController) }
    ) { Platform.runLater { onCompleted(this) } }

    private fun createNPCLoadTask(cache: Cache, onCompleted: (List<NPC>) -> Unit) = createLoadTask(
        definitions = cache.getNPCs(),
        mapper = { NPC(cache, this, animationController) }
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
                        PlatformImpl.runLater {
                            updateProgress(count.toLong(), total.toLong())
                            updateMessage("Loading $name ($count / $total)")
                        }
                    }
                    if (it.name.isNotBlank() && it.name != "null" && it.modelIds.isNotEmpty())
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