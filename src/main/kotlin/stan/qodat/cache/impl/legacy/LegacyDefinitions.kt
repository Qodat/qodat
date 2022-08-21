package stan.qodat.cache.impl.legacy

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import qodat.cache.definition.*
import qodat.cache.models.FaceNormal
import qodat.cache.models.VertexNormal

@Serializable
data class LegacyNpcDefinition(
    override val name: String,
    override val modelIds: Array<String>,
    override val animationIds: Array<String>,
    override val findColor: ShortArray? = null,
    override val replaceColor: ShortArray? = null,
) : NPCDefinition

@Serializable
data class LegacyObjectDefinition(
    override val name: String,
    override val modelIds: Array<String>,
    override val animationIds: Array<String>,
    override val findColor: ShortArray? = null,
    override val replaceColor: ShortArray? = null,
) : ObjectDefinition

@Serializable
data class LegacyItemDefinition(
    override val name: String,
    override val modelIds: Array<String>,
    override val findColor: ShortArray? = null,
    override val replaceColor: ShortArray? = null,
) : ItemDefinition


@Serializable
data class LegacyKitDefinition(
    override val name: String,
    override val bodyPartId: Int,
    override val modelIds: Array<String>,
    override val findColor: ShortArray? = null,
    override val replaceColor: ShortArray? = null,
) : BodyKitDefinition

@Serializable
data class LegacyAnimationDefinition(
    override val id: String,
    override val frameHashes: IntArray,
    override val frameLengths: IntArray,
    override val loopOffset: Int,
    override val leftHandItem: Int,
    override val rightHandItem: Int
) : AnimationDefinition

@Serializable
data class
LegacyAnimationSkeletonDefinition(
    override val id: Int,
    override val transformationTypes: IntArray,
    override val targetVertexGroupsIndices: Array<IntArray>
) : AnimationTransformationGroup

@Serializable
data class LegacyAnimationFrameDefinition(
    val frameHash: Int,
    val showing: Boolean,
    override val transformationCount: Int,
    override val transformationGroupAccessIndices: IntArray,
    override val transformationDeltaX: IntArray,
    override val transformationDeltaY: IntArray,
    override val transformationDeltaZ: IntArray,
    override val transformationGroup: LegacyAnimationSkeletonDefinition
) : AnimationFrameDefinition

@Serializable
data class LegacyModelDefinition(
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
    override fun getFaceNormals() = emptyArray<FaceNormal?>()
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
        fun create(modelDefinition: ModelDefinition) = LegacyModelDefinition(
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
