package stan.qodat.scene.transform

import javafx.scene.Group
import javafx.scene.transform.Rotate
import javafx.scene.transform.Translate

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
class TransformGroup : Group() {

    val translate = Translate()
    val xRotate = Rotate(-20.0, 0.0, 0.0, 0.0, Rotate.X_AXIS)
    val yRotate = Rotate(-20.0, 0.0, 0.0, 0.0, Rotate.Y_AXIS)

    init {
        transforms.addAll(translate, xRotate, yRotate)
    }
}