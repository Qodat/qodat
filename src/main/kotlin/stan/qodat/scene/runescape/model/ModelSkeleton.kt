package stan.qodat.scene.runescape.model

import stan.qodat.cache.definition.ModelDefinition
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.TransformationType
import stan.qodat.scene.transform.Transformable
import stan.qodat.util.COSINE
import stan.qodat.util.SINE

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

    private fun scale(
            targetVertexGroupIndices: IntArray,
            deltaX: Int,
            deltaY: Int,
            deltaZ: Int
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
            deltaZ: Int
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
            deltaZ: Int
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
            deltaZ: Int
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

    fun getX(vertex: Int) = getPointXValues()[vertex]
    fun getY(vertex: Int) = getPointYValues()[vertex]
    fun getZ(vertex: Int) = getPointZValues()[vertex]

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
}