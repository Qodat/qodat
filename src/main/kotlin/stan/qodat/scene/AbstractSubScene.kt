package stan.qodat.scene

import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.scene.SubScene
import javafx.scene.input.*
import javafx.scene.layout.Region
import stan.qodat.Properties

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
abstract class AbstractSubScene {

    val subSceneProperty = SimpleObjectProperty<SubScene>()

    abstract val mouseEventHandler : EventHandler<MouseEvent>
    abstract val dragEventHandler : EventHandler<DragEvent>
    abstract val keyEventHandler : EventHandler<KeyEvent>
    abstract val scrollEventHandler : EventHandler<ScrollEvent>
    abstract val zoomEventHandler : EventHandler<ZoomEvent>

    abstract fun createSubScene(copyNodes: Boolean = true): SubScene

    fun rebuildSubScene() {

        val currentSubScene = subSceneProperty.get()

        if (currentSubScene != null) {
            // scene root cannot be null
            currentSubScene.root = Region()
            currentSubScene.camera = null
            currentSubScene.removeEventHandler(MouseEvent.ANY, mouseEventHandler)
            currentSubScene.removeEventHandler(DragEvent.ANY, dragEventHandler)
            currentSubScene.removeEventHandler(KeyEvent.ANY, keyEventHandler)
            currentSubScene.removeEventHandler(ScrollEvent.ANY, scrollEventHandler)
            currentSubScene.removeEventHandler(ZoomEvent.ANY, zoomEventHandler)
            currentSubScene.fillProperty().unbind()
        }

        val newSubScene = createSubScene()
        newSubScene.addEventHandler(MouseEvent.ANY, mouseEventHandler)
        newSubScene.addEventHandler(DragEvent.ANY, dragEventHandler)
        newSubScene.addEventHandler(KeyEvent.ANY, keyEventHandler)
        newSubScene.addEventHandler(ScrollEvent.ANY, scrollEventHandler)
        newSubScene.addEventHandler(ZoomEvent.ANY, zoomEventHandler)
        newSubScene.fillProperty().bind(Properties.subSceneBackgroundColor)
        subSceneProperty.set(newSubScene)
    }
}