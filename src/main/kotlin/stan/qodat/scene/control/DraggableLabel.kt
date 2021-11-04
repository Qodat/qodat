package stan.qodat.scene.control

import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.value.ObservableValue
import javafx.css.PseudoClass
import javafx.geometry.Orientation
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.SplitPane
import javafx.scene.input.MouseEvent
import stan.qodat.util.setAndBind


/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   01/02/2021
 */
class SplitSceneDividerDragRegion(
    private val splitPane: SplitPane,
    private val node: Node,
    private val dividerIndex: IntegerProperty,
    positionProperty: DoubleProperty,
    private val relativeBounds: RelativeBounds? = null
) {

    private var x = 0.0
    private var y = 0.0
    private var previousCursor: Cursor? = null
    private var inside = false

    init {
        splitPane.dividers[dividerIndex.get()].positionProperty().setAndBind(positionProperty, true)

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
            when (splitPane.orientation!!) {
                Orientation.HORIZONTAL -> x = it.getRelativeX()
                Orientation.VERTICAL -> y = it.getRelativeY()
            }
        }
        node.setOnMouseReleased {
            node.pseudoClassStateChanged(DRAGGING_PSEUDO_CLASS, false)
        }
        node.setOnMouseDragged {
            val dividerIndex = dividerIndex.get()
            if (dividerIndex == -1)
                return@setOnMouseDragged
            when (splitPane.orientation!!) {
                Orientation.HORIZONTAL -> {
                    val newX = it.getRelativeX()
                    val deltaX = x - newX
                    x = newX
                    val newDividerPosition =
                        maxOf(0.0, minOf(1.0, splitPane.dividerPositions[dividerIndex] - deltaX))
                    splitPane.setDividerPosition(dividerIndex, newDividerPosition)

                }
                Orientation.VERTICAL -> {
                    val newY = it.getRelativeY()
                    val deltaY = y - newY
                    y = newY
                    val newDividerPosition =
                        maxOf(0.0, minOf(1.0, splitPane.dividerPositions[dividerIndex] - deltaY))
                    splitPane.setDividerPosition(dividerIndex, newDividerPosition)
                }
            }
        }
    }

    private fun startDragging() {
        inside = true
        previousCursor = node.scene.cursor
        node.scene.cursor = when (splitPane.orientation!!) {
            Orientation.HORIZONTAL -> Cursor.H_RESIZE
            Orientation.VERTICAL -> Cursor.V_RESIZE
        }
    }

    private fun stopDragging() {
        node.scene.cursor = previousCursor
        inside = false
    }

    fun MouseEvent.inDragRegion() : Boolean {
        if (relativeBounds != null) {
            return relativeBounds.contains(node, x, y)
        }
        return true
    }
    fun MouseEvent.getRelativeX() : Double {
        return sceneX / splitPane.width
    }
    fun MouseEvent.getRelativeY() : Double {
        return sceneY / splitPane.height
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