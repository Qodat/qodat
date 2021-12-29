package stan.qodat.scene

import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.scene.CacheHint
import javafx.scene.Group
import javafx.scene.SubScene
import javafx.scene.input.*
import stan.qodat.Properties

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
object SubScene2D : AbstractSubScene() {

    private val root = Group()

    val contextProperty = SimpleObjectProperty<SceneContext>()

    fun init() {
        contextProperty.addListener { _, oldValue, newValue ->
            if (oldValue != null)
                root.children.remove(oldValue.getSceneNode())
            if (newValue != null)
                root.children.add(newValue.getSceneNode())
            Properties.selectedScene.set(newValue?.name)
        }
    }

    override val mouseEventHandler = EventHandler<MouseEvent> {  }
    override val dragEventHandler = EventHandler<DragEvent> {}
    override val keyEventHandler = EventHandler<KeyEvent> {}
    override val scrollEventHandler = EventHandler<ScrollEvent> {}
    override val zoomEventHandler = EventHandler<ZoomEvent> {}

    override fun createSubScene(): SubScene {
        return SubScene(root,
            Properties.subSceneInitialWidth.get(),
            Properties.subSceneInitialHeight.get()
        ).also { it.cacheHint = CacheHint.QUALITY }
    }
}