package stan.qodat.scene.shape

import fxyz3d.geometry.Face3
import fxyz3d.geometry.Point3F
import fxyz3d.shapes.primitives.TexturedMesh
import fxyz3d.shapes.primitives.TriangulatedMesh
import fxyz3d.shapes.primitives.helper.MeshHelper
import javafx.beans.binding.StringBinding
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Point3D
import javafx.scene.DepthTest
import javafx.scene.Group
import javafx.scene.shape.*
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.transform.Translate
import javafx.util.Callback
import stan.qodat.util.onInvalidation
import java.util.concurrent.atomic.AtomicInteger

class Text3D(
    text: String = "",
    font: Font = Font.getDefault(),
    level: Int = 1,
    height: Double = 10.0,
    gap: Double = 0.0
) : Group() {

    private lateinit var meshes: ObservableList<TexturedMesh>
    private lateinit var offset: List<Point3D>

    private lateinit var letterIndexCounter : AtomicInteger
    private lateinit var segmentIndexCounter : AtomicInteger
    private lateinit var letterPath : Shape

    val textProperty = SimpleStringProperty(text).updateMeshesOnInvalidation()
    val fontProperty = SimpleObjectProperty(font).updateMeshesOnInvalidation()
    val joinSegmentsProperty = SimpleBooleanProperty(true).updateMeshesOnInvalidation()
    val levelProperty = SimpleIntegerProperty(level)
    val heightProperty = SimpleDoubleProperty(height)
    val gapProperty = SimpleDoubleProperty(gap)
    // TODO: bind to meshes
    val depthProperty = SimpleBooleanProperty(false).updateMeshesOnInvalidation()

    private val vertexCount = Callback { param: List<Point3F> -> param.size }
    private val faceCount = Callback { param: List<Face3> -> param.size }

    private val vertCountBinding: StringBinding = object : StringBinding() {
        override fun computeValue(): String {
            val sum = meshes.stream()
                .mapToInt { m: TexturedMesh -> vertexCount.call(m.listVertices) }
                .sum()
            return sum.toString()
        }
    }

    private val faceCountBinding: StringBinding = object : StringBinding() {
        override fun computeValue(): String {
            val sum = meshes.stream()
                .mapToInt { m: TexturedMesh -> faceCount.call(m.listFaces) }
                .sum()
            return sum.toString()
        }
    }

    init {
        updateMeshes()
    }

    private fun updateMeshes(){

        val textString = textProperty.value
        val text = Text(textString).apply {
            font = fontProperty().get()
        }
        val builder = TextMeshBuilder(text)
        offset = builder.getOffset()

        meshes = FXCollections.observableArrayList()
        letterIndexCounter = AtomicInteger()
        segmentIndexCounter = AtomicInteger()
        letterPath = Path()
        textString.forEach {
            if (it != ' ')
                it.createLetter()
        }
        children.setAll(meshes)
        updateTransforms()
    }

    private fun updateTransforms() {
        meshes.forEach { it.updateTransforms() }
    }


    private fun Char.createLetter() = apply {
        val string = this.toString()
        val text = Text(string).apply {
            font = fontProperty().get()
        }
        val helper = TextMeshBuilder(text)
        val origin = helper.getOffset()

        val index = letterIndexCounter.get()
        for (segment in helper.segments)
            letterPath = Shape.union(letterPath, segment.path)
        for (segment in helper.segments) {
            val points = segment.points
            val holes = if (segment.holes.isNotEmpty()) segment.holes.map { it.points } else null
            val invert = points.indices.map { points[points.size-1-it] }.distinct()
            val bounds = if (joinSegmentsProperty.value) letterPath.boundsInParent else null
            //TODO: use fxyz3d TriangulatedMesh ?
            val polyMesh = TriangulatedMesh(invert, holes, levelProperty.get(), heightProperty.get(), 0.0, bounds)
            if (segmentIndexCounter.get() > index && joinSegmentsProperty.value) {
                val mh = MeshHelper(meshes[meshes.size - 1].mesh as TriangleMesh)
                val mh1 = MeshHelper(polyMesh.mesh as TriangleMesh)
                mh1.addMesh(mh)
                polyMesh.updateMesh(mh1)
                meshes[meshes.size - 1] = polyMesh
            } else
                meshes.add(polyMesh)
            polyMesh.transforms.addAll(
                Translate(
                    offset[index].x - origin[0].x + letterIndexCounter.get() * gapProperty.doubleValue(), 0.0, 0.0))
            polyMesh.cullFace = CullFace.BACK
            polyMesh.drawMode = DrawMode.FILL
            polyMesh.depthTest = DepthTest.ENABLE
            polyMesh.id = segment.letter
            segmentIndexCounter.getAndIncrement()
        }
        letterIndexCounter.getAndIncrement()
        vertCountBinding.invalidate()
        faceCountBinding.invalidate()
    }

    private fun<T> Property<T>.updateMeshesOnInvalidation() = apply {
        onInvalidation {
            if (this@Text3D::meshes.isInitialized)
                updateMeshes()
        }
        return this
    }
}