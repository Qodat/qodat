package stan.qodat.javafx

import javafx.beans.property.BooleanProperty
import javafx.beans.property.StringProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
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

fun HBox.checkBox(bindProperty: BooleanProperty, biDirectional: Boolean = false) {
    children.add(CheckBox().apply { selectedProperty().setAndBind(bindProperty, biDirectional) })
}
fun HBox.label(text: String) {
    children.add(Label(text))
}
fun HBox.label(textProperty: StringProperty) {
    children.add(Label().apply { textProperty().setAndBind(textProperty) })
}
fun TreeItem<*>.setGraphic(text: String, color: Color, init: Text.() -> Unit = {}) {
    graphic = Text(text).apply {
        fill = color
        init()
    }
}
fun TreeItem<*>.setValue(textProperty: StringProperty, init: Label.() -> Unit = {}) {
    graphic = Label().apply {
        textProperty().setAndBind(textProperty)
        init()
    }
}
fun TreeItem<*>.onExpanded(action: Boolean.() -> Unit) {
    expandedProperty().onInvalidation { action.invoke(value) }
}