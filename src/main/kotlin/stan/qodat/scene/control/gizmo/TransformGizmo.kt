package stan.qodat.scene.control.gizmo

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.AmbientLight
import javafx.scene.DepthTest
import javafx.scene.Group
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

    private fun handleGroup(mode: Mode, axis: GizmoAxisType, vararg shapes: Shape3D): Group {
        val group = Group(*shapes)
        val idle = axisColor(axis)
        group.setOnMousePressed { event ->
            selectedMode.set(mode)
            selectedAxis.set(axis)
            hasPreviousHit = false
            tint(shapes, idle.darker())
            event.consume()
        }
        group.setOnMouseReleased { event ->
            endDrag()
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
