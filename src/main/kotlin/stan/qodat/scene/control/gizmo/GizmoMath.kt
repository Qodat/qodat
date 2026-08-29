package stan.qodat.scene.control.gizmo

import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * View-stable translate / rotate deltas for a 3D gizmo.
 *
 * Translate intersects the plane that contains [axis] and faces the camera
 * as much as possible, then keeps only the component along that axis.
 * Rotate intersects the plane of the ring (normal = [axis]) and measures
 * the signed angle around that axis.
 */
object GizmoMath {

    const val RS_UNITS_PER_TURN = 256
    private const val PARALLEL_EPSILON = 1e-6f
    private const val TINY = 1e-8f

    data class Ray(val origin: Vector3f, val direction: Vector3f) {
        fun point(t: Float): Vector3f =
            Vector3f(origin).add(Vector3f(direction).mul(t))
    }

    fun axisVector(axis: GizmoAxisType): Vector3f = when (axis) {
        GizmoAxisType.X -> Vector3f(1f, 0f, 0f)
        GizmoAxisType.Y -> Vector3f(0f, 1f, 0f)
        GizmoAxisType.Z -> Vector3f(0f, 0f, 1f)
    }

    /**
     * Plane normal that contains [axis] and is as camera-facing as possible.
     * When the view is along the axis, falls back to a stable cardinal plane.
     */
    fun translatePlaneNormal(axis: Vector3f, cameraForward: Vector3f): Vector3f {
        val a = Vector3f(axis).normalize()
        val view = Vector3f(cameraForward)
        if (view.lengthSquared() < TINY)
            return fallbackNormal(a)
        view.normalize()
        val rejected = Vector3f(view).sub(Vector3f(a).mul(view.dot(a)))
        if (rejected.lengthSquared() < PARALLEL_EPSILON)
            return fallbackNormal(a)
        return rejected.normalize()
    }

    fun intersectRayPlane(ray: Ray, planePoint: Vector3f, planeNormal: Vector3f): Vector3f? {
        val n = Vector3f(planeNormal)
        if (n.lengthSquared() < TINY)
            return null
        n.normalize()
        val denom = ray.direction.dot(n)
        if (abs(denom) < PARALLEL_EPSILON)
            return null
        val t = Vector3f(planePoint).sub(ray.origin).dot(n) / denom
        return ray.point(t)
    }

    fun translateDelta(previous: Vector3f, current: Vector3f, axis: Vector3f): Float {
        val a = Vector3f(axis)
        if (a.lengthSquared() < TINY)
            return 0f
        a.normalize()
        return Vector3f(current).sub(previous).dot(a)
    }

    /**
     * Signed degrees around [axis] from [previous] to [current], both in the
     * same space as [origin].
     */
    fun rotateDeltaDegrees(
        previous: Vector3f,
        current: Vector3f,
        origin: Vector3f,
        axis: Vector3f
    ): Float {
        val n = Vector3f(axis)
        if (n.lengthSquared() < TINY)
            return 0f
        n.normalize()
        val v0 = projectOnPlane(Vector3f(previous).sub(origin), n)
        val v1 = projectOnPlane(Vector3f(current).sub(origin), n)
        if (v0.lengthSquared() < TINY || v1.lengthSquared() < TINY)
            return 0f
        v0.normalize()
        v1.normalize()
        val sin = Vector3f(v0).cross(v1).dot(n)
        val cos = v0.dot(v1)
        return Math.toDegrees(atan2(sin.toDouble(), cos.toDouble())).toFloat()
    }

    /** 256 RS units = 360°. [ModelSkeleton] applies `(value & 255) * 8`. */
    fun degreesToRs(degrees: Float): Int =
        (degrees * RS_UNITS_PER_TURN / 360f).roundToInt()

    fun rsToDegrees(rs: Int): Float =
        (rs and 255) * 360f / RS_UNITS_PER_TURN

    /** Shortest distance from [ray] to the segment [a]→[b]. */
    fun raySegmentDistance(ray: Ray, a: Vector3f, b: Vector3f): Float {
        val dir = Vector3f(ray.direction)
        if (dir.lengthSquared() < TINY)
            return Float.POSITIVE_INFINITY
        dir.normalize()
        val ab = Vector3f(b).sub(a)
        val ao = Vector3f(a).sub(ray.origin)
        val abLen2 = ab.lengthSquared()
        if (abLen2 < TINY)
            return Vector3f(ao).cross(dir).length()
        val d = dir.dot(ab)
        val denom = abLen2 - d * d
        val t = if (abs(denom) < TINY)
            0f
        else
            ((ao.dot(dir) * d - ao.dot(ab)) / denom).coerceIn(0f, 1f)
        val closest = Vector3f(a).add(Vector3f(ab).mul(t))
        val toClosest = Vector3f(closest).sub(ray.origin)
        val along = toClosest.dot(dir).coerceAtLeast(0f)
        return Vector3f(ray.origin).add(Vector3f(dir).mul(along)).sub(closest).length()
    }

    /** Distance from the ray-plane hit to a circle of [radius] around [origin]. */
    fun rayCircleDistance(ray: Ray, origin: Vector3f, axis: Vector3f, radius: Float): Float {
        val hit = intersectRayPlane(ray, origin, axis) ?: return Float.POSITIVE_INFINITY
        return abs(Vector3f(hit).sub(origin).length() - radius)
    }

    private fun projectOnPlane(v: Vector3f, unitNormal: Vector3f): Vector3f =
        v.sub(Vector3f(unitNormal).mul(v.dot(unitNormal)))

    private fun fallbackNormal(unitAxis: Vector3f): Vector3f {
        val absX = abs(unitAxis.x)
        val absY = abs(unitAxis.y)
        val absZ = abs(unitAxis.z)
        return when {
            absX <= absY && absX <= absZ -> Vector3f(unitAxis).cross(Vector3f(1f, 0f, 0f)).normalize()
            absY <= absZ -> Vector3f(unitAxis).cross(Vector3f(0f, 1f, 0f)).normalize()
            else -> Vector3f(unitAxis).cross(Vector3f(0f, 0f, 1f)).normalize()
        }
    }
}
