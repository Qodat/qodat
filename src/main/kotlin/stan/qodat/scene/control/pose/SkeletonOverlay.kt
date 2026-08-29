package stan.qodat.scene.control.pose

import javafx.scene.DepthTest
import javafx.scene.Group
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.Cylinder
import javafx.scene.shape.DrawMode
import javafx.scene.shape.Sphere
import javafx.scene.transform.Rotate
import stan.qodat.scene.runescape.model.Model
import stan.qodat.scene.runescape.model.distinctColor
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Stick skeleton: a joint sphere per vertex group and a bone cylinder from
 * parent centroid to child. Click a joint to select it for the pose gizmo.
 */
class SkeletonOverlay(
    private val model: Model,
    private val onSelect: (Int) -> Unit
) : Group() {

    private val joints = HashMap<Int, Sphere>()
    private val bones = HashMap<Int, Cylinder>()
    private var selectedGroup: Int? = null

    var skeleton: PoseSkeleton = PoseSkeleton(emptyList())
        private set

    init {
        isMouseTransparent = false
    }

    fun rebuild(next: PoseSkeleton) {
        skeleton = next
        children.clear()
        joints.clear()
        bones.clear()
        for (bone in next.bones) {
            val joint = jointSphere(bone.groupIndex)
            joints[bone.groupIndex] = joint
            children.add(joint)
            if (bone.parentGroupIndex != null) {
                val shaft = boneCylinder(bone.groupIndex)
                bones[bone.groupIndex] = shaft
                children.add(shaft)
            }
        }
        syncPositions()
        highlight(selectedGroup)
    }

    fun syncPositions() {
        for ((group, sphere) in joints) {
            val (x, y, z) = model.groupCentroid(group) ?: continue
            sphere.translateX = x
            sphere.translateY = y
            sphere.translateZ = z
        }
        for (bone in skeleton.bones) {
            val parent = bone.parentGroupIndex ?: continue
            val shaft = bones[bone.groupIndex] ?: continue
            val from = model.groupCentroid(parent) ?: continue
            val to = model.groupCentroid(bone.groupIndex) ?: continue
            alignBone(shaft, from, to)
        }
    }

    fun highlight(groupIndex: Int?) {
        selectedGroup = groupIndex
        for ((group, sphere) in joints) {
            val selected = group == groupIndex
            sphere.radius = if (selected) SELECTED_JOINT_RADIUS else JOINT_RADIUS
            sphere.material = PhongMaterial(
                if (selected) Color.web("#FFD54F") else distinctColor(group)
            )
        }
    }

    fun selectedGroup(): Int? = selectedGroup

    private fun jointSphere(groupIndex: Int): Sphere =
        Sphere(JOINT_RADIUS, 10).apply {
            material = PhongMaterial(distinctColor(groupIndex))
            depthTest = DepthTest.DISABLE
            cullFace = CullFace.NONE
            drawMode = DrawMode.FILL
            userData = groupIndex
            setOnMousePressed { event ->
                onSelect(groupIndex)
                event.consume()
            }
        }

    private fun boneCylinder(childGroup: Int): Cylinder =
        Cylinder(BONE_RADIUS, 1.0, 8).apply {
            material = PhongMaterial(distinctColor(childGroup).deriveColor(0.0, 0.7, 0.85, 1.0))
            depthTest = DepthTest.DISABLE
            cullFace = CullFace.NONE
            isMouseTransparent = true
        }

    companion object {
        private const val JOINT_RADIUS = 4.0
        private const val SELECTED_JOINT_RADIUS = 6.0
        private const val BONE_RADIUS = 1.15

        internal fun alignBone(
            cylinder: Cylinder,
            from: Triple<Double, Double, Double>,
            to: Triple<Double, Double, Double>
        ) {
            val dx = to.first - from.first
            val dy = to.second - from.second
            val dz = to.third - from.third
            val length = sqrt(dx * dx + dy * dy + dz * dz)
            if (length < 1e-4) {
                cylinder.isVisible = false
                return
            }
            cylinder.isVisible = true
            cylinder.height = length
            cylinder.translateX = (from.first + to.first) / 2.0
            cylinder.translateY = (from.second + to.second) / 2.0
            cylinder.translateZ = (from.third + to.third) / 2.0
            cylinder.transforms.setAll(rotationFromYAxis(dx / length, dy / length, dz / length))
        }

        internal fun rotationFromYAxis(x: Double, y: Double, z: Double): Rotate {
            val axisX = z
            val axisY = 0.0
            val axisZ = -x
            val axisLength = sqrt(axisX * axisX + axisZ * axisZ)
            val angle = Math.toDegrees(acos(y.coerceIn(-1.0, 1.0)))
            return if (axisLength < 1e-6)
                Rotate(if (y >= 0.0) 0.0 else 180.0, Rotate.X_AXIS)
            else
                Rotate(angle, axisX / axisLength, axisY, axisZ / axisLength)
        }
    }
}
