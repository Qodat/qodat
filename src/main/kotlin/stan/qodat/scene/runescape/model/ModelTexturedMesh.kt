package stan.qodat.scene.runescape.model

import fxyz3d.scene.paint.ColorPalette
import fxyz3d.shapes.primitives.TexturedMesh
import fxyz3d.shapes.primitives.helper.MeshHelper
import fxyz3d.shapes.primitives.helper.TriangleMeshHelper
import javafx.scene.paint.Color
import javafx.scene.shape.MeshView
import qodat.cache.definition.ModelDefinition
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.runescape.*
import stan.qodat.util.ModelUtil
import kotlin.collections.HashMap

class ModelTexturedMesh(private val model: Model, private val atlas: ModelAtlasMesh) : TexturedMesh(), ModelSkin {

    val vertexMap : MutableMap<Int, Int>
    init {
        vertexMap = mutableMapOf<Int, Int>()
        sectionType = TriangleMeshHelper.SectionType.TRIANGLE
        textureType = TriangleMeshHelper.TextureType.COLORED_VERTICES_3D
        helper.sectionType = sectionType
        updateMesh()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val npc = OldschoolCacheRuneLite.npcManager.npcs.find { it.name.contains("abyssal demon", true) }!!
            val model = OldschoolCacheRuneLite.getModelDefinition(npc.models.first().toString())

            model.draw(false)


        }
    }

    data class Vertex(val x: Int, val y: Int, val z: Int, val face: Int)

    override fun updateMesh() {
        val definition = model.modelDefinition

        val (colors1, colors2, colors3) = definition.calculateFaceColors(
            brightnessOffset = 64,
            shadowModifier = 850,
            sizeX = -30,
            sizeY = -50,
            sizeZ = -30,
            shade = true
        )
        val uniqueColorHSBValues = (colors1 + colors2 + colors3).toSet().toList()
        val uniqueColors = uniqueColorHSBValues.map { ModelUtil.hsbToColor(it, null) }

        val meshHelper = MeshHelper()
        val faceCount = definition.getFaceCount()
        val f = FloatArray(faceCount * 3)
        val points = FloatArray(faceCount * 9)
        val faces = IntArray(faceCount * 6)
        val faceSmoothinGroups = IntArray(faceCount)
        var vi = 0
        var pi = 0
        var fi = 0
        var gi = 0
        for (face in 0 until faceCount) {
            val (v1, v2, v3) = definition.getVertices(face)
            f[vi++] = face.toFloat()
            f[vi++] = face.toFloat()
            f[vi++] = face.toFloat()
            val v1Onset = addVertex(pi, points, definition, v1)
            pi += 3
            val v2Onset = addVertex(pi, points, definition, v2)
            pi += 3
            val v3Onset = addVertex(pi, points, definition, v3)
            pi += 3
            faces[fi++] = v1Onset
            faces[fi++] = 0
            faces[fi++] = v2Onset
            faces[fi++] = 0
            faces[fi++] = v3Onset
            faces[fi++] = 0

            faceSmoothinGroups[gi++] = 0
        }
        meshHelper.points = points
        meshHelper.f = f
        meshHelper.faceSmoothingGroups = faceSmoothinGroups
        meshHelper.faces = faces

        val pointColorIndexMap = HashMap<Vertex, Int>()
        var idx = 0
        val colors = arrayOfNulls<Color>(definition.getFaceCount() * 3)
        for (face in 0 until definition.getFaceCount()) {
            val alpha = definition.getFaceAlphas()?.get(face)
            val type = definition.getFaceTypes()?.get(face)?.toInt()?.let { it and 3 } ?: 0
            val (p1, p2, p3) = definition.getPoints(face)
            val v1 = Vertex(p1.first, p1.second, p1.third, face)
            val v2 = Vertex(p2.first, p2.second, p2.third, face)
            val v3 = Vertex(p3.first, p3.second, p3.third, face)
//            assert(!pointColorIndexMap.containsKey(v1))
//            assert(!pointColorIndexMap.containsKey(v2))
//            assert(!pointColorIndexMap.containsKey(v3))

            if (type == RENDER_SHADED_TRIANGLE) {
                pointColorIndexMap[v1] = idx
                colors[idx++] = ModelUtil.hsbToColor(colors1[face], alpha)
                pointColorIndexMap[v2] = idx
                colors[idx++] = ModelUtil.hsbToColor(colors2[face], alpha)
                pointColorIndexMap[v3] = idx
                colors[idx++] = ModelUtil.hsbToColor(colors3[face], alpha)
            } else if (type == RENDER_FLAT_TRIANGLE) {
                pointColorIndexMap[v1] = idx
                colors[idx++] = ModelUtil.hsbToColor(colors1[face], alpha)
                pointColorIndexMap[v2] = idx
                colors[idx++] = ModelUtil.hsbToColor(colors1[face], alpha)
                pointColorIndexMap[v3] = idx
                colors[idx++] = ModelUtil.hsbToColor(colors1[face], alpha)
            }
        }
        // Face3{p0=41, p1=50, p2=51}
        val palette = object : ColorPalette {
            override fun getNumColors() = colors.size
            override fun getColor(i: Int) = colors[i]!!
        }

        setTextureModeVertices3D(palette) { point3F ->
            val key = Vertex(point3F.x.toInt(), point3F.y.toInt(), point3F.z.toInt(), point3F.f.toInt())
            val colorIndex = pointColorIndexMap[key]!!
            colorIndex
        }


        updateMesh(meshHelper)
    }


    private fun addVertex(
        pi: Int,
        points: FloatArray,
        definition: ModelDefinition,
        index: Int
    ): Int {
        val onset = pi / 3
        points[pi] = definition.getX(index).toFloat()
        points[pi + 1] = definition.getY(index).toFloat()
        points[pi + 2] = definition.getZ(index).toFloat()
        return onset
    }

    override fun updatePoints(skeleton: ModelSkeleton) {
        val points = mesh.points
        for((vertex, localVertex) in vertexMap){
            val x = skeleton.getX(vertex).toFloat()
            val y = skeleton.getY(vertex).toFloat()
            val z = skeleton.getZ(vertex).toFloat()
            (localVertex * 3 ).let { if (points.get(it) != x) points.set(it, x) }
            (localVertex * 3 + 1).let { if (points.get(it) != y) points.set(it, y) }
            (localVertex * 3 + 2).let { if (points.get(it) != z) points.set(it, z) }
        }
    }

    override fun getSceneNode(): MeshView {
        return this
    }
}