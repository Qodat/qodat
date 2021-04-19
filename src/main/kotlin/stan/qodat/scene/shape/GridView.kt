package stan.qodat.scene.shape

import javafx.scene.Group
import javafx.scene.transform.Rotate
import stan.qodat.Properties
import stan.qodat.util.setAndBindMaterial

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
class GridView(
    size: Double,
    delta: Double
) : Group() {

    init {
        val plane = createQuadrilateralMesh(size, size, (size / delta).toInt(), (size / delta).toInt())
        val planeView = PolygonMeshView(plane)
        planeView.setAndBindMaterial(Properties.gridPlaneMaterial)
        planeView.transforms.add(Rotate(90.0, Rotate.X_AXIS))
        children.setAll(planeView)
    }

    private fun createQuadrilateralMesh(width: Double, height: Double, subDivX: Int, subDivY: Int): PolygonMesh {
        val minX = -width / 2f
        val minY = -height / 2f
        val maxX = width / 2f
        val maxY = height / 2f

        val pointSize = 3
        val texCoordSize = 2
        // 4 point indices and 4 texCoord indices per face
        val faceSize = 8
        val numDivX = subDivX + 1
        val numVerts = (subDivY + 1) * numDivX
        val points = FloatArray(numVerts * pointSize)
        val texCoords = FloatArray(numVerts * texCoordSize)
        val faceCount = subDivX * subDivY
        val faces = Array(faceCount) { IntArray(faceSize) }

        // Create points and texCoords
        for (y in 0..subDivY) {
            val dy = y.toFloat() / subDivY
            val fy = ((1 - dy) * minY + dy * maxY)

            for (x in 0..subDivX) {
                val dx = x.toFloat() / subDivX
                val fx = ((1 - dx) * minX + dx * maxX)

                var index = y * numDivX * pointSize + x * pointSize
                points[index] = fx.toFloat()
                points[index + 1] = fy.toFloat()
                points[index + 2] = 0.0f

                index = y * numDivX * texCoordSize + x * texCoordSize
                texCoords[index] = dx
                texCoords[index + 1] = dy
            }
        }

        // Create faces
        var index = 0
        for (y in 0 until subDivY) {
            for (x in 0 until subDivX) {
                val p00 = y * numDivX + x
                val p01 = p00 + 1
                val p10 = p00 + numDivX
                val p11 = p10 + 1
                val tc00 = y * numDivX + x
                val tc01 = tc00 + 1
                val tc10 = tc00 + numDivX
                val tc11 = tc10 + 1

                faces[index][0] = p00
                faces[index][1] = tc00
                faces[index][2] = p10
                faces[index][3] = tc10
                faces[index][4] = p11
                faces[index][5] = tc11
                faces[index][6] = p01
                faces[index++][7] = tc01
            }
        }
        val smooth = IntArray(faceCount)
        val mesh = PolygonMesh(points, texCoords, faces)
        mesh.faceSmoothingGroups.addAll(*smooth)
        return mesh
    }
}