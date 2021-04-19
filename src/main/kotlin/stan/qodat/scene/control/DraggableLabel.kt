package stan.qodat.scene.control

import javafx.beans.property.DoubleProperty
import javafx.geometry.Orientation
import javafx.scene.Cursor
import javafx.scene.control.SplitPane
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import stan.qodat.util.setAndBind

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   01/02/2021
 */
class SplitSceneDividerDragRegion(
    private val splitPane: SplitPane,
    node: Region,
    dividerIndex: Int,
    positionProperty: DoubleProperty
) {

    private var x = 0.0
    private var y = 0.0

    private var previousCursor: Cursor? = null

    init {
        splitPane.dividers[0].positionProperty().setAndBind(positionProperty, true)
        node.setOnMouseEntered {
            previousCursor = node.scene.cursor
            node.scene.cursor = when(splitPane.orientation!!) {
                Orientation.HORIZONTAL -> Cursor.H_RESIZE
                Orientation.VERTICAL -> Cursor.V_RESIZE
            }
        }
        node.setOnMouseExited {
            node.scene.cursor = previousCursor
        }
        node.setOnMousePressed {
            when(splitPane.orientation!!) {
                Orientation.HORIZONTAL -> x = it.getRelativeX()
                Orientation.VERTICAL -> y = it.getRelativeY()
            }
        }
        node.setOnMouseDragged {

            when(splitPane.orientation!!) {
                Orientation.HORIZONTAL -> {
                    val newX = it.getRelativeX()
                    val deltaX = x - newX
                    x = newX
                    val newDividerPosition = maxOf(0.0, minOf(1.0, splitPane.dividerPositions[dividerIndex] - deltaX))
                    splitPane.setDividerPosition(dividerIndex, newDividerPosition)

                }
                Orientation.VERTICAL -> {
                    val newY = it.getRelativeY()
                    val deltaY = y - newY
                    y = newY
                    val newDividerPosition = maxOf(0.0, minOf(1.0, splitPane.dividerPositions[dividerIndex] - deltaY))
                    splitPane.setDividerPosition(dividerIndex, newDividerPosition)
                }
            }
        }
    }

    fun MouseEvent.getRelativeX() : Double {
        return sceneX / splitPane.width
    }
    fun MouseEvent.getRelativeY() : Double {
        return sceneY / splitPane.height
    }
}