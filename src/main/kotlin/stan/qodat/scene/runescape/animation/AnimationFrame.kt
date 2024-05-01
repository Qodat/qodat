package stan.qodat.scene.runescape.animation

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.util.Duration
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.scene.transform.TransformationGroup
import stan.qodat.util.FrameTimeUtil
import stan.qodat.util.Searchable

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
sealed class AnimationFrame(
    name: String,
    duration: Int
) : TransformationGroup, Searchable, ViewNodeProvider {

    private lateinit var viewBox : HBox
    val labelProperty = SimpleStringProperty(name)
    val durationProperty = SimpleObjectProperty(FrameTimeUtil.frame(duration))

    val idProperty = SimpleIntegerProperty()
    val enabledProperty = SimpleBooleanProperty(true)

    internal fun getFileId(hexString: String): Int {
        return Integer.parseInt(hexString.substring(0, hexString.length - 4), 16)
    }

    internal fun getFrameId(hexString: String): Int {
        return Integer.parseInt(hexString.substring(hexString.length - 4), 16)
    }

    override fun durationProperty(): SimpleObjectProperty<Duration> = durationProperty

    override fun getDuration() = durationProperty().get()

    override fun getViewNode(): Node {
        if (!this::viewBox.isInitialized)
            viewBox = LabeledHBox(labelProperty)
        return viewBox
    }

    abstract fun getLength(): Long

    override fun getName() = labelProperty.get()

    override fun toString() = getName()
}

