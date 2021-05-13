package stan.qodat.scene.control

import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import kotlin.math.abs
import kotlin.math.min

/**
 * Handles a selection rectangle that can be created by holding down CTRL and dragging inside the scene.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   30/01/2021
 */
class SelectionHandler {

    private var mouseDownX = 0.0
    private var mouseDownY = 0.0

    val selectionRectangle = Rectangle()

    init {
        selectionRectangle.stroke = Color.CYAN
        selectionRectangle.fill = Color.TRANSPARENT
        selectionRectangle.strokeDashArray.addAll(5.0, 5.0)
    }

    fun setMousePosition(x: Double, y: Double) {
        mouseDownX = x
        mouseDownY = y
        selectionRectangle.y = mouseDownY
        selectionRectangle.width = 0.0
        selectionRectangle.height = 0.0
    }

    fun onMouseDragged(x: Double, y: Double) {
        selectionRectangle.x = min(x, mouseDownX)
        selectionRectangle.width = abs(x - mouseDownX)
        selectionRectangle.y = min(y, mouseDownY)
        selectionRectangle.height = abs(y - mouseDownY)
    }
}