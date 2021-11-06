package stan.qodat.cache.impl.legacy.storage

import stan.qodat.cache.definition.ModelDefinition
import stan.qodat.cache.util.CompressionUtil
import stan.qodat.cache.util.RSModelLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   11/10/2019
 * @version 1.0
 */
object LegacyModelStorage {

    private val modelMap = HashMap<String, ModelDefinition>()

    fun getModel(cachePath: Path, modelId: String) = modelMap.getOrPut(modelId) {
        val compressedData = Files.readAllBytes(cachePath.resolve("all_models").resolve("$modelId.gz"))!!
        RSModelLoader().load(modelId, CompressionUtil.uncrompressGzip(compressedData))
    }
}