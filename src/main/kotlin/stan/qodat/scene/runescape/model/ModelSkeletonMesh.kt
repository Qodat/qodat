package stan.qodat.scene.runescape.model

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.DepthTest
import javafx.scene.Group
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import stan.qodat.util.setAndBind

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   10/02/2021
 */
class ModelSkeletonMesh(private val model: Model) : ModelSkin {

    private lateinit var group: Group

    val visibleProperty = SimpleBooleanProperty()
    val drawModeProperty = SimpleObjectProperty<DrawMode>()
    val cullFaceProperty = SimpleObjectProperty<CullFace>()
    val depthTestProperty = SimpleObjectProperty<DepthTest>()

    init {
        visibleProperty.setAndBind(model.visibleProperty)
        drawModeProperty.setAndBind(model.drawModeProperty)
        cullFaceProperty.setAndBind(model.cullFaceProperty)
        depthTestProperty.setAndBind(model.depthTestProperty)
    }

    override fun updatePoints(skeleton: ModelSkeleton) {
        val group = getSceneNode()
        for(child in group.children){
            if(child is MeshView){
                val mesh = child.mesh
                if (mesh is ModelSkin)
                    mesh.updatePoints(skeleton)
            }
        }
    }

    override fun getSceneNode(): Group {
        if (!this::group.isInitialized){
            buildMesh()
            group.visibleProperty().setAndBind(visibleProperty, true)
        }
        return group
    }

    /**
     * Build this mesh from the specified [model].
     */
    private fun buildMesh() {

        val definition = model.modelDefinition

        definition.computeAnimationTables()

        group = Group()

        for (faceGroup in definition.getFaceGroups()?:return){
            val groupMesh = ModelAtlasMesh(model, faceGroup.toList())
            group.children.add(groupMesh.getSceneNode())
        }
    }
}