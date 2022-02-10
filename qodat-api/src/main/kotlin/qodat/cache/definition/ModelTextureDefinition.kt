package qodat.cache.definition

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   2019-05-24
 * @version 1.0
 */
class ModelTextureDefinition(
        val renderTypes : ByteArray,
        var coordinates : ByteArray?,
        var textures : ShortArray?,
        val triangleVertexIndices1 : ShortArray,
        val triangleVertexIndices2 : ShortArray,
        val triangleVertexIndices3 : ShortArray,
        val texturedFaces1 : ShortArray? = null,
        val texturedFaces2 : ShortArray? = null,
        val texturedFaces3 : ShortArray? = null,
        val texturedFaces4 : ShortArray? = null,
        val texturedFaces5 : ByteArray? = null,
        val texturedFaces6 : ShortArray? = null,
        val primaryColors : ShortArray? = null
)