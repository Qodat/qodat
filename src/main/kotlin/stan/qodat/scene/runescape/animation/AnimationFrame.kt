package stan.qodat.scene.runescape.animation

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.util.Duration
import qodat.cache.definition.AnimationFrameDefinition
import qodat.cache.definition.AnimationTransformationGroup
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.scene.transform.TransformationGroup
import stan.qodat.util.FrameTimeUtil
import stan.qodat.util.Searchable
import stan.qodat.util.onInvalidation

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class AnimationFrame(
    name: String,
    val definition: AnimationFrameDefinition?,
    duration: Int
) : TransformationGroup, Searchable, ViewNodeProvider {

    private lateinit var viewBox : HBox
    val labelProperty = SimpleStringProperty(name)

    val idProperty = SimpleIntegerProperty()
    val transformationCountProperty = SimpleIntegerProperty(definition?.transformationCount?:0)
    val transformationList = FXCollections.observableArrayList<Transformation>()
    val durationProperty = SimpleObjectProperty(FrameTimeUtil.frame(duration))
    val enabledProperty = SimpleBooleanProperty(true)

    fun getLength() = FrameTimeUtil.toFrame(durationProperty.get())

    init {
        if (definition != null) {
            val transformations = Array(transformationCountProperty.get()) {
                val groupIndex = definition.transformationGroupAccessIndices[it]
                Transformation(
                    "transform[$it]",
                    definition.transformationGroup.targetVertexGroupsIndices[groupIndex],
                    definition.transformationGroup.transformationTypes[groupIndex],
                    definition.transformationDeltaX[it],
                    definition.transformationDeltaY[it],
                    definition.transformationDeltaZ[it]
                ).apply {
                    idProperty.set(it)
                    groupIndexProperty.set(groupIndex)
                }
            }
            transformationList.addAll(transformations)
        }
        transformationList.onInvalidation {
            transformationCountProperty.set(transformationList.size)
        }
    }

    fun getTransformationCount() = transformationCountProperty.get()

    override fun durationProperty(): SimpleObjectProperty<Duration> = durationProperty

    override fun getDuration() = durationProperty().get()

    override fun getViewNode(): Node {
        if (!this::viewBox.isInitialized)
            viewBox = LabeledHBox(labelProperty)
        return viewBox
    }

    override fun getName() = labelProperty.get()

    override fun toString() = getName()

    fun clone(name: String) = AnimationFrame(
        name = name,
        definition = object : AnimationFrameDefinition {
            override val transformationCount: Int = transformationCountProperty.get()
            override val transformationGroupAccessIndices: IntArray =
                transformationList.map { it.groupIndexProperty.get() }.toIntArray()
            override val transformationDeltaX: IntArray =
                transformationList.map { it.getDeltaX() }.toIntArray()
            override val transformationDeltaY: IntArray =
                transformationList.map { it.getDeltaY() }.toIntArray()
            override val transformationDeltaZ: IntArray =
                transformationList.map { it.getDeltaZ() }.toIntArray()
            override val transformationGroup: AnimationTransformationGroup =
                definition!!.transformationGroup
            override val framemapArchiveIndex: Int = -1
        },
        duration = getLength().toInt()
    ).apply {
        val hex = Integer.toHexString(this@AnimationFrame.idProperty.get())
        val fileId = getFileId(hex)
        val frameId = getFrameId(hex) + 1
        this.idProperty.set(((fileId and  0xFFFF) shl 16) or (frameId and 0xFFFF))
    }

    internal fun getFileId(hexString: String): Int {
        return Integer.parseInt(hexString.substring(0, hexString.length - 4), 16)
    }

    internal fun getFrameId(hexString: String): Int {
        return Integer.parseInt(hexString.substring(hexString.length - 4), 16)
    }

}
