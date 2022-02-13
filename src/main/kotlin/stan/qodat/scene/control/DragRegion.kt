package stan.qodat.scene.control

import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.value.ObservableValue
import javafx.css.PseudoClass
import javafx.geometry.Orientation
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.input.MouseEvent


/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   01/02/2021
 */
class DragRegion(
    private val widthProperty: ReadOnlyDoubleProperty,
    private val heightProperty: ReadOnlyDoubleProperty,
    private val node: Node,
    private val relativeBounds: RelativeBounds? = null,
    private val orientation: Orientation = Orientation.HORIZONTAL,
    private val onSizeChange: (Double) -> Unit
) {

    private var x = 0.0
    private var y = 0.0
    private var previousCursor: Cursor? = null
    private var inside = false

    init {
        if (relativeBounds != null) {
            node.setOnMouseMoved {
                if (it.inDragRegion()) {
                    if (!inside)
                        startDragging()
                } else if (inside)
                    stopDragging()
            }
        } else {
            node.setOnMouseEntered {
                startDragging()
            }
        }
        node.setOnMouseExited {
            stopDragging()
        }

        node.setOnMousePressed {
            node.pseudoClassStateChanged(DRAGGING_PSEUDO_CLASS, true)
            when (orientation) {
                Orientation.HORIZONTAL -> x = it.getRelativeX()
                Orientation.VERTICAL -> y = it.getRelativeY()
            }
        }
        node.setOnMouseReleased {
            node.pseudoClassStateChanged(DRAGGING_PSEUDO_CLASS, false)
        }
        node.setOnMouseDragged {

            when (orientation) {
                Orientation.HORIZONTAL -> {
                    val newX = it.getRelativeX()
                    val deltaX = x - newX
                    x = newX
                    onSizeChange(deltaX)
                }
                Orientation.VERTICAL -> {
                    val newY = it.getRelativeY()
                    val deltaY = y - newY
                    y = newY
                    onSizeChange(deltaY)
                }
            }
        }
    }

    private fun startDragging() {
        inside = true
        previousCursor = node.scene.cursor
        node.scene.cursor = when (orientation) {
            Orientation.HORIZONTAL -> Cursor.H_RESIZE
            Orientation.VERTICAL -> Cursor.V_RESIZE
        }
    }

    private fun stopDragging() {
        node.scene.cursor = previousCursor
        inside = false
    }

    private fun MouseEvent.inDragRegion() : Boolean {
        if (relativeBounds != null) {
            return relativeBounds.contains(node, x, y)
        }
        return true
    }
    private fun MouseEvent.getRelativeX() : Double {
        return sceneX / widthProperty.get()
    }
    private fun MouseEvent.getRelativeY() : Double {
        return sceneY / heightProperty.get()
    }

    enum class Placement {
        TOP_RIGHT
    }
    class RelativeBounds(private val placement: Placement,
                         private val widthProperty: ObservableValue<Number>,
                         private val heightProperty: ObservableValue<Number>
     ) {

        fun contains(base: Node, mouseX: Double, mouseY: Double) : Boolean {
            val bound = base.boundsInLocal
            when(placement) {
                Placement.TOP_RIGHT -> {
                    val startX = bound.maxX - widthProperty.value.toDouble()
                    val startY = heightProperty.value.toDouble()
                    return mouseX >= startX && mouseY <= startY
                }
            }
        }
    }

    companion object {

        private val DRAGGING_PSEUDO_CLASS = PseudoClass.getPseudoClass("dragging")
    }
}