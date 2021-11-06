package stan.qodat.javafx

import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontSmoothingType
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import stan.qodat.util.onInvalidation
import stan.qodat.util.setAndBind

fun ContextMenu.menu(name: String, init: Menu.() -> Unit) {
    val menu = Menu(name)
    menu.init()
    items.add(menu)
}
fun Menu.menuItem(name: String, action: () -> Unit) {
    items.add(MenuItem(name).apply { setOnAction { action.invoke() } })
}

fun<E> ObservableList<E>.onChange(init: ListChangeListener.Change<out E>.() -> Unit){
    addListener(ListChangeListener { init.invoke(it) })
}

fun Pane.checkBox(bindProperty: BooleanProperty, biDirectional: Boolean = false) {
    children.add(CheckBox().apply { selectedProperty().setAndBind(bindProperty, biDirectional) })
}
fun Pane.label(text: String) {
    children.add(Label(text))
}
fun Pane.label(textProperty: StringProperty) {
    children.add(Label().apply { textProperty().setAndBind(textProperty) })
}
fun<E> Pane.comboBox(text: String, values: Array<E>, bindProperty: ObjectProperty<E>, biDirectional: Boolean = false, init: ComboBox<E>.() -> Unit = {}) {
    children += createComboBox(text, values, bindProperty, biDirectional, init)
}
fun Pane.checkBox(text: String, booleanProperty: BooleanProperty, biDirectional: Boolean = false) {
    children.add(CheckBox(text).apply { selectedProperty().setAndBind(booleanProperty, biDirectional) })
}
fun TreeItem<*>.text(text: String, color: Color, init: Text.() -> Unit = {}) {
    graphic = Text(text).apply {
        fill = color
        init()
    }
}
fun TreeItem<*>.button(text: String, action: () -> Unit = {}) {
    value = Button(text).apply { setOnAction { action.invoke() } }
}
fun TreeItem<*>.label(text: String, init: Label.() -> Unit = {}) {
    value = Label(text).apply { init() }
}
fun TreeItem<*>.label(textProperty: StringProperty, init: Label.() -> Unit = {}) {
    value = Label().apply {
        textProperty().setAndBind(textProperty)
        init()
    }
}
fun TreeItem<*>.checkBox(text: String, booleanProperty: BooleanProperty, biDirectional: Boolean = false) {
    value = CheckBox(text).apply { selectedProperty().setAndBind(booleanProperty, biDirectional) }
}

fun<E> TreeItem<*>.comboBox(text: String, values: Array<E>, bindProperty: ObjectProperty<E>, biDirectional: Boolean = false, init: ComboBox<E>.() -> Unit = {}) {
    value = createComboBox(text, values, bindProperty, biDirectional, init)
}

fun TreeItem<*>.hBox(spacing: Double = 5.0, alignment: Pos = Pos.CENTER_LEFT, init: HBox.() -> Unit) {
    value = HBox(spacing).apply {
        this.alignment = alignment
        init()
    }
}
fun TreeItem<*>.vBox(spacing: Double = 5.0, alignment: Pos = Pos.CENTER_LEFT, init: VBox.() -> Unit) {
    value = VBox(spacing).apply {
        this.alignment = alignment
        init()
    }
}

fun TreeItem<*>.onExpanded(action: Boolean.() -> Unit) {
    expandedProperty().onInvalidation { action.invoke(value) }
}
fun<T> TreeItem<T>.treeItem(text: String? = null, init: TreeItem<T>.() -> Unit) {
    children.add(TreeItem<T>().apply {
        if (text != null)
            label(text)
        init()
    })
}

fun<T> SelectionModel<T>.onSelected(onSelected: (T?, T?) -> Unit) {
    selectedItemProperty().addListener { _, oldValue, newValue ->
        onSelected.invoke(oldValue, newValue)
    }
}

fun TextFlow.text(vararg pairs: Pair<String, Color>) {
    for ((string, color) in pairs) {
        children.add(Text(string).apply {
            if (font != null)
                this.font = font
            fontSmoothingType = FontSmoothingType.GRAY
            fill = color
        })
    }
}

fun TextFlow.menloText(vararg pairs: Pair<String, Color>) {
    children.addAll(creeateMenloText(*pairs))
}

fun creeateMenloText(vararg pairs: Pair<String, Color>) =
    pairs.map { (string, color) ->
        Text(string).apply {
            font = Font.font("Menlo", 13.0)
            fontSmoothingType = FontSmoothingType.GRAY
            fill = color
        }
    }

private fun<E> createComboBox(text: String, values: Array<E>, bindProperty: ObjectProperty<E>, biDirectional: Boolean = false, init: ComboBox<E>.() -> Unit = {}) = ComboBox(FXCollections.observableArrayList(*values)).apply {
    promptText = text
    selectionModel.select(bindProperty.get())
    if (biDirectional) {
        selectionModel.onSelected { _, newValue ->
            if (bindProperty.get() != newValue)
                bindProperty.set(newValue)
        }
        bindProperty.addListener { _, _, newValue ->
            if (selectionModel.selectedItem != newValue)
                selectionModel.select(newValue)
        }
    } else
        bindProperty.bind(selectionModel.selectedItemProperty())
    init()
}