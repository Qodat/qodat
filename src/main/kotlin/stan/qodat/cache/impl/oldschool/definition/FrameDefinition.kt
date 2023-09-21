package stan.qodat.cache.impl.oldschool.definition

import net.runelite.cache.io.InputStream

class FrameDefinition(framemap: FrameMapDefinition, id: Int, b: ByteArray) {
    var id: Int = 0 // file id
    var framemap: FrameMapDefinition
    var translator_x: IntArray
    var translator_y: IntArray
    var translator_z: IntArray
    var translatorCount: Int = -1
    var indexFrameIds: IntArray
    var showing: Boolean = false
    var framemapArchiveIndex: Int

    init {
        val `in` = InputStream(b)
        val data = InputStream(b)

        this.id = id
        this.framemap = framemap

        framemapArchiveIndex = `in`.readUnsignedShort()
        val length = `in`.readUnsignedByte()

        data.skip(3 + length)

        val indexFrameIds = IntArray(500)
        val scratchTranslatorX = IntArray(500)
        val scratchTranslatorY = IntArray(500)
        val scratchTranslatorZ = IntArray(500)

        var lastI = -1
        var index = 0
        for (i in 0 until length) {
            val var9 = `in`.readUnsignedByte()

            if (var9 <= 0) {
                continue
            }

            if (framemap.types[i] != 0) {
                for (var10 in i - 1 downTo lastI + 1) {
                    if (framemap.types[var10] == 0) {
                        indexFrameIds[index] = var10
                        scratchTranslatorX[index] = 0
                        scratchTranslatorY[index] = 0
                        scratchTranslatorZ[index] = 0
                        index++
                        break
                    }
                }
            }

            indexFrameIds[index] = i
            var var11: Short = 0
            if (framemap.types[i] == 3) {
                var11 = 128
            }

            if ((var9 and 1) != 0) {
                scratchTranslatorX[index] = data.readShortSmart()
            } else {
                scratchTranslatorX[index] = var11.toInt()
            }

            if ((var9 and 2) != 0) {
                scratchTranslatorY[index] = data.readShortSmart()
            } else {
                scratchTranslatorY[index] = var11.toInt()
            }

            if ((var9 and 4) != 0) {
                scratchTranslatorZ[index] = data.readShortSmart()
            } else {
                scratchTranslatorZ[index] = var11.toInt()
            }

            lastI = i
            index++
            if (framemap.types[i] == 5) {
                showing = true
            }
        }

        if (data.getOffset() != b.size) {
            throw RuntimeException()
        }

        translatorCount = index
        this.indexFrameIds = IntArray(index)
        translator_x = IntArray(index)
        translator_y = IntArray(index)
        translator_z = IntArray(index)

        for (i in 0 until index) {
            this.indexFrameIds[i] = indexFrameIds[i]
            translator_x[i] = scratchTranslatorX[i]
            translator_y[i] = scratchTranslatorY[i]
            translator_z[i] = scratchTranslatorZ[i]
        }
    }
}
