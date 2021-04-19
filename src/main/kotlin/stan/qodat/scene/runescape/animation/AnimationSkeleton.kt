package stan.qodat.scene.runescape.animation

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import stan.qodat.cache.definition.AnimationSkeletonDefinition
import stan.qodat.util.Searchable

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   11/02/2021
 */
class AnimationSkeleton(
    name: String,
    private val definition: AnimationSkeletonDefinition
) : Searchable {

    private lateinit var transformationGroups: ObservableList<TransformationGroup>

    val labelProperty = SimpleStringProperty(name)

    fun getTransformationGroups(): ObservableList<TransformationGroup> {
        if (!this::transformationGroups.isInitialized){
            val list = ArrayList<TransformationGroup>()
            for ((i, groupIndices) in definition.targetVertexGroupsIndices.withIndex()){
                val type = TransformationType.get(definition.transformationTypes[i])
                list.add(TransformationGroup("$i", type, groupIndices))
            }
            transformationGroups = FXCollections.observableArrayList(list)
        }
        return transformationGroups
    }

    override fun getName(): String {
        return labelProperty.get()
    }
}