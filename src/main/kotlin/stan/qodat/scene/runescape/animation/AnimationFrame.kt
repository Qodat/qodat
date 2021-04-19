package stan.qodat.scene.runescape.animation

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.layout.HBox
import stan.qodat.cache.definition.AnimationFrameDefinition
import stan.qodat.util.ViewNodeProvider
import stan.qodat.scene.control.LabeledHBox
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
    definition: AnimationFrameDefinition?,
    duration: Int
) : TransformationGroup, Searchable, ViewNodeProvider {

    private lateinit var viewBox : HBox
    private val labelProperty = SimpleStringProperty(name)

    val transformationCountProperty = SimpleIntegerProperty(definition?.transformationCount?:0)
    val transformationList = FXCollections.observableArrayList<Transformation>()
    val durationProperty = SimpleObjectProperty(FrameTimeUtil.frame(duration))
    val enabledProperty = SimpleBooleanProperty(true)

    init {
        if (definition != null) {
            transformationList.addAll(Array(transformationCountProperty.get()) {
                val groupIndex = definition.transformationGroupAccessIndices[it]
                Transformation(
                    "transform[$it]",
                    definition.transformationGroup.targetVertexGroupsIndices[groupIndex],
                    definition.transformationGroup.transformationTypes[groupIndex],
                    definition.transformationDeltaX[it],
                    definition.transformationDeltaY[it],
                    definition.transformationDeltaZ[it]
                )
            })
        }
        transformationList.onInvalidation {
            transformationCountProperty.set(transformationList.size)
        }
    }

    fun getTransformationCount() = transformationCountProperty.get()

    override fun getDuration() = durationProperty.get()

    override fun getViewNode(): Node {
        if (!this::viewBox.isInitialized)
            viewBox = LabeledHBox(labelProperty)
        return viewBox
    }

    override fun getName() = labelProperty.get()

    override fun toString() = getName()
}