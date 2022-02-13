package stan.qodat.scene.controller

import javafx.fxml.Initializable
import stan.qodat.Properties
import stan.qodat.event.SelectedTabChangeEvent
import stan.qodat.scene.SceneContext
import stan.qodat.scene.provider.SceneNodeProvider
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.util.setAndBind

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
abstract class SceneController(val name: String)
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
            if (!Properties.lockScene.value) {
                if (it.selected)
                    selectThisContext()
                else if (!it.otherSelected)
                    SubScene3D.contextProperty.set(null)
            }
        }
    }

    internal fun selectThisContext() {
        val oldContext = SubScene3D.contextProperty.get()
        SubScene3D.contextProperty.set(sceneContext)
        if (oldContext != sceneContext)
            oldContext?.getController()?.onSwitch(this)
        onSelect(oldContext?.getController())
    }

    abstract fun onSwitch(next: SceneController)

    open fun onSelect(old: SceneController?) = Unit

    override fun getSceneNode() = sceneContext.getSceneNode()
}