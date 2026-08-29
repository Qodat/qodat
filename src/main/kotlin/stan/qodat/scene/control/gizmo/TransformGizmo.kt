package stan.qodat.scene.control.gizmo

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.AmbientLight
import javafx.scene.DepthTest
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import javafx.scene.shape.Shape3D
import javafx.scene.shape.TriangleMesh
import javafx.scene.transform.Rotate
import javafx.scene.transform.Scale
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

/**
 * RGB translate arrows + rotate rings. Drag math lives in [GizmoMath]; this
 * node only owns visuals, hover, and the active-handle drag session.
 */
class TransformGizmo : Group() {

    enum class Mode { TRANSLATE, ROTATE, BOTH }

    val modeProperty = SimpleObjectProperty(Mode.BOTH)
    val selectedAxis = SimpleObjectProperty<GizmoAxisType?>()
    val selectedMode = SimpleObjectProperty<Mode?>()

    var onTranslate: ((dx: Float, dy: Float, dz: Float) -> Unit)? = null
    var onRotate: ((degX: Float, degY: Float, degZ: Float) -> Unit)? = null

    private val screenScale = Scale(1.0, 1.0, 1.0)
    private val previousHit = Vector3f()
    private var hasPreviousHit = false
    private val translateAcc = FloatArray(3)
    private val rotateAcc = FloatArray(3)

    init {
        transforms.add(screenScale)
        children.addAll(
            handleGroup(Mode.TRANSLATE, GizmoAxisType.X, translateShaft(GizmoAxisType.X), translateHead(GizmoAxisType.X)),
            handleGroup(Mode.TRANSLATE, GizmoAxisType.Y, translateShaft(GizmoAxisType.Y), translateHead(GizmoAxisType.Y)),
            handleGroup(Mode.TRANSLATE, GizmoAxisType.Z, translateShaft(GizmoAxisType.Z), translateHead(GizmoAxisType.Z)),
            handleGroup(Mode.ROTATE, GizmoAxisType.X, rotateRing(GizmoAxisType.X)),
            handleGroup(Mode.ROTATE, GizmoAxisType.Y, rotateRing(GizmoAxisType.Y)),
            handleGroup(Mode.ROTATE, GizmoAxisType.Z, rotateRing(GizmoAxisType.Z)),
            centerCube(),
            AmbientLight(Color.WHITE)
        )
        modeProperty.addListener { _, _, _ -> refreshVisibility() }
        refreshVisibility()
    }

    fun isDragging(): Boolean = selectedAxis.get() != null

    fun beginDrag(mode: Mode, axis: GizmoAxisType) {
        selectedMode.set(mode)
        selectedAxis.set(axis)
        hasPreviousHit = false
    }

    /**
     * Ray-pick a translate shaft or rotate ring in the gizmo parent's space.
     * Used so posing does not depend on JavaFX 3D mesh picking.
     */
    fun pick(localRay: GizmoMath.Ray): Pair<Mode, GizmoAxisType>? {
        val origin = Vector3f(translateX.toFloat(), translateY.toFloat(), translateZ.toFloat())
        val scale = screenScale.x.toFloat().coerceAtLeast(0.01f)
        val shaft = (AXIS_LENGTH * scale).toFloat()
        val ring = RING_RADIUS * scale
        val pickR = (8f * scale).coerceAtLeast(10f)
        var best: Pair<Mode, GizmoAxisType>? = null
        var bestDist = pickR
        val mode = modeProperty.get() ?: Mode.BOTH
        if (mode == Mode.BOTH || mode == Mode.TRANSLATE) {
            for (axis in GizmoAxisType.entries) {
                val tip = Vector3f(origin).add(GizmoMath.axisVector(axis).mul(shaft))
                val dist = GizmoMath.raySegmentDistance(localRay, origin, tip)
                if (dist < bestDist) {
                    bestDist = dist
                    best = Mode.TRANSLATE to axis
                }
            }
        }
        if (mode == Mode.BOTH || mode == Mode.ROTATE) {
            for (axis in GizmoAxisType.entries) {
                val dist = GizmoMath.rayCircleDistance(localRay, origin, GizmoMath.axisVector(axis), ring)
                if (dist < bestDist) {
                    bestDist = dist
                    best = Mode.ROTATE to axis
                }
            }
        }
        return best
    }

