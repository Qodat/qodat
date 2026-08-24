package stan.qodat.scene.presentation

import javafx.beans.value.ChangeListener

/**
 * Binds a planar scene graph to [PlanarView.exploded] so interfaces and sprites
 * share the same 2D / 3D toggle.
 */
class PlanarSceneHandle(
    private val applyExploded: (exploded: Boolean, animate: Boolean) -> Unit,
) {
    private val listener = ChangeListener<Boolean> { _, _, exploded ->
        applyExploded(exploded, true)
    }

    fun bind() {
        PlanarView.exploded.removeListener(listener)
        PlanarView.exploded.addListener(listener)
        applyExploded(PlanarView.exploded.get(), false)
    }

    fun unbind() {
        PlanarView.exploded.removeListener(listener)
    }
}
