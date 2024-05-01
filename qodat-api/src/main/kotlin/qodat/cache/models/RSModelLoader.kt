package qodat.cache.models

import qodat.cache.definition.ModelDefinition
import qodat.cache.definition.ModelTextureDefinition
import qodat.cache.io.InputStream
import java.util.logging.Logger
import kotlin.experimental.and

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   2019-05-24
 * @version 1.0
 */
class RSModelLoader {

    private val logger = Logger.getLogger(RSModelLoader::class.java.simpleName)

    fun load(modelId: String, data: ByteArray) =
        when {
            isRS3(data) -> {
                try {
                    loadRS3(modelId, data)
                } catch (e: Exception){
                    tryOtherLoadMethods(modelId, data, "RS3")
                }
            }
            isType3(data) || isType2(data) || isType1(data) -> {
                try {
                    ModelLoader().load(modelId.toIntOrNull() ?: hashCode(), data).let {

                        RS2Model().apply {
                            setId(it.id.toString())
                            setPriority(it.priority)
                            setFaceCount(it.faceCount)
                            setFaceColors(it.faceColors)
                            setFaceAlphas(it.faceTransparencies)
                            setVertexSkins(it.packedVertexGroups)
                            setVertexCount(it.vertexCount)
                            setVertexPositionsX(it.vertexX)
                            setVertexPositionsY(it.vertexY)
                            setVertexPositionsZ(it.vertexZ)
                            setFaceVertexIndices1(it.faceIndices1)
                            setFaceVertexIndices2(it.faceIndices2)
                            setFaceVertexIndices3(it.faceIndices3)
//                            setFaceSkins(it.packedTransparencyVertexGroups)
                            setFaceTextures(it.faceTextures)
                            setTextureRenderTypes(it.textureRenderTypes)
                            texturePrimaryColors = it.texturePrimaryColors
                            setTextureTriangleVertexIndices1(it.texIndices1)
                            setTextureTriangleVertexIndices2(it.texIndices2)
                            setTextureTriangleVertexIndices3(it.texIndices3)
                            setFaceTextureConfigs(it.textureCoords)
                            faceRenderPriorities = it.faceRenderPriorities
                            faceRenderTypes = it.faceRenderTypes
                            setMayaGroups(it.animayaGroups)
                            setMayaScales(it.animayaScales)

                        }
                    }
                } catch (e: Exception) {
                    tryOtherLoadMethods(modelId, data, "OSRS")
                }
            }
            else -> loadLowRev(modelId, data)
        }

    private fun tryOtherLoadMethods(
        modelId: String,
        data: ByteArray,
        current: String,
    ): ModelDefinition {
        logger.severe("Failed to load model $modelId as a $current model")
        logger.info("Trying to load as RS2 High rev model")
        return try {
            decodeHighRev(modelId, data)
        } catch (e2: Exception) {
            logger.severe("Failed to load model $modelId as a high rev model")
            logger.info("Trying to load as RS2 low rev model")
            loadLowRev(modelId, data)
        }
    }

    private fun loadRS3(modelId: String, data: ByteArray): ModelDefinition {
        TODO("Not yet implemented")
    }

