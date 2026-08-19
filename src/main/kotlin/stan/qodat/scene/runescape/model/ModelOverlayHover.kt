package stan.qodat.scene.runescape.model

import javafx.scene.Node
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseEvent

/**
 * One shared tooltip for overlay markers and diagnostic surface hover.
 * Installing a tooltip per marker would create hundreds of nodes.
 */
internal object ModelOverlayHover {

    private val tooltip = Tooltip().apply {
        isWrapText = false
    }

    fun show(node: Node, event: MouseEvent, text: String) {
        tooltip.text = text
        val window = node.scene?.window
        if (window != null)
            tooltip.show(window, event.screenX + 12, event.screenY + 12)
        else
            tooltip.show(node, event.screenX + 12, event.screenY + 12)
    }

    fun hide() {
        tooltip.hide()
    }
}
