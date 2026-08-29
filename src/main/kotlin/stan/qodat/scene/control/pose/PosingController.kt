package stan.qodat.scene.control.pose

import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import stan.qodat.Properties
import stan.qodat.scene.SceneContext
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.gizmo.GizmoAxisType
import stan.qodat.scene.control.gizmo.GizmoMath
import stan.qodat.scene.control.gizmo.GizmoPick
import stan.qodat.scene.control.gizmo.TransformGizmo
import stan.qodat.scene.runescape.model.Model
import java.util.IdentityHashMap
import kotlin.math.hypot

/**
 * Posing mode: stick skeleton on scene models, click a joint, drag the gizmo
 * to rotate / translate that bone (FK to descendants).
 *
 * All press / drag / release goes through the SubScene mouse listener so a
 * thin handle cannot cancel the stroke when the cursor leaves it, and so
 * the mesh does not steal the pick from the gizmo.
 */
object PosingController {

    private val overlays = IdentityHashMap<Model, SkeletonOverlay>()
    private val gizmo = TransformGizmo()
    private var selectedModel: Model? = null
    private var selectedGroup: Int? = null
    private var sceneChildrenListener: ListChangeListener<Node>? = null
    private val mouseHandler = EventHandler<MouseEvent> { event ->
        when (event.eventType) {
            MouseEvent.MOUSE_PRESSED -> onPressed(event)
            MouseEvent.MOUSE_DRAGGED -> onDragged(event)
            MouseEvent.MOUSE_RELEASED -> onReleased(event)
        }
    }

    init {
        gizmo.onTranslate = { dx, dy, dz ->
            applyDelta { model, group ->
                model.pose.addTranslate(group, dx.toInt(), dy.toInt(), dz.toInt())
            }
        }
        gizmo.onRotate = { degX, degY, degZ ->
            applyDelta { model, group ->
                model.pose.addRotate(
                    group,
                    GizmoMath.degreesToRs(degX),
                    GizmoMath.degreesToRs(degY),
                    GizmoMath.degreesToRs(degZ)
                )
            }
        }
    }

    fun install() {
        Properties.posingMode.addListener { _, _, on ->
            if (on) enable() else disable()
        }
        Properties.selectedEntity.addListener { _, _, _ ->
            if (Properties.posingMode.get()) refreshOverlays()
        }
        Properties.selectedAnimation.addListener { _, _, _ ->
            if (Properties.posingMode.get()) refreshOverlays()
        }
        SubScene3D.contextProperty.addListener { _, oldContext, newContext ->
            unwatch(oldContext)
            watch(newContext)
            if (Properties.posingMode.get()) refreshOverlays()
        }
        watch(SubScene3D.contextProperty.get())
        if (Properties.posingMode.get())
            enable()
    }

    fun resetPose() {
        val models = sceneModels()
        for (model in models)
            model.pose.clear()
        for (model in models)
            model.applyPoseAndRefresh()
        placeGizmo()
    }

    fun selectBone(model: Model, groupIndex: Int) {
        selectedModel = model
        selectedGroup = groupIndex
        for ((owner, overlay) in overlays)
            overlay.highlight(if (owner === model) groupIndex else null)
        attachGizmo(model)
        placeGizmo()
    }

    internal fun selectedBone(): Pair<Model, Int>? {
        val model = selectedModel ?: return null
        val group = selectedGroup ?: return null
        return model to group
    }

    private fun onPressed(event: MouseEvent) {
        if (!event.isPrimaryButtonDown)
            return
        val hit = pickGizmo(event)
        if (hit != null) {
            gizmo.beginDrag(hit.first, hit.second)
            event.consume()
            return
        }
        if (gizmo.isDragging()) {
            event.consume()
            return
        }
        val joint = pickJoint(event)
        if (joint != null) {
            selectBone(joint.first, joint.second)
            event.consume()
        }
    }

    private fun onDragged(event: MouseEvent) {
        if (!gizmo.isDragging() || gizmo.parent == null)
            return
        val camera = SubScene3D.cameraHandler.camera
        val parent = gizmo.parent
        gizmo.updateScreenScale(
            GizmoPick.cameraDistance(camera, parent, gizmo.translateX, gizmo.translateY, gizmo.translateZ)
        )
        gizmo.dragScreen(parent, event.screenX, event.screenY)
        event.consume()
    }

    private fun onReleased(event: MouseEvent) {
        if (!gizmo.isDragging())
            return
        gizmo.endDrag()
        event.consume()
    }

    private fun pickGizmo(event: MouseEvent): Pair<TransformGizmo.Mode, GizmoAxisType>? {
        val parent = gizmo.parent ?: return null
        if (selectedModel == null || selectedGroup == null)
            return null
        return gizmo.pickScreen(parent, event.screenX, event.screenY)
    }

