package stan.qodat.util

import javafx.beans.Observable
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.transformation.FilteredList
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.Region
import javafx.scene.paint.Material
import javafx.scene.shape.Shape3D
import qodat.cache.Cache
import qodat.cache.definition.ModelDefinition
import stan.qodat.Qodat
import stan.qodat.event.SelectedTabChangeEvent
import stan.qodat.scene.control.SplitSceneDividerDragRegion
import stan.qodat.scene.paint.ColorMaterial
import stan.qodat.scene.paint.TextureMaterial
import stan.qodat.scene.provider.TreeItemProvider
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.shape.PolygonMeshView

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
    provider.removeTreeItemReference()
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
            val id = newValue?.toIntOrNull()
            filteredList.setPredicate { containsName<S>(it, newValue) || (containsId<S>(it, id, newValue)) }
        }
    }
}
private fun <S : Searchable> containsName(it: S, newValue: String) = it.getName().contains(newValue, ignoreCase = true)
private fun <S : Searchable> containsId(it: S, id: Int?, newValue: String) =
    it is Entity<*> && id != null && it.definition.getOptionalId().orElse(-1).toString().contains(newValue)


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

/**
 * @param size either width or height depending on orientation.
 */
fun SplitPane.createDragSpace(
    property: DoubleProperty,
    dividerIndex: IntegerProperty,
    size: Int = 5,
    styleClass: String? = "drag-space"
) : Node {
    val region = Region().apply {
        if (styleClass != null)
            this.styleClass.add(styleClass)
        if (orientation == Orientation.HORIZONTAL) {
            maxWidth = size.toDouble()
            minWidth = maxWidth
            prefHeight = Double.POSITIVE_INFINITY
        } else {
            maxHeight = size.toDouble()
            minHeight = maxHeight
            prefWidth = Double.POSITIVE_INFINITY
        }
    }
    SplitSceneDividerDragRegion(
        splitPane = this,
        node = region,
        dividerIndex = dividerIndex,
        positionProperty = property,
    )
    return region
}

fun ToggleButton.createSelectTabListener(property: SimpleStringProperty, tabContents: ObjectProperty<Node?>, node: Node) {
    selectedProperty().addListener(createSelectTabListener(id, property, tabContents, node))
}

fun createSelectTabListener(id: String, selectProperty: SimpleStringProperty, tabContents: ObjectProperty<Node?>, node: Node): ChangeListener<Boolean> {
    return ChangeListener { _, oldValue, newValue ->

        val otherSelected = tabContents.value != node && tabContents.value != null

        if (!oldValue && newValue) {
            tabContents.set(node)
            selectProperty.set(id)
        } else if(!otherSelected) {
            tabContents.set(null)
            selectProperty.set(null)
        } else {
            selectProperty.set(id)
        }

        node.fireEvent(
            SelectedTabChangeEvent(
                selected = newValue,
                otherSelected = otherSelected)
        )
    }
}

fun ModelDefinition.getMaterial(face: Int, cache: Cache): stan.qodat.scene.paint.Material {
    val faceAlpha = getFaceAlphas()?.getOrNull(face)
    val faceColor = getFaceColors()[face]
    val faceTexture = getFaceTextures()
        ?.getOrNull(face)?.toInt()
        ?.takeIf { textureId -> textureId != -1 }
        ?.let { cache.getTexture(it) }
    if (faceTexture != null) {
        val textureMaterial = TextureMaterial(faceTexture)
        if (textureMaterial.load())
            return textureMaterial
    }
    return ColorMaterial(faceColor, faceAlpha)
}

fun Searchable.formatName() = getName().let {
    if (it.startsWith("<col=")){
        it.substringAfter(">").substringBefore("<")
    } else
        it
}