    private fun decodeHighRev(modelId: String, data: ByteArray): ModelDefinition {

        val input1 = InputStream(data)
        val input2 = InputStream(data)
        val input3 = InputStream(data)
        val input4 = InputStream(data)
        val input5 = InputStream(data)
        val input6 = InputStream(data)
        val input7 = InputStream(data)

        input1.offset = data.size - HIGH_REV_HEADER_LENGTH

        val vertexCount = input1.readUnsignedShort()
        val faceCount = input1.readUnsignedShort()
        val textureConfigCount = input1.readUnsignedByte()

        val l1: Int = input1.readUnsignedByte()
        val renderFlag = 0x1 and l1 xor -0x1 == -2
        val bool_78_ = l1 and 0x2 xor -0x1 == -3
        val bool_25_ = 0x4 and l1 == 4
        val bool_26_ = 0x8 and l1 == 8
        if (!bool_26_) {
            return loadMidRev(
                modelId,
                vertexCount,
                faceCount,
                textureConfigCount,
                l1,
                input1,
                input2,
                input3,
                input4,
                input5,
                input6,
                input7
            )
        }
        var newformat = 0
        if (bool_26_) {
            input1.offset -= 7
            newformat = input1.readUnsignedByte()
            input1.offset += 6
        }
        println("format = $newformat")

        val renderPriority = input1.readUnsignedByte()
        val transparencyFlag = input1.readUnsignedByte() == 1
        val animationFaceFlag = input1.readUnsignedByte() == 1
        val textureFlag = input1.readUnsignedByte() == 1
        val animationVertexFlag = input1.readUnsignedByte() == 1
        val pointsXLength = input1.readUnsignedShort()
        val pointsYLength = input1.readUnsignedShort()
        val pointsZLength = input1.readUnsignedShort()
        val triangleLength = input1.readUnsignedShort()
        val texturedCoordLength = input1.readUnsignedShort()

        var textureCount1 = 0
        var textureCount2 = 0
        var textureCount3 = 0

        var textureRenderTypes: ByteArray? = null
        if (textureConfigCount > 0) {
            input1.offset = 0
            textureRenderTypes = ByteArray(textureConfigCount) {
                val type = input1.readByte()
                when (type.toInt()) {
                    0 -> ++textureCount1
                    in 1..3 -> ++textureCount2
                    2 -> ++textureCount3
                }
                type
            }
        }

        var position = textureConfigCount + vertexCount
        val renderTypeStart = position

        if (renderFlag)
            position += faceCount

        if (l1 == 1)
            position += faceCount

        val vertexOffsetStart = position
        position += faceCount

        val vertexPriorityStart = position
        if (renderPriority == 255) position += faceCount

        val triangleSkinStart = position
        if (animationFaceFlag) position += faceCount

        val vertexSkinStart = position
        if (animationVertexFlag) position += vertexCount

        val alphaStart = position
        if (transparencyFlag) position += faceCount

        val triangleCoordStart = position
        position += triangleLength

        val texturedTriangleStart = position
        if (textureFlag) position += (faceCount * 2)

        val texturedTriangleCoordStart = position
        position += texturedCoordLength

        val coloredTriangleCoordStart = position
        position += (faceCount * 2)
        val pointsXStart = position
        position += pointsXLength
        val pointsYStart = position
        position += pointsYLength
        val pointsZStart = position
        position += pointsZLength
        val texture1Start = position
        position += (textureCount1 * 6)
        val texture2Start1 = position
        position += (textureCount2 * 6)
        var i_59_ = 6
        if (newformat != 14) {
            if (newformat >= 15) i_59_ = 9
        } else i_59_ = 7
        val texture2Start2: Int = position
        position += i_59_ * textureCount2
        val texture2Start3: Int = position
        position += textureCount2
        val texture2Start: Int = position
        position += textureCount2
        val texture3Start: Int = position
        position += textureCount2 + textureCount3 * 2
        val vertexPositionsX = IntArray(vertexCount)
        val vertexPositionsY = IntArray(vertexCount)
        val vertexPositionsZ = IntArray(vertexCount)

        val faceVertexIndices1 = IntArray(faceCount)
        val faceVertexIndices2 = IntArray(faceCount)
        val faceVertexIndices3 = IntArray(faceCount)

        val vertexSkins = if (animationVertexFlag) IntArray(vertexCount) else null
        val faceRenderTypes = if (renderFlag) ByteArray(faceCount) else null
        val faceRenderPriorities = if (renderPriority == 255) ByteArray(faceCount) else null
        val priority = if (renderPriority != 255) renderPriority.toByte() else 0.toByte()
        val faceAlphas = if (transparencyFlag) ByteArray(faceCount) else null
        val faceSkins = if (animationFaceFlag) IntArray(faceCount) else null
        val faceTextures = if (textureFlag) ShortArray(faceCount) else null
        val textureCoordinates = if (textureFlag && textureConfigCount > 0) ByteArray(faceCount) else null
        val faceColors = ShortArray(faceCount)

        val textureDefinition = if (textureConfigCount > 0) loadTextureDefinition(
            textureConfigCount,
            textureCount2,
            textureCount3,
            textureRenderTypes!!,
            textureCoordinates,
            faceTextures
        ) else null

        input1.offset = textureConfigCount
        input2.offset = pointsXStart
        input3.offset = pointsYStart
        input4.offset = pointsZStart
        input5.offset = vertexSkinStart

        readVertices(
            input1,
            input2,
            input3,
            input4,
            input5,
            animationVertexFlag,
            vertexCount,
            vertexSkins,
            vertexPositionsX,
            vertexPositionsZ,
            vertexPositionsY
        )

        input1.offset = coloredTriangleCoordStart
        input2.offset = renderTypeStart
        input3.offset = vertexPriorityStart
        input4.offset = alphaStart
        input5.offset = triangleSkinStart
        input6.offset = texturedTriangleStart
        input7.offset = texturedTriangleCoordStart

        readTriangleRenderInformation(
            input1, input2, input3, input4, input5, input6, input7,
            faceCount,
            textureCoordinates,
            faceRenderPriorities,
            faceRenderTypes,
            faceTextures,
            faceSkins,
            faceColors,
            faceAlphas
        )

        input1.offset = triangleCoordStart
        input2.offset = vertexOffsetStart

        readVertexIndices(
            input1, input2,
            faceCount,
            faceVertexIndices1,
            faceVertexIndices2,
            faceVertexIndices3
        )

        input1.offset = texture1Start
        input2.offset = texture2Start1
        input3.offset = texture2Start2
        input4.offset = texture2Start3
        input5.offset = texture2Start
        input6.offset = texture3Start

        if (textureDefinition != null)
            readTextureData(false, textureDefinition, textureConfigCount, input1, input2, input3, input4, input5, input6)

        input1.offset = position

        val unknown = input1.readUnsignedByte()

        if (unknown != 0) {
            input1.readUnsignedShort()
            input1.readUnsignedShort()
            input1.readUnsignedShort()
            input1.readInt()
        }

        val definition = RS2Model()
        definition.setId(modelId)
        definition.setFormat(newformat)
        definition.setPriority(priority)
        definition.setVertexCount(vertexCount)
        definition.setVertexPositionsX(vertexPositionsX)
        definition.setVertexPositionsY(vertexPositionsY)
        definition.setVertexPositionsZ(vertexPositionsZ)
        definition.setVertexSkins(vertexSkins)

        definition.setFaceCount(faceCount)
        definition.setFaceVertexIndices1(faceVertexIndices1)
        definition.setFaceVertexIndices2(faceVertexIndices2)
        definition.setFaceVertexIndices3(faceVertexIndices3)
        definition.setFaceSkins(faceSkins)
        definition.setFaceColors(faceColors)
        definition.setFaceAlphas(faceAlphas)
        definition.faceRenderPriorities = faceRenderPriorities
        definition.faceRenderTypes = faceRenderTypes
        if (textureDefinition != null) {
            definition.setFaceTextures(textureDefinition.textures)
            definition.setFaceTextureConfigs(textureDefinition.coordinates)
            definition.setTextureRenderTypes(textureDefinition.renderTypes)
            definition.setTextureTriangleVertexIndices1(textureDefinition.triangleVertexIndices1)
            definition.setTextureTriangleVertexIndices2(textureDefinition.triangleVertexIndices2)
            definition.setTextureTriangleVertexIndices3(textureDefinition.triangleVertexIndices3)
        }
        return definition
    }

