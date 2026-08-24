package qodat.cache.models

import qodat.cache.definition.ModelDefinition
import qodat.cache.definition.ModelTextureDefinition
import com.displee.io.impl.InputBuffer
import java.util.logging.Logger

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
                    fromOsrsModel(ModelLoader().load(modelId.toIntOrNull() ?: hashCode(), data))
                } catch (e: Exception) {
                    tryOtherLoadMethods(modelId, data, "OSRS")
                }
            }
            else -> loadLowRev(modelId, data)
        }

    private fun fromOsrsModel(src: net.runelite.cache.definitions.ModelDefinition) = RS2Model().apply {
        setId(src.id.toString())
        setPriority(src.priority)
        setFaceCount(src.faceCount)
        setFaceColors(src.faceColors)
        setFaceAlphas(src.faceTransparencies)
        setVertexSkins(src.packedVertexGroups)
        setVertexCount(src.vertexCount)
        setVertexPositionsX(src.vertexX)
        setVertexPositionsY(src.vertexY)
        setVertexPositionsZ(src.vertexZ)
        setFaceVertexIndices1(src.faceIndices1)
        setFaceVertexIndices2(src.faceIndices2)
        setFaceVertexIndices3(src.faceIndices3)
        setFaceTextures(src.faceTextures)
        setTextureRenderTypes(src.textureRenderTypes)
        texturePrimaryColors = src.texturePrimaryColors
        setTextureTriangleVertexIndices1(src.texIndices1)
        setTextureTriangleVertexIndices2(src.texIndices2)
        setTextureTriangleVertexIndices3(src.texIndices3)
        setFaceTextureConfigs(src.textureCoords)
        faceRenderPriorities = src.faceRenderPriorities
        faceRenderTypes = src.faceRenderTypes
        setMayaGroups(src.animayaGroups)
        setMayaScales(src.animayaScales)
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

    // TODO(perf): seven InputBuffer copies per model. Reuse a small pool or one buffer + offsets during cache load.
    private fun decodeHighRev(modelId: String, data: ByteArray): ModelDefinition {
        val streams = modelStreams(data, 7)
        val input1 = streams[0]
        input1.offset = data.size - HIGH_REV_HEADER_LENGTH

        val vertexCount = input1.readUnsignedShort()
        val faceCount = input1.readUnsignedShort()
        val textureConfigCount = input1.readUnsignedByte()

        val l1: Int = input1.readUnsignedByte()
        val renderFlag = 0x1 and l1 xor -0x1 == -2
        val highRevExtended = 0x8 and l1 == 8
        if (!highRevExtended) {
            return loadMidRev(
                modelId,
                vertexCount,
                faceCount,
                textureConfigCount,
                l1,
                streams[0],
                streams[1],
                streams[2],
                streams[3],
                streams[4],
                streams[5],
                streams[6]
            )
        }
        input1.offset -= 7
        val newformat = input1.readUnsignedByte()
        input1.offset += 6

        val header = readTexturedHeader(input1)
        val textureHeader = readTextureTypeHeader(input1, textureConfigCount)
        val texture2Stride = when {
            newformat == 14 -> 7
            newformat >= 15 -> 9
            else -> 6
        }
        val offsets = computeTexturedOffsets(
            textureConfigCount = textureConfigCount,
            vertexCount = vertexCount,
            faceCount = faceCount,
            includeRenderFlagBlock = renderFlag,
            includeL1RenderTypes = l1 == 1,
            header = header,
            textureHeader = textureHeader,
            texture2Stride = texture2Stride
        )
        return readTexturedRevision(
            modelId = modelId,
            format = newformat,
            midRevTextures = false,
            vertexCount = vertexCount,
            faceCount = faceCount,
            textureConfigCount = textureConfigCount,
            renderFlag = renderFlag,
            header = header,
            textureHeader = textureHeader,
            offsets = offsets,
            streams = streams,
            skipFooter = true
        )
    }

    fun loadMidRev(
        modelId: String,
        vertexCount: Int,
        faceCount: Int,
        textureConfigCount: Int,
        l1: Int,
        input1: InputBuffer,
        input2: InputBuffer,
        input3: InputBuffer,
        input4: InputBuffer,
        input5: InputBuffer,
        input6: InputBuffer,
        input7: InputBuffer
    ): RS2Model {
        val renderFlag = (0x1 and l1).inv() == -2
        val header = readTexturedHeader(input1)
        val textureHeader = readTextureTypeHeader(input1, textureConfigCount)
        val offsets = computeTexturedOffsets(
            textureConfigCount = textureConfigCount,
            vertexCount = vertexCount,
            faceCount = faceCount,
            includeRenderFlagBlock = false,
            includeL1RenderTypes = l1 == 1,
            header = header,
            textureHeader = textureHeader,
            texture2Stride = 6
        )
        return readTexturedRevision(
            modelId = modelId,
            format = 0,
            midRevTextures = true,
            vertexCount = vertexCount,
            faceCount = faceCount,
            textureConfigCount = textureConfigCount,
            renderFlag = renderFlag,
            header = header,
            textureHeader = textureHeader,
            offsets = offsets,
            streams = arrayOf(input1, input2, input3, input4, input5, input6, input7),
            skipFooter = false
        )
    }

    private fun loadLowRev(modelId: String, data: ByteArray): ModelDefinition {
        val streams = modelStreams(data, 5)
        val input1 = streams[0]
        val input2 = streams[1]
        val input3 = streams[2]
        val input4 = streams[3]
        val input5 = streams[4]

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
                        if (textureDefinition.triangleVertexIndices1[coord].toInt() == faceVertexIndices1[vertex]
                            && textureDefinition.triangleVertexIndices2[coord].toInt() == faceVertexIndices2[vertex]
                            && textureDefinition.triangleVertexIndices3[coord].toInt() == faceVertexIndices3[vertex]
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

        return assembleModel(
            modelId = modelId,
            priority = priority,
            vertexCount = vertexCount,
            vertexPositionsX = vertexPositionsX,
            vertexPositionsY = vertexPositionsY,
            vertexPositionsZ = vertexPositionsZ,
            vertexSkins = vertexSkins,
            faceCount = faceCount,
            faceVertexIndices1 = faceVertexIndices1,
            faceVertexIndices2 = faceVertexIndices2,
            faceVertexIndices3 = faceVertexIndices3,
            faceSkins = faceSkins,
            faceColors = faceColors,
            faceAlphas = faceAlphas,
            faceRenderPriorities = faceRenderPriorities,
            faceRenderTypes = faceRenderTypes,
            textureDefinition = textureDefinition
        )
    }

    private fun readTexturedRevision(
        modelId: String,
        format: Int,
        midRevTextures: Boolean,
        vertexCount: Int,
        faceCount: Int,
        textureConfigCount: Int,
        renderFlag: Boolean,
        header: TexturedHeader,
        textureHeader: TextureTypeHeader?,
        offsets: TexturedOffsets,
        streams: Array<InputBuffer>,
        skipFooter: Boolean
    ): RS2Model {
        val input1 = streams[0]
        val input2 = streams[1]
        val input3 = streams[2]
        val input4 = streams[3]
        val input5 = streams[4]
        val input6 = streams[5]
        val input7 = streams[6]

        val vertexPositionsX = IntArray(vertexCount)
        val vertexPositionsY = IntArray(vertexCount)
        val vertexPositionsZ = IntArray(vertexCount)

        val faceVertexIndices1 = IntArray(faceCount)
        val faceVertexIndices2 = IntArray(faceCount)
        val faceVertexIndices3 = IntArray(faceCount)

        val vertexSkins = if (header.animationVertexFlag) IntArray(vertexCount) else null
        val faceRenderTypes = if (renderFlag) ByteArray(faceCount) else null
        val faceRenderPriorities = if (header.renderPriority == 255) ByteArray(faceCount) else null
        val priority = if (header.renderPriority == 255) 0.toByte() else header.renderPriority.toByte()
        val faceAlphas = if (header.transparencyFlag) ByteArray(faceCount) else null
        val faceSkins = if (header.animationFaceFlag) IntArray(faceCount) else null
        val faceTextures = if (header.textureFlag) ShortArray(faceCount) else null
        val textureCoordinates = if (header.textureFlag && textureConfigCount > 0) ByteArray(faceCount) else null
        val faceColors = ShortArray(faceCount)

        val textureDefinition = if (textureHeader != null) loadTextureDefinition(
            textureConfigCount,
            textureHeader.count2,
            textureHeader.count3,
            textureHeader.types,
            textureCoordinates,
            faceTextures
        ) else null

        input1.offset = textureConfigCount
        input2.offset = offsets.pointsXStart
        input3.offset = offsets.pointsYStart
        input4.offset = offsets.pointsZStart
        input5.offset = offsets.vertexSkinStart

        readVertices(
            input1,
            input2,
            input3,
            input4,
            input5,
            header.animationVertexFlag,
            vertexCount,
            vertexSkins,
            vertexPositionsX,
            vertexPositionsZ,
            vertexPositionsY
        )

        input1.offset = offsets.coloredTriangleCoordStart
        input2.offset = offsets.renderTypeStart
        input3.offset = offsets.vertexPriorityStart
        input4.offset = offsets.alphaStart
        input5.offset = offsets.triangleSkinStart
        input6.offset = offsets.texturedTriangleStart
        input7.offset = offsets.texturedTriangleCoordStart

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

        input1.offset = offsets.triangleCoordStart
        input2.offset = offsets.vertexOffsetStart

        readVertexIndices(
            input1, input2,
            faceCount,
            faceVertexIndices1,
            faceVertexIndices2,
            faceVertexIndices3
        )

        input1.offset = offsets.texture1Start
        input2.offset = offsets.texture2Start1
        input3.offset = offsets.texture2Start2
        input4.offset = offsets.texture2Start3
        input5.offset = offsets.texture2Start
        input6.offset = offsets.texture3Start

        if (textureDefinition != null)
            readTextureData(midRevTextures, textureDefinition, textureConfigCount, input1, input2, input3, input4, input5, input6)

        if (skipFooter) {
            input1.offset = offsets.end
            val unknown = input1.readUnsignedByte()
            if (unknown != 0) {
                input1.readUnsignedShort()
                input1.readUnsignedShort()
                input1.readUnsignedShort()
                input1.readInt()
            }
        }

        return assembleModel(
            modelId = modelId,
            priority = priority,
            vertexCount = vertexCount,
            vertexPositionsX = vertexPositionsX,
            vertexPositionsY = vertexPositionsY,
            vertexPositionsZ = vertexPositionsZ,
            vertexSkins = vertexSkins,
            faceCount = faceCount,
            faceVertexIndices1 = faceVertexIndices1,
            faceVertexIndices2 = faceVertexIndices2,
            faceVertexIndices3 = faceVertexIndices3,
            faceSkins = faceSkins,
            faceColors = faceColors,
            faceAlphas = faceAlphas,
            faceRenderPriorities = faceRenderPriorities,
            faceRenderTypes = faceRenderTypes,
            textureDefinition = textureDefinition,
            format = format
        )
    }

    private fun readTexturedHeader(input1: InputBuffer) = TexturedHeader(
        renderPriority = input1.readUnsignedByte(),
        transparencyFlag = input1.readUnsignedByte() == 1,
        animationFaceFlag = input1.readUnsignedByte() == 1,
        textureFlag = input1.readUnsignedByte() == 1,
        animationVertexFlag = input1.readUnsignedByte() == 1,
        pointsXLength = input1.readUnsignedShort(),
        pointsYLength = input1.readUnsignedShort(),
        pointsZLength = input1.readUnsignedShort(),
        triangleLength = input1.readUnsignedShort(),
        texturedCoordLength = input1.readUnsignedShort()
    )

    private fun readTextureTypeHeader(input1: InputBuffer, textureConfigCount: Int): TextureTypeHeader? {
        if (textureConfigCount <= 0) return null
        var count1 = 0
        var count2 = 0
        var count3 = 0
        input1.offset = 0
        val types = ByteArray(textureConfigCount) {
            val type = input1.readByte()
            when (type.toInt()) {
                0 -> ++count1
                in 1..3 -> ++count2
                2 -> ++count3
            }
            type
        }
        return TextureTypeHeader(types, count1, count2, count3)
    }

    private fun computeTexturedOffsets(
        textureConfigCount: Int,
        vertexCount: Int,
        faceCount: Int,
        includeRenderFlagBlock: Boolean,
        includeL1RenderTypes: Boolean,
        header: TexturedHeader,
        textureHeader: TextureTypeHeader?,
        texture2Stride: Int
    ): TexturedOffsets {
        val textureCount1 = textureHeader?.count1 ?: 0
        val textureCount2 = textureHeader?.count2 ?: 0
        val textureCount3 = textureHeader?.count3 ?: 0

        var position = textureConfigCount + vertexCount
        val renderTypeStart = position
        if (includeRenderFlagBlock) position += faceCount
        if (includeL1RenderTypes) position += faceCount

        val vertexOffsetStart = position
        position += faceCount
        val vertexPriorityStart = position
        if (header.renderPriority == 255) position += faceCount
        val triangleSkinStart = position
        if (header.animationFaceFlag) position += faceCount
        val vertexSkinStart = position
        if (header.animationVertexFlag) position += vertexCount
        val alphaStart = position
        if (header.transparencyFlag) position += faceCount
        val triangleCoordStart = position
        position += header.triangleLength
        val texturedTriangleStart = position
        if (header.textureFlag) position += faceCount * 2
        val texturedTriangleCoordStart = position
        position += header.texturedCoordLength
        val coloredTriangleCoordStart = position
        position += faceCount * 2
        val pointsXStart = position
        position += header.pointsXLength
        val pointsYStart = position
        position += header.pointsYLength
        val pointsZStart = position
        position += header.pointsZLength
        val texture1Start = position
        position += textureCount1 * 6
        val texture2Start1 = position
        position += textureCount2 * 6
        val texture2Start2 = position
        position += texture2Stride * textureCount2
        val texture2Start3 = position
        position += textureCount2
        val texture2Start = position
        position += textureCount2
        val texture3Start = position
        position += textureCount2 + textureCount3 * 2

        return TexturedOffsets(
            renderTypeStart = renderTypeStart,
            vertexOffsetStart = vertexOffsetStart,
            vertexPriorityStart = vertexPriorityStart,
            triangleSkinStart = triangleSkinStart,
            vertexSkinStart = vertexSkinStart,
            alphaStart = alphaStart,
            triangleCoordStart = triangleCoordStart,
            texturedTriangleStart = texturedTriangleStart,
            texturedTriangleCoordStart = texturedTriangleCoordStart,
            coloredTriangleCoordStart = coloredTriangleCoordStart,
            pointsXStart = pointsXStart,
            pointsYStart = pointsYStart,
            pointsZStart = pointsZStart,
            texture1Start = texture1Start,
            texture2Start1 = texture2Start1,
            texture2Start2 = texture2Start2,
            texture2Start3 = texture2Start3,
            texture2Start = texture2Start,
            texture3Start = texture3Start,
            end = position
        )
    }

    private fun assembleModel(
        modelId: String,
        priority: Byte,
        vertexCount: Int,
        vertexPositionsX: IntArray,
        vertexPositionsY: IntArray,
        vertexPositionsZ: IntArray,
        vertexSkins: IntArray?,
        faceCount: Int,
        faceVertexIndices1: IntArray,
        faceVertexIndices2: IntArray,
        faceVertexIndices3: IntArray,
        faceSkins: IntArray?,
        faceColors: ShortArray,
        faceAlphas: ByteArray?,
        faceRenderPriorities: ByteArray?,
        faceRenderTypes: ByteArray?,
        textureDefinition: ModelTextureDefinition?,
        format: Int = 0
    ): RS2Model {
        val definition = RS2Model()
        definition.setId(modelId)
        definition.setFormat(format)
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
        input1: InputBuffer,
        input2: InputBuffer,
        input3: InputBuffer,
        input4: InputBuffer,
        input5: InputBuffer,
        input6: InputBuffer
    ) {
        for (config in 0 until textureConfigCount) {
            val type = textureDefinition.renderTypes[config].toInt() and 255
            when (type) {
                0 -> readTexturedTrianglePositions(textureDefinition, config, input1)
                1, 2, 3 -> {
                    readTexturedTrianglePositions(textureDefinition, config, input2)
                    readTextureAnimation(midRev, textureDefinition, config, input3, input4, input5, input6)
                    if (type == 2) {
                        textureDefinition.primaryColors?.set(config, input6.readUnsignedShort().toShort())
                    }
                }
            }
        }
    }

    private fun readTextureData(
        textureDefinition: ModelTextureDefinition,
        texturedTriangleCount: Int,
        input1: InputBuffer
    ) {
        for (triangle in 0 until texturedTriangleCount) {
            textureDefinition.renderTypes[triangle] = 0
            readTexturedTrianglePositions(textureDefinition, triangle, input1)
        }
    }

    private fun readTexturedTrianglePositions(
        textureDefinition: ModelTextureDefinition,
        triangle: Int,
        input1: InputBuffer
    ) {
        textureDefinition.triangleVertexIndices1[triangle] = input1.readUnsignedShort().toShort()
        textureDefinition.triangleVertexIndices2[triangle] = input1.readUnsignedShort().toShort()
        textureDefinition.triangleVertexIndices3[triangle] = input1.readUnsignedShort().toShort()
    }

    private fun readTextureAnimation(
        midRev: Boolean,
        textureDefinition: ModelTextureDefinition,
        triangle: Int,
        input3: InputBuffer,
        input4: InputBuffer,
        input5: InputBuffer,
        input6: InputBuffer
    ) {
        textureDefinition.texturedFaces1!![triangle] = input3.readUnsignedShort().toShort()
        textureDefinition.texturedFaces2!![triangle] = input3.readUnsignedShort().toShort()
        textureDefinition.texturedFaces3!![triangle] = input3.readUnsignedShort().toShort()
        textureDefinition.texturedFaces4!![triangle] = if(midRev) input4.readByte().toShort() else input4.readUnsignedShort().toShort()
        textureDefinition.texturedFaces5!![triangle] = input5.readByte()
        textureDefinition.texturedFaces6!![triangle] = if(midRev) input6.readByte().toShort() else input6.readUnsignedShort().toShort()
    }

    private fun readVertices(
        input1: InputBuffer,
        input2: InputBuffer,
        input3: InputBuffer,
        input4: InputBuffer,
        input5: InputBuffer,
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
            val vertexXOffset = if (isFlagged(flag, 1)) input2.readSmart() else 0
            val vertexYOffset = if (isFlagged(flag, 2)) input3.readSmart() else 0
            val vertexZOffset = if (isFlagged(flag, 4)) input4.readSmart() else 0

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
        input1: InputBuffer,
        input2: InputBuffer,
        input3: InputBuffer,
        input4: InputBuffer,
        input5: InputBuffer,
        input6: InputBuffer,
        input7: InputBuffer,
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
        input1: InputBuffer,
        input2: InputBuffer,
        input3: InputBuffer,
        input4: InputBuffer,
        input5: InputBuffer,
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

    @Suppress("unused")
    private fun readVertexIndicesRS3(
        input1: InputBuffer,
        input2: InputBuffer,
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

            val triangleMask = input2.readUnsignedByte()
            val triangleType = triangleMask and 7

            when (triangleType) {
                1 -> {
                    vertexOffset +=  input1.readSmart()
                    vertex1 = vertexOffset
                    vertexOffset +=  input1.readSmart()
                    vertex2 = vertexOffset
                    vertexOffset +=  input1.readSmart()
                    vertex3 = vertexOffset
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex2
                    faceVertexIndices3[i] = vertex3
                }
                2 -> {
                    vertex2 = vertex3
                    vertex3 = input1.readSmart() + vertexOffset
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex2
                    faceVertexIndices3[i] = vertex3
                }
                3 -> {
                    vertex1 = vertex3
                    vertex3 = input1.readSmart() + vertexOffset
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex2
                    faceVertexIndices3[i] = vertex3
                }
                4 -> {
                    val vertex1Copy = vertex1
                    vertex1 = vertex2
                    vertex2 = vertex1Copy
                    vertex3 = input1.readSmart() + vertexOffset
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex1Copy
                    faceVertexIndices3[i] = vertex3
                }
            }
        }
    }

    private fun readVertexIndices(
        input1: InputBuffer,
        input2: InputBuffer,
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
                    vertex1 = input1.readSmart() + vertexOffset
                    vertex2 = input1.readSmart() + vertex1
                    vertex3 = input1.readSmart() + vertex2
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex2
                    faceVertexIndices3[i] = vertex3
                }
                2 -> {
                    vertex2 = vertex3
                    vertex3 = input1.readSmart() + vertex2
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex2
                    faceVertexIndices3[i] = vertex3
                }
                3 -> {
                    vertex1 = vertex3
                    vertex3 = input1.readSmart() + vertexOffset
                    vertexOffset = vertex3
                    faceVertexIndices1[i] = vertex1
                    faceVertexIndices2[i] = vertex2
                    faceVertexIndices3[i] = vertex3
                }
                4 -> {
                    val vertex1Copy = vertex1
                    vertex1 = vertex2
                    vertex2 = vertex1Copy
                    vertex3 = input1.readSmart() + vertexOffset
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

    private class TexturedHeader(
        val renderPriority: Int,
        val transparencyFlag: Boolean,
        val animationFaceFlag: Boolean,
        val textureFlag: Boolean,
        val animationVertexFlag: Boolean,
        val pointsXLength: Int,
        val pointsYLength: Int,
        val pointsZLength: Int,
        val triangleLength: Int,
        val texturedCoordLength: Int
    )

    private class TextureTypeHeader(
        val types: ByteArray,
        val count1: Int,
        val count2: Int,
        val count3: Int
    )

    private class TexturedOffsets(
        val renderTypeStart: Int,
        val vertexOffsetStart: Int,
        val vertexPriorityStart: Int,
        val triangleSkinStart: Int,
        val vertexSkinStart: Int,
        val alphaStart: Int,
        val triangleCoordStart: Int,
        val texturedTriangleStart: Int,
        val texturedTriangleCoordStart: Int,
        val coloredTriangleCoordStart: Int,
        val pointsXStart: Int,
        val pointsYStart: Int,
        val pointsZStart: Int,
        val texture1Start: Int,
        val texture2Start1: Int,
        val texture2Start2: Int,
        val texture2Start3: Int,
        val texture2Start: Int,
        val texture3Start: Int,
        val end: Int
    )

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

private fun modelStreams(data: ByteArray, count: Int) = Array(count) { InputBuffer(data) }
