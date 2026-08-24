package qodat.cache.models

import qodat.cache.io.OutputStream
import qodat.cache.definition.ModelDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RSModelLoaderDecodeTest {

    @Test
    fun decodesLowRevTriangleCountsCoordsAndColor() {
        val bytes = lowRevTriangle()
        assertFalse(RSModelLoader.isType1(bytes))
        assertFalse(RSModelLoader.isType2(bytes))
        assertFalse(RSModelLoader.isType3(bytes))

        val model = RSModelLoader().load("42", bytes)
        assertTriangle(model, id = "42", priority = 5)
    }

    @Test
    fun decodesLowRevOptionalFaceAndVertexFlags() {
        val bytes = OutputStream().apply {
            writeByte(7)
            writeByte(4)
            writeByte(3)
            writeByte(1)
            writeByte(7)
            writeByte(4)
            writeByte(1)
            writeByte(1)
            writeByte(2)
            writeByte(3)
            writeByte(32)
            writeSmart(0)
            writeSmart(1)
            writeSmart(1)
            writeShort(0x1234)
            writeSmart(10)
            writeSmart(5)
            writeSmart(20)
            writeSmart(5)
            writeSmart(30)
            writeSmart(10)
            writeShort(3)
            writeShort(1)
            writeByte(0)
            writeByte(1)
            writeByte(255)
            writeByte(1)
            writeByte(1)
            writeByte(1)
            writeShort(2)
            writeShort(2)
            writeShort(2)
            writeShort(3)
        }.flip()

        val model = RSModelLoader().load("8", bytes)
        assertEquals(3, model.vertexCount)
        assertEquals(1, model.faceCount)
        assertEquals(0, model.priority)
        assertEquals(10, model.vertexPositionsX[0])
        assertEquals(40, model.vertexPositionsZ[1])
        assertEquals(0x1234.toShort(), model.faceColors[0])
        assertTrue(model.faceTypes!!.contentEquals(byteArrayOf(1)))
        assertTrue(model.facePriorities!!.contentEquals(byteArrayOf(7)))
        assertTrue(model.faceAlphas!!.contentEquals(byteArrayOf(32)))
        assertTrue(model.faceSkins!!.contentEquals(intArrayOf(4)))
        assertTrue(model.vertexSkins!!.contentEquals(intArrayOf(1, 2, 3)))
    }

    @Test
    fun decodesLowRevFaceIndexOpcodes() {
        val bytes = OutputStream().apply {
            writeByte(0)
            writeByte(1)
            writeByte(1)
            writeByte(1)
            writeByte(1)
            writeByte(1)
            writeByte(2)
            writeByte(3)
            writeSmart(0)
            writeSmart(1)
            writeSmart(1)
            writeSmart(1)
            writeSmart(1)
            writeShort(0x1111)
            writeShort(0x2222)
            writeShort(0x3333)
            writeSmart(1)
            writeSmart(1)
            writeSmart(1)
            writeSmart(1)
            writeShort(5)
            writeShort(3)
            writeByte(0)
            writeByte(0)
            writeByte(0)
            writeByte(0)
            writeByte(0)
            writeByte(0)
            writeShort(4)
            writeShort(0)
            writeShort(0)
            writeShort(5)
        }.flip()

        val model = RSModelLoader().load("9", bytes)
        assertEquals(5, model.vertexCount)
        assertEquals(3, model.faceCount)
        assertTrue(model.vertexPositionsX.contentEquals(intArrayOf(0, 1, 2, 3, 4)))
        assertTrue(model.faceVertexIndices1.contentEquals(intArrayOf(0, 0, 3)))
        assertTrue(model.faceVertexIndices2.contentEquals(intArrayOf(1, 2, 2)))
        assertTrue(model.faceVertexIndices3.contentEquals(intArrayOf(2, 3, 4)))
        assertEquals(0x1111.toShort(), model.faceColors[0])
        assertEquals(0x3333.toShort(), model.faceColors[2])
    }

    @Test
    fun decodesEmptyLowRevModel() {
        val bytes = ByteArray(RSModelLoader.LOW_REV_HEADER_LENGTH)
        val model = RSModelLoader().load("0", bytes)
        assertEquals(0, model.vertexCount)
        assertEquals(0, model.faceCount)
        assertTrue(model.vertexPositionsX.isEmpty())
        assertTrue(model.faceColors.isEmpty())
        assertNull(model.faceAlphas)
        assertNull(model.vertexSkins)
    }

    @Test
    fun decodesSingleVertexWithoutFaces() {
        val atOrigin = OutputStream().apply {
            writeByte(0)
            writeLowRevHeader(
                vertexCount = 1,
                faceCount = 0,
                pointXLength = 0,
                pointYLength = 0,
                pointZLength = 0,
                triangleLength = 0,
            )
        }.flip()
        val origin = RSModelLoader().load("10", atOrigin)
        assertEquals(1, origin.vertexCount)
        assertEquals(0, origin.faceCount)
        assertEquals(0, origin.vertexPositionsX[0])
        assertEquals(0, origin.vertexPositionsY[0])
        assertEquals(0, origin.vertexPositionsZ[0])

        val displaced = OutputStream().apply {
            writeByte(7)
            writeSmart(7)
            writeSmart(8)
            writeSmart(9)
            writeLowRevHeader(
                vertexCount = 1,
                faceCount = 0,
                pointXLength = 1,
                pointYLength = 1,
                pointZLength = 1,
                triangleLength = 0,
            )
        }.flip()
        val vertex = RSModelLoader().load("11", displaced)
        assertEquals(1, vertex.vertexCount)
        assertEquals(0, vertex.faceCount)
        assertEquals(7, vertex.vertexPositionsX[0])
        assertEquals(8, vertex.vertexPositionsY[0])
        assertEquals(9, vertex.vertexPositionsZ[0])
        assertTrue(vertex.faceColors.isEmpty())
    }

    @Test
    fun decodesType1Triangle() {
        val bytes = OutputStream().apply {
            writeTriangleBody()
            writeType1Header(priority = 5)
        }.flip()
        assertTrue(RSModelLoader.isType1(bytes))
        assertFalse(RSModelLoader.isType2(bytes))
        assertFalse(RSModelLoader.isType3(bytes))
        assertTriangle(RSModelLoader().load("1", bytes), id = "1", priority = 5)
    }

    @Test
    fun decodesType2Triangle() {
        val bytes = OutputStream().apply {
            writeTriangleBody()
            writeType2Header(priority = 5)
        }.flip()
        assertTrue(RSModelLoader.isType2(bytes))
        assertFalse(RSModelLoader.isType1(bytes))
        assertTriangle(RSModelLoader().load("2", bytes), id = "2", priority = 5)
    }

    @Test
    fun decodesType3Triangle() {
        val bytes = OutputStream().apply {
            writeTriangleBody()
            writeType3Header(priority = 5)
        }.flip()
        assertTrue(RSModelLoader.isType3(bytes))
        assertFalse(RSModelLoader.isType1(bytes))
        assertTriangle(RSModelLoader().load("3", bytes), id = "3", priority = 5)
    }

    @Test
    fun decodesEmptyTypeTrailerModels() {
        val type1 = OutputStream().apply { writeType1Header(vertexCount = 0, faceCount = 0) }.flip()
        val type2 = OutputStream().apply { writeType2Header(vertexCount = 0, faceCount = 0) }.flip()
        val type3 = OutputStream().apply { writeType3Header(vertexCount = 0, faceCount = 0) }.flip()

        assertTrue(RSModelLoader.isType1(type1))
        assertTrue(RSModelLoader.isType2(type2))
        assertTrue(RSModelLoader.isType3(type3))

        for ((id, bytes) in listOf("1" to type1, "2" to type2, "3" to type3)) {
            val model = RSModelLoader().load(id, bytes)
            assertEquals(0, model.vertexCount, "id=$id")
            assertEquals(0, model.faceCount, "id=$id")
            assertTrue(model.vertexPositionsX.isEmpty(), "id=$id")
            assertTrue(model.faceColors.isEmpty(), "id=$id")
        }
    }

    private fun assertTriangle(model: ModelDefinition, id: String, priority: Int) {
        assertEquals(id, model.name)
        assertEquals(3, model.vertexCount)
        assertEquals(1, model.faceCount)
        assertEquals(priority.toByte(), model.priority)
        assertTrue(model.vertexPositionsX.contentEquals(intArrayOf(10, 10, 15)))
        assertTrue(model.vertexPositionsY.contentEquals(intArrayOf(20, 20, 25)))
        assertTrue(model.vertexPositionsZ.contentEquals(intArrayOf(30, 40, 40)))
        assertTrue(model.faceVertexIndices1.contentEquals(intArrayOf(0)))
        assertTrue(model.faceVertexIndices2.contentEquals(intArrayOf(1)))
        assertTrue(model.faceVertexIndices3.contentEquals(intArrayOf(2)))
        assertEquals(0x1234.toShort(), model.faceColors[0])
        assertNull(model.faceAlphas)
        assertNull(model.vertexSkins)
        assertNull(model.faceTypes)
    }

    private fun lowRevTriangle(): ByteArray = OutputStream().apply {
        writeTriangleBody()
        writeLowRevHeader(priority = 5)
    }.flip()

    private fun OutputStream.writeTriangleBody() {
        writeByte(7)
        writeByte(4)
        writeByte(3)
        writeByte(1)
        writeSmart(0)
        writeSmart(1)
        writeSmart(1)
        writeShort(0x1234)
        writeSmart(10)
        writeSmart(5)
        writeSmart(20)
        writeSmart(5)
        writeSmart(30)
        writeSmart(10)
    }

    private fun OutputStream.writeLowRevHeader(
        vertexCount: Int = 3,
        faceCount: Int = 1,
        textureConfigCount: Int = 0,
        renderFlag: Int = 0,
        priority: Int = 0,
        transparencyFlag: Int = 0,
        animationFaceFlag: Int = 0,
        animationVertexFlag: Int = 0,
        pointXLength: Int = 2,
        pointYLength: Int = 2,
        pointZLength: Int = 2,
        triangleLength: Int = 3,
    ) {
        writeShort(vertexCount)
        writeShort(faceCount)
        writeByte(textureConfigCount)
        writeByte(renderFlag)
        writeByte(priority)
        writeByte(transparencyFlag)
        writeByte(animationFaceFlag)
        writeByte(animationVertexFlag)
        writeShort(pointXLength)
        writeShort(pointYLength)
        writeShort(pointZLength)
        writeShort(triangleLength)
    }

    private fun OutputStream.writeType1Header(
        vertexCount: Int = 3,
        faceCount: Int = 1,
        priority: Int = 0,
        pointXLength: Int = if (vertexCount == 0) 0 else 2,
        pointYLength: Int = if (vertexCount == 0) 0 else 2,
        pointZLength: Int = if (vertexCount == 0) 0 else 2,
        triangleLength: Int = if (faceCount == 0) 0 else 3,
    ) {
        writeShort(vertexCount)
        writeShort(faceCount)
        writeByte(0)
        writeByte(0)
        writeByte(priority)
        writeByte(0)
        writeByte(0)
        writeByte(0)
        writeByte(0)
        writeShort(pointXLength)
        writeShort(pointYLength)
        writeShort(pointZLength)
        writeShort(triangleLength)
        writeShort(0)
        writeByte(255)
        writeByte(255)
    }

    private fun OutputStream.writeType2Header(
        vertexCount: Int = 3,
        faceCount: Int = 1,
        priority: Int = 0,
        pointXLength: Int = if (vertexCount == 0) 0 else 2,
        pointYLength: Int = if (vertexCount == 0) 0 else 2,
        pointZLength: Int = if (vertexCount == 0) 0 else 2,
        triangleLength: Int = if (faceCount == 0) 0 else 3,
    ) {
        writeShort(vertexCount)
        writeShort(faceCount)
        writeByte(0)
        writeByte(0)
        writeByte(priority)
        writeByte(0)
        writeByte(0)
        writeByte(0)
        writeByte(0)
        writeShort(pointXLength)
        writeShort(pointYLength)
        writeShort(pointZLength)
        writeShort(triangleLength)
        writeShort(0)
        writeByte(255)
        writeByte(254)
    }

    private fun OutputStream.writeType3Header(
        vertexCount: Int = 3,
        faceCount: Int = 1,
        priority: Int = 0,
        pointXLength: Int = if (vertexCount == 0) 0 else 2,
        pointYLength: Int = if (vertexCount == 0) 0 else 2,
        pointZLength: Int = if (vertexCount == 0) 0 else 2,
        triangleLength: Int = if (faceCount == 0) 0 else 3,
    ) {
        writeShort(vertexCount)
        writeShort(faceCount)
        writeByte(0)
        writeByte(0)
        writeByte(priority)
        writeByte(0)
        writeByte(0)
        writeByte(0)
        writeByte(0)
        writeByte(0)
        writeShort(pointXLength)
        writeShort(pointYLength)
        writeShort(pointZLength)
        writeShort(triangleLength)
        writeShort(0)
        writeShort(0)
        writeByte(255)
        writeByte(253)
    }

    private fun OutputStream.writeSmart(value: Int) {
        require(value in -64..63) { "test smarts stay in one-byte range: $value" }
        writeByte(value + 64)
    }
}

private val ModelDefinition.name get() = getName()
private val ModelDefinition.vertexCount get() = getVertexCount()
private val ModelDefinition.faceCount get() = getFaceCount()
private val ModelDefinition.priority get() = getPriority()
private val ModelDefinition.vertexPositionsX get() = getVertexPositionsX()
private val ModelDefinition.vertexPositionsY get() = getVertexPositionsY()
private val ModelDefinition.vertexPositionsZ get() = getVertexPositionsZ()
private val ModelDefinition.faceVertexIndices1 get() = getFaceVertexIndices1()
private val ModelDefinition.faceVertexIndices2 get() = getFaceVertexIndices2()
private val ModelDefinition.faceVertexIndices3 get() = getFaceVertexIndices3()
private val ModelDefinition.faceColors get() = getFaceColors()
private val ModelDefinition.faceTypes get() = getFaceTypes()
private val ModelDefinition.facePriorities get() = getFacePriorities()
private val ModelDefinition.faceAlphas get() = getFaceAlphas()
private val ModelDefinition.faceSkins get() = getFaceSkins()
private val ModelDefinition.vertexSkins get() = getVertexSkins()