    fun loadMidRev(
        modelId: String,
        vertexCount: Int,
        faceCount: Int,
        textureConfigCount: Int,
        l1: Int,
        input1: InputStream,
        input2: InputStream,
        input3: InputStream,
        input4: InputStream,
        input5: InputStream,
        input6: InputStream,
        input7: InputStream
    ): RS2Model {

        val renderFlag = (0x1 and l1).inv() == -2

        val renderPriority = input1.readUnsignedByte()
        val transparencyFlag = input1.readUnsignedByte() == 1
        val animationFaceFlag = input1.readUnsignedByte() == 1
        val textureFlag = input1.readUnsignedByte() == 1
        val animationVertexFlag = input1.readUnsignedByte() == 1
        val pointsXLength = input1.readUnsignedShort()
        val pointsYLength = input1.readUnsignedShort()
        val pointsZLength = input1.readUnsignedShort()
        val triangleLength = input1.readUnsignedShort()
        val texturedCoordLength = input1.readUnsignedShort()

        var textureCount1 = 0
        var textureCount2 = 0
        var textureCount3 = 0

        var textureRenderTypes: ByteArray? = null
        if (textureConfigCount > 0) {
            input1.offset = 0
            textureRenderTypes = ByteArray(textureConfigCount) {
                val type = input1.readByte()
                when (type.toInt()) {
                    0 -> ++textureCount1
                    in 1..3 -> ++textureCount2
                    2 -> ++textureCount3
                }
                type
            }
        }

        var position = textureConfigCount + vertexCount
        val renderTypeStart = position
        if (l1 == 1)
            position += faceCount

        val vertexOffsetStart = position
        position += faceCount
        val vertexPriorityStart = position
        if (renderPriority == 255) position += faceCount
        val triangleSkinStart = position
        if (animationFaceFlag) position += faceCount
        val vertexSkinStart = position
        if (animationVertexFlag) position += vertexCount
        val alphaStart = position
        if (transparencyFlag) position += faceCount
        val triangleCoordStart = position
        position += triangleLength
        val texturedTriangleStart = position
        if (textureFlag) position += faceCount * 2
        val texturedTriangleCoordStart = position
        position += texturedCoordLength
        val coloredTriangleCoordStart = position
        position += faceCount * 2
        val pointsXStart = position
        position += pointsXLength
        val pointsYStart = position
        position += pointsYLength
        val pointsZStart = position
        position += pointsZLength
        val texture1Start = position
        position += textureCount1 * 6
        val texture2Start1 = position
        position += textureCount2 * 6
        val texture2Start2 = position
        position += textureCount2 * 6
        val texture2Start3 = position
        position += textureCount2
        val texture2Start = position
        position += textureCount2
        val texture3Start = position
        position += textureCount2 + textureCount3 * 2

        val vertexPositionsX = IntArray(vertexCount)
        val vertexPositionsY = IntArray(vertexCount)
        val vertexPositionsZ = IntArray(vertexCount)

        val faceVertexIndices1 = IntArray(faceCount)
        val faceVertexIndices2 = IntArray(faceCount)
        val faceVertexIndices3 = IntArray(faceCount)

        val vertexSkins = if (animationVertexFlag) IntArray(vertexCount) else null
        val faceRenderTypes = if (renderFlag) ByteArray(faceCount) else null
        val faceRenderPriorities = if (renderPriority == 255) ByteArray(faceCount) else null
        val priority = if (renderPriority == 255) 0.toByte() else renderPriority.toByte()
        val faceAlphas = if (transparencyFlag) ByteArray(faceCount) else null
        val faceSkins = if (animationFaceFlag) IntArray(faceCount) else null
        val faceTextures = if (textureFlag) ShortArray(faceCount) else null
        val textureCoordinates = if (textureFlag && textureConfigCount > 0) ByteArray(faceCount) else null
        val faceColors = ShortArray(faceCount)

        val textureDefinition = if (textureConfigCount > 0) loadTextureDefinition(
            textureConfigCount,
            textureCount2,
            textureCount3,
            textureRenderTypes!!,
            textureCoordinates,
            faceTextures
        ) else null

        input1.offset = textureConfigCount
        input2.offset = pointsXStart
        input3.offset = pointsYStart
        input4.offset = pointsZStart
        input5.offset = vertexSkinStart

        readVertices(
            input1,
            input2,
            input3,
            input4,
            input5,
            animationVertexFlag,
            vertexCount,
            vertexSkins,
            vertexPositionsX,
            vertexPositionsZ,
            vertexPositionsY
        )

        input1.offset = coloredTriangleCoordStart
        input2.offset = renderTypeStart
        input3.offset = vertexPriorityStart
        input4.offset = alphaStart
        input5.offset = triangleSkinStart
        input6.offset = texturedTriangleStart
        input7.offset = texturedTriangleCoordStart

        readTriangleRenderInformation(
            input1, input2, input3, input4, input5, input6, input7,
            faceCount,
            textureCoordinates,
            faceRenderPriorities,
            faceRenderTypes,
            faceTextures,
            faceSkins,
            faceColors,
            faceAlphas
        )

        input1.offset = triangleCoordStart
        input2.offset = vertexOffsetStart

        readVertexIndices(
            input1, input2,
            faceCount,
            faceVertexIndices1,
            faceVertexIndices2,
            faceVertexIndices3
        )

        input1.offset = texture1Start
        input2.offset = texture2Start1
        input3.offset = texture2Start2
        input4.offset = texture2Start3
        input5.offset = texture2Start
        input6.offset = texture3Start

        if (textureDefinition != null)
            readTextureData(true, textureDefinition, textureConfigCount, input1, input2, input3, input4, input5, input6)

        val definition = RS2Model()
        definition.setId(modelId)
        definition.setPriority(priority)
        definition.setVertexCount(vertexCount)
        definition.setVertexPositionsX(vertexPositionsX)
        definition.setVertexPositionsY(vertexPositionsY)
        definition.setVertexPositionsZ(vertexPositionsZ)
        definition.setVertexSkins(vertexSkins)

        definition.setFaceCount(faceCount)
        definition.setFaceVertexIndices1(faceVertexIndices1)
        definition.setFaceVertexIndices2(faceVertexIndices2)
        definition.setFaceVertexIndices3(faceVertexIndices3)
        definition.setFaceSkins(faceSkins)
        definition.setFaceColors(faceColors)
        definition.setFaceAlphas(faceAlphas)
        definition.faceRenderPriorities = faceRenderPriorities
        definition.faceRenderTypes = faceRenderTypes

        if (textureDefinition != null) {
            definition.setFaceTextures(textureDefinition.textures)
            definition.setFaceTextureConfigs(textureDefinition.coordinates)
            definition.setTextureRenderTypes(textureDefinition.renderTypes)
            definition.setTextureTriangleVertexIndices1(textureDefinition.triangleVertexIndices1)
            definition.setTextureTriangleVertexIndices2(textureDefinition.triangleVertexIndices2)
            definition.setTextureTriangleVertexIndices3(textureDefinition.triangleVertexIndices3)
        }
        return definition
    }
    private fun loadLowRev(modelId: String, data: ByteArray): ModelDefinition {

        val input1 = InputStream(data)
        val input2 = InputStream(data)
        val input3 = InputStream(data)
        val input4 = InputStream(data)
        val input5 = InputStream(data)

        input1.offset = data.size - LOW_REV_HEADER_LENGTH

        val vertexCount = input1.readUnsignedShort()
        val faceCount = input1.readUnsignedShort()
        val textureConfigCount = input1.readUnsignedByte()
        val renderFlag = input1.readUnsignedByte() == 1
        val renderPriority = input1.readUnsignedByte()
        val transparencyFlag = input1.readUnsignedByte() == 1
        val animationFaceFlag = input1.readUnsignedByte() == 1
        val animationVertexFlag = input1.readUnsignedByte() == 1
        val pointXLength = input1.readUnsignedShort()
        val pointYLength = input1.readUnsignedShort()
        val pointZLength = input1.readUnsignedShort()
        val triangleLength = input1.readUnsignedShort()

        var dataLength = vertexCount

        val faceTypesOnset = dataLength
        dataLength += faceCount

        val faceRenderPriorityOnset = dataLength
        if (renderPriority == 255) dataLength += faceCount

        val faceSkinsOnset = dataLength
        if (animationFaceFlag) dataLength += faceCount

        val faceRenderTypeOnset = dataLength
        if (renderFlag) dataLength += faceCount

        val vertexSkinsOnset = dataLength
        if (animationVertexFlag) dataLength += vertexCount

        val faceTransparencyOnset = dataLength
        if (transparencyFlag) dataLength += faceCount

        val faceVertexIndicesOnset = dataLength
        dataLength += triangleLength

        val faceColorsOnset = dataLength
        dataLength += (faceCount * 2)

        val texturedFaceOnset = dataLength
        dataLength += (textureConfigCount * 6)

        val pointXOnset = dataLength
        dataLength += pointXLength

        val pointYOnset = dataLength
        dataLength += pointYLength

        val vertexPositionsX = IntArray(vertexCount)
        val vertexPositionsY = IntArray(vertexCount)
        val vertexPositionsZ = IntArray(vertexCount)

        val faceVertexIndices1 = IntArray(faceCount)
        val faceVertexIndices2 = IntArray(faceCount)
        val faceVertexIndices3 = IntArray(faceCount)

        val faceSkins = if (animationFaceFlag) IntArray(faceCount) else null
        val vertexSkins = if (animationVertexFlag) IntArray(vertexCount) else null


        val faceRenderPriorities = if (renderPriority == 255) ByteArray(faceCount) else null
        var faceRenderTypes = if (renderFlag) ByteArray(faceCount) else null
        val faceTextures = if (renderFlag) ShortArray(faceCount) else null
        val faceAlphas = if (transparencyFlag) ByteArray(faceCount) else null
        val faceColors = ShortArray(faceCount)

        val priority = if (renderPriority != 255) renderPriority.toByte() else 0.toByte()

        val textureCoordinates = if (renderFlag) ByteArray(faceCount) else null
        val textureDefinition = if (textureConfigCount > 0) loadTextureDefinition(
            textureConfigCount,
            textureCoordinates,
            faceTextures
        ) else null

        input1.offset = 0
        input2.offset = pointXOnset
        input3.offset = pointYOnset
        input4.offset = dataLength
        input5.offset = vertexSkinsOnset

        readVertices(
            input1,
            input2,
            input3,
            input4,
            input5,
            animationVertexFlag,
            vertexCount,
            vertexSkins,
            vertexPositionsX,
            vertexPositionsZ,
            vertexPositionsY
        )

        input1.offset = faceColorsOnset
        input2.offset = faceRenderTypeOnset
        input3.offset = faceRenderPriorityOnset
        input4.offset = faceTransparencyOnset
        input5.offset = faceSkinsOnset

        val flagPairs = readTriangleRenderInformation(
            input1, input2, input3, input4, input5,
            faceCount,
            textureCoordinates,
            faceRenderPriorities,
            faceRenderTypes,
            faceTextures,
            faceSkins,
            faceColors,
            faceAlphas
        )

        input1.offset = faceVertexIndicesOnset
        input2.offset = faceTypesOnset

        readVertexIndices(
            input1, input2,
            faceCount,
            faceVertexIndices1,
            faceVertexIndices2,
            faceVertexIndices3
        )

        input1.offset = texturedFaceOnset

        if (textureDefinition != null) {
            readTextureData(textureDefinition, textureConfigCount, input1)

            if (textureCoordinates != null) {
                var flagged = false

                for (vertex in 0 until vertexCount) {

                    if(vertex >= textureCoordinates.size)
                        break

                    val coord = textureCoordinates[vertex].toInt() and 255
                    if (coord != 255) {
                        if (textureDefinition.triangleVertexIndices1[coord].and('\uffff'.toShort()).toInt() == faceVertexIndices1[vertex]
                            && textureDefinition.triangleVertexIndices2[coord].and('\uffff'.toShort()).toInt() == faceVertexIndices2[vertex]
                            && textureDefinition.triangleVertexIndices3[coord].and('\uffff'.toShort()).toInt() == faceVertexIndices3[vertex]
                        ) {
                            textureCoordinates[vertex] = -1
                        } else
                            flagged = true
                    }
                }

                if (!flagged)
                    textureDefinition.coordinates = null
            }
        }

        if (!flagPairs.second)
            textureDefinition?.textures = null

        if (!flagPairs.first)
            faceRenderTypes = null

        val definition = RS2Model()
        definition.setId(modelId)
        definition.setPriority(priority)
        definition.setVertexCount(vertexCount)
        definition.setVertexPositionsX(vertexPositionsX)
        definition.setVertexPositionsY(vertexPositionsY)
        definition.setVertexPositionsZ(vertexPositionsZ)
        definition.setVertexSkins(vertexSkins)

        definition.setFaceCount(faceCount)
        definition.setFaceVertexIndices1(faceVertexIndices1)
        definition.setFaceVertexIndices2(faceVertexIndices2)
        definition.setFaceVertexIndices3(faceVertexIndices3)
        definition.setFaceSkins(faceSkins)
        definition.setFaceColors(faceColors)
        definition.setFaceAlphas(faceAlphas)
        definition.faceRenderPriorities = faceRenderPriorities
        definition.faceRenderTypes = faceRenderTypes
        if (textureDefinition != null) {
            definition.setFaceTextures(textureDefinition.textures)
            definition.setFaceTextureConfigs(textureDefinition.coordinates)
            definition.setTextureRenderTypes(textureDefinition.renderTypes)
            definition.setTextureTriangleVertexIndices1(textureDefinition.triangleVertexIndices1)
            definition.setTextureTriangleVertexIndices2(textureDefinition.triangleVertexIndices2)
            definition.setTextureTriangleVertexIndices3(textureDefinition.triangleVertexIndices3)
        }
        return definition
    }

