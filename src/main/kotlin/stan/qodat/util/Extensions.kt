package stan.qodat.util

import javafx.beans.Observable
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.scene.control.ComboBox
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.paint.Material
import javafx.scene.shape.Shape3D
import stan.qodat.Qodat
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.shape.PolygonMeshView

fun Qodat.Companion.getAnimations() : ObservableList<Animation> =
    mainController.viewerController.animationController.animations

fun Qodat.Companion.getAnimationsView() : ListView<Animation> =
    mainController.viewerController.animationController.animationsListView

//fun Qodat.Companion.addTo3DScene(node: Node) {
//    SubScene3D.contextProperty.get().getController().getSceneNode().children.add(node)
//}
//fun Qodat.Companion.removeFrom3DScene(node: Node) {
//    SubScene3D.contextProperty.get().getController().getSceneNode().children.remove(node)
//}

fun Qodat.Companion.addSceneTreeItem(provider: TreeItemProvider) {
    mainController.sceneTreeView.root.children.add(
        provider.getTreeItem(mainController.sceneTreeView)
    )
}
fun Qodat.Companion.removeSceneTreeItem(provider: TreeItemProvider) {
    mainController.sceneTreeView.root.children.remove(
        provider.getTreeItem(mainController.sceneTreeView)
    )
}
inline fun<reified N> Collection<*>.filterAndMap() : List<N> {
    val list = ArrayList<N>()
    for (old in this){
        if (old is N) {
            list.add(old)
        }
    }
    return list
}

fun ListView<*>.onIndexSelected(func: Int.() -> Unit) {
    selectionModel.selectedIndexProperty().addListener { _, _, newValue ->
        func.invoke(newValue.toInt())
    }
}

fun<T> ListView<T>.onItemSelected(func: (T?, T?) -> Unit) {
    selectionModel.selectedItemProperty().addListener { _, oldValue : T?, newValue :T? ->
        func.invoke(oldValue, newValue)
    }
}

fun<O : Observable> O.onInvalidation(func : O.() -> Unit) {
    addListener {
        func.invoke(this)
    }
}

fun<S : Searchable> TextField.configureSearchFilter(filteredList: FilteredList<S>) {
    textProperty().addListener { _, _, newValue ->
        if (newValue != null && newValue.isEmpty())
            filteredList.setPredicate { true }
        else {
            filteredList.setPredicate { it.getName().contains(newValue, ignoreCase = true) }
        }
    }
}


fun<M : Material> PolygonMeshView.setAndBindMaterial(materialProperty: ObjectProperty<M>){
    this.materialProperty.set(materialProperty.get())
    this.materialProperty.bind(materialProperty)
}

fun<M : Material> Shape3D.setAndBindMaterial(materialProperty: ObjectProperty<M>) {
    this.materialProperty().set(materialProperty.get())
    this.materialProperty().bind(materialProperty)
}

fun<P, O : Property<P>> O.setAndBind(newProperty: ObservableValue<P>) {
    value = newProperty.value
    bind(newProperty)
}

fun<P, O : Property<P>> O.setAndBind(newProperty: O, biDirectional: Boolean = false) {
    value = newProperty.value
    if (biDirectional)
        bindBidirectional(newProperty)
    else
        bind(newProperty)
}

fun<T, P : Property<T>> ComboBox<T>.bind(property: P) {
    selectionModel.selectedItemProperty().onInvalidation {
        property.value = value
    }
    property.onInvalidation {
        selectionModel.select(value)
    }
}