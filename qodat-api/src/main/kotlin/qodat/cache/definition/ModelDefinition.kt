package qodat.cache.definition

import qodat.cache.models.FaceNormal
import qodat.cache.models.VertexNormal

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
interface ModelDefinition {

    fun getName() : String

    fun getVertexCount() : Int
    fun getVertexPositionsX() : IntArray
    fun getVertexPositionsY() : IntArray
    fun getVertexPositionsZ() : IntArray
    fun getVertexSkins() : IntArray?
    fun getVertexGroups() : Array<IntArray>?
    fun getVertexNormals() : Array<VertexNormal>

    fun getFaceCount() : Int
    fun getFaceVertexIndices1() : IntArray
    fun getFaceVertexIndices2() : IntArray
    fun getFaceVertexIndices3() : IntArray
    fun getFaceSkins() : IntArray?
    fun getFaceGroups() : Array<IntArray>?
    fun getFaceColors() : ShortArray
    fun getFaceAlphas() : ByteArray?
    fun getFacePriorities() : ByteArray?
    fun getFaceTypes() : ByteArray?
    fun getFaceNormals() : Array<FaceNormal>
    fun getPriority() : Byte

    fun getTextureConfigCount() : Int
    fun getTextureRenderTypes() : ByteArray?
    fun getFaceTextures() : ShortArray?
    fun getFaceTextureConfigs() : ByteArray?
    fun getTextureTriangleVertexIndices1() : ShortArray?
    fun getTextureTriangleVertexIndices2() : ShortArray?
    fun getTextureTriangleVertexIndices3() : ShortArray?

    fun getFaceTextureUCoordinates() : Array<FloatArray>?
    fun getFaceTextureVCoordinates() : Array<FloatArray>?

    fun computeAnimationTables()
    fun computeTextureUVCoordinates()
    fun computeNormals()

}