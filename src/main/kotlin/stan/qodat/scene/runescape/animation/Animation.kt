package stan.qodat.scene.runescape.animation

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.util.Callback
import qodat.cache.Cache
import qodat.cache.definition.AnimationDefinition
import stan.qodat.javafx.menu
import stan.qodat.javafx.menuItem
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.control.tree.AnimationTreeItem
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.scene.transform.Transformable
import stan.qodat.scene.transform.Transformer
import stan.qodat.util.Searchable
import tornadofx.contextmenu
import tornadofx.item
import tornadofx.stringBinding

/**
 * Represents a [Transformer] for [Model] objects.
 *
 * JavaFX properties are created on first access so wrapping 14k cache
 * sequences does not allocate seven property objects per row.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
abstract class Animation(
    label: String,
    open val definition: AnimationDefinition? = null,
    val cache: Cache? = null,
    numericId: Int = 0,
) : Transformer<AnimationFrame>, Searchable, ViewNodeProvider {
    private lateinit var viewBox: HBox
    private var currentFrameIndex = 0
    private val initialLabel = label
    private val initialNumericId = numericId
    private var treeItemProp: SimpleObjectProperty<AnimationTreeItem>? = null
    private var labelProp: SimpleStringProperty? = null
    private var nameProp: SimpleStringProperty? = null
    private var idProp: SimpleIntegerProperty? = null
    private var loopOffsetProp: SimpleIntegerProperty? = null
    private var leftHandItemProp: SimpleIntegerProperty? = null
    private var rightHandItemProp: SimpleIntegerProperty? = null

    val treeItemProperty: SimpleObjectProperty<AnimationTreeItem>
        get() = treeItemProp ?: SimpleObjectProperty<AnimationTreeItem>().also { treeItemProp = it }
    val labelProperty: SimpleStringProperty
        get() = labelProp ?: SimpleStringProperty(initialLabel).also { labelProp = it }
    val nameProperty: SimpleStringProperty
        get() = nameProp ?: SimpleStringProperty(initialLabel).also { nameProp = it }
    val idProperty: SimpleIntegerProperty
        get() = idProp ?: SimpleIntegerProperty(initialNumericId).also { idProp = it }
    val loopOffsetProperty: SimpleIntegerProperty
        get() = loopOffsetProp ?: SimpleIntegerProperty(definition?.loopOffset ?: -1).also { loopOffsetProp = it }
    val leftHandItemProperty: SimpleIntegerProperty
        get() = leftHandItemProp ?: SimpleIntegerProperty(definition?.leftHandItem ?: -1).also { leftHandItemProp = it }
    val rightHandItemProperty: SimpleIntegerProperty
        get() = rightHandItemProp ?: SimpleIntegerProperty(definition?.rightHandItem ?: -1).also { rightHandItemProp = it }

    abstract override fun getFrameList(): ObservableList<AnimationFrame>

    protected fun getCacheSafe() = requireNotNull(cache)
    { "Cache must not be null if loading from cache definition!" }

    override fun setFrame(frameIndex: Int) {
        currentFrameIndex = frameIndex
    }

    override fun update(transformableList: List<Transformable>) {

        val frames = getFrameList()
        val frame = frames[currentFrameIndex]

        if (!frame.enabledProperty.get())
            return

        transformableList.forEach {
            it.animate(frame)
        }
    }

    override fun getName() = nameProp?.get() ?: initialLabel

    fun catalogId(): String = definition?.id ?: numericId().toString()

    fun numericId(): Int = idProp?.get() ?: initialNumericId

    abstract fun copy(): AnimationLegacy

    override fun getViewNode(): Node {
        if (!this::viewBox.isInitialized) {
            viewBox = LabeledHBox(labelProperty, labelPrefix = "animation").apply {
                nameProperty.bind(editableValueProperty.stringBinding(labelProperty) {
                    "${editableValueProperty.get()} (${labelProperty.get()})"
                })
                contextmenu {
                    item("Rename") {
                        setOnAction {
                            editableProperty.set(true)
                        }
                    }
                    menu("export") {
                        menuItem("GIF") {
                            exportAsGif()
                        }
                        menuItem("mp4") {
                            exportAsMp4()
                        }
                    }
                }
            }
        }
        return viewBox
    }

    abstract fun exportAsMp4()

    abstract fun exportAsGif()

    companion object {
        fun createCellFactory() = Callback<ListView<Animation>, ListCell<Animation>> {
            object : ListCell<Animation>() {
                override fun updateItem(item: Animation?, empty: Boolean) {
                    super.updateItem(item, empty)
                    graphic = if (empty || item == null) null else item.getViewNode()
                }
            }
        }
    }
}