    private fun readTextureData(
        midRev: Boolean = false,
        textureDefinition: ModelTextureDefinition,
        textureConfigCount: Int,
        input1: InputStream,
        input2: InputStream,
        input3: InputStream,
        input4: InputStream,
        input5: InputStream,
        input6: InputStream
    ) {
        for (config in 0 until textureConfigCount) {

            val type = textureDefinition.renderTypes[config].toInt() and 255

            when (type) {
                0 ->
                    readTexturedTrianglePositions(textureDefinition, config, input1)
                1 -> {
                    readTexturedTrianglePositions(textureDefinition, config, input2)
                    readTextureAnimation(midRev, textureDefinition, config, input3, input4, input5, input6)
                }
                2 -> {
                    readTexturedTrianglePositions(textureDefinition, config, input2)
                    readTextureAnimation(midRev, textureDefinition, config, input3, input4, input5, input6)
                    textureDefinition.primaryColors?.set(config, input6.readUnsignedShort().toShort())
                }
                3 -> {
                    readTexturedTrianglePositions(textureDefinition, config, input2)
                    readTextureAnimation(midRev, textureDefinition, config, input3, input4, input5, input6)
                }
            }
        }
    }

    private fun readTextureData(
        textureDefinition: ModelTextureDefinition,
        texturedTriangleCount: Int,
        input1: InputStream
    ) {
        for (triangle in 0 until texturedTriangleCount) {
            textureDefinition.renderTypes[triangle] = 0
            readTexturedTrianglePositions(textureDefinition, triangle, input1)
        }
    }

