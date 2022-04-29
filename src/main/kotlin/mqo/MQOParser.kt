package mqo

import java.io.BufferedReader
import java.io.IOException

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   2019-07-18
 * @version 1.0
 */
class MQOParser(private val reader: BufferedReader) {

    private lateinit var rootGroup: MQOGroup

    fun convertFigure(): MQOFigure {
        return MQOFigure(rootGroup)
    }

    fun parse(){
        reader.readLine()
        reader.readLine()

        rootGroup = MQOGroup()

        var line = reader.readLine()

        while (line != null){
            val chunk = readChunk(line)
            if (chunk != null) {
                rootGroup.addChild(chunk)
            }
            line = reader.readLine()
        }
    }

    @Throws(IOException::class)
    private fun readChunk(header: String): MQOGroup? {

        val result = MQOGroup()
        val mqHead = MQOLine.create(header) ?: return null

        if (!mqHead.isChunkHead())
            return null

        result.header = mqHead

        var str = reader.readLine()

        println("reading $str")
        while (str != null){
            when (str.last()) {
                '{' -> result.addChild(readChunk(str)!!)
                '}' -> return result
                else -> result.addLine(MQOLine.create(str)!!)
            }
            str = reader.readLine()
        }
        return result
    }

}