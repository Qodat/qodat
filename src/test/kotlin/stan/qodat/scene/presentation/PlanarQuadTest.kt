package stan.qodat.scene.presentation

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanarQuadTest {

    @Test
    fun meshFacesTheCameraAndFlipsV() {
        val mesh = PlanarQuad.mesh(10.0, 4.0)
        assertContentEquals(
            floatArrayOf(
                0f, 0f, 0f,
                10f, 0f, 0f,
                10f, -4f, 0f,
                0f, -4f, 0f,
            ),
            mesh.points,
        )
        assertContentEquals(
            floatArrayOf(
                0f, 1f,
                1f, 1f,
                1f, 0f,
                0f, 0f,
            ),
            mesh.texCoords,
        )
        assertContentEquals(
            intArrayOf(
                0, 0, 1, 1, 2, 2,
                0, 0, 2, 2, 3, 3,
            ),
            mesh.faces,
        )
        assertTrue(faceNormalZ(mesh, 0) < 0.0, "first triangle must face the camera at -Z")
        assertTrue(faceNormalZ(mesh, 1) < 0.0, "second triangle must face the camera at -Z")
    }

    private fun faceNormalZ(mesh: PlanarQuad.Mesh, triangle: Int): Double {
        val base = triangle * 6
        fun vx(corner: Int) = mesh.points[mesh.faces[base + corner * 2] * 3].toDouble()
        fun vy(corner: Int) = mesh.points[mesh.faces[base + corner * 2] * 3 + 1].toDouble()
        val e1x = vx(1) - vx(0)
        val e1y = vy(1) - vy(0)
        val e2x = vx(2) - vx(0)
        val e2y = vy(2) - vy(0)
        return e1x * e2y - e1y * e2x
    }

    @Test
    fun degenerateSizeStillProducesAQuad() {
        val mesh = PlanarQuad.mesh(0.0, 0.0)
        assertEquals(12, mesh.points.size)
        assertTrue(mesh.points[3] >= 0.5f)
        assertTrue(mesh.points[7] <= -0.5f)
    }
}
