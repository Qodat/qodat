package stan.qodat.scene.runescape.ui

import stan.qodat.scene.presentation.PlanarView

/**
 * @deprecated Use [PlanarView]. Kept so in-flight controller edits still compile.
 */
@Deprecated("Use PlanarView", ReplaceWith("stan.qodat.scene.presentation.PlanarView"))
object InterfacePresentation {
    val active get() = PlanarView.active
    val exploded get() = PlanarView.exploded
    val cameraNavigationEnabled get() = PlanarView.cameraNavigationEnabled
}