    /**
     * Screen-space handle pick. Independent of SubScene vs Scene ray math so
     * a click on a visible shaft / ring still wins over the camera orbit.
     */
    fun pickScreen(parent: Node, screenX: Double, screenY: Double): Pair<Mode, GizmoAxisType>? {
        val scale = screenScale.x
        val origin = parent.localToScreen(translateX, translateY, translateZ) ?: return null
        var best: Pair<Mode, GizmoAxisType>? = null
        var bestDist = SCREEN_PICK_PIXELS
        val mode = modeProperty.get() ?: Mode.BOTH
        if (mode == Mode.BOTH || mode == Mode.TRANSLATE) {
            for (axis in GizmoAxisType.entries) {
                val v = GizmoMath.axisVector(axis)
                val tip = parent.localToScreen(
                    translateX + v.x * AXIS_LENGTH * scale,
                    translateY + v.y * AXIS_LENGTH * scale,
                    translateZ + v.z * AXIS_LENGTH * scale
                ) ?: continue
                val dist = GizmoMath.pointToSegment2D(
                    screenX, screenY, origin.x, origin.y, tip.x, tip.y
                )
                if (dist < bestDist) {
                    bestDist = dist
                    best = Mode.TRANSLATE to axis
                }
            }
        }
        if (mode == Mode.BOTH || mode == Mode.ROTATE) {
            for (axis in GizmoAxisType.entries) {
                val dist = ringScreenDistance(parent, axis, scale, screenX, screenY)
                if (dist < bestDist) {
                    bestDist = dist
                    best = Mode.ROTATE to axis
                }
            }
        }
        return best
    }

    fun setWorldPosition(x: Double, y: Double, z: Double) {
        translateX = x
        translateY = y
        translateZ = z
    }

    fun updateScreenScale(distance: Double) {
        val s = (distance / SCREEN_SCALE_DISTANCE).coerceIn(0.2, 6.0)
        screenScale.x = s
        screenScale.y = s
        screenScale.z = s
    }

    fun drag(localRay: GizmoMath.Ray, cameraForward: Vector3f) {
        val axis = selectedAxis.get() ?: return
        val mode = selectedMode.get() ?: return
        val axisVec = GizmoMath.axisVector(axis)
        val origin = Vector3f(translateX.toFloat(), translateY.toFloat(), translateZ.toFloat())
        val planeNormal = when (mode) {
            Mode.TRANSLATE -> GizmoMath.translatePlaneNormal(axisVec, cameraForward)
            Mode.ROTATE -> axisVec
            Mode.BOTH -> return
        }
        val hit = GizmoMath.intersectRayPlane(localRay, origin, planeNormal) ?: return
        if (!hasPreviousHit) {
            previousHit.set(hit)
            hasPreviousHit = true
            return
        }
        when (mode) {
            Mode.TRANSLATE -> emitTranslate(axis, GizmoMath.translateDelta(previousHit, hit, axisVec))
            Mode.ROTATE -> emitRotate(axis, GizmoMath.rotateDeltaDegrees(previousHit, hit, origin, axisVec))
            Mode.BOTH -> Unit
        }
        previousHit.set(hit)
    }

    fun endDrag() {
        selectedAxis.set(null)
        selectedMode.set(null)
        hasPreviousHit = false
        translateAcc.fill(0f)
        rotateAcc.fill(0f)
        restoreHandleColors()
    }

    private fun emitTranslate(axis: GizmoAxisType, delta: Float) {
        val i = axis.ordinal
        translateAcc[i] += delta
        val step = translateAcc[i].toInt()
        if (step == 0)
            return
        translateAcc[i] -= step.toFloat()
        val dx = if (axis == GizmoAxisType.X) step.toFloat() else 0f
        val dy = if (axis == GizmoAxisType.Y) step.toFloat() else 0f
        val dz = if (axis == GizmoAxisType.Z) step.toFloat() else 0f
        onTranslate?.invoke(dx, dy, dz)
    }

    private fun emitRotate(axis: GizmoAxisType, degrees: Float) {
        val i = axis.ordinal
        rotateAcc[i] += degrees
        val rs = GizmoMath.degreesToRs(rotateAcc[i])
        if (rs == 0)
            return
        rotateAcc[i] -= GizmoMath.rsToDegrees(rs)
        val dx = if (axis == GizmoAxisType.X) GizmoMath.rsToDegrees(rs) else 0f
        val dy = if (axis == GizmoAxisType.Y) GizmoMath.rsToDegrees(rs) else 0f
        val dz = if (axis == GizmoAxisType.Z) GizmoMath.rsToDegrees(rs) else 0f
        onRotate?.invoke(dx, dy, dz)
    }