    private fun pickJoint(event: MouseEvent): Pair<Model, Int>? {
        var best: Pair<Model, Int>? = null
        var bestDist = JOINT_PICK_PIXELS
        for ((model, overlay) in overlays) {
            val parent = overlay.parent ?: model.getSceneNode()
            val positions = HashMap<Int, Triple<Double, Double, Double>>()
            for (bone in overlay.skeleton.bones) {
                val joint = PoseSkeletons.jointPosition(model, overlay.skeleton, bone.groupIndex) ?: continue
                positions[bone.groupIndex] = joint
                val screen = parent.localToScreen(joint.first, joint.second, joint.third) ?: continue
                val dist = hypot(screen.x - event.screenX, screen.y - event.screenY)
                if (dist < bestDist) {
                    bestDist = dist
                    best = model to bone.groupIndex
                }
            }
            for (bone in overlay.skeleton.bones) {
                val parentGroup = bone.parentGroupIndex ?: continue
                val from = positions[parentGroup] ?: continue
                val to = positions[bone.groupIndex] ?: continue
                val a = parent.localToScreen(from.first, from.second, from.third) ?: continue
                val b = parent.localToScreen(to.first, to.second, to.third) ?: continue
                val dist = GizmoMath.pointToSegment2D(
                    event.screenX, event.screenY, a.x, a.y, b.x, b.y
                )
                if (dist < bestDist) {
                    bestDist = dist
                    best = model to bone.groupIndex
                }
            }
        }
        return best
    }

    private fun enable() {
        refreshOverlays()
        installMouseListener()
    }

    private fun disable() {
        detachGizmo()
        selectedModel = null
        selectedGroup = null
        for ((model, overlay) in overlays) {
            model.setMeshPickable(true)
            model.getSceneNode().children.remove(overlay)
            model.skeletonOverlay = null
        }
        overlays.clear()
        if (SubScene3D.mouseListener.get() === mouseHandler)
            SubScene3D.mouseListener.set(null)
    }

    private fun refreshOverlays() {
        val live = sceneModels().toHashSet()
        val stale = overlays.keys.filter { it !in live }
        for (model in stale) {
            val overlay = overlays.remove(model) ?: continue
            model.setMeshPickable(true)
            model.getSceneNode().children.remove(overlay)
            model.skeletonOverlay = null
            if (selectedModel === model) {
                detachGizmo()
                selectedModel = null
                selectedGroup = null
            }
        }
        for (model in live) {
            model.setMeshPickable(false)
            val overlay = overlays.getOrPut(model) {
                SkeletonOverlay(model) { group -> selectBone(model, group) }.also {
                    model.skeletonOverlay = it
                    if (!model.getSceneNode().children.contains(it))
                        model.getSceneNode().children.add(it)
                }
            }
            if (!model.getSceneNode().children.contains(overlay))
                model.getSceneNode().children.add(overlay)
            model.invalidatePoseSkeleton()
            overlay.rebuild(model.poseSkeleton())
            if (model === selectedModel)
                overlay.highlight(selectedGroup)
            gizmo.toFront()
        }
        if (selectedModel != null && selectedModel !in live) {
            detachGizmo()
            selectedModel = null
            selectedGroup = null
        } else {
            placeGizmo()
        }
        installMouseListener()
    }

    private fun applyDelta(mutate: (Model, Int) -> Unit) {
        val model = selectedModel ?: return
        val group = selectedGroup ?: return
        mutate(model, group)
        model.applyPoseAndRefresh()
        placeGizmo()
    }

    private fun attachGizmo(model: Model) {
        val node = model.getSceneNode()
        if (gizmo.parent !== node) {
            (gizmo.parent as? Group)?.children?.remove(gizmo)
            if (!node.children.contains(gizmo))
                node.children.add(gizmo)
        }
        gizmo.toFront()
        installMouseListener()
    }

    private fun detachGizmo() {
        (gizmo.parent as? Group)?.children?.remove(gizmo)
        gizmo.endDrag()
    }

    private fun placeGizmo() {
        val model = selectedModel ?: return
        val group = selectedGroup ?: return
        val overlay = overlays[model]
        val joint = if (overlay != null)
            PoseSkeletons.jointPosition(model, overlay.skeleton, group)
        else
            model.groupCentroid(group)
        if (joint == null)
            return
        gizmo.setWorldPosition(joint.first, joint.second, joint.third)
        val parent = gizmo.parent ?: return
        val camera = SubScene3D.cameraHandler.camera
        gizmo.updateScreenScale(
            GizmoPick.cameraDistance(camera, parent, gizmo.translateX, gizmo.translateY, gizmo.translateZ)
        )
    }

    private fun installMouseListener() {
        SubScene3D.mouseListener.set(mouseHandler)
    }

    private fun watch(context: SceneContext?) {
        if (context == null)
            return
        val listener = ListChangeListener<Node> {
            if (Properties.posingMode.get())
                refreshOverlays()
        }
        sceneChildrenListener = listener
        context.getSceneNode().children.addListener(listener)
    }

    private fun unwatch(context: SceneContext?) {
        val listener = sceneChildrenListener ?: return
        context?.getSceneNode()?.children?.removeListener(listener)
        sceneChildrenListener = null
    }

    private fun sceneModels(): List<Model> {
        val context = SubScene3D.contextProperty.get() ?: return emptyList()
        return context.getModels().filter { it.getVertexGroups().any { group -> group.isNotEmpty() } }
    }

    private const val JOINT_PICK_PIXELS = 18.0
}
