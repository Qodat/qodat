package stan.qodat.cache.impl.qodat

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.runelite.cache.models.FaceNormal
import net.runelite.cache.models.VertexNormal
import stan.qodat.cache.definition.*

@Serializable
data class QodatNpcDefinition(
    override val name: String,
    override val modelIds: Array<String>,
    override val animationIds: Array<String>,
) : NPCDefinition

@Serializable
data class QodatObjectDefinition(
    override val name: String,
    override val modelIds: Array<String>,
    override val animationIds: Array<String>,
) : ObjectDefinition

@Serializable
data class QodatItemDefinition(
    override val name: String,
    override val modelIds: Array<String>,
) : ItemDefinition

@Serializable
data class QodatAnimationDefinition(
    override val id: String,
    override val frameHashes: IntArray,
    override val frameLengths: IntArray
) : AnimationDefinition

@Serializable
data class QodatAnimationSkeletonDefinition(
    override val id: Int,
    override val transformationTypes: IntArray,
    override val targetVertexGroupsIndices: Array<IntArray>
) : AnimationSkeletonDefinition

@Serializable
data class QodatAnimationFrameDefinition(
    val frameHash: Int,
    override val transformationCount: Int,
    override val transformationGroupAccessIndices: IntArray,
    override val transformationDeltaX: IntArray,
    override val transformationDeltaY: IntArray,
    override val transformationDeltaZ: IntArray,
    override val transformationGroup: QodatAnimationSkeletonDefinition
) : AnimationFrameDefinition

@Serializable
data class QodatModelDefinition(
    private val name: String,
    private val vertexCount: Int,
    private val vertexPositionsX: IntArray,
    private val vertexPositionsY: IntArray,
    private val vertexPositionsZ: IntArray,
    private val vertexSkins: IntArray?,
    private val faceCount: Int,
    private val faceVertexIndices1: IntArray,
    private val faceVertexIndices2: IntArray,
    private val faceVertexIndices3: IntArray,
    private val faceSkins: IntArray?,
    private val faceAlphas: ByteArray?,
    private val facePriorities: ByteArray?,
    private val faceTypes: ByteArray?,
    private val priority: Byte = 10.toByte(),
    private val faceColors: ShortArray
) : ModelDefinition {

    @Transient
    private lateinit var vertexGroups: Array<IntArray>
    @Transient
    private lateinit var faceGroups: Array<IntArray>

    override fun getName() = name
    override fun getVertexCount() = vertexCount
    override fun getVertexPositionsX() = vertexPositionsX
    override fun getVertexPositionsY() = vertexPositionsY
    override fun getVertexPositionsZ() = vertexPositionsZ
    override fun getVertexSkins() = vertexSkins
    override fun getVertexGroups() = vertexGroups
    override fun getVertexNormals() = emptyArray<VertexNormal>()
    override fun getFaceCount() = faceCount
    override fun getFaceVertexIndices1() = faceVertexIndices1
    override fun getFaceVertexIndices2() = faceVertexIndices2
    override fun getFaceVertexIndices3() = faceVertexIndices3
    override fun getFaceSkins() = faceSkins
    override fun getFaceGroups() = faceGroups
    override fun getFaceColors() = faceColors
    override fun getFaceAlphas() = faceAlphas
    override fun getFacePriorities() = facePriorities
    override fun getFaceTypes() = faceTypes
    override fun getFaceNormals() = emptyArray<FaceNormal>()
    override fun getPriority() = priority
    override fun getTextureConfigCount() = 0
    override fun getTextureRenderTypes() = null
    override fun getFaceTextures() = null
    override fun getFaceTextureConfigs() = null
    override fun getTextureTriangleVertexIndices1() = null
    override fun getTextureTriangleVertexIndices2() = null
    override fun getTextureTriangleVertexIndices3() = null
    override fun getFaceTextureUCoordinates() = null
    override fun getFaceTextureVCoordinates() = null

    override fun computeAnimationTables() {

        var groupCounts : IntArray
        var numGroups : Int

        if (vertexSkins != null){

            groupCounts = IntArray(256)
            numGroups = 0

            for (i in 0 until vertexCount) {
                val group = vertexSkins[i]
                ++groupCounts[group]
                if (group > numGroups)
                    numGroups = group
            }

            vertexGroups = Array(numGroups + 1) {
                val count = groupCounts[it]
                groupCounts[it] = 0
                IntArray(count)
            }

            for (i in 0 until vertexCount) {
                val group = vertexSkins[i]
                vertexGroups[group][groupCounts[group]++] = i
            }
        }
        if (faceSkins != null) {

            groupCounts = IntArray(256)
            numGroups = 0

            for (i in 0 until faceCount) {
                val group = faceSkins[i]
                ++groupCounts[group]
                if (group > numGroups)
                    numGroups = group
            }
            faceGroups = Array(numGroups + 1) {
                val count = groupCounts[it]
                groupCounts[it] = 0
                IntArray(count)
            }
            for (i in 0 until faceCount) {
                val group = faceSkins[i]
                faceGroups[group][groupCounts[group]++] = i
            }
        }
    }

    override fun computeTextureUVCoordinates() {
        TODO("Not yet implemented")
    }

    override fun computeNormals() {
        TODO("Not yet implemented")
    }

    companion object {
        fun create(modelDefinition: ModelDefinition) = QodatModelDefinition(
            name = modelDefinition.getName(),
            vertexCount = modelDefinition.getVertexCount(),
            vertexPositionsX = modelDefinition.getVertexPositionsX(),
            vertexPositionsY = modelDefinition.getVertexPositionsY(),
            vertexPositionsZ = modelDefinition.getVertexPositionsZ(),
            vertexSkins = modelDefinition.getVertexSkins(),
            faceCount = modelDefinition.getFaceCount(),
            faceVertexIndices1 = modelDefinition.getFaceVertexIndices1(),
            faceVertexIndices2 = modelDefinition.getFaceVertexIndices2(),
            faceVertexIndices3 = modelDefinition.getFaceVertexIndices3(),
            faceSkins = modelDefinition.getFaceSkins(),
            faceAlphas = modelDefinition.getFaceAlphas(),
            facePriorities = modelDefinition.getFacePriorities(),
            faceTypes = modelDefinition.getFaceTypes(),
            priority = modelDefinition.getPriority(),
            faceColors = modelDefinition.getFaceColors()
        )
    }
}