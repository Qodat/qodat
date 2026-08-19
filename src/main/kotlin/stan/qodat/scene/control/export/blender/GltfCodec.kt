package stan.qodat.scene.control.export.blender

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import qodat.cache.definition.ModelDefinition
import stan.qodat.cache.impl.qodat.QodatModelDefinition
import stan.qodat.util.HslPalette
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.roundToInt

/**
 * glTF 2.0 interchange matching modelkit's writer.
 *
 * Coordinate space is qodat WaveFront: (x, -y, -z). Vertex skins become a
 * one-weight-per-vert armature (`vskin_N`). Face HSL / priorities / alphas
 * ride in `extras.rs` so Blender can round-trip them.
 */
object GltfCodec {

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    fun write(definition: ModelDefinition, destination: Path, name: String) {
        val built = build(definition, name)
        Files.createDirectories(destination.parent)
        Files.write(destination, built)
    }

    fun read(path: Path): QodatModelDefinition {
        val bytes = Files.readAllBytes(path)
        require(bytes.size >= 12 && String(bytes, 0, 4) == "glTF") {
            "Not a GLB: $path"
        }
        val docAndBin = parseGlb(bytes)
        return decode(docAndBin.first, docAndBin.second, path.fileName.toString().substringBeforeLast('.'))
    }

