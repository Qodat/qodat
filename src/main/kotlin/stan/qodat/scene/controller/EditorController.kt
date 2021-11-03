package stan.qodat.scene.controller

import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.impl.qodat.QodatNpcDefinition
import stan.qodat.javafx.AutoCompleteTextField
import stan.qodat.scene.control.ViewNodeListView
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.entity.NPC
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.Searchable
import java.net.URL
import java.util.*


/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class EditorController : EntityController("editor-scene") {

    @FXML lateinit var addNpcButton: Button

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        super.initialize(location, resources)
        addNpcButton.setOnMouseClicked {

            val dialog = Dialog<AnimatedEntityBuildResult>().apply {
                title = "Npc Builder"
                headerText = "Please enter details about the new NPC."
                isResizable = true

                val nameField = TextField()
                val modelList = ViewNodeListView<Model>().apply {
                    enableDragAndDrop(
                        fromFile = { Model.fromFile(this) },
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
                            modelList.items.setAll(*npc.getModels())
                            animationList.items.setAll(*npc.getAnimations())
                        }
                    }
                }
                dialogPane.apply {
                    content = GridPane().apply {

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

    override fun cacheProperty() = Properties.editorCache

    data class AnimatedEntityBuildResult(
        val name: String,
        val models: Array<Model>,
        val animations: Array<Animation>
    )

    private val <T : Searchable> Array<T>.ids: Array<String>
        get() = map { it.getName() }.toTypedArray()

}