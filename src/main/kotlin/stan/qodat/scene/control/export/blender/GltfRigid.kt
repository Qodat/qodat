package stan.qodat.scene.control.export.blender

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Rigid fit of a posed vertex-skin group onto a glTF joint.
 *
 * RS legacy frames transform each skin group as a rigid body (offset, then
 * translate / rotate / scale). Fitting bind verts → posed verts with Horn 1987
 * recovers that motion as joint translation + rotation, which is what Blender
 * actually plays back on an armature.
 */
internal object GltfRigid {

    private val identity = floatArrayOf(0f, 0f, 0f, 1f)

    data class Pose(
        val translation: FloatArray,
        val rotation: FloatArray,
    )

    fun pose(bind: FloatArray, posed: FloatArray, skins: IntArray, skin: Int, nVert: Int): Pose {
        var cx = 0f
        var cy = 0f
        var cz = 0f
        var px = 0f
        var py = 0f
        var pz = 0f
        var n = 0
        for (i in 0 until nVert) {
            if (skins[i] != skin) continue
            cx += bind[i * 3]
            cy += bind[i * 3 + 1]
            cz += bind[i * 3 + 2]
            px += posed[i * 3]
            py += posed[i * 3 + 1]
            pz += posed[i * 3 + 2]
            n++
        }
        if (n == 0) return Pose(floatArrayOf(0f, 0f, 0f), identity.copyOf())
        val inv = 1f / n
        cx *= inv
        cy *= inv
        cz *= inv
        px *= inv
        py *= inv
        pz *= inv
        if (n < 2) return Pose(floatArrayOf(px, py, pz), identity.copyOf())

        var s00 = 0f
        var s01 = 0f
        var s02 = 0f
        var s10 = 0f
        var s11 = 0f
        var s12 = 0f
        var s20 = 0f
        var s21 = 0f
        var s22 = 0f
        var spread = 0f
        for (i in 0 until nVert) {
            if (skins[i] != skin) continue
            val bx = bind[i * 3] - cx
            val by = bind[i * 3 + 1] - cy
            val bz = bind[i * 3 + 2] - cz
            val qx = posed[i * 3] - px
            val qy = posed[i * 3 + 1] - py
            val qz = posed[i * 3 + 2] - pz
            s00 += bx * qx
            s01 += bx * qy
            s02 += bx * qz
            s10 += by * qx
            s11 += by * qy
            s12 += by * qz
            s20 += bz * qx
            s21 += bz * qy
            s22 += bz * qz
            spread += bx * bx + by * by + bz * bz
        }
        if (spread < 1e-12f) return Pose(floatArrayOf(px, py, pz), identity.copyOf())
        return Pose(floatArrayOf(px, py, pz), quaternionFromCovariance(s00, s01, s02, s10, s11, s12, s20, s21, s22))
    }

    /**
     * Unit quaternion `xyzw` rotating centered bind points onto centered posed
     * points. [sij] is `sum(bind_i * posed_j)`.
     */
    fun quaternionFromCovariance(
        s00: Float, s01: Float, s02: Float,
        s10: Float, s11: Float, s12: Float,
        s20: Float, s21: Float, s22: Float,
    ): FloatArray {
        val n00 = s00 + s11 + s22
        val n01 = s12 - s21
        val n02 = s20 - s02
        val n03 = s01 - s10
        val n11 = s00 - s11 - s22
        val n12 = s01 + s10
        val n13 = s02 + s20
        val n22 = -s00 + s11 - s22
        val n23 = s12 + s21
        val n33 = -s00 - s11 + s22
        val n = arrayOf(
            floatArrayOf(n00, n01, n02, n03),
            floatArrayOf(n01, n11, n12, n13),
            floatArrayOf(n02, n12, n22, n23),
            floatArrayOf(n03, n13, n23, n33),
        )
        val q = largestEigenvector(n)
        if (q[0] < 0f) {
            q[0] = -q[0]
            q[1] = -q[1]
            q[2] = -q[2]
            q[3] = -q[3]
        }
        return floatArrayOf(q[1], q[2], q[3], q[0])
    }

    private fun largestEigenvector(a: Array<FloatArray>): FloatArray {
        val m = Array(4) { r -> a[r].copyOf() }
        val v = Array(4) { i -> FloatArray(4) { if (it == i) 1f else 0f } }
        for (sweep in 0 until 16) {
            var p = 0
            var q = 1
            var max = 0f
            for (i in 0..3) {
                for (j in i + 1..3) {
                    val mag = abs(m[i][j])
                    if (mag > max) {
                        max = mag
                        p = i
                        q = j
                    }
                }
            }
            if (max < 1e-8f) break
            val app = m[p][p]
            val aqq = m[q][q]
            val apq = m[p][q]
            val tau = (aqq - app) / (2f * apq)
            val t = if (tau >= 0f)
                1f / (tau + sqrt(1f + tau * tau))
            else
                -1f / (-tau + sqrt(1f + tau * tau))
            val c = 1f / sqrt(1f + t * t)
            val s = t * c
            for (k in 0..3) {
                if (k == p || k == q) continue
                val mkp = m[p][k]
                val mkq = m[q][k]
                val rkp = c * mkp - s * mkq
                val rkq = s * mkp + c * mkq
                m[p][k] = rkp
                m[k][p] = rkp
                m[q][k] = rkq
                m[k][q] = rkq
            }
            val appNew = c * c * app - 2f * s * c * apq + s * s * aqq
            val aqqNew = s * s * app + 2f * s * c * apq + c * c * aqq
            m[p][p] = appNew
            m[q][q] = aqqNew
            m[p][q] = 0f
            m[q][p] = 0f
            for (k in 0..3) {
                val vkp = v[k][p]
                val vkq = v[k][q]
                v[k][p] = c * vkp - s * vkq
                v[k][q] = s * vkp + c * vkq
            }
        }
        var best = 0
        for (i in 1..3) {
            if (m[i][i] > m[best][best]) best = i
        }
        val x = floatArrayOf(v[0][best], v[1][best], v[2][best], v[3][best])
        var len = 0f
        for (c in x) len += c * c
        if (len < 1e-20f || !len.isFinite()) return floatArrayOf(1f, 0f, 0f, 0f)
        len = sqrt(len)
        for (i in 0..3) x[i] /= len
        return x
    }
}
