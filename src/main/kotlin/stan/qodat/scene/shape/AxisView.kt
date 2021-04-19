package stan.qodat.scene.shape

import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Group
import stan.qodat.Properties
import stan.qodat.util.onInvalidation
import stan.qodat.util.setAndBindMaterial

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
class AxisView(
    span: Double = 2560.0,
    width: Double = 1.0,
    radius: Double = 2.0
) : Group() {

    val spanProperty = SimpleDoubleProperty(span)
    val widthProperty = SimpleDoubleProperty(width)
    val radiusProperty = SimpleDoubleProperty(radius)

    private lateinit var axis: Axis

    init {
        spanProperty.onInvalidation { buildAxis(true) }
        widthProperty.onInvalidation { buildAxis(true) }
        radiusProperty.onInvalidation { buildAxis(true) }
        buildAxis(true)
    }

    private fun buildAxis(unbindProperties: Boolean = true){

        val span = spanProperty.get()
        val width = widthProperty.get()
        val radius = radiusProperty.get()

        axis = Axis(span, width, radius)

        axis.xBox.setAndBindMaterial(Properties.xAxisMaterial)
        axis.yBox.setAndBindMaterial(Properties.yAxisMaterial)
        axis.zBox.setAndBindMaterial(Properties.zAxisMaterial)

        axis.xSphere.setAndBindMaterial(Properties.xAxisMaterial)
        axis.ySphere.setAndBindMaterial(Properties.yAxisMaterial)
        axis.zSphere.setAndBindMaterial(Properties.zAxisMaterial)

        children.setAll(axis)
    }
}