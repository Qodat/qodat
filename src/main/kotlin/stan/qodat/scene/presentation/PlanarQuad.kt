package stan.qodat.scene.presentation

import javafx.scene.DepthTest
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.MeshView
import javafx.scene.shape.TriangleMesh

/**
 * Client-space quad (X right, Y down mapped to -Y) that faces the SubScene camera.
 *
 * Faces are CCW from -Z so the texture is not mirrored. V is flipped because
 * JavaFX TriangleMesh uploads images with V=0 at the bottom.
 */
object PlanarQuad {

    data class Mesh(
        val points: FloatArray,
        val texCoords: FloatArray,
        val faces: IntArray,
    )

    fun mesh(width: Double, height: Double): Mesh {
        val w = width.toFloat().coerceAtLeast(0.5f)
        val h = height.toFloat().coerceAtLeast(0.5f)
        return Mesh(
            points = floatArrayOf(
                0f, 0f, 0f,
                w, 0f, 0f,
                w, -h, 0f,
                0f, -h, 0f,
            ),
            texCoords = floatArrayOf(
                0f, 1f,
                1f, 1f,
                1f, 0f,
                0f, 0f,
            ),
            faces = intArrayOf(
                0, 0, 3, 3, 2, 2,
                0, 0, 2, 2, 1, 1,
            ),
        )
    }

    fun unlit(image: Image): PhongMaterial = PhongMaterial(Color.WHITE).apply {
        diffuseMap = image
        selfIlluminationMap = image
        specularColor = Color.BLACK
    }

    fun of(width: Double, height: Double, material: PhongMaterial): MeshView {
        val data = mesh(width, height)
        val triangle = TriangleMesh()
        triangle.points.setAll(*data.points)
        triangle.texCoords.setAll(*data.texCoords)
        triangle.faces.setAll(*data.faces)
        return MeshView(triangle).apply {
            this.material = material
            cullFace = CullFace.BACK
            depthTest = DepthTest.ENABLE
        }
    }

    fun image(image: Image, width: Double = image.width, height: Double = image.height): MeshView =
        of(width, height, unlit(image))
}