    private fun readTexturedTrianglePositions(
        textureDefinition: ModelTextureDefinition,
        triangle: Int,
        input1: InputStream
    ) {
        textureDefinition.triangleVertexIndices1[triangle] = input1.readUnsignedShort().toShort()
        textureDefinition.triangleVertexIndices2[triangle] = input1.readUnsignedShort().toShort()
        textureDefinition.triangleVertexIndices3[triangle] = input1.readUnsignedShort().toShort()
    }

    private fun readTextureAnimation(
        midRev: Boolean,
        textureDefinition: ModelTextureDefinition,
        triangle: Int,
        input3: InputStream,
        input4: InputStream,
        input5: InputStream,
        input6: InputStream
    ) {
        textureDefinition.texturedFaces1!![triangle] = input3.readUnsignedShort().toShort()
        textureDefinition.texturedFaces2!![triangle] = input3.readUnsignedShort().toShort()
        textureDefinition.texturedFaces3!![triangle] = input3.readUnsignedShort().toShort()
        textureDefinition.texturedFaces4!![triangle] = if(midRev) input4.readByte().toShort() else input4.readUnsignedShort().toShort()
        textureDefinition.texturedFaces5!![triangle] = input5.readByte()
        textureDefinition.texturedFaces6!![triangle] = if(midRev) input6.readByte().toShort() else input6.readUnsignedShort().toShort()
    }

