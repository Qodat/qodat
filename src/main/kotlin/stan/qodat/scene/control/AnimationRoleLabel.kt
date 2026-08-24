package stan.qodat.scene.control

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import stan.qodat.scene.runescape.animation.Animation
import java.util.WeakHashMap

/**
 * Stance-role chip shown next to an animation id (Idle, Walk, …).
 */
object AnimationRoleLabel {
    val fill: Color = Color.web("#7EB8A6")
    private val font = Font.font("Menlo", FontWeight.BOLD, 13.0)
    private val wrappers = WeakHashMap<Animation, Pair<HBox, Text>>()

    fun text(role: String = ""): Text = Text(role).apply {
        font = AnimationRoleLabel.font
        fill = AnimationRoleLabel.fill
    }

    fun wrap(animation: Animation, role: String?): Node {
        val (box, roleText) = wrappers.getOrPut(animation) {
            val label = text()
            HBox(10.0, label, animation.getViewNode()).apply {
                alignment = Pos.CENTER_LEFT
            } to label
        }
        val show = !role.isNullOrBlank()
        roleText.text = role.orEmpty()
        roleText.isVisible = show
        roleText.isManaged = show
        return box
    }
}