    private fun ringScreenDistance(
        parent: Node,
        axis: GizmoAxisType,
        scale: Double,
        screenX: Double,
        screenY: Double
    ): Double {
        val n = GizmoMath.axisVector(axis)
        val u = when (axis) {
            GizmoAxisType.X -> Vector3f(0f, 1f, 0f)
            GizmoAxisType.Y -> Vector3f(1f, 0f, 0f)
            GizmoAxisType.Z -> Vector3f(1f, 0f, 0f)
        }
        val v = Vector3f(n).cross(u).normalize()
        val radius = RING_RADIUS * scale
        var prev: javafx.geometry.Point2D? = null
        var best = Double.POSITIVE_INFINITY
        val steps = 24
        for (i in 0..steps) {
            val a = i * Math.PI * 2.0 / steps
            val cos = kotlin.math.cos(a).toFloat()
            val sin = kotlin.math.sin(a).toFloat()
            val point = parent.localToScreen(
                translateX + (u.x * cos + v.x * sin) * radius,
                translateY + (u.y * cos + v.y * sin) * radius,
                translateZ + (u.z * cos + v.z * sin) * radius
            ) ?: continue
            val last = prev
            if (last != null) {
                val dist = GizmoMath.pointToSegment2D(
                    screenX, screenY, last.x, last.y, point.x, point.y
                )
                if (dist < best)
                    best = dist
            }
            prev = point
        }
        return best
    }

    private fun handleGroup(mode: Mode, axis: GizmoAxisType, vararg shapes: Shape3D): Group {
        val group = Group(*shapes)
        val idle = axisColor(axis)
        group.setOnMousePressed { event ->
            beginDrag(mode, axis)
            tint(shapes, idle.darker())
            event.consume()
        }
        group.setOnMouseReleased { event ->
            // Drag ends on the SubScene mouse-up so leaving the thin handle
            // mid-stroke does not cancel the pose.
            event.consume()
        }
        group.setOnMouseEntered { event ->
            if (selectedAxis.get() == null)
                tint(shapes, idle.brighter())
            event.consume()
        }
        group.setOnMouseExited { event ->
            if (selectedAxis.get() != axis)
                tint(shapes, idle)
            event.consume()
        }
        return group
    }

    private fun refreshVisibility() {
        val mode = modeProperty.get() ?: Mode.BOTH
        for (child in children) {
            if (child !is Group || child.children.isEmpty())
                continue
            val first = child.children.firstOrNull() as? Shape3D ?: continue
            val handleMode = first.userData as? Mode ?: continue
            child.isVisible = mode == Mode.BOTH || mode == handleMode
        }
    }

    private fun restoreHandleColors() {
        for (child in children) {
            if (child !is Group)
                continue
            val shapes = child.children.filterIsInstance<Shape3D>()
            val axis = shapes.firstOrNull()?.properties?.get(AXIS_KEY) as? GizmoAxisType ?: continue
            tint(shapes.toTypedArray(), axisColor(axis))
        }
    }

    private fun translateShaft(axis: GizmoAxisType): Box {
        val box = when (axis) {
            GizmoAxisType.X -> Box(AXIS_LENGTH, AXIS_WIDTH, AXIS_WIDTH).apply { translateX = AXIS_LENGTH / 2 }
            GizmoAxisType.Y -> Box(AXIS_WIDTH, AXIS_LENGTH, AXIS_WIDTH).apply { translateY = AXIS_LENGTH / 2 }
            GizmoAxisType.Z -> Box(AXIS_WIDTH, AXIS_WIDTH, AXIS_LENGTH).apply { translateZ = AXIS_LENGTH / 2 }
        }
        styleHandle(box, axis, Mode.TRANSLATE)
        return box
    }

    private fun translateHead(axis: GizmoAxisType): MeshView {
        val cone = MeshView(coneMesh(CONE_RADIUS, CONE_HEIGHT))
        when (axis) {
            GizmoAxisType.X -> {
                cone.transforms.add(Rotate(90.0, Rotate.Z_AXIS))
                cone.translateX = AXIS_LENGTH + CONE_HEIGHT / 2
            }
            GizmoAxisType.Y -> {
                cone.translateY = AXIS_LENGTH + CONE_HEIGHT / 2
            }
            GizmoAxisType.Z -> {
                cone.transforms.add(Rotate(-90.0, Rotate.X_AXIS))
                cone.translateZ = AXIS_LENGTH + CONE_HEIGHT / 2
            }
        }
        styleHandle(cone, axis, Mode.TRANSLATE)
        return cone
    }

