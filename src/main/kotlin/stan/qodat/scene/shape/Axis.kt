package stan.qodat.scene.shape

import javafx.scene.Group
import javafx.scene.shape.Box
import javafx.scene.shape.Sphere

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
class Axis(
    span: Double = 2560.0,
    width: Double = 1.0,
    radius: Double = 2.0
) : Group() {

    val xBox = Box(span, width, width)
    val yBox = Box(width, span, width)
    val zBox = Box(width, width, span)
    val xSphere = Sphere(radius)
    val ySphere = Sphere(radius)
    val zSphere = Sphere(radius)

    init {
        xSphere.translateX = span / 2
        ySphere.translateY = span / 2
        zSphere.translateZ = span / 2
        children.setAll(xBox, yBox, zBox, xSphere, ySphere, zSphere)
    }
}