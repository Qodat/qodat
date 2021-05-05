package stan.qodat.scene.control

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ToggleButton
import javafx.scene.control.Tooltip

class LockButton(toolTipText: String = "") : ToggleButton() {

    init {
        alignment = Pos.CENTER_LEFT
        padding = Insets(0.0, 10.0, 0.0, 0.0)
        styleClass.add("lock-toggle-button")
        tooltip = Tooltip(toolTipText)
    }

}