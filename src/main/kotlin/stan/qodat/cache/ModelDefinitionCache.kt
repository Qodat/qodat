package stan.qodat.cache

import qodat.cache.definition.ModelDefinition

/**
 * LRU of decoded model definitions. Humanoid NPCs reuse the same body models,
 * so arrow-key browse should not re-read and re-decode those archives.
 *
 * Soft caps keep recent preview hits without retaining every viewed model.
 */
object ModelDefinitionCache {

    const val MAX_ENTRIES = 256
    const val MAX_WEIGHT_BYTES = 96L * 1024L * 1024L

    private val map = BoundedLruCache<String, ModelDefinition>(
        maxEntries = MAX_ENTRIES,
        maxWeight = MAX_WEIGHT_BYTES,
        weigher = ::estimateBytes,
    )

    fun getOrLoad(cacheName: String, id: String, load: () -> ModelDefinition): ModelDefinition {
        val key = "$cacheName:$id"
        return map.getOrLoad(key, load)
    }

    fun clear() {
        map.clear()
    }

    internal val size: Int
        get() = map.size

    internal fun estimateBytes(def: ModelDefinition): Long {
        val vertices = def.getVertexCount().toLong().coerceAtLeast(0L)
        val faces = def.getFaceCount().toLong().coerceAtLeast(0L)
        var bytes = 64L + vertices * 12L + faces * 14L
        def.getVertexSkins()?.let { bytes += it.size * 4L }
        def.getFaceSkins()?.let { bytes += it.size * 4L }
        def.getVertexGroups()?.let { groups -> bytes += groups.sumOf { it.size * 4L } }
        def.getFaceGroups()?.let { groups -> bytes += groups.sumOf { it.size * 4L } }
        def.getVertexNormals()?.let { bytes += it.size * 32L }
        def.getFaceNormals()?.let { bytes += it.size * 24L }
        def.getFaceAlphas()?.let { bytes += it.size.toLong() }
        def.getFaceTextures()?.let { bytes += it.size * 2L }
        def.getFaceTextureUCoordinates()?.let { bytes += it.size * 24L }
        def.getFaceTextureVCoordinates()?.let { bytes += it.size * 24L }
        return bytes
    }
}
