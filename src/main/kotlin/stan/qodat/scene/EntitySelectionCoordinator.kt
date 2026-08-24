package stan.qodat.scene

import javafx.animation.PauseTransition
import javafx.application.Platform
import javafx.util.Duration
import stan.qodat.scene.provider.SceneNodeProvider
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.util.PerfTrace

/**
 * Serialises list selection onto a single browse slot.
 *
 * Arrow-key spam used to fire unselect/select through two capacity-0 actors;
 * `trySend` dropped events and left two NPCs in the scene. This coordinator
 * keeps one 3D preview, coalesces queued key-repeat onto the latest entity,
 * and only builds the scene tree / extra lists after the user pauses ([SETTLE_MS]).
 */
class EntitySelectionCoordinator(
    private val sceneContext: SceneContext,
) {

    private var generation = 0L
    private var pending: ViewNodeProvider? = null
    private var flushScheduled = false
    private val settle = PauseTransition(Duration.millis(SETTLE_MS))

    var onPreview: ((ViewNodeProvider, Long) -> Unit)? = null
    var onSettled: ((ViewNodeProvider, Long) -> Unit)? = null

    init {
        settle.setOnFinished {
            val node = pending ?: return@setOnFinished
            val gen = generation
            if (node is SceneNodeProvider)
                PerfTrace.span("select.attachTree") { sceneContext.attachBrowseTree(node) }
            onSettled?.invoke(node, gen)
        }
    }

    fun isCurrent(generation: Long): Boolean = this.generation == generation

    fun select(node: ViewNodeProvider) {
        generation++
        pending = node
        requestFlush()
        settle.playFromStart()
    }

    fun unselect(node: ViewNodeProvider) {
        if (pending === node) {
            pending = null
            settle.stop()
        }
        if (node is SceneNodeProvider)
            sceneContext.removeBrowseNodeIfCurrent(node)
    }

    private fun requestFlush() {
        if (flushScheduled)
            return
        flushScheduled = true
        Platform.runLater {
            flushScheduled = false
            val node = pending ?: return@runLater
            val gen = generation
            if (node is SceneNodeProvider) {
                val start = PerfTrace.begin()
                sceneContext.replaceBrowseNode(node)
                PerfTrace.end("select.preview3d", start)
            }
            onPreview?.invoke(node, gen)
            if (pending !== node)
                requestFlush()
        }
    }

    companion object {
        const val SETTLE_MS = 80.0
    }
}
