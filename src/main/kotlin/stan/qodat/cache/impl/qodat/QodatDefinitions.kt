package stan.qodat.cache.impl.qodat

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import qodat.cache.definition.*
import qodat.cache.models.FaceNormal
import qodat.cache.models.VertexNormal

@Serializable
data class QodatNpcDefinition(
    override val name: String,
    override val modelIds: Array<String>,
    override val animationIds: Array<String>,
    override val findColor: ShortArray? = null,
    override val replaceColor: ShortArray? = null,
) : NPCDefinition

@Serializable
data class QodatObjectDefinition(
    override val name: String,
    override val modelIds: Array<String>,
    override val animationIds: Array<String>,
    override val findColor: ShortArray? = null,
    override val replaceColor: ShortArray? = null,
) : ObjectDefinition

@Serializable
data class QodatItemDefinition(
    override val name: String,
    override val modelIds: Array<String>,
    override val findColor: ShortArray? = null,
    override val replaceColor: ShortArray? = null,
) : ItemDefinition

@Serializable
data class QodatAnimationDefinition(
    override val id: String,
    override val frameHashes: IntArray,
    override val frameLengths: IntArray,
    override val loopOffset: Int,
    override val leftHandItem: Int,
    override val rightHandItem: Int
) : AnimationDefinition

@Serializable
data class QodatAnimationTransformationGroup(
    override val id: Int,
    override val transformationTypes: IntArray,
    override val targetVertexGroupsIndices: Array<IntArray>
) : AnimationTransformationGroup