    private fun rotateRing(axis: GizmoAxisType): MeshView {
        val ring = MeshView(torusMesh(RING_RADIUS, RING_TUBE))
        when (axis) {
            GizmoAxisType.X -> ring.transforms.add(Rotate(90.0, Rotate.Y_AXIS))
            GizmoAxisType.Y -> ring.transforms.add(Rotate(90.0, Rotate.X_AXIS))
            GizmoAxisType.Z -> Unit
        }
        styleHandle(ring, axis, Mode.ROTATE)
        return ring
    }

    private fun centerCube(): Box = Box(CENTER, CENTER, CENTER).apply {
        material = PhongMaterial(Color.web("#DDDDDD"))
        depthTest = DepthTest.DISABLE
        cullFace = CullFace.NONE
        drawMode = DrawMode.FILL
        isMouseTransparent = true
    }

    private fun styleHandle(shape: Shape3D, axis: GizmoAxisType, mode: Mode) {
        shape.material = PhongMaterial(axisColor(axis))
        shape.depthTest = DepthTest.DISABLE
        shape.cullFace = CullFace.NONE
        shape.drawMode = DrawMode.FILL
        shape.userData = mode
        shape.properties[AXIS_KEY] = axis
    }

    private fun tint(shapes: Array<out Shape3D>, color: Color) {
        for (shape in shapes)
            shape.material = PhongMaterial(color)
    }

    companion object {
        private const val AXIS_KEY = "gizmo-axis"
        private const val AXIS_LENGTH = 48.0
        private const val AXIS_WIDTH = 2.2
        private const val CONE_RADIUS = 4.0f
        private const val CONE_HEIGHT = 10.0f
        private const val RING_RADIUS = 28.0f
        private const val RING_TUBE = 1.4f
        private const val CENTER = 5.0
        private const val SCREEN_SCALE_DISTANCE = 800.0
        private const val SCREEN_PICK_PIXELS = 16.0

        fun axisColor(axis: GizmoAxisType): Color = when (axis) {
            GizmoAxisType.X -> Color.web("#E74C3C")
            GizmoAxisType.Y -> Color.web("#2ECC71")
            GizmoAxisType.Z -> Color.web("#3498DB")
        }

        private fun coneMesh(radius: Float, height: Float, divisions: Int = 20): TriangleMesh {
            val mesh = TriangleMesh()
            mesh.texCoords.addAll(0f, 0f)
            mesh.points.addAll(0f, height / 2f, 0f)
            val step = (Math.PI * 2.0 / divisions).toFloat()
            for (i in 0..divisions) {
                val a = step * i
                mesh.points.addAll(radius * cos(a), -height / 2f, radius * sin(a))
            }
            mesh.points.addAll(0f, -height / 2f, 0f)
            val baseCenter = divisions + 2
            for (i in 1..divisions) {
                mesh.faces.addAll(0, 0, i + 1, 0, i, 0)
                mesh.faces.addAll(baseCenter, 0, i, 0, i + 1, 0)
            }
            return mesh
        }

        private fun torusMesh(radius: Float, tube: Float, radial: Int = 48, tubular: Int = 12): TriangleMesh {
            val mesh = TriangleMesh()
            mesh.texCoords.addAll(0f, 0f)
            for (i in 0 until radial) {
                val u = (i * Math.PI * 2.0 / radial).toFloat()
                for (j in 0 until tubular) {
                    val v = (j * Math.PI * 2.0 / tubular).toFloat()
                    val cx = (radius + tube * cos(v)) * cos(u)
                    val cy = (radius + tube * cos(v)) * sin(u)
                    val cz = tube * sin(v)
                    mesh.points.addAll(cx, cy, cz)
                }
            }
            for (i in 0 until radial) {
                val i2 = (i + 1) % radial
                for (j in 0 until tubular) {
                    val j2 = (j + 1) % tubular
                    val p0 = i * tubular + j
                    val p1 = i2 * tubular + j
                    val p2 = i2 * tubular + j2
                    val p3 = i * tubular + j2
                    mesh.faces.addAll(p0, 0, p1, 0, p2, 0, p0, 0, p2, 0, p3, 0)
                }
            }
            return mesh
        }
    }
}
