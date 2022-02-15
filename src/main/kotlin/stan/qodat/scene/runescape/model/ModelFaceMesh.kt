package stan.qodat.scene.runescape.model

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.scene.DepthTest
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Material
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import stan.qodat.util.onInvalidation
import stan.qodat.util.setAndBind

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/09/2019
 * @version 1.0
 */
class ModelFaceMesh(val face: Int, material: Material) : ModelMesh() {

    private lateinit var meshView: MeshView
    val previousMaterialProperty = SimpleObjectProperty<Material>()
    val visibleProperty = SimpleBooleanProperty()
    val drawModeProperty = SimpleObjectProperty<DrawMode>()
    val cullFaceProperty = SimpleObjectProperty<CullFace>()
    val materialProperty = SimpleObjectProperty(material)
    val editableProperty = SimpleObjectProperty<(EditContext.() -> Unit)?>(null)
    val depthTestProperty = SimpleObjectProperty<DepthTest>()

    private var textured = false
    private val mouseEventHandler = EventHandler<MouseEvent> {
        editableProperty.get()?.invoke(EditContext(this, it))
    }

    init {
        materialProperty.onInvalidation {
//            if (get() is TextureMaterial) {
//
//            }
        }
    }

    class EditContext(val mesh: ModelFaceMesh, val mouseEvent: MouseEvent) {

        val material: PhongMaterial
            get() = mesh.materialProperty.get() as PhongMaterial

        fun changeMaterial(newMaterial: Material) {
//            if (newMaterial is TextureMaterial) {
//                if (!mesh.textured){
//
//                }
//            }
            mesh.previousMaterialProperty.set(mesh.materialProperty.get())
            mesh.materialProperty.set(newMaterial)
        }

        fun revertMaterialChange() {
            if (mesh.previousMaterialProperty.get() != null) {
                mesh.materialProperty.set(mesh.previousMaterialProperty.get())
                mesh.previousMaterialProperty.set(null)
            }
        }
    }

    override fun getSceneNode(): MeshView {
        if (!this::meshView.isInitialized) {
            meshView = MeshView(this)
            editableProperty.addListener { _, oldValue, newValue ->
                if (oldValue != null)
                    meshView.removeEventHandler(MouseEvent.ANY, mouseEventHandler)
                if (newValue != null)
                    meshView.addEventHandler(MouseEvent.ANY, mouseEventHandler)
            }
            meshView.isPickOnBounds = false
            meshView.isMouseTransparent = true
            meshView.visibleProperty().setAndBind(visibleProperty, true)
            meshView.cullFaceProperty().setAndBind(cullFaceProperty, true)
            meshView.materialProperty().setAndBind(materialProperty, true)
            meshView.drawModeProperty().setAndBind(drawModeProperty, true)
            meshView.depthTestProperty().setAndBind(depthTestProperty, true)
        }
        return meshView
    }
}