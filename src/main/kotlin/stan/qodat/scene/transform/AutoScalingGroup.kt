package stan.qodat.scene.transform

import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.Group
import javafx.scene.transform.Scale
import javafx.scene.transform.Translate
import stan.qodat.util.onInvalidation

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
class AutoScalingGroup(
    private val scale: Scale = Scale(1.0, 1.0, 1.0, 0.0, 0.0, 0.0),
    private val translate: Translate = Translate(0.0, 0.0, 0.0)
) : Group() {

    private val enabledProperty = SimpleBooleanProperty(false)

    init {
        transforms.setAll(scale, translate)
        enabledProperty.onInvalidation {
            if (get())
                transforms.setAll(scale, translate)
            else
                transforms.clear()
        }
    }

    fun setEnabled(enabled: Boolean) {
        enabledProperty.set(enabled)
    }
}