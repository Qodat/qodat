package stan.qodat.scene.controller

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.impl.displee.DispleeCache
import stan.qodat.cache.impl.qodat.QodatCache
import stan.qodat.cache.impl.qodat.QodatNpcDefinition
import stan.qodat.scene.control.AutoCompleteTextField
import stan.qodat.scene.control.ViewNodeListView
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationLegacy
import stan.qodat.scene.runescape.entity.NPC
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.Searchable
import tornadofx.*
import java.net.URL
import java.util.*


/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class EditorController : EntityViewController("editor-scene") {

    @FXML
    lateinit var addNpcButton: Button

    private val modelFileFilters = arrayOf(
        FileChooser.ExtensionFilter("RuneScape Model File", "*.model", "*.dat"),
        FileChooser.ExtensionFilter("Metasequoia Model File", "*.mqo"),
    )
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        super.initialize(location, resources)
        npcList.contextmenu {
            item("Delete") {
                setOnAction {
                    val selectedNpc = npcList.selectedItem?:return@setOnAction
                    npcs.remove(selectedNpc)
                    (cache as? QodatCache)?.remove(selectedNpc)
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
                    contextmenu {
                        item("Delete") {
                            setOnAction {

                                selectedItem?.apply(models::remove)
                            }
                        }
                    }
                    enableDragAndDrop(
                        fromFile = { Model.fromFile(this) },
                        onDropFrom = {
                            for ((file, model) in it) {
                                models.add(model)
                                val currentName = nameField.text
                                if (currentName == null || currentName.isBlank())
                                    nameField.text = file.nameWithoutExtension
                            }
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
                copyFromTextField.textProperty().addListener { _, oldValue, newValue ->
                    if (newValue != oldValue && newValue.isNotBlank()) {
                        val npc = npcMap[newValue]
                        if (npc != null) {
                            if (Properties.copyModelsFromNpc.get())
                                models.setAll(*npc.getDistinctModels())
                            if (Properties.copyAnimationsFromNpc.get())
                                animationList.items.setAll(npc.getAnimations().map(Animation::copy))
                        }
                    }
                }
                dialogPane.apply {
                    content = GridPane().apply {

                        vgap = 10.0

                        add(Label("Name"), 1, 1)
                        add(nameField, 2, 1)

                        add(Label("Copy From"), 1, 2)
                        add(VBox(5.0).apply {
                            children.add(copyFromTextField)
                            checkbox("Copy Models", Properties.copyModelsFromNpc)
                            checkbox("Copy Animations", Properties.copyAnimationsFromNpc)
                        }, 2, 2)

                        add(Label("Models"), 1, 3)
                        add(Label("Animations"), 2, 3)

                        add(modelList, 1, 4)
                        add(animationList, 2, 4)

                        add(Button("Add Model").apply {
                            setOnAction {
                                chooseFile("Choose model file", filters = modelFileFilters, mode = FileChooserMode.Multi)
                                    .map { Model.fromFile(it) }
                                    .forEach(models::add)
                            }
                        }, 1, 5)
                        add(HBox(5.0).apply {
                            val textField = textfield {
                                stripNonInteger()
                            }
                            button("Add Animation") {
                                setOnAction {
                                    val animationDefinition = DispleeCache.getAnimationDefinitions().find { it.id == textField.text }
                                    if (animationDefinition != null) {
                                        val animation = AnimationLegacy(animationDefinition.id, animationDefinition, DispleeCache)
                                        animationList.items.add(animation)
                                    }
                                }
                            }
                        }, 2, 5)
                    }
                    buttonTypes.add(addButton)
                }
                setResultConverter {
                    if (it == addButton) {
                        AnimatedEntityBuildResult(
                            name = nameField.text,
                            models = modelList.items.toTypedArray(),
                            animations = animationList.items.toTypedArray()
                        )
                    } else
                        null
                }
            }
            dialog.initOwner(addNpcButton.scene.window)
            dialog.showAndWait().ifPresent { result ->

                // overwrites if exists atm
                result.models.forEach(cache::add)

                val npcDefinition = QodatNpcDefinition(result.name, result.models.ids, result.animations.ids)
                val npc = NPC(cache, npcDefinition, animationController::resolve)

                cache.add(npc)
                npcs.add(npc)

                npcList.selectionModel.select(npc)
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
}
