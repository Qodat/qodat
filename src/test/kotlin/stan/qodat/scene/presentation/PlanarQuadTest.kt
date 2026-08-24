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
                0, 0, 3, 3, 2, 2,
                0, 0, 2, 2, 1, 1,
            ),
            mesh.faces,
        )
    }

    @Test
    fun degenerateSizeStillProducesAQuad() {
        val mesh = PlanarQuad.mesh(0.0, 0.0)
        assertEquals(12, mesh.points.size)
        assertTrue(mesh.points[3] >= 0.5f)
        assertTrue(mesh.points[7] <= -0.5f)
    }
}
