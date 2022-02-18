package stan.qodat.scene.controller

import fxyz3d.geometry.Point3F
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Point3D
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.paint.PhongMaterial
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.cache.impl.qodat.QodatNpcDefinition
import stan.qodat.javafx.onSelected
import stan.qodat.scene.control.AutoCompleteTextField
import stan.qodat.scene.control.ViewNodeListView
import stan.qodat.scene.paint.TextureMaterial
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.entity.NPC
import stan.qodat.scene.runescape.model.Model
import stan.qodat.scene.runescape.model.ModelFaceMesh
import stan.qodat.util.ModelUtil
import stan.qodat.util.ModelUtil.encode
import stan.qodat.util.Searchable
import stan.qodat.util.setAndBind
import java.net.URL
import java.util.*


/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class EditorController : EntityViewController("editor-scene") {

    @FXML lateinit var selectedFaceId: Label
    @FXML lateinit var hoveredFaceId: Label
    @FXML lateinit var selectedTextureImage: ImageView

    @FXML lateinit var editTabPane: TabPane
    
    @FXML lateinit var textureList: ListView<TextureMaterial>
    @FXML lateinit var saveChangesButton: Button
    @FXML lateinit var brushColorPicker: ColorPicker
    @FXML lateinit var colorsList: ListView<Any>
    @FXML lateinit var addNpcButton: Button

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        onEntitySelected = { entity ->
            modelController.models.setAll(*entity.getModels())
            for (model in entity.getModels()) {
                model.editProperty.set {
                    when (editTabPane.selectionModel.selectedItem.text) {
                        "Animations" -> {}
                        "Recolor" -> handleColorBrush(model)
                        "Textures" -> handleTextureBrush(model)
                    }
                }
            }
            configureColorPicker(entity)
        }
        super.initialize(location, resources)
        cacheProperty().addListener { _ ->
            val texture = TextureMaterial(OldschoolCacheRuneLite.getTexture(40))
            textureList.items.setAll(texture)
            textureList.selectionModel.onSelected { _, current ->
                if (current != null) {
                    selectedTextureImage.imageProperty().unbind()
                    selectedTextureImage.imageProperty().setAndBind(current.imageProperty)
                }
            }
        }

        addNpcButton.setOnMouseClicked {

            val dialog = Dialog<AnimatedEntityBuildResult>().apply {
                title = "Npc Builder"
                headerText = "Please enter details about the new NPC."
                isResizable = true

                val nameField = TextField()

                val models = FXCollections.observableArrayList<Model>()

                val modelList = ViewNodeListView<Model>().apply {
                    items = models
                    enableDragAndDrop(
                        fromFile = { Model.fromFile(this) },
                        onDropFrom = {
                            for ((_, model) in it)
                                models.add(model)
                        },
                        supportedExtensions = Model.supportedExtensions
                    )
                }

                val animationList = ListView<Animation>().apply {
                    cellFactory = Animation.createCellFactory()
                }

                val addButton = ButtonType("Add", ButtonBar.ButtonData.OK_DONE)
                val copyFromTextField = AutoCompleteTextField()

                val npcMap = Qodat.mainController.viewerController.npcs.associateBy { it.getName() }

                copyFromTextField.entries.addAll(npcMap.keys)

                copyFromTextField.textProperty().addListener { observable, oldValue, newValue ->
                    if (newValue != oldValue && newValue.isNotBlank()) {
                        val npc = npcMap[newValue]
                        if (npc != null) {
                            models.setAll(*npc.getDistinctModels())
                            animationList.items.setAll(*npc.getAnimations())
                        }
                    }
                }
                dialogPane.apply {
                    content = GridPane().apply {

                        vgap = 10.0

                        add(Label("Name"), 1, 1)
                        add(nameField, 2, 1)

                        add(Label("Copy From"), 1, 2)
                        add(copyFromTextField, 2, 2)

                        add(Label("Models"), 1, 3)
                        add(Label("Animations"), 2, 3)

                        add(modelList, 1, 4)
                        add(animationList, 2, 4)

                        add(Button("Add Model"), 1, 5)
                        add(Button("Add Animations"), 2, 5)
                    }
                    buttonTypes.add(addButton)
                }
                setResultConverter {
                    if (it == addButton) {
                        val npcName = nameField.text
                        AnimatedEntityBuildResult(
                            name = nameField.text,
                            models = modelList.items.toTypedArray(),
                            animations = animationList.items.toTypedArray()
                        )
                    } else
                        null
                }
            }

            dialog.showAndWait().ifPresent { result ->

                // overwrites if exists atm
                result.models.forEach(cache::add)

                val npcDefinition = QodatNpcDefinition(result.name, result.models.ids, result.animations.ids)
                val npc = NPC(cache, npcDefinition, animationController)

                cache.add(npc)
                npcs.add(npc)
            }
        }

    }

    private fun ModelFaceMesh.EditContext.handleColorBrush(model: Model) {
        when (mouseEvent.eventType) {
            MouseEvent.MOUSE_ENTERED -> {
                if (mouseEvent.isControlDown) {
                    mesh.previousMaterialProperty.set(material)
                    brushColorPicker.value = material.diffuseColor
                } else {
                    changeMaterial(PhongMaterial(brushColorPicker.value))
                }
            }
            MouseEvent.MOUSE_EXITED -> {
                revertMaterialChange()
            }
            MouseEvent.MOUSE_CLICKED -> {
                changeMaterial(PhongMaterial(brushColorPicker.value))
                model.modelDefinition.getFaceColors()[mesh.face] = material.diffuseColor.encode().toShort()
            }
        }
    }

    private fun ModelFaceMesh.EditContext.handleTextureBrush(model: Model) {
        val selectedTexture = textureList.selectionModel.selectedItem?:return
        when (mouseEvent.eventType) {
            MouseEvent.MOUSE_ENTERED -> {
                val u = FloatArray(3)
                val v = FloatArray(3)

                val def = model.modelDefinition
                val texCon = def.getFaceTextureConfigs()?.get(mesh.face)?.toInt()?:0

                val (t1, t2, t3) = if (texCon != -1) {
                    val t1 = def.getTextureTriangleVertexIndices1()?.get(texCon)?.toInt()?:0
                    val t2 = def.getTextureTriangleVertexIndices2()?.get(texCon)?.toInt()?:0
                    val t3 = def.getTextureTriangleVertexIndices3()?.get(texCon)?.toInt()?:0
                    Triple(model.getPoint(t1), model.getPoint(t2), model.getPoint(t3))
                } else
                    Triple(
                        Point3F(0F, 10F, 10F),
                        Point3F(0F, 10F, 10F),
                        Point3F(0F, 10F, 10F),
                    )

                val (v1, v2, v3) = model.getVertices(mesh.face)
                hoveredFaceId.text = "${mesh.face} \t$v1\t$v2\t$v3\t|\t$t1\t$t2\t$t3"
                model.mapToUV(
                    v,
                    u,
                    mesh.face,
                    t1,
                    t2,
                    t3
                )
                mesh.texCoords.setAll(
                    u[0], v[0],
                    u[1], v[1],
                    u[2], v[2]
                )
//                changeMaterial(selectedTexture)
            }
            MouseEvent.MOUSE_EXITED -> {
                revertMaterialChange()
            }
            MouseEvent.MOUSE_CLICKED -> {
//                val (v1, v2, v3) = model.getVertices(mesh.face)
//                selectedFaceId.text = "${mesh.face} \t$v1\t$v2\t$v3"
//                changeMaterial(selectedTexture)
//                mesh.previousMaterialProperty.set(selectedTexture)
            }
        }
    }

    private fun configureColorPicker(it: Entity<*>) {
        colorsList.items.clear()
        it.definition.findColor?.run {
            for (i in indices) {
                val find = ColorPicker(ModelUtil.hsbToColor(it.definition.findColor!![i], null))
                val replace = ColorPicker(ModelUtil.hsbToColor(it.definition.replaceColor!![i], null))
                replace.valueProperty().addListener { _, oldColor, newColor ->
                    if (oldColor != newColor) {
                        val coloro = newColor.encode()
                        println(
                            "hex: ${
                                String.format(
                                    "#%02X%02X%02X",
                                    (newColor.red * 255).toInt(),
                                    (newColor.green * 255).toInt(),
                                    (newColor.blue * 255).toInt()
                                )
                            }"
                        )
                        println("rs2: $coloro")
                        it.definition.replaceColor!![i] = coloro.toShort()
                        for (model in it.getModels()) {
                            model.recolor()
                        }
                    }
                }
                find.isEditable = false
                val box = HBox(find, replace)
                colorsList.items.add(box)
            }
        }
    }

    override fun cacheProperty() = Properties.editorCache

    data class AnimatedEntityBuildResult(
        val name: String,
        val models: Array<Model>,
        val animations: Array<Animation>
    )

    private val <T : Searchable> Array<T>.ids: Array<String>
        get() = map { it.getName() }.toTypedArray()

    fun Model.mapToUV(
        v: FloatArray,
        u: FloatArray,
        face: Int,
        triangleVertexIdx1: Point3D,
        triangleVertexIdx2: Point3D,
        triangleVertexIdx3: Point3D
    ) {
        val (v1, v2, v3) = getVertices(face)
        val x1 = getX(v1); val y1 = getY(v1); val z1 = getZ(v1)
        val x2 = getX(v2); val y2 = getY(v2); val z2 = getZ(v2)
        val x3 = getX(v3); val y3 = getY(v3); val z3 = getZ(v3)
        val triangleX: Float = triangleVertexIdx1.x.toFloat()
        val triangleY: Float = triangleVertexIdx1.y.toFloat()
        val triangleZ: Float = triangleVertexIdx1.z.toFloat()
        val f_882_: Float = triangleVertexIdx2.x.toFloat() - triangleX
        val f_883_: Float = triangleVertexIdx2.y.toFloat() - triangleY
        val f_884_: Float = triangleVertexIdx2.z.toFloat() - triangleZ
        val f_885_: Float = triangleVertexIdx3.x.toFloat() - triangleX
        val f_886_: Float = triangleVertexIdx3.y.toFloat() - triangleY
        val f_887_: Float = triangleVertexIdx3.z.toFloat() - triangleZ
        val f_888_: Float = x1 - triangleX
        val f_889_: Float = y1 - triangleY
        val f_890_: Float = z1 - triangleZ
        val f_891_: Float = x2 - triangleX
        val f_892_: Float = y2 - triangleY
        val f_893_: Float = z2 - triangleZ
        val f_894_: Float = x3 - triangleX
        val f_895_: Float = y3 - triangleY
        val f_896_: Float = z3 - triangleZ
        val f_897_ = f_883_ * f_887_ - f_884_ * f_886_
        val f_898_ = f_884_ * f_885_ - f_882_ * f_887_
        val f_899_ = f_882_ * f_886_ - f_883_ * f_885_
        var f_900_ = f_886_ * f_899_ - f_887_ * f_898_
        var f_901_ = f_887_ * f_897_ - f_885_ * f_899_
        var f_902_ = f_885_ * f_898_ - f_886_ * f_897_
        var f_903_ = 1.0f / (f_900_ * f_882_ + f_901_ * f_883_ + f_902_ * f_884_)
        u[0] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_
        u[1] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_
        u[2] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_
        f_900_ = f_883_ * f_899_ - f_884_ * f_898_
        f_901_ = f_884_ * f_897_ - f_882_ * f_899_
        f_902_ = f_882_ * f_898_ - f_883_ * f_897_
        f_903_ = 1.0f / (f_900_ * f_885_ + f_901_ * f_886_ + f_902_ * f_887_)
        v[0] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_
        v[1] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_
        v[2] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_
    }
}