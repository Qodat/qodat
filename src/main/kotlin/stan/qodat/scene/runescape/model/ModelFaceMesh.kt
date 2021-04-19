package stan.qodat.scene.runescape.model

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Material
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.Mesh
import javafx.scene.shape.MeshView
import stan.qodat.Properties
import stan.qodat.util.BABY_BLUE
import stan.qodat.util.setAndBind
import java.util.function.Consumer

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/09/2019
 * @version 1.0
 */
class ModelFaceMesh(val face: Int, material: Material) : ModelMesh() {

    private lateinit var meshView: MeshView
    private lateinit var previousMaterial: Material
    val visibleProperty = SimpleBooleanProperty()
    val drawModeProperty = SimpleObjectProperty<DrawMode>()
    val cullFaceProperty = SimpleObjectProperty<CullFace>()
    val materialProperty = SimpleObjectProperty(material)
    val selectProperty = SimpleObjectProperty<Consumer<ModelFaceMesh>?>(null)

    private val mouseEventHandler = EventHandler<MouseEvent> {
        if (it.eventType == MouseEvent.MOUSE_ENTERED){
            previousMaterial = meshView.material
            meshView.material = PhongMaterial(BABY_BLUE)
        }
        if (it.eventType == MouseEvent.MOUSE_EXITED)
            meshView.material = previousMaterial
        if (it.eventType == MouseEvent.MOUSE_CLICKED){
            selectProperty.get()?.accept(this)
        }
    }

    override fun getSceneNode() : MeshView {
        if (!this::meshView.isInitialized) {
            meshView = MeshView(this)
            selectProperty.addListener { _, oldValue, newValue ->
                if (oldValue != null)
                    meshView.removeEventHandler(MouseEvent.ANY, mouseEventHandler)
                if (newValue != null)
                    meshView.addEventHandler(MouseEvent.ANY, mouseEventHandler)
            }
            meshView.visibleProperty().setAndBind(visibleProperty, true)
            meshView.cullFaceProperty().setAndBind(cullFaceProperty, true)
            meshView.materialProperty().setAndBind(materialProperty, true)
            meshView.drawModeProperty().setAndBind(drawModeProperty, true)
        }
        return meshView
    }
}