    private fun readVertices(
        input1: InputStream,
        input2: InputStream,
        input3: InputStream,
        input4: InputStream,
        input5: InputStream,
        readVertexSkins: Boolean,
        vertexCount: Int,
        vertexSkins: IntArray?,
        vertexPositionsX: IntArray,
        vertexPositionsZ: IntArray,
        vertexPositionsY: IntArray
    ) {
        var lastVertexPositionX = 0
        var lastVertexPositionY = 0
        var lastVertexPositionZ = 0

        for (point in 0 until vertexCount) {

            val flag = input1.readUnsignedByte()
            val vertexXOffset = if (isFlagged(flag, 1)) input2.readShortSmart() else 0
            val vertexYOffset = if (isFlagged(flag, 2)) input3.readShortSmart() else 0
            val vertexZOffset = if (isFlagged(flag, 4)) input4.readShortSmart() else 0

            vertexPositionsX[point] = lastVertexPositionX + vertexXOffset
            vertexPositionsY[point] = lastVertexPositionY + vertexYOffset
            vertexPositionsZ[point] = lastVertexPositionZ + vertexZOffset

            lastVertexPositionX = vertexPositionsX[point]
            lastVertexPositionY = vertexPositionsY[point]
            lastVertexPositionZ = vertexPositionsZ[point]

            if (readVertexSkins)
                vertexSkins!![point] = input5.readUnsignedByte()
        }
    }