    private fun build(definition: ModelDefinition, name: String): ByteArray {
        val nVert = definition.getVertexCount()
        val nFace = definition.getFaceCount()
        val vx = definition.getVertexPositionsX()
        val vy = definition.getVertexPositionsY()
        val vz = definition.getVertexPositionsZ()
        val fa = definition.getFaceVertexIndices1()
        val fb = definition.getFaceVertexIndices2()
        val fc = definition.getFaceVertexIndices3()
        val colours = definition.getFaceColors()
        val skins = definition.getVertexSkins() ?: IntArray(nVert)
        val pri = definition.getFacePriorities()
        val alphas = definition.getFaceAlphas()
        val faceSkins = definition.getFaceSkins()

        val pts = FloatArray(nVert * 3)
        for (i in 0 until nVert) {
            pts[i * 3] = vx[i].toFloat()
            pts[i * 3 + 1] = (-vy[i]).toFloat()
            pts[i * 3 + 2] = (-vz[i]).toFloat()
        }
        val vertRgb = FloatArray(nVert * 3)
        val hits = IntArray(nVert)
        for (f in 0 until nFace) {
            val rgb = HslPalette.rgb(colours[f].toInt() and 0xFFFF)
            val r = ((rgb shr 16) and 255) / 255f
            val g = ((rgb shr 8) and 255) / 255f
            val b = (rgb and 255) / 255f
            for (vi in intArrayOf(fa[f], fb[f], fc[f])) {
                vertRgb[vi * 3] += r
                vertRgb[vi * 3 + 1] += g
                vertRgb[vi * 3 + 2] += b
                hits[vi]++
            }
        }
        for (i in 0 until nVert) {
            val n = hits[i].coerceAtLeast(1).toFloat()
            vertRgb[i * 3] = vertRgb[i * 3] / n
            vertRgb[i * 3 + 1] = vertRgb[i * 3 + 1] / n
            vertRgb[i * 3 + 2] = vertRgb[i * 3 + 2] / n
        }
        val indices = ShortArray(nFace * 3)
        for (f in 0 until nFace) {
            indices[f * 3] = fa[f].toShort()
            indices[f * 3 + 1] = fb[f].toShort()
            indices[f * 3 + 2] = fc[f].toShort()
        }

        val unique = skins.distinct().sorted()
        val skinToJoint = unique.withIndex().associate { it.value to it.index }
        val joints = ShortArray(nVert * 4)
        val weights = FloatArray(nVert * 4)
        for (i in 0 until nVert) {
            joints[i * 4] = (skinToJoint[skins[i]] ?: 0).toShort()
            weights[i * 4] = 1f
        }
        val centroids = Array(unique.size) { FloatArray(3) }
        val counts = IntArray(unique.size)
        for (i in 0 until nVert) {
            val j = skinToJoint[skins[i]] ?: continue
            centroids[j][0] += pts[i * 3]
            centroids[j][1] += pts[i * 3 + 1]
            centroids[j][2] += pts[i * 3 + 2]
            counts[j]++
        }
        for (j in unique.indices) {
            val n = counts[j].coerceAtLeast(1).toFloat()
            val c = centroids[j]
            c[0] = c[0] / n
            c[1] = c[1] / n
            c[2] = c[2] / n
        }

        val bin = BinWriter()
        val accessors = JsonArray()
        val views = JsonArray()

        fun pushFloats(data: FloatArray, type: String, count: Int, extra: JsonObject? = null): Int {
            val off = bin.addFloats(data)
            views.add(viewObj(off, data.size * 4))
            val acc = JsonObject()
            acc.addProperty("bufferView", views.size() - 1)
            acc.addProperty("componentType", 5126)
            acc.addProperty("count", count)
            acc.addProperty("type", type)
            extra?.entrySet()?.forEach { acc.add(it.key, it.value) }
            accessors.add(acc)
            return accessors.size() - 1
        }

        fun pushShorts(data: ShortArray, type: String, count: Int): Int {
            val off = bin.addShorts(data)
            views.add(viewObj(off, data.size * 2))
            val acc = JsonObject()
            acc.addProperty("bufferView", views.size() - 1)
            acc.addProperty("componentType", 5123)
            acc.addProperty("count", count)
            acc.addProperty("type", type)
            accessors.add(acc)
            return accessors.size() - 1
        }

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (i in 0 until nVert) {
            val x = pts[i * 3]
            val y = pts[i * 3 + 1]
            val z = pts[i * 3 + 2]
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }
        val posExtra = JsonObject().apply {
            add("min", jsonFloats(minX, minY, minZ))
            add("max", jsonFloats(maxX, maxY, maxZ))
        }
        val posI = pushFloats(pts, "VEC3", nVert, posExtra)
        val colI = pushFloats(vertRgb, "VEC3", nVert)
        val jntI = pushShorts(joints, "VEC4", nVert)
        val wgtI = pushFloats(weights, "VEC4", nVert)
        val idxI = pushShorts(indices, "SCALAR", indices.size)

        val ibm = FloatArray(unique.size * 16)
        for (j in unique.indices) {
            // column-major identity with translation -centroid
            ibm[j * 16 + 0] = 1f
            ibm[j * 16 + 5] = 1f
            ibm[j * 16 + 10] = 1f
            ibm[j * 16 + 15] = 1f
            ibm[j * 16 + 12] = -centroids[j][0]
            ibm[j * 16 + 13] = -centroids[j][1]
            ibm[j * 16 + 14] = -centroids[j][2]
        }
        val ibmI = pushFloats(ibm, "MAT4", unique.size)

        val extras = JsonObject().apply {
            add("rs", JsonObject().apply {
                addProperty("coordSpace", "qodat_wavefront")
                addProperty("skeleton", "labels")
                add("vertexSkins", jsonInts(skins))
                add("faceColors", jsonShorts(colours))
                if (pri != null) add("facePriorities", jsonBytes(pri))
                addProperty("facePriorityDefault", definition.getPriority().toInt() and 0xFF)
                if (alphas != null) add("faceAlphas", jsonBytes(alphas))
                if (faceSkins != null) add("faceSkins", jsonInts(faceSkins))
                val bones = JsonArray()
                for (lab in unique) {
                    bones.add(JsonObject().apply {
                        addProperty("name", "vskin_$lab")
                        addProperty("skin", lab)
                        addProperty("pivot", lab)
                        add("parent", com.google.gson.JsonNull.INSTANCE)
                    })
                }
                add("bones", bones)
            })
        }

        val nodes = JsonArray()
        nodes.add(JsonObject().apply {
            addProperty("name", name)
            addProperty("mesh", 0)
            addProperty("skin", 0)
            add("children", jsonInts((1..unique.size).toList().toIntArray()))
        })
        for (j in unique.indices) {
            nodes.add(JsonObject().apply {
                addProperty("name", "vskin_${unique[j]}")
                add("translation", jsonFloats(centroids[j][0], centroids[j][1], centroids[j][2]))
                add("extras", JsonObject().apply {
                    addProperty("rsSkin", unique[j])
                    addProperty("rsPivot", unique[j])
                })
            })
        }

        val gltf = JsonObject().apply {
            add("asset", JsonObject().apply {
                addProperty("version", "2.0")
                addProperty("generator", "qodat")
            })
            addProperty("scene", 0)
            add("scenes", JsonArray().apply { add(JsonObject().apply { add("nodes", jsonInts(intArrayOf(0))) }) })
            add("nodes", nodes)
            add("meshes", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("name", name)
                    add("primitives", JsonArray().apply {
                        add(JsonObject().apply {
                            add("attributes", JsonObject().apply {
                                addProperty("POSITION", posI)
                                addProperty("COLOR_0", colI)
                                addProperty("JOINTS_0", jntI)
                                addProperty("WEIGHTS_0", wgtI)
                            })
                            addProperty("indices", idxI)
                            addProperty("material", 0)
                        })
                    })
                    add("extras", extras)
                })
            })
            add("skins", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("name", "labels")
                    add("joints", jsonInts((1..unique.size).toList().toIntArray()))
                    addProperty("inverseBindMatrices", ibmI)
                })
            })
            add("materials", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("name", name)
                    add("pbrMetallicRoughness", JsonObject().apply {
                        add("baseColorFactor", jsonFloats(1f, 1f, 1f, 1f))
                        addProperty("metallicFactor", 0)
                        addProperty("roughnessFactor", 1)
                    })
                })
            })
            add("accessors", accessors)
            add("bufferViews", views)
            add("buffers", JsonArray().apply {
                add(JsonObject().apply { addProperty("byteLength", bin.size()) })
            })
            add("extras", extras)
        }

        val jsonBytes = pad4(gson.toJson(gltf).toByteArray(Charsets.UTF_8), 0x20)
        val binBytes = pad4(bin.toByteArray(), 0)
        val total = 12 + 8 + jsonBytes.size + 8 + binBytes.size
        val out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        out.put("glTF".toByteArray())
        out.putInt(2)
        out.putInt(total)
        out.putInt(jsonBytes.size)
        out.put("JSON".toByteArray())
        out.put(jsonBytes)
        out.putInt(binBytes.size)
        out.put("BIN\u0000".toByteArray())
        out.put(binBytes)
        return out.array()
    }

    private fun decode(doc: JsonObject, blob: ByteArray, fallbackName: String): QodatModelDefinition {
        val extras = rsExtras(doc)
        val mesh = doc.getAsJsonArray("meshes")[0].asJsonObject
        val prim = mesh.getAsJsonArray("primitives")[0].asJsonObject
        val attrs = prim.getAsJsonObject("attributes")
        val ptsB = readF32(doc, blob, attrs.get("POSITION").asInt, 3)
        val nVert = ptsB.size / 3
        val vx = IntArray(nVert)
        val vy = IntArray(nVert)
        val vz = IntArray(nVert)
        for (i in 0 until nVert) {
            vx[i] = ptsB[i * 3].roundToInt()
            vy[i] = (-ptsB[i * 3 + 1]).roundToInt()
            vz[i] = (-ptsB[i * 3 + 2]).roundToInt()
        }
        val idx = readU16(doc, blob, prim.get("indices").asInt)
        require(idx.size % 3 == 0) { "index count not divisible by 3" }
        val nFace = idx.size / 3
        val fa = IntArray(nFace) { idx[it * 3] }
        val fb = IntArray(nFace) { idx[it * 3 + 1] }
        val fc = IntArray(nFace) { idx[it * 3 + 2] }

        val colours = extras?.getAsJsonArray("faceColors")?.let { arr ->
            ShortArray(arr.size()) { arr[it].asInt.toShort() }
        } ?: ShortArray(nFace) { 0 }

        val skins = extras?.getAsJsonArray("vertexSkins")?.let { arr ->
            IntArray(arr.size()) { arr[it].asInt }
        } ?: IntArray(nVert)

        val pri = extras?.getAsJsonArray("facePriorities")?.let { arr ->
            ByteArray(arr.size()) { arr[it].asInt.toByte() }
        }
        val alphas = extras?.getAsJsonArray("faceAlphas")?.let { arr ->
            ByteArray(arr.size()) { arr[it].asInt.toByte() }
        }
        val faceSkins = extras?.getAsJsonArray("faceSkins")?.let { arr ->
            IntArray(arr.size()) { arr[it].asInt }
        }
        val priority = extras?.get("facePriorityDefault")?.asInt?.toByte() ?: 10
        val name = mesh.get("name")?.asString ?: fallbackName
        val def = QodatModelDefinition(
            name = name,
            vertexCount = nVert,
            vertexPositionsX = vx,
            vertexPositionsY = vy,
            vertexPositionsZ = vz,
            vertexSkins = skins,
            faceCount = nFace,
            faceVertexIndices1 = fa,
            faceVertexIndices2 = fb,
            faceVertexIndices3 = fc,
            faceSkins = faceSkins,
            faceAlphas = alphas,
            facePriorities = pri,
            faceTypes = null,
            priority = priority,
            faceColors = colours,
        )
        def.computeAnimationTables()
        return def
    }

    private fun rsExtras(doc: JsonObject): JsonObject? {
        doc.getAsJsonObject("extras")?.getAsJsonObject("rs")?.let { return it }
        val mesh = doc.getAsJsonArray("meshes")?.get(0)?.asJsonObject ?: return null
        return mesh.getAsJsonObject("extras")?.getAsJsonObject("rs")
    }

    private fun parseGlb(bytes: ByteArray): Pair<JsonObject, ByteArray> {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(12)
        var json: JsonObject? = null
        var bin = ByteArray(0)
        while (buf.remaining() >= 8) {
            val len = buf.int
            val type = ByteArray(4).also { buf.get(it) }
            val chunk = ByteArray(len).also { buf.get(it) }
            when (String(type, Charsets.US_ASCII)) {
                "JSON" -> json = gson.fromJson(String(chunk, Charsets.UTF_8).trim(), JsonObject::class.java)
                "BIN\u0000" -> bin = chunk
            }
        }
        return (json ?: error("GLB missing JSON")) to bin
    }

    private fun readF32(doc: JsonObject, blob: ByteArray, accessor: Int, width: Int): FloatArray {
        val (off, n) = accessorRange(doc, accessor, width, 4)
        val buf = ByteBuffer.wrap(blob, off, n).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(n / 4)
        for (i in out.indices) out[i] = buf.float
        return out
    }

    private fun readU16(doc: JsonObject, blob: ByteArray, accessor: Int): IntArray {
        val (off, n) = accessorRange(doc, accessor, 1, 2)
        val buf = ByteBuffer.wrap(blob, off, n).order(ByteOrder.LITTLE_ENDIAN)
        val out = IntArray(n / 2)
        for (i in out.indices) out[i] = buf.short.toInt() and 0xFFFF
        return out
    }

    private fun accessorRange(doc: JsonObject, index: Int, width: Int, elem: Int): Pair<Int, Int> {
        val acc = doc.getAsJsonArray("accessors")[index].asJsonObject
        val view = doc.getAsJsonArray("bufferViews")[acc.get("bufferView").asInt].asJsonObject
        val off = view.get("byteOffset")?.asInt ?: 0
        val count = acc.get("count").asInt
        return off to count * width * elem
    }

    private fun viewObj(offset: Int, length: Int) = JsonObject().apply {
        addProperty("buffer", 0)
        addProperty("byteOffset", offset)
        addProperty("byteLength", length)
    }

    private fun jsonFloats(vararg v: Float) = JsonArray().apply { v.forEach { add(it) } }
    private fun jsonInts(v: IntArray) = JsonArray().apply { v.forEach { add(it) } }
    private fun jsonShorts(v: ShortArray) = JsonArray().apply { v.forEach { add(it.toInt() and 0xFFFF) } }
    private fun jsonBytes(v: ByteArray) = JsonArray().apply { v.forEach { add(it.toInt() and 0xFF) } }

    private fun pad4(data: ByteArray, fill: Int): ByteArray {
        val pad = (4 - data.size % 4) % 4
        if (pad == 0) return data
        return data + ByteArray(pad) { fill.toByte() }
    }

    private class BinWriter {
        private val out = ByteArrayOutputStream()
        fun size() = out.size()
        fun toByteArray() = out.toByteArray()
        fun addFloats(data: FloatArray): Int {
            align4()
            val off = out.size()
            val buf = ByteBuffer.allocate(data.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            data.forEach { buf.putFloat(it) }
            out.write(buf.array())
            return off
        }
        fun addShorts(data: ShortArray): Int {
            align4()
            val off = out.size()
            val buf = ByteBuffer.allocate(data.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            data.forEach { buf.putShort(it) }
            out.write(buf.array())
            return off
        }
        private fun align4() {
            while (out.size() % 4 != 0) out.write(0)
        }
    }
}
