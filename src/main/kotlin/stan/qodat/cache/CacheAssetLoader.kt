package stan.qodat.cache

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import javafx.concurrent.Task
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.definition.EntityDefinition
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.control.ViewNodeListView
import stan.qodat.scene.controller.AnimationController
import stan.qodat.scene.controller.ModelController
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.entity.Item
import stan.qodat.scene.runescape.entity.NPC
import stan.qodat.util.Searchable
import stan.qodat.util.createNpcAnimsJsonDir
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

class CacheAssetLoader(
    private val cache: Cache,
    private val npcs: ObservableList<NPC>,
    private val items: ObservableList<Item>,
    private val itemList: ViewNodeListView<Item>,
    private val npcList: ViewNodeListView<NPC>,
    private val animationController: AnimationController
) {

    fun loadAnimations() {
        Qodat.mainController.executeBackgroundTasks(createLoadAnimationsTask(cache))
    }

    fun loadItems() {
        Qodat.mainController.executeBackgroundTasks(createItemsLoadTask(cache))
    }

    fun loadNpcs() {
        if (cache is OldschoolCacheRuneLite) {
            val npcAnimsDir = Properties.osrsCachePath.get().resolve("npc_anims").toFile()
            if (!npcAnimsDir.exists()) {
                println("Did not find npc_anims dir, creating...")
                npcAnimsDir.mkdir()
                val task = createNpcAnimsJsonDir(OldschoolCacheRuneLite.store, OldschoolCacheRuneLite.npcManager)
                task.setOnSucceeded {
                    Qodat.mainController.executeBackgroundTasks(createNPCLoadTask(cache))
                }
                Qodat.mainController.executeBackgroundTasks(task)
                return
            }
        }
        Qodat.mainController.executeBackgroundTasks(createNPCLoadTask(cache))
    }

    private fun createLoadAnimationsTask(cache: Cache) = object : Task<Void?>() {
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
                animationController.animations.setAll(animations)
            }
            return null
        }
    }

    private fun createNPCLoadTask(cache: Cache) = createLoadTask(
        definitions = cache.getNPCs(),
        mapper = { NPC(cache, this, animationController) }
    ) {
        val npcToSelect = lastSelectedEntity(Properties.selectedNpcName)
        val animationToSelect = animationController.animations.lastSelectedEntity(Properties.selectedNpcName)
        Platform.runLater {
            npcs.setAll(this)
            Qodat.mainController.postCacheLoading()
            if (npcToSelect != null)
                npcList.selectionModel.select(npcToSelect)
            if (animationToSelect != null)
                animationController.animationsListView.selectionModel.select(animationToSelect)
        }
    }

    private fun createItemsLoadTask(cache: Cache) = createLoadTask(
        definitions = cache.getItems(),
        mapper = { Item(cache, this) })
    {
        val itemToSelect = lastSelectedEntity(Properties.selectedItemName)
        Platform.runLater {
            items.setAll(this)
            Qodat.mainController.postCacheLoading()
            if (itemToSelect != null)
                itemList.selectionModel.select(itemToSelect)
        }
    }

    private fun<T : Searchable> List<T>.lastSelectedEntity(stringProperty: StringProperty) : T? {
        val lastSelectedName = stringProperty.get()?:""
        return if (lastSelectedName.isBlank())
            null
        else
            find { lastSelectedName == it.getName() }
    }

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
                    if (it.modelIds.isNotEmpty())
                        mapper(it)
                    else
                        null
                }.toArray { arrayOfNulls<T>(it) }.filterNotNull()
                onLoaded(values)
            }
        }
    }
}