import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.scene.control.TextField
import javafx.scene.input.KeyEvent

// helper text field subclass which restricts text input to a given range of natural int numbers
// and exposes the current numeric int value of the edit box as a value property.
class IntField(minValue: Int, maxValue: Int, initialValue: Int) : TextField() {

    private val value: IntegerProperty
    private val minValue: Int
    private val maxValue: Int

    // expose an integer value property for the text field.
    fun getValue(): Int {
        return value.value
    }

    fun setValue(newValue: Int) {
        value.value = newValue
    }

    fun valueProperty(): IntegerProperty {
        return value
    }

    init {
        require(minValue <= maxValue) { "IntField min value $minValue greater than max value $maxValue" }
        require(maxValue >= minValue) { "IntField max value $minValue less than min value $maxValue" }
        require(minValue <= initialValue && initialValue <= maxValue) { "IntField initialValue $initialValue not between $minValue and $maxValue" }

        // initialize the field values.
        this.minValue = minValue
        this.maxValue = maxValue
        value = SimpleIntegerProperty(initialValue)
        text = initialValue.toString() + ""
        val intField = this

        // make sure the value property is clamped to the required range
        // and update the field's text to be in sync with the value.
        value.addListener { _, _, newValue ->
            if (newValue == null) {
                intField.text = ""
            } else {
                if (newValue.toInt() < intField.minValue) {
                    value.setValue(intField.minValue)
                    return@addListener
                }
                if (newValue.toInt() > intField.maxValue) {
                    value.setValue(intField.maxValue)
                    return@addListener
                }
                if (newValue.toInt() == 0 && (textProperty().get() == null || "" == textProperty().get())) {
                    // no action required, text property is already blank, we don't need to set it to 0.
                } else {
                    intField.text = newValue.toString()
                }
            }
        }


        // restrict key input to numerals.
        this.addEventFilter(KeyEvent.KEY_TYPED) { keyEvent ->
            if (intField.minValue < 0) {
                if (!"-0123456789".contains(keyEvent.character)) {
                    keyEvent.consume()
                }
            } else {
                if (!"0123456789".contains(keyEvent.character)) {
                    keyEvent.consume()
                }
            }
        }

        // ensure any entered values lie inside the required range.
        textProperty().addListener { observableValue, oldValue, newValue ->
            if (newValue == null || "" == newValue || intField.minValue < 0 && "-" == newValue) {
                value.setValue(0)
                return@addListener
            }
            val intValue = newValue.toInt()
            if (intField.minValue > intValue || intValue > intField.maxValue) {
                textProperty().value = oldValue
            }
            value.set(textProperty().get().toInt())
        }
    }
}