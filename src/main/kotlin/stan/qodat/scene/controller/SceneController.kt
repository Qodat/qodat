package stan.qodat.scene.controller

import javafx.fxml.Initializable
import stan.qodat.event.SelectedTabChangeEvent
import stan.qodat.scene.SceneContext
import stan.qodat.util.SceneNodeProvider
import stan.qodat.scene.SubScene3D
import stan.qodat.util.ViewNodeProvider
import stan.qodat.util.setAndBind

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
abstract class SceneController(name: String)
    : Initializable, SceneNodeProvider, ViewNodeProvider {

    internal val sceneContext = object : SceneContext(name) {
        override fun getController(): SceneController {
            return this@SceneController
        }
    }

    init {
        sceneContext.activeContext.setAndBind(SubScene3D.contextProperty.isEqualTo(sceneContext))
    }

    fun addTabSelectedListener(){
        getViewNode().addEventHandler(SelectedTabChangeEvent.EVENT_TYPE) {
            if (it.selected) {
                val oldContext = SubScene3D.contextProperty.get()
                oldContext?.getController()?.onSwitch(this)
                SubScene3D.contextProperty.set(sceneContext)
            } else if (!it.otherSelected)
                SubScene3D.contextProperty.set(null)
        }
    }

    abstract fun onSwitch(other: SceneController)

    override fun getSceneNode() = sceneContext.getSceneNode()
}