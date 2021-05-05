package stan.qodat.scene.layout

import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Bounds
import javafx.scene.Group
import javafx.scene.SubScene
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import org.w3c.dom.css.Rect
import stan.qodat.Qodat
import stan.qodat.scene.control.SelectionHandler
import stan.qodat.scene.SubScene3D
import stan.qodat.util.onInvalidation

/**
 * Represents a [Pane] that auto-resizes a [SubScene] and [overlayRectangleGroup].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
class AutoScaleSubScenePane(
    minHeight: Double = 50.0,
    minWidth: Double = 50.0,
    maxHeight: Double = Double.MAX_VALUE,
    maxWidth: Double = Double.MAX_VALUE,
    parentWidthProperty: ReadOnlyDoubleProperty
) : Pane() {

    val overlayMiscGroup = Group()
    val subSceneProperty = SimpleObjectProperty<SubScene>()
    val selectionHandler = SelectionHandler()

    init {
        setMinSize(minWidth, minHeight)
        setMaxSize(maxWidth, maxHeight)
        subSceneProperty.onInvalidation {
            onSubSceneChanged()
        }
        setOnMousePressed {
            if (it.isPrimaryButtonDown && it.isControlDown) {
                if (!children.contains(selectionHandler.selectionRectangle))
                    children.add(selectionHandler.selectionRectangle)
                selectionHandler.setMousePosition(it.x, it.y)
                it.consume()
            }
        }
        fun createRectangle(bounds: Bounds, strokeColor: Color) = Rectangle(bounds.width, bounds.height, Color.TRANSPARENT).also {
            it.stroke = strokeColor
            it.strokeDashArray.addAll(5.0, 5.0)
            it.x = bounds.minX
            it.y = bounds.minY
        }
        setOnMouseReleased {
            if (children.contains(selectionHandler.selectionRectangle))
                children.remove(selectionHandler.selectionRectangle)
            if (it.isPrimaryButtonDown && it.isControlDown) {
                /**
                 * TODO: FIX THIS
                 */
                val widthOffset = parentWidthProperty.get() - width
                val selection = selectionHandler.selectionRectangle
                val selectionBounds = selection.boundsInParent

                val context = SubScene3D.contextProperty.get()
                overlayMiscGroup.children.clear()
                context.getModels().forEach { model ->
                    for (mesh in model.collectMeshes()){
                        val meshView = mesh.getSceneNode()
                        val selectionBox = mesh.getSelectionBox()
                        val localBounds = meshView.localToScene(meshView.boundsInLocal, true)
                        val rec = createRectangle(selectionBounds, Color.RED)
                        rec.translateX -= Qodat.mainController.filesWindow.width
                        val view = model.getSceneNode()
                        if (rec.boundsInLocal.intersects(localBounds) && !view.children.contains(selectionBox)){
                            view.children.add(selectionBox)
                        } else
                            view.children.remove(selectionBox)
                    }
                }
            }
        }
        setOnMouseDragged {
            if (it.isPrimaryButtonDown && it.isControlDown) {
                selectionHandler.onMouseDragged(it.x, it.y)
                it.consume()
            }
        }
    }

    private fun onSubSceneChanged(){
        val subScene = subSceneProperty.get()?:return
        setPrefSize(subScene.width, subScene.height)
        children.setAll(subScene, overlayMiscGroup)
    }

    override fun layoutChildren() {
        val subScene = subSceneProperty.get()
        if (subScene != null){
            subScene.width = width
            subScene.height = height
        }
        val nodeWidth = snapSize(overlayMiscGroup.prefWidth(-1.0))
        val nodeHeight = snapSize(overlayMiscGroup.prefHeight(-1.0))
        overlayMiscGroup.resizeRelocate(width - nodeWidth, 0.0, nodeWidth, nodeHeight)
    }
}