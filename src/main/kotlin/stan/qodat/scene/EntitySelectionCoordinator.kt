package stan.qodat.scene

import javafx.animation.PauseTransition
import javafx.util.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stan.qodat.Qodat
import stan.qodat.scene.provider.SceneNodeProvider
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.util.PerfTrace

/**
 * Serialises list selection onto a single browse slot.
 *
 * Arrow-key spam used to fire unselect/select through two capacity-0 actors;
 * `trySend` dropped events and left two NPCs in the scene. This coordinator
 * keeps one 3D preview, coalesces queued key-repeat onto the latest entity,
 * and only builds the scene tree / extra lists after the user pauses ([SETTLE_MS]).
 *
 * Cache decode and model merge run on Default; [Group]/[javafx.scene.shape.MeshView]
 * attachment stays on the JavaFX thread.
 */
class EntitySelectionCoordinator(
    private val sceneContext: SceneContext,
) {

    private var generation = 0L
    private var pending: ViewNodeProvider? = null
    private var previewJob: Job? = null
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
        previewJob?.cancel()
        val gen = generation
        previewJob = Qodat.applicationScope.launch {
            try {
                if (node is Entity<*>) {
                    withContext(Dispatchers.Default) {
                        ensureActive()
                        PerfTrace.span("select.prepareModels ${node.getName()}") {
                            node.prepareModels()
                        }
                    }
                }
                if (!isCurrent(gen))
                    return@launch
                withContext(Dispatchers.JavaFx) {
                    if (!isCurrent(gen))
                        return@withContext
                    applyPreview(node, gen)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Qodat.logException("Failed to preview selection", e)
            }
        }
        settle.playFromStart()
    }

    fun unselect(node: ViewNodeProvider) {
        if (pending === node) {
            pending = null
            settle.stop()
            previewJob?.cancel()
            previewJob = null
        }
        if (node is SceneNodeProvider)
            sceneContext.removeBrowseNodeIfCurrent(node)
    }

    private fun applyPreview(node: ViewNodeProvider, gen: Long) {
        if (node is SceneNodeProvider) {
            val start = PerfTrace.begin()
            sceneContext.replaceBrowseNode(node)
            PerfTrace.end("select.preview3d", start)
        }
        onPreview?.invoke(node, gen)
    }

    companion object {
        const val SETTLE_MS = 80.0
    }
}
