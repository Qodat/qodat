package stan.qodat.scene.runescape.model

import fxyz3d.geometry.Point3F
import jagex.BoneTransform
import javafx.geometry.Point3D
import qodat.cache.definition.ModelDefinition
import qodat.cache.models.RS2Model
import qodat.cache.models.VertexNormal
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.AnimationFrameLegacy
import stan.qodat.scene.runescape.animation.AnimationFrameMaya
import stan.qodat.scene.runescape.animation.TransformationType
import stan.qodat.scene.transform.Transformable
import stan.qodat.util.COSINE
import stan.qodat.util.SINE
import kotlin.math.sqrt

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
open class ModelSkeleton(internal val modelDefinition: ModelDefinition)
    : Transformable {

    @Transient private var pointXOffset = 0
    @Transient private var pointYOffset = 0
    @Transient private var pointZOffset = 0

    @Transient private lateinit var originalVertexXValues : IntArray
    @Transient private lateinit var originalVertexYValues : IntArray
    @Transient private lateinit var originalVertexZValues : IntArray

    @Transient private lateinit var vertexGroups : Array<IntArray>
    @Transient private lateinit var faceGroups : Array<IntArray>

    private lateinit var vertexPositionsX : IntArray
    private lateinit var vertexPositionsY : IntArray
    private lateinit var vertexPositionsZ : IntArray

    private lateinit var sharedVertexGroupIndices : IntArray

    override fun animate(frame: AnimationFrame) {

        if (!this::vertexPositionsX.isInitialized){
            vertexPositionsX = modelDefinition.getVertexPositionsX().copyOf()
            vertexPositionsY = modelDefinition.getVertexPositionsY().copyOf()
            vertexPositionsZ = modelDefinition.getVertexPositionsZ().copyOf()
        }

        // Initialises the vertexGroups array
        getVertexGroups()

        // Used to save performance, less int array allocations
        if (!this::sharedVertexGroupIndices.isInitialized)
            sharedVertexGroupIndices = IntArray(500)

        copyOriginalVertexValues()


        if (frame is AnimationFrameLegacy) {
            animateLegacyFrame(frame)
        } else if (frame is AnimationFrameMaya) {
            animationMayaFrame(frame)
        }
    }

    private fun animateLegacyFrame(frame: AnimationFrameLegacy) {
        resetPointOffset()
        for (transformation in frame.transformationList) {

            if (!transformation.enabledProperty.get())
                continue

            val vertexGroupIndices = transformation.groupIndices.toArray(null)
            val type = transformation.getType()
            val deltaX = transformation.getDeltaX()
            val deltaY = transformation.getDeltaY()
            val deltaZ = transformation.getDeltaZ()

            if (!this::originalVertexXValues.isInitialized) {
                originalVertexXValues = vertexPositionsX.copyOf()
                originalVertexYValues = vertexPositionsY.copyOf()
                originalVertexZValues = vertexPositionsZ.copyOf()
            }

            when (type) {
                TransformationType.SET_OFFSET ->
                    offset(vertexGroupIndices, deltaX, deltaY, deltaZ)

                TransformationType.TRANSLATE ->
                    translate(vertexGroupIndices, deltaX, deltaY, deltaZ)

                TransformationType.ROTATE ->
                    rotate(vertexGroupIndices, deltaX, deltaY, deltaZ)

                TransformationType.SCALE ->
                    scale(vertexGroupIndices, deltaX, deltaY, deltaZ)

                TransformationType.TRANSPARENCY -> {
                    // TODO: Implement
                }

                TransformationType.UNDEFINED -> {
                    // TODO: Throw undefined exception?
                }
            }
        }
    }

    val field2674 = BoneTransform()
    var field2739: BoneTransform = BoneTransform()
    var field2703: BoneTransform = BoneTransform()

    private fun animationMayaFrame(frame: AnimationFrameMaya) {
        val anim = frame.animation
        val animLength = anim.duration
        val animSkeleton = anim.skeleton.mayaAnimationSkeleton

        anim.skeleton
        if (modelDefinition !is RS2Model)
            error("ModelDefinition is not an RS2Model")

        animSkeleton.applyAnimation(anim, frame.index)

        val duration = anim.duration
        for (vertex in 0 until getVertexCount()) {
            val boneIndices = modelDefinition.mayaGroups[vertex]
            if (boneIndices != null && boneIndices.isNotEmpty()) {
                val scales = modelDefinition.mayaScales[vertex]
                field2703.zeroMatrix()
                for ((index, boneIndex) in boneIndices.withIndex()) {
                    val bone = animSkeleton.getBone(boneIndex)
                    if (bone != null) {
                        field2674.setUniformScale(scales[index] / 255.0F)
                        field2739.copy(bone.getTransform(duration))
                        field2739.combine(field2674)
                        field2703.addTransform(field2739)
                    }
                }
                applyMayaTransform(vertex, field2703)
            }
        }

        if (anim.hasTransformations()) {
//            this.method4707(var1, var2)
        }
    }

    private fun applyMayaTransform(vertex: Int, transform: BoneTransform) {
        val x = getX(vertex)
        val y = -getY(vertex)
        val z = -getZ(vertex)
        val d = 1.0F
        if (!this::originalVertexXValues.isInitialized) {
            originalVertexXValues = vertexPositionsX.copyOf()
            originalVertexYValues = vertexPositionsY.copyOf()
            originalVertexZValues = vertexPositionsZ.copyOf()
        }
        setX(vertex, (transform.matrix[0] * x + transform.matrix[4] * y + transform.matrix[8] * z + transform.matrix[12] * d).toInt())
        setY(vertex, -(transform.matrix[1] * x + transform.matrix[5] * y + transform.matrix[9] * z + transform.matrix[13] * d).toInt())
        setZ(vertex, -(transform.matrix[2] * x + transform.matrix[6] * y + transform.matrix[10] * z + transform.matrix[14] * d).toInt())
//        println("\tv[$vertex] -> ${transform.matrix.contentToString()}")
    }

    private fun scale(
        targetVertexGroupIndices: IntArray,
        deltaX: Int,
        deltaY: Int,
        deltaZ: Int,
    ) {
        for (vertexGroupIndex in targetVertexGroupIndices) {
            if (vertexGroupIndex < vertexGroups.size) {
                for (vertex in vertexGroups[vertexGroupIndex]) {
                    vertexPositionsX[vertex] -= pointXOffset
                    vertexPositionsY[vertex] -= pointYOffset
                    vertexPositionsZ[vertex] -= pointZOffset
                    setX(vertex, getX(vertex) * deltaX / 128)
                    setY(vertex, getY(vertex) * deltaY / 128)
                    setZ(vertex, getZ(vertex) * deltaZ / 128)
                    vertexPositionsX[vertex] += pointXOffset
                    vertexPositionsY[vertex] += pointYOffset
                    vertexPositionsZ[vertex] += pointZOffset
                }
            }
        }
    }

    private fun rotate(
        targetVertexGroupIndices: IntArray,
        deltaX: Int,
        deltaY: Int,
        deltaZ: Int,
    ) {
        val rotationX = convertRotationValue(deltaX)
        val rotationY = convertRotationValue(deltaY)
        val rotationZ = convertRotationValue(deltaZ)
        for (vertexGroupIndex in targetVertexGroupIndices) {
            if (vertexGroupIndex < vertexGroups.size) {
                for (vertex in vertexGroups[vertexGroupIndex]) {
                    vertexPositionsX[vertex] -= pointXOffset
                    vertexPositionsY[vertex] -= pointYOffset
                    vertexPositionsZ[vertex] -= pointZOffset
                    if (rotationZ != 0)
                        rotateXY(vertex, rotationZ)
                    if (rotationX != 0)
                        rotateZY(vertex, rotationX)
                    if (rotationY != 0)
                        rotateXZ(vertex, rotationY)
                    vertexPositionsX[vertex] += pointXOffset
                    vertexPositionsY[vertex] += pointYOffset
                    vertexPositionsZ[vertex] += pointZOffset
                }
            }
        }
    }

    private fun rotateXY(vertex: Int, rotationZ: Int) {
        val sin = SINE[rotationZ]
        val cos = COSINE[rotationZ]
        val x = getX(vertex)
        val y = getY(vertex)
        val newX = (sin.times(y) + cos.times(x)) shr 16
        val newY = (cos.times(y) - sin.times(x)) shr 16
        setX(vertex, newX)
        setY(vertex, newY)
    }

    private fun rotateZY(vertex: Int, rotationX: Int) {
        val sin = SINE[rotationX]
        val cos = COSINE[rotationX]
        val y = getY(vertex)
        val z = getZ(vertex)
        val newZ = (sin.times(y) + cos.times(z)) shr 16
        val newY = (cos.times(y) - sin.times(z)) shr 16
        setZ(vertex, newZ)
        setY(vertex, newY)
    }

    private fun rotateXZ(vertex: Int, rotationY: Int) {
        val sin = SINE[rotationY]
        val cos = COSINE[rotationY]
        val x = getX(vertex)
        val z = getZ(vertex)
        val newX = (sin.times(z) + cos.times(x)) shr 16
        val newZ = (cos.times(z) - sin.times(x)) shr 16
        setX(vertex, newX)
        setZ(vertex, newZ)
    }

    private fun convertRotationValue(value: Int) = (value and 255).times(8)

    private fun translate(
        targetVertexGroupIndices: IntArray,
        deltaX: Int,
        deltaY: Int,
        deltaZ: Int,
    ) {
        for (vertexGroupIndex in targetVertexGroupIndices) {
            if (vertexGroupIndex < vertexGroups.size) {
                for (vertex in vertexGroups[vertexGroupIndex]) {
                    vertexPositionsX[vertex] += deltaX
                    vertexPositionsY[vertex] += deltaY
                    vertexPositionsZ[vertex] += deltaZ
                }
            }
        }
    }

    private fun offset(
        targetVertexGroupIndices: IntArray,
        deltaX: Int,
        deltaY: Int,
        deltaZ: Int,
    ) {
        resetPointOffset()
        var iteratedVertexCount = 0
        for (vertexGroupIndex in targetVertexGroupIndices) {
            if (vertexGroupIndex < vertexGroups.size) {
                for (vertex in vertexGroups[vertexGroupIndex]) {
                    pointXOffset += vertexPositionsX[vertex]
                    pointYOffset += vertexPositionsY[vertex]
                    pointZOffset += vertexPositionsZ[vertex]
                    iteratedVertexCount++
                }
            }
        }
        if (iteratedVertexCount > 0) {
            pointXOffset = deltaX + pointXOffset.div(iteratedVertexCount)
            pointYOffset = deltaY + pointYOffset.div(iteratedVertexCount)
            pointZOffset = deltaZ + pointZOffset.div(iteratedVertexCount)
        } else {
            pointXOffset = deltaX
            pointYOffset = deltaY
            pointZOffset = deltaZ
        }
    }

    private fun resetPointOffset() {
        pointXOffset = 0
        pointYOffset = 0
        pointZOffset = 0
    }

    private fun checkGroupInitialised(){
        if (!this::faceGroups.isInitialized && !this::vertexGroups.isInitialized){
            modelDefinition.computeAnimationTables()
            vertexGroups = modelDefinition.getVertexGroups()?.copyOf()?: emptyArray()
            faceGroups = modelDefinition.getFaceGroups()?.copyOf()?: emptyArray()
        }
    }

    fun getVertexGroups() : Array<IntArray> {
        checkGroupInitialised()
        return vertexGroups
    }

    fun getVertexGroup(groupIndex: Int) = getVertexGroups()[groupIndex]

    fun getFaceGroups() : Array<IntArray> {
        checkGroupInitialised()
        return faceGroups
    }

    fun getFaceGroup(groupIndex: Int) = getFaceGroups()[groupIndex]

    fun getPointXValues() : IntArray {
        if (!this::vertexPositionsX.isInitialized)
            vertexPositionsX = modelDefinition.getVertexPositionsX().copyOf()
        return vertexPositionsX
    }
    fun getPointYValues() : IntArray {
        if (!this::vertexPositionsY.isInitialized)
            vertexPositionsY = modelDefinition.getVertexPositionsY().copyOf()
        return vertexPositionsY
    }
    fun getPointZValues() : IntArray {
        if (!this::vertexPositionsZ.isInitialized)
            vertexPositionsZ = modelDefinition.getVertexPositionsZ().copyOf()
        return vertexPositionsZ
    }

    fun getVertices(face: Int) = Triple(
        modelDefinition.getFaceVertexIndices1()[face],
        modelDefinition.getFaceVertexIndices2()[face],
        modelDefinition.getFaceVertexIndices3()[face])

    fun getTextureVertices(face: Int) = Triple(
        modelDefinition.getTextureTriangleVertexIndices1()!![face].toInt(),
        modelDefinition.getTextureTriangleVertexIndices2()!![face].toInt(),
        modelDefinition.getTextureTriangleVertexIndices3()!![face].toInt())

    fun getPoints(face: Int) = getVertices(face).let { (v1, v2, v3) ->
        Triple(getPoint(v1), getPoint(v2), getPoint(v3))
    }

    fun getFaceCount() = modelDefinition.getFaceCount()

    fun getVertexCount() = modelDefinition.getVertexCount()

    fun getPoint(vertex: Int) =
        Point3F(getX(vertex).toFloat(), getY(vertex).toFloat(), getZ(vertex).toFloat())

    fun getCenterPoint(face: Int) = getPoints(face).let { (p1, p2, p3) ->
        Point3D(
            (p1.x+p2.x+p3.x).div(3),
            (p1.y+p2.y+p3.y).div(3),
            (p1.z+p2.z+p3.z).div(3))
    }

    fun getX(vertex: Int) = getPointXValues()[vertex]
    fun getY(vertex: Int) = getPointYValues()[vertex]
    fun getZ(vertex: Int) = getPointZValues()[vertex]

    fun getXYZ(vertex: Int) = Triple(getX(vertex), getY(vertex), getZ(vertex))

    fun setX(vertex: Int, value: Int) {
        getPointXValues()[vertex] = value
    }
    fun setY(vertex: Int, value: Int) {
        getPointYValues()[vertex] = value
    }
    fun setZ(vertex: Int, value: Int) {
        getPointZValues()[vertex] = value
    }

    /**
     * Copies [originalVertexXValues], [originalVertexYValues], [originalVertexZValues]
     * into  [vertexPositionsX], [vertexPositionsY], [vertexPositionsZ] respectively.
     */
    protected fun copyOriginalVertexValues(){

        if (!this::originalVertexXValues.isInitialized)
            return

        System.arraycopy(originalVertexXValues, 0, vertexPositionsX, 0, originalVertexXValues.size)
        System.arraycopy(originalVertexYValues, 0, vertexPositionsY, 0, originalVertexYValues.size)
        System.arraycopy(originalVertexZValues, 0, vertexPositionsZ, 0, originalVertexZValues.size)
    }

    fun calculateVertexNormals() : Array<VertexNormal> {
        val normals = Array(getVertexCount()) {
            VertexNormal()
        }

        for (face in 0 until getFaceCount()) {

            val (vA, vB, vC) = getVertices(face) // indices (TODO: optimise)
            val (v1, v2, v3) = getPoints(face)   // point instances

            val xA = (v2.x - v1.x)
            val yA = (v2.y - v1.y)
            val zA = (v2.z - v1.z)

            val xB = (v3.x - v1.x)
            val yB = (v3.y - v1.y)
            val zB = (v3.z - v1.z)

            // Compute cross product
            var var11 = (yA * zB - yB * zA).toInt()
            var var12 = (zA * xB - zB * xA).toInt()
            var var13 = (xA * yB - xB * yA).toInt()

            while (var11 > 8192 || var12 > 8192 || var13 > 8192 || var11 < -8192 || var12 < -8192 || var13 < -8192) {
                var11 = var11 shr 1
                var12 = var12 shr 1
                var13 = var13 shr 1
            }

            val length = sqrt((var11 * var11 + var12 * var12 + var13 * var13).toDouble())
                .toInt()
                .coerceAtLeast(1)

            var11 = var11 * 256 / length
            var12 = var12 * 256 / length
            var13 = var13 * 256 / length

            val var15 = modelDefinition.getFaceTypes()?.get(face)?.toInt()?:0
            if (var15 == 0) {
                normals[vA].apply {
                    x += var11
                    y += var12
                    z += var13
                    ++magnitude
                }
                normals[vB].apply {
                    x += var11
                    y += var12
                    z += var13
                    ++magnitude
                }
                normals[vC].apply {
                    x += var11
                    y += var12
                    z += var13
                    ++magnitude
                }
            }
        }
        return normals
    }
}
