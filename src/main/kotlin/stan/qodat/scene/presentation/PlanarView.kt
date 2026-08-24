package stan.qodat.scene.presentation

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty

/**
 * Shared 2D / 3D viewport mode for planar cache assets (interfaces, sprites).
 *
 * 2D locks the camera to a front view. 3D explodes layers and enables orbit.
 */
object PlanarView {

    const val LAYER_SPACING = 56.0

    val active = SimpleBooleanProperty(false)
    val exploded = SimpleBooleanProperty(false)

    val cameraNavigationEnabled = Bindings.or(active.not(), exploded)!!
}
