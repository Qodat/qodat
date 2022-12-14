package stan.qodat.scene

import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.transform.Translate
import stan.qodat.Qodat
import stan.qodat.scene.controller.SceneController
import stan.qodat.scene.provider.SceneNodeProvider
import stan.qodat.scene.provider.TreeItemProvider
import stan.qodat.scene.runescape.animation.AnimationPlayer
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.addSceneTreeItem
import stan.qodat.util.filterAndMap
import stan.qodat.util.onInvalidation
import stan.qodat.util.removeSceneTreeItem

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
abstract class SceneContext(val name: String) : SceneNodeProvider {

    /**
     * Group holding all scene nodes in this context.
     */
    private val group = Group()

    private val nodeProviderMap = HashMap<Node, SceneNodeProvider>()

    /**
     * Is this context the currently active context in the sub scene?
     */
    internal val activeContext = SimpleBooleanProperty()

    internal val animationPlayer = AnimationPlayer()

    init {
        group.id = name
        val t = Translate(0.0, 0.0, 0.0)
        val childrenChangeListener = ListChangeListener<Node> {
            if (activeContext.get()) {
                while (it.next()) {
                    it.addedSubList.forEach { node ->
                        val provider = nodeProviderMap[node]
                        if (provider is TreeItemProvider)
                            Qodat.addSceneTreeItem(provider)
                    }
                    it.removed.forEach { node ->
                        val provider = nodeProviderMap[node]
                        if (provider is TreeItemProvider)
                            Qodat.removeSceneTreeItem(provider)
                    }
                }
                t.y = -group.boundsInParent.centerY
            }
        }
        group.transforms.add(t)
        group.children.addListener(childrenChangeListener)
        activeContext.onInvalidation {
            group.children.forEach {
                val provider = nodeProviderMap[it]
                if (provider is TreeItemProvider) {
                    if (value)
                        Qodat.addSceneTreeItem(provider)
                    else
                        Qodat.removeSceneTreeItem(provider)
                }
            }
        }
    }

    fun addNode(nodeProvider: SceneNodeProvider) {
        try {
            val sceneNode = nodeProvider.getSceneNode()
            nodeProviderMap[sceneNode] = nodeProvider
            group.children.add(sceneNode)
        } catch (e: Exception) {
            Qodat.logException("Failed to add node {$nodeProvider} to scene $name", e)
        }
    }

    fun removeNode(nodeProvider: SceneNodeProvider) {
        try {
            val sceneNode = nodeProvider.getSceneNode()
            group.children.remove(sceneNode)
            nodeProviderMap.remove(sceneNode)
            nodeProvider.removeSceneNodeReference()
        } catch (e: Exception) {
            Qodat.logException("Failed to remove node {$nodeProvider} from scene $name", e)
        }
    }

    fun clear() {
        group.children.clear()
        nodeProviderMap.clear()
    }

    fun getModels(): List<Model> {
        val entities = nodeProviderMap.values.filterAndMap<Entity<*>>()
        val models = ArrayList<Model>()
        for (entity in entities){
            models.addAll(entity.getModels())
        }
        models.addAll(nodeProviderMap.values.filterAndMap())
        return models
    }

    abstract fun getController() : SceneController

    override fun getSceneNode(): Group = group

    override fun toString(): String = group.id
}
