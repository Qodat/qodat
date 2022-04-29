package mqo

import mqo.MQOConstants.MAT_PREFIX
import mqo.MQOConstants.VERTEX_PREFIX
import kotlin.math.roundToInt

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   2019-07-18
 * @version 1.0
 */
class MQOMesh(obj: MQOGroup) {

    val name = obj.header[1]

    var vertexCount: Int
    var faceCount: Int
    var triangleViewPortX : IntArray
    var triangleViewPortY : IntArray
    var triangleViewPortZ : IntArray
    var triangleMaterials : IntArray
    var faceAlpha : ByteArray
    var faceColors : ShortArray

    var verticesX: FloatArray
    var verticesY: FloatArray
    var verticesZ: FloatArray
    var verticesWeights: HashMap<Int, Int>

    init {

        val verticesGroup = obj.getChild("vertex")!!
        vertexCount = verticesGroup.getEntries()
        verticesX = FloatArray(vertexCount)
        verticesY = FloatArray(vertexCount)
        verticesZ = FloatArray(vertexCount)

        for(i in 0 until vertexCount){

            val words = verticesGroup[i]

            words[0].let { verticesX[i] = (it.toDoubleOrNull()?.roundToInt()?:it.toInt()).toFloat() }
            words[1].let { verticesY[i] = (it.toDoubleOrNull()?.roundToInt()?:it.toInt()).toFloat() }
            words[2].let { verticesZ[i] = (it.toDoubleOrNull()?.roundToInt()?:it.toInt()).toFloat() }
        }

        for(i in 0 until vertexCount){
            verticesY[i] *= -1f
            verticesZ[i] *= -1f
        }

        verticesWeights = HashMap(vertexCount)
        obj.getChild("vertexattr")?.getChild("weit")?.let {
            for(i in 0 until it.getEntries()){
                val words = it[i]
                verticesWeights[words[0].toInt()] = (100.0 * words[1].toDouble()).roundToInt()
//                println("${words[0]} = ${verticesWeights[words[0].toInt()]} -> ${words[1].toDouble()}")
            }
        }

        val faceGroup = obj.getChild("face")!!
        faceCount = faceGroup.getEntries()
        triangleViewPortX = IntArray(faceCount)
        triangleViewPortY = IntArray(faceCount)
        triangleViewPortZ = IntArray(faceCount)
        triangleMaterials = IntArray(faceCount)
        faceColors = ShortArray(faceCount)
        faceAlpha = ByteArray(faceCount)

        for(i in 0 until faceCount){

            val line = faceGroup[i]
            val wordCount = line.countStrings()

            var j = 0

            while (j < wordCount){

                val word = line[j]

                when {
                    word.startsWith(VERTEX_PREFIX) -> {
                        triangleViewPortZ[i] = word.drop(VERTEX_PREFIX.length).toInt()
                        triangleViewPortY[i] = line[++j].toInt()
                        triangleViewPortX[i] = line[++j].dropLast(1).toInt()
                    }
                    word.startsWith(MAT_PREFIX) ->
                        triangleMaterials[i] = word.substringAfter(MAT_PREFIX).dropLast(1).toInt()
                }
                j++
            }
        }
    }


}