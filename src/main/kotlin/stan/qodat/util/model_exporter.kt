package stan.qodat.util

import net.runelite.cache.io.OutputStream
import qodat.cache.definition.ModelDefinition

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   2019-08-14
 * @version 1.0
 */
const val BUFFER_SIZE = 30_000

//fun exportToFile(modelDefinition: ModelDefinition, file: File, exportSkinsAsColor: Boolean){
//    if(file.name.endsWith(".mqo")){
//        MQOExporter().export(file, modelDefinition)
//    } else {
//        var data = export(modelDefinition).flip()
//        if(file.name.endsWith(".gz"))
//           data = GZip.compress(data)
//        Files.write(file.toPath(), data)
//    }
//}

fun export(modelDefinition: ModelDefinition) : OutputStream {

    val out = OutputStream(BUFFER_SIZE)

    val outVertexFlags = OutputStream()
    val outPointX = OutputStream()
    val outPointY = OutputStream()
    val outPointZ = OutputStream()
    val outVertexSkins = OutputStream()

    val vertexCount = modelDefinition.getVertexCount()
    val vertexSkins = modelDefinition.getVertexSkins()
    writeVertexData(
        outVertexFlags,
        outPointX,
        outPointY,
        outPointZ,
        outVertexSkins,
        vertexCount,
        modelDefinition.getVertexPositionsX(),
        modelDefinition.getVertexPositionsY(),
        modelDefinition.getVertexPositionsZ(),
        vertexSkins
    )

    val outFaceColors = OutputStream()
    val outFaceSkins = OutputStream()
    val outFaceVertexIndices = OutputStream()
    val outFaceVertexIndicesMasks = OutputStream()
    val outFaceRenderTypes = OutputStream()
    val outFaceRenderPriorities = OutputStream()
    val outFaceAlphas = OutputStream()

    val faceCount = modelDefinition.getFaceCount()
    val faceColors = modelDefinition.getFaceColors()
    val faceTextures = modelDefinition.getFaceTextures()
    val faceTextureConfigs = modelDefinition.getFaceTextureConfigs()
    val faceRenderTypes = modelDefinition.getFaceTypes()
    val faceRenderPriorities = modelDefinition.getFacePriorities()
    val faceAlphas = modelDefinition.getFaceAlphas()
    val faceSkins = modelDefinition.getFaceSkins()

    writeFaceData(
        outFaceColors,
        outFaceRenderTypes,
        outFaceRenderPriorities,
        outFaceAlphas,
        outFaceSkins,
        faceCount,
        faceColors,
        faceTextures,
        faceTextureConfigs,
        faceRenderTypes,
        faceRenderPriorities,
        faceAlphas,
        faceSkins
    )
    writeFaceVertexIndices(
        outFaceVertexIndices,
        outFaceVertexIndicesMasks,
        faceCount,
        modelDefinition.getFaceVertexIndices1(),
        modelDefinition.getFaceVertexIndices2(),
        modelDefinition.getFaceVertexIndices3()
    )

    val outFaceTextures = OutputStream()
    val textureConfigCount = modelDefinition.getTextureConfigCount()
    if(textureConfigCount > 0) {
        writeFaceTextures(
            outFaceTextures,
            textureConfigCount,
            modelDefinition.getTextureTriangleVertexIndices1()!!,
            modelDefinition.getTextureTriangleVertexIndices2()!!,
            modelDefinition.getTextureTriangleVertexIndices3()!!
        )
    }
//    println("outVertexFlags ->  at ${out.offset}")
    write(out, outVertexFlags)
//    println("outFaceVertexIndicesMasks ->  at ${out.offset}")
    write(out, outFaceVertexIndicesMasks)
//    println("outFaceRenderPriorities ->  at ${out.offset}")
    write(out, outFaceRenderPriorities)
//    println("outFaceSkins ->  at ${out.offset}")
    write(out, outFaceSkins)
//    println("outFaceRenderTypes ->  at ${out.offset}")
    write(out, outFaceRenderTypes)
//    println("outVertexSkins ->  at ${out.offset}")
    write(out, outVertexSkins)
//    println("outFaceAlphas ->  at ${out.offset}")
    write(out, outFaceAlphas)
//    println("outFaceVertexIndices ->  at ${out.offset}")
    write(out, outFaceVertexIndices)
//    println("outFaceColors ->  at ${out.offset}")
    write(out, outFaceColors)
//    println("outFaceTextures ->  at ${out.offset}")
    write(out, outFaceTextures)
//    println("outPointX ->  at ${out.offset}")
    write(out, outPointX)
//    println("outPointY ->  at ${out.offset}")
    write(out, outPointY)
//    println("outPointZ ->  at ${out.offset}")
    write(out, outPointZ)
//    println("header ->  at ${out.offset}")

    val faceRenderFlag = if(faceRenderTypes == null && faceTextures == null && faceTextureConfigs == null)
        0 else 1

    writeHeader(
        out,
        vertexCount,
        faceCount,
        textureConfigCount,
        faceRenderFlag = faceRenderFlag,
        faceSkinsFlag = faceSkins.getFlag(),
        vertexSkinsFlag = vertexSkins.getFlag(),
        priorityFlag = faceRenderPriorities.getFlagOr(modelDefinition.getPriority()),
        transparencyFlag = faceAlphas.getFlag(),
        pointXLength = outPointX.offset,
        pointYLength = outPointY.offset,
        pointZLength = outPointZ.offset,
        faceVertexDataLength = outFaceVertexIndices.offset
    )
//    println("length =   ${out.offset}")
    return out
}

