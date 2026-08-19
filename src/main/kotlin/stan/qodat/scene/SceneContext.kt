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
    private val providerNodeMap = HashMap<SceneNodeProvider, Node>()
    private val suppressTree = HashSet<SceneNodeProvider>()
    private var browseProvider: SceneNodeProvider? = null

    /**
     * Is this context the currently active context in the sub scene?
     */
    internal val activeContext = SimpleBooleanProperty()

    internal val animationPlayer = AnimationPlayer()

    init {
        group.id = name
        val t = Translate(0.0, 0.0, 0.0)
        val childrenChangeListener = ListChangeListener<Node> {
            while (it.next()) {
                if (activeContext.get()) {
                    it.addedSubList.forEach { node ->
                        val provider = nodeProviderMap[node]
                        if (provider is TreeItemProvider && provider !in suppressTree)
                            Qodat.addSceneTreeItem(provider)
                    }
                    it.removed.forEach { node ->
                        val provider = nodeProviderMap[node]
                        if (provider is TreeItemProvider && provider !in suppressTree)
                            Qodat.removeSceneTreeItem(provider)
                    }
                }
            }
            EntitySceneLayout.apply(group, nodeProviderMap)
            if (activeContext.get() && group.children.isNotEmpty())
                t.y = -group.boundsInLocal.centerY
        }
        group.transforms.add(t)
        group.children.addListener(childrenChangeListener)
        activeContext.onInvalidation {
            group.children.forEach {
                val provider = nodeProviderMap[it]
                if (provider is TreeItemProvider && provider !in suppressTree) {
                    if (value)
                        Qodat.addSceneTreeItem(provider)
                    else
                        Qodat.removeSceneTreeItem(provider)
                }
            }
        }
    }

    fun addNode(nodeProvider: SceneNodeProvider, attachTree: Boolean = true) {
        try {
            if (!attachTree)
                suppressTree.add(nodeProvider)
            else
                suppressTree.remove(nodeProvider)
            val sceneNode = nodeProvider.getSceneNode()
            nodeProviderMap[sceneNode] = nodeProvider
            providerNodeMap[nodeProvider] = sceneNode
            group.children.add(sceneNode)
        } catch (e: Exception) {
            suppressTree.remove(nodeProvider)
            Qodat.logException("Failed to add node {$nodeProvider} to scene $name", e)
        }
    }

    /**
     * Browse slot: at most one unlocked entity in the 3D view.
     * Tree items are not attached until [attachBrowseTree].
     */
    fun replaceBrowseNode(nodeProvider: SceneNodeProvider) {
        val previous = browseProvider
        if (previous != null && previous !== nodeProvider) {
            val locked = (previous as? Entity<*>)?.locked?.get() == true
            if (!locked)
                removeNode(previous)
        }
        if (browseProvider === nodeProvider && providerNodeMap.containsKey(nodeProvider))
            return
        browseProvider = nodeProvider
        addNode(nodeProvider, attachTree = false)
    }

    fun removeBrowseNodeIfCurrent(nodeProvider: SceneNodeProvider) {
        if (browseProvider !== nodeProvider)
            return
        val locked = (nodeProvider as? Entity<*>)?.locked?.get() == true
        if (locked)
            return
        removeNode(nodeProvider)
        if (browseProvider === nodeProvider)
            browseProvider = null
    }

    fun attachBrowseTree(nodeProvider: SceneNodeProvider) {
        if (browseProvider !== nodeProvider)
            return
        if (nodeProvider !is TreeItemProvider)
            return
        if (!suppressTree.remove(nodeProvider))
            return
        if (activeContext.get())
            Qodat.addSceneTreeItem(nodeProvider)
    }

    fun removeNode(nodeProvider: SceneNodeProvider) {
        try {
            val sceneNode = providerNodeMap.remove(nodeProvider)
                ?: nodeProviderMap.entries.find { it.value === nodeProvider }?.key
                ?: nodeProvider.getSceneNode()
            group.children.remove(sceneNode)
            nodeProviderMap.remove(sceneNode)
            suppressTree.remove(nodeProvider)
            if (browseProvider === nodeProvider)
                browseProvider = null
            nodeProvider.removeSceneNodeReference()
        } catch (e: Exception) {
            Qodat.logException("Failed to remove node {$nodeProvider} from scene $name", e)
        }
    }

    fun clear() {
        nodeProviderMap.values.forEach { it.removeSceneNodeReference() }
        group.children.clear()
        nodeProviderMap.clear()
        providerNodeMap.clear()
        suppressTree.clear()
        browseProvider = null
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