@Serializable
data class QodatAnimationFrameDefinition(
    val frameHash: Int,
    override val transformationCount: Int,
    override val transformationGroupAccessIndices: IntArray,
    override val transformationDeltaX: IntArray,
    override val transformationDeltaY: IntArray,
    override val transformationDeltaZ: IntArray,
    override val transformationGroup: QodatAnimationTransformationGroup
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
    private val faceColors: ShortArray,
    private var faceTextures: ShortArray? = null,
    private var faceTextureConfigs: ByteArray? = null,
    private var textureConfigCount: Int = 0,
    private var textureRenderTypes: ByteArray? = null,
    private var textureTriangleVertexIndices1: ShortArray? = null,
    private var textureTriangleVertexIndices2: ShortArray? = null,
    private var textureTriangleVertexIndices3: ShortArray? = null,
) : ModelDefinition {

    @Transient
    private var vertexGroups: Array<IntArray>? = null
    @Transient
    private var faceGroups: Array<IntArray>? = null

    @Transient
    private var faceTextureUCoordinates: Array<FloatArray>? = null
    @Transient
    private var faceTextureVCoordinates: Array<FloatArray>? = null

    @Transient
    private var faceNormals: Array<FaceNormal?>? = null

    @Transient
    private var vertexNormals: Array<VertexNormal>? = null

    override fun getName() = name
    override fun getVertexCount() = vertexCount
    override fun getVertexPositionsX() = vertexPositionsX
    override fun getVertexPositionsY() = vertexPositionsY
    override fun getVertexPositionsZ() = vertexPositionsZ
    override fun getVertexSkins() = vertexSkins
    override fun getVertexGroups() = vertexGroups
    override fun getVertexNormals() = vertexNormals
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
    override fun getFaceNormals() = faceNormals
    override fun getPriority() = priority
    override fun getTextureConfigCount() = textureConfigCount
    override fun getTextureRenderTypes() = textureRenderTypes
    override fun getFaceTextures() = faceTextures
    override fun getFaceTextureConfigs() = faceTextureConfigs
    override fun getTextureTriangleVertexIndices1() = textureTriangleVertexIndices1
    override fun getTextureTriangleVertexIndices2() = textureTriangleVertexIndices2
    override fun getTextureTriangleVertexIndices3() = textureTriangleVertexIndices3
    override fun getFaceTextureUCoordinates() = faceTextureUCoordinates
    override fun getFaceTextureVCoordinates() = faceTextureVCoordinates

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
                vertexGroups!![group][groupCounts[group]++] = i
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
                faceGroups!![group][groupCounts[group]++] = i
            }
        }
    }

    override fun computeTextureUVCoordinates() {
        faceTextureUCoordinates = Array(faceCount) { FloatArray(3) }
        faceTextureVCoordinates =  Array(faceCount) { FloatArray(3) }
        for (i in 0 until faceCount){
            var textureConfig = faceTextureConfigs?.get(i)?.toInt()?:-1
            val textureIdx = faceTextures?.get(i)?.toInt()?.and(0xFFFF) ?:-1
            if (textureIdx != -1){
                val u = faceTextureUCoordinates!![i]
                val v = faceTextureVCoordinates!![i]
                if (textureConfig == -1)
                {
                    u[0] = 0.0F;
                    v[0] = 1.0F;

                    u[1] = 1.0F;
                    v[1] = 1.0F;

                    u[2] = 0.0F;
                    v[2] = 0.0F;
                }
                else
                {
                    textureConfig = textureConfig and 0xFF;

                    val textureRenderType = textureRenderTypes?.get(textureConfig)?:0
                    if (textureRenderType == 0.toByte())
                    {
                        val faceVertexIdx1 = faceVertexIndices1[i];
                        val faceVertexIdx2 = faceVertexIndices2[i];
                        val faceVertexIdx3 = faceVertexIndices3[i];

                        val triangleVertexIdx1 = textureTriangleVertexIndices1!![textureConfig].toInt()
                        val triangleVertexIdx2 = textureTriangleVertexIndices2!![textureConfig].toInt()
                        val triangleVertexIdx3 = textureTriangleVertexIndices3!![textureConfig].toInt()

                        mapToUV(u, v, faceVertexIdx1, faceVertexIdx2, faceVertexIdx3, triangleVertexIdx1, triangleVertexIdx2, triangleVertexIdx3);
                    }
                }
            }
        }
    }

    fun mapToUV(
        u: FloatArray,
        v: FloatArray,
        faceVertexIdx1: Int,
        faceVertexIdx2: Int,
        faceVertexIdx3: Int,
        triangleVertexIdx1: Int,
        triangleVertexIdx2: Int,
        triangleVertexIdx3: Int
    ) {
        val triangleX = vertexPositionsX[triangleVertexIdx1].toFloat()
        val triangleY = vertexPositionsY[triangleVertexIdx1].toFloat()
        val triangleZ = vertexPositionsZ[triangleVertexIdx1].toFloat()
        val f_882_ = vertexPositionsX[triangleVertexIdx2] - triangleX
        val f_883_ = vertexPositionsY[triangleVertexIdx2] - triangleY
        val f_884_ = vertexPositionsZ[triangleVertexIdx2] - triangleZ
        val f_885_ = vertexPositionsX[triangleVertexIdx3] - triangleX
        val f_886_ = vertexPositionsY[triangleVertexIdx3] - triangleY
        val f_887_ = vertexPositionsZ[triangleVertexIdx3] - triangleZ
        val f_888_ = vertexPositionsX[faceVertexIdx1] - triangleX
        val f_889_ = vertexPositionsY[faceVertexIdx1] - triangleY
        val f_890_ = vertexPositionsZ[faceVertexIdx1] - triangleZ
        val f_891_ = vertexPositionsX[faceVertexIdx2] - triangleX
        val f_892_ = vertexPositionsY[faceVertexIdx2] - triangleY
        val f_893_ = vertexPositionsZ[faceVertexIdx2] - triangleZ
        val f_894_ = vertexPositionsX[faceVertexIdx3] - triangleX
        val f_895_ = vertexPositionsY[faceVertexIdx3] - triangleY
        val f_896_ = vertexPositionsZ[faceVertexIdx3] - triangleZ
        val f_897_ = f_883_ * f_887_ - f_884_ * f_886_
        val f_898_ = f_884_ * f_885_ - f_882_ * f_887_
        val f_899_ = f_882_ * f_886_ - f_883_ * f_885_
        var f_900_ = f_886_ * f_899_ - f_887_ * f_898_
        var f_901_ = f_887_ * f_897_ - f_885_ * f_899_
        var f_902_ = f_885_ * f_898_ - f_886_ * f_897_
        var f_903_ = 1.0f / (f_900_ * f_882_ + f_901_ * f_883_ + f_902_ * f_884_)
        u[0] = f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_ * f_903_
        u[1] = f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_ * f_903_
        u[2] = f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_ * f_903_
        f_900_ = f_883_ * f_899_ - f_884_ * f_898_
        f_901_ = f_884_ * f_897_ - f_882_ * f_899_
        f_902_ = f_882_ * f_898_ - f_883_ * f_897_
        f_903_ = 1.0f / (f_900_ * f_885_ + f_901_ * f_886_ + f_902_ * f_887_)
        v[0] = f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_ * f_903_
        v[1] = f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_ * f_903_
        v[2] = f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_ * f_903_
    }
    override fun computeNormals() {

        if (this.vertexNormals != null)
            return

        vertexNormals =  Array(vertexCount) { VertexNormal() }

        for (var1 in 0 until faceCount) {
            val vertexA = faceVertexIndices1[var1]
            val vertexB = faceVertexIndices2[var1]
            val vertexC = faceVertexIndices3[var1]
            val xA = vertexPositionsX[vertexB] - vertexPositionsX[vertexA]
            val yA = vertexPositionsY[vertexB] - vertexPositionsY[vertexA]
            val zA = vertexPositionsZ[vertexB] - vertexPositionsZ[vertexA]

            val xB = vertexPositionsX[vertexC] - vertexPositionsX[vertexA]
            val yB = vertexPositionsY[vertexC] - vertexPositionsY[vertexA]
            val zB = vertexPositionsZ[vertexC] - vertexPositionsZ[vertexA]

            // Compute cross product
            var var11: Int = yA * zB - yB * zA
            var var12: Int = zA * xB - zB * xA
            var var13: Int = xA * yB - xB * yA

            while (var11 > 8192 || var12 > 8192 || var13 > 8192 || var11 < -8192 || var12 < -8192 || var13 < -8192) {
                var11 = var11 shr 1
                var12 = var12 shr 1
                var13 = var13 shr 1
            }

            var length = Math.sqrt((var11 * var11 + var12 * var12 + var13 * var13).toDouble()).toInt()
            if (length <= 0) {
                length = 1
            }

            var11 = var11 * 256 / length
            var12 = var12 * 256 / length
            var13 = var13 * 256 / length

            val var15 = faceTypes?.get(var1)?:0

            if (var15.toInt() == 0) {
                var var16 = vertexNormals!![vertexA]
                var16.x += var11
                var16.y += var12
                var16.z += var13
                ++var16.magnitude
                var16 = vertexNormals!![vertexB]
                var16.x += var11
                var16.y += var12
                var16.z += var13
                ++var16.magnitude
                var16 = vertexNormals!![vertexC]
                var16.x += var11
                var16.y += var12
                var16.z += var13
                ++var16.magnitude
            } else if (var15.toInt() == 1) {
                if (faceNormals == null)
                    faceNormals = arrayOfNulls(faceCount)
                faceNormals!![var1] = FaceNormal().apply {
                    x = var11
                    y = var12
                    z = var13
                }
            }
        }
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
            faceColors = modelDefinition.getFaceColors(),
            faceTextures = modelDefinition.getFaceTextures(),
            faceTextureConfigs = modelDefinition.getFaceTextureConfigs(),
            textureRenderTypes = modelDefinition.getTextureRenderTypes(),
            textureTriangleVertexIndices1 = modelDefinition.getTextureTriangleVertexIndices1(),
            textureTriangleVertexIndices2 = modelDefinition.getTextureTriangleVertexIndices2(),
            textureTriangleVertexIndices3 = modelDefinition.getTextureTriangleVertexIndices3()
        )
    }
}
