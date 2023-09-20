package stan.qodat.cache.impl.oldschool.definition

import net.runelite.cache.io.InputStream
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos


class SkeletalBone(val id: Int, val poseCount: Int) {
    var parentId = -1
    val localMatrices: Array<Matrix4f> = Array(poseCount) {Matrix4f()}
    val modelMatrices: Array<Matrix4f> = Array(poseCount) {Matrix4f()}
    val invertedModelMatrices: Array<Matrix4f> = Array(poseCount) {Matrix4f()}

    var animMatrix: Matrix4f = Matrix4f()
    var parent: SkeletalBone? = null
    var updateAnimModelMatrix: Boolean = false
    var updateFinalMatrix: Boolean = false
    val rotations: Array<Vector3f> = Array(poseCount) {Vector3f()}
    val translations: Array<Vector3f> = Array(poseCount) {Vector3f()}
    val scalings: Array<Vector3f> = Array(poseCount) {Vector3f()}

    var animModelMatrix: Matrix4f = Matrix4f()
    var finalMatrix: Matrix4f = Matrix4f()


    constructor(id: Int, poseCount: Int, compactMatrix: Boolean, stream: InputStream): this(id, poseCount) {
        decodeValues(stream, compactMatrix)
    }

    fun getRotation(matrix: Matrix4f): Vector3f {
        val euler = Vector3f(-asin(matrix[2,0].toDouble()).toFloat(), 0.0f, 0.0f)
        val cosRotationX = cos(euler[0].toDouble()).toFloat()

        if (abs(cosRotationX) > 0.005f) {
            val sinRotationY = matrix[0,2].toDouble()
            val cosRotationY = matrix[0,0].toDouble()
            val sinRotationZ = matrix[1,0].toDouble()
            val cosRotationZ = matrix[1,1].toDouble()

            euler.setComponent(1, atan2(sinRotationY, cosRotationY).toFloat())
            euler.setComponent(2, atan2(sinRotationZ, cosRotationZ).toFloat())
        } else {
            val sinRotationY = matrix[0,2].toDouble()
            val cosRotationY = matrix[0,0].toDouble()

            if (matrix[2,0] < 0.0f) {
                euler.setComponent(1, atan2(sinRotationY, cosRotationY).toFloat())
            } else {
                euler.setComponent(1, -atan2(sinRotationY, cosRotationY).toFloat())
            }
            euler.setComponent(2, 0.0f)
        }
        return euler
    }

    private fun SkeletalBone.decodeValues(stream: InputStream, compact: Boolean) {
        parentId = stream.readShort().toInt()

        for (i in 0 ..  poseCount) {
            localMatrices[i] = readMat4(stream, compact)
            stream.readFloat()//unused
            stream.readFloat()//unused
            stream.readFloat()//unused
        }
        extractTransformations()
    }

    private fun SkeletalBone.extractTransformations() {
        for (i in 0..poseCount) {
            val localMatrix = localMatrices[i]
            val invertedLocalMatrix = Matrix4f(localMatrix).invert()
            rotations[i] = getRotation(invertedLocalMatrix)
            localMatrix.getTranslation(translations[i])
            localMatrix.getScale(scalings[i])
        }
    }

    fun readMat4(buffer: InputStream, compact: Boolean): Matrix4f {
        if (compact) {
            throw Error("Not implemented")
        } else {
            val m = FloatArray(16)
            for (i in 0 until 16) {
                m[i] = buffer.readFloat()
            }
            return Matrix4f().set(m)
        }
    }

    private fun InputStream.readFloat(): Float {
        return java.lang.Float.intBitsToFloat(readInt())
    }


}