private fun write(out: OutputStream, section: OutputStream) {
    out.writeBytes(section.array, 0, section.offset)
}

private fun writeFaceTextures(
    out1: OutputStream,
    textureConfigCount: Int,
    triangleVertexIndices1: ShortArray,
    triangleVertexIndices2: ShortArray,
    triangleVertexIndices3: ShortArray
){
    for(config in 0 until textureConfigCount){
        out1.writeShort(triangleVertexIndices1[config].toInt())
        out1.writeShort(triangleVertexIndices2[config].toInt())
        out1.writeShort(triangleVertexIndices3[config].toInt())
    }
}

private fun writeFaceVertexIndices(
    out1: OutputStream,
    out2: OutputStream,
    faceCount: Int,
    faceVertexIndices1: IntArray,
    faceVertexIndices2: IntArray,
    faceVertexIndices3: IntArray
) {
    var previousVertex1 = 0
    var previousVertex2 = 0
    var previousVertex3 = 0

    for(face in 0 until faceCount) {
        val vertex1 = faceVertexIndices1[face]
        val vertex2 = faceVertexIndices2[face]
        val vertex3 = faceVertexIndices3[face]
        if (previousVertex1 == vertex1 && previousVertex3 == vertex2) {
            out2.writeByte(2)
            out1.writeByteOrShort(vertex3 - previousVertex3)
        } else if (previousVertex3 == vertex1 && previousVertex2 == vertex2) {
            out2.writeByte(3)
            out1.writeByteOrShort(vertex3 - previousVertex3)
        } else if (previousVertex1 == vertex2 && previousVertex2 == vertex1) {
            out2.writeByte(4)
            out1.writeByteOrShort(vertex3 - previousVertex3)
        } else {
            out2.writeByte(1)
            out1.writeByteOrShort(vertex1 - previousVertex3)
            out1.writeByteOrShort(vertex2 - vertex1)
            out1.writeByteOrShort(vertex3 - vertex2)
        }
        previousVertex1 = vertex1
        previousVertex2 = vertex2
        previousVertex3 = vertex3
    }
}

private fun writeFaceData(
    out1: OutputStream,
    out2: OutputStream,
    out3: OutputStream,
    out4: OutputStream,
    out5: OutputStream,
    faceCount: Int,
    faceColors: ShortArray,
    faceTextures: ShortArray?,
    faceTextureConfigs: ByteArray?,
    faceRenderTypes: ByteArray?,
    faceRenderPriorities: ByteArray?,
    faceAlphas: ByteArray?,
    faceSkins: IntArray?
) {
    for (i in 0 until faceCount) {
        out1.writeShort(faceColors[i].toInt())
        faceRenderTypes?.let {
            var flag = 0

            if(it[i] == 1.toByte())
                flag++


            faceTextureConfigs?.let { configs ->
                val config = configs[i].toInt()
                faceTextures?.let {
                    if(it[i] != (-1).toShort()){
                        flag += 2
                        flag = flag shl (config and 0xF0)
                    }
                }
            }
            out2.writeByte(flag)
        }
        faceRenderPriorities?.let { out3.writeByte(it[i].toInt())}
        faceAlphas?.let { out4.writeByte(it[i].toInt()) }
        faceSkins?.let { out5.writeByte(it[i]) }
    }
}

private fun writeVertexData(
    out1: OutputStream,
    out2: OutputStream,
    out3: OutputStream,
    out4: OutputStream,
    out5: OutputStream,
    vertexCount: Int,
    vertexPositionsX: IntArray,
    vertexPositionsY: IntArray,
    vertexPositionsZ: IntArray,
    vertexSkins: IntArray?
) {
    var previousX = 0
    var previousY = 0
    var previousZ = 0

    for (i in 0 until vertexCount) {

        var flag = 0
        val x = vertexPositionsX[i]
        val y = vertexPositionsY[i]
        val z = vertexPositionsZ[i]

        if (previousX != x) {
            out2.writeByteOrShort(x - previousX)
            flag++
            previousX = x
        }
        if (previousY != y) {
            out3.writeByteOrShort(y - previousY)
            flag += 2
            previousY = y
        }
        if (previousZ != z) {
            out4.writeByteOrShort(z - previousZ)
            flag += 4
            previousZ = z
        }
        vertexSkins?.let { out5.writeByte(it[i]) }
        out1.writeByte(flag)
    }
}

private fun writeHeader(
    stream: OutputStream,
    vertexCount: Int,
    faceCount: Int,
    faceTextureConfigCount: Int,
    faceRenderFlag: Int,
    faceSkinsFlag: Int,
    vertexSkinsFlag: Int,
    priorityFlag: Int,
    transparencyFlag: Int,
    pointXLength: Int,
    pointYLength: Int,
    pointZLength: Int,
    faceVertexDataLength: Int
) {
    stream.writeShort(vertexCount)
    stream.writeShort(faceCount)
    stream.writeByte(faceTextureConfigCount)
    stream.writeByte(faceRenderFlag) // texture flag 2
    stream.writeByte(priorityFlag)
    stream.writeByte(transparencyFlag)
    stream.writeByte(faceSkinsFlag)
    stream.writeByte(vertexSkinsFlag)
    stream.writeShort(pointXLength)
    stream.writeShort(pointYLength)
    stream.writeShort(pointZLength)
    stream.writeShort(faceVertexDataLength)
}

private fun Any?.getFlag() = if(this == null) 0 else 1
private fun Any?.getFlagOr(other: Byte) = if(this == null) other.toInt() else 255