    private fun readTriangleRenderInformation(
        input1: InputStream,
        input2: InputStream,
        input3: InputStream,
        input4: InputStream,
        input5: InputStream,
        input6: InputStream,
        input7: InputStream,
        triangleCount: Int,
        textureCoordinates: ByteArray?,
        faceRenderPriorities: ByteArray?,
        faceRenderTypes: ByteArray?,
        faceTextures: ShortArray?,
        faceSkins: IntArray?,
        faceColors: ShortArray,
        faceAlphas: ByteArray?
    ) {
        for (point in 0 until triangleCount) {

            faceColors[point] = input1.readUnsignedShort().toShort()

            faceRenderTypes?.set(point, input2.readByte())
            faceRenderPriorities?.set(point, input3.readByte())
            faceAlphas?.set(point, input4.readByte())
            faceSkins?.set(point, input5.readUnsignedByte())
            faceTextures?.set(point, (input6.readUnsignedShort() - 1).toShort())

            if (textureCoordinates != null && faceTextures!![point] != (-1).toShort())
                textureCoordinates[point] = (input7.readUnsignedByte() - 1).toByte()
        }
    }

    private fun readTriangleRenderInformation(
        input1: InputStream,
        input2: InputStream,
        input3: InputStream,
        input4: InputStream,
        input5: InputStream,
        triangleCount: Int,
        faceTextureConfigs: ByteArray?,
        faceRenderPriorities: ByteArray?,
        faceRenderTypes: ByteArray?,
        faceTextures: ShortArray?,
        faceSkins: IntArray?,
        faceColors: ShortArray,
        faceAlphas: ByteArray?
    ): Pair<Boolean, Boolean> {

        var faceRenderTypeFlag1 = false
        var faceRenderTypeFlag2 = false

        for (point in 0 until triangleCount) {

            faceColors[point] = input1.readUnsignedShort().toShort()

            if (faceRenderTypes != null) {
                val mask = input2.readUnsignedByte()

                if ((mask and 1) == 1) {
                    faceRenderTypes[point] = 1
                    faceRenderTypeFlag1 = true
                } else
                    faceRenderTypes[point] = 0

                if ((mask and 2) == 2) {
                    faceTextureConfigs!![point] = (mask shr 2).toByte()
                    faceTextures!![point] = faceColors[point]
                    faceColors[point] = 127
                    if (faceTextures[point] != (-1).toShort())
                        faceRenderTypeFlag2 = true
                } else {
                    faceTextureConfigs!![point] = -1
                    faceTextures!![point] = -1
                }
            }

            faceRenderPriorities?.set(point, input3.readByte())
            faceAlphas?.set(point, input4.readByte())
            faceSkins?.set(point, input5.readUnsignedByte())
        }
        return Pair(faceRenderTypeFlag1, faceRenderTypeFlag2)
    }
    private fun readVertexIndicesRS3(
        input1: InputStream,
        input2: InputStream,
        faceCount: Int,
        faceVertexIndices1: IntArray,
        faceVertexIndices2: IntArray,
        faceVertexIndices3: IntArray
    ) {

        var vertex1 = 0
        var vertex2 = 0
        var vertex3 = 0
        var vertexOffset = 0

        val c = 6397
        var fail1 = 0;var fail2 = 0;var fail3 = 0;var fail4 = 0
        for (i in 0 until faceCount) {

            val triangleMask = input2.readUnsignedByte()
            val triangleType = triangleMask and 7

            when (triangleType) {
                1 -> {
                    vertexOffset +=  input1.readShortSmart()
                    vertex1 = vertexOffset
                    vertexOffset +=  input1.readShortSmart()
                    vertex2 = vertexOffset
                    vertexOffset +=  input1.readShortSmart()
                    vertex3 = vertexOffset
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex2
                    faceVertexIndices3[i] = vertex3
                    if(vertex1 >= c || vertex2 >= c || vertex3 >= c)
                        fail1++
                }
                2 -> {
                    vertex2 = vertex3
                    vertex3 = input1.readShortSmart() + vertexOffset
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex2
                    faceVertexIndices3[i] = vertex3
                    if(vertex3 >= c)
                        fail2++
                }
                3 -> {
                    vertex1 = vertex3
                    vertex3 = input1.readShortSmart() + vertexOffset
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex2
                    faceVertexIndices3[i] = vertex3
                    if(vertex3 >= c)
                        fail3++
                }
                4 -> {
                    val vertex1Copy = vertex1
                    vertex1 = vertex2
                    vertex2 = vertex1Copy
                    vertex3 = input1.readShortSmart() + vertexOffset
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex1Copy
                    faceVertexIndices3[i] = vertex3
                    if(vertex3 >= c)
                        fail4++
                }
            }
        }
        println("$fail1 $fail2  $fail3  $fail4")
    }
    private fun readVertexIndices(
        input1: InputStream,
        input2: InputStream,
        faceCount: Int,
        faceVertexIndices1: IntArray,
        faceVertexIndices2: IntArray,
        faceVertexIndices3: IntArray
    ) {

        var vertex1 = 0
        var vertex2 = 0
        var vertex3 = 0
        var vertexOffset = 0

        for (i in 0 until faceCount) {

            val type = input2.readUnsignedByte()

            when (type) {
                1 -> {
                    vertex1 = input1.readShortSmart() + vertexOffset
                    vertex2 = input1.readShortSmart() + vertex1
                    vertex3 = input1.readShortSmart() + vertex2
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex2
                    faceVertexIndices3[i] = vertex3
                }
                2 -> {
                    vertex2 = vertex3
                    vertex3 = input1.readShortSmart() + vertex2
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex2
                    faceVertexIndices3[i] = vertex3
                }
                3 -> {
                    vertex1 = vertex3
                    vertex3 = input1.readShortSmart() + vertexOffset
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex2
                    faceVertexIndices3[i] = vertex3
                }
                4 -> {
                    val vertex1Copy = vertex1
                    vertex1 = vertex2
                    vertex2 = vertex1Copy
                    vertex3 = input1.readShortSmart() + vertexOffset
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex1Copy
                    faceVertexIndices3[i] = vertex3
                }
            }
        }
    }

