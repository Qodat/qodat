package stan.qodat.scene.runescape.ui

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty

/**
 * Viewport presentation for the selected interface: flat 2D (camera locked)
 * versus exploded 3D hierarchy (orbit enabled).
 */
object InterfacePresentation {

    val active = SimpleBooleanProperty(false)
    val exploded = SimpleBooleanProperty(false)

    val cameraNavigationEnabled = Bindings.or(active.not(), exploded)!!
}
