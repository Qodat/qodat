package stan.qodat.scene.control

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import stan.qodat.Properties
import stan.qodat.util.LabelMapping
import tornadofx.*
import kotlin.error

/**
 * Represents a [HBox] containing a [Label] bound to the text property.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class LabeledHBox(
    private val originalValueProperty: SimpleStringProperty,
    spacing: Double = 10.0,
    alignment: Pos = Pos.CENTER_LEFT,
    private val labelPrefix: String? = null
) : HBox(spacing) {

    private val actualTextProperty = stringProperty(mappingOrOriginal())

    val editableValueProperty = stringProperty(mappingOrNull()?:"").apply {
        onChange {
            if (it != null) {
                LabelMapping[key()] = it
                actualTextProperty.value = it.ifBlank { originalValueProperty.value }
            }
        }
    }

    private fun mappingOrOriginal() =
        mappingOrNull()?: originalValueProperty.value
    private fun mappingOrNull() =
        labelPrefix?.let { LabelMapping[key()] }
    private fun key() =
        "${labelPrefix ?: error("Label prefix is required for editable labeled hbox!")}.${originalValueProperty.value}"

    val editableProperty = booleanProperty(false)

    init {
        this.alignment = alignment
        editableProperty.onChange { updateChildren() }
        focusedProperty().onChange { if (!it) editableProperty.set(false) }
        updateChildren()
    }

    private fun LabeledHBox.updateChildren() {
        children.clear()
        if (editableProperty.get()) {
            textfield(editableValueProperty) {
                focusedProperty().onChange {
                    if (!it)
                        editableProperty.set(false)
                }
                setOnKeyPressed {
                    if (it.code.name == "ENTER")
                        editableProperty.set(false)
                }
            }
        } else {
            if (actualTextProperty.value.isNullOrBlank()) {
                actualTextProperty.value = originalValueProperty.value
            }
            text(actualTextProperty) {
                font = Font.font("Menlo", FontWeight.BOLD, 13.0)
                fill = Color(214 / 255.0, 214 / 255.0, 214 / 255.0, 1.0)
                if (labelPrefix != null) {
                    onDoubleClick {
                        editableProperty.set(true)
                    }
                }
            }
            if (actualTextProperty.value != originalValueProperty.value && Properties.showIdAfterLabel.value) {
                text(originalValueProperty) {
                    font = Font.font("Menlo", FontWeight.EXTRA_LIGHT, 13.0)
                    fill = Color.web("#A4B8C8")
                }
            }
        }
    }
}