    private fun loadTextureDefinition(
        triangleCount: Int,
        coordinates: ByteArray?,
        textures: ShortArray?
    ) = ModelTextureDefinition(
        ByteArray(triangleCount),
        coordinates,
        textures,
        ShortArray(triangleCount),
        ShortArray(triangleCount),
        ShortArray(triangleCount)
    )

    private fun loadTextureDefinition(
        triangleCount: Int,
        textureCount2: Int,
        textureCount3: Int,
        textureRenderTypes: ByteArray,
        coordinates: ByteArray?,
        textures: ShortArray?
    ): ModelTextureDefinition {
        val triangleVertexIndices1 = ShortArray(triangleCount)
        val triangleVertexIndices2 = ShortArray(triangleCount)
        val triangleVertexIndices3 = ShortArray(triangleCount)
        val texturedFaces1 = if (textureCount2 > 0) ShortArray(textureCount2) else null
        val texturedFaces2 = if (textureCount2 > 0) ShortArray(textureCount2) else null
        val texturedFaces3 = if (textureCount2 > 0) ShortArray(textureCount2) else null
        val texturedFaces4 = if (textureCount2 > 0) ShortArray(textureCount2) else null
        val texturedFaces5 = if (textureCount2 > 0) ByteArray(textureCount2) else null
        val texturedFaces6 = if (textureCount2 > 0) ShortArray(textureCount2) else null
        val texturePrimaryColors = if (textureCount3 > 0) ShortArray(textureCount3) else null
        return ModelTextureDefinition(
            textureRenderTypes,
            coordinates,
            textures,
            triangleVertexIndices1,
            triangleVertexIndices2,
            triangleVertexIndices3,
            texturedFaces1,
            texturedFaces2,
            texturedFaces3,
            texturedFaces4,
            texturedFaces5,
            texturedFaces6,
            texturePrimaryColors
        )
    }

    companion object {
        const val RS3_HEADER_LENGTH = 26
        const val HIGH_REV_HEADER_LENGTH = 23
        const val LOW_REV_HEADER_LENGTH = 18

        const val ZOOMED_FORMAT = 0
        const val STANDARD_FORMAT = 15

        fun isFlagged(flag: Int, mask: Int) = (flag and mask) != 0

        fun isType3(data: ByteArray) = data[data.size - 1] == (-3).toByte() && data[data.size - 2] == (-1).toByte()
        fun isType2(data: ByteArray) = data[data.size - 1] == (-2).toByte() && data[data.size - 2] == (-1).toByte()
        fun isType1(data: ByteArray) = data[data.size - 1] == (-1).toByte() && data[data.size - 2] == (-1).toByte()

        fun isRS3(data: ByteArray) = false//data[0] == 1.toByte() && data[1] == 1.toByte()
    }

}

private fun InputStream.readDecrSmart(): Int {
    return readUnsignedShortSmart() - 1
}
