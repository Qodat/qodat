package stan.qodat.util

import javafx.beans.property.*
import javafx.scene.paint.Color
import org.slf4j.LoggerFactory
import stan.qodat.Qodat
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class PropertiesManager(private val saveFilePath: Path) {

    private val logger = LoggerFactory.getLogger(PropertiesManager::class.java)
    private val properties = Properties()
    private lateinit var saveThread: Thread

    fun loadFromFile(): Boolean {

        if (!saveFilePath.toFile().exists())
            return false

        try {
            properties.load(saveFilePath.toFile().reader())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    fun startSaveThread() {
        if (!this::saveThread.isInitialized) {
            saveThread = Thread {
                saveToFile()
                Thread.sleep(2500L)
            }
            saveThread.start()
        }
    }

    fun saveToFile() {
        logger.info("Saving properties to $saveFilePath")
        try {
            val saveFile = saveFilePath.toFile()
            if (!saveFile.parentFile.exists())
                saveFile.parentFile.mkdir()
            properties.store(saveFile.writer(), "Contains properties for the Qodat application.")
        } catch (e: Exception) {
            Qodat.logException("Encountered error during saving", e)
        }
    }

    /**
     * Binds a property to a stored value.
     *
     * A stored value that cannot be parsed is discarded rather than propagated, so that a session
     * file written by an older build can never stop the application from starting.
     *
     * @param serializer how to write the value back out; must round-trip through [transformer].
     */
    fun <T> bind(
        key: String,
        property: Property<T>,
        serializer: (T) -> String = { it.toString() },
        transformer: (String) -> T
    ) {
        val value = properties.getProperty(key)
        if (value != null) {
            try {
                property.value = transformer.invoke(value)
            } catch (e: Exception) {
                logger.warn("Discarding unreadable value '$value' for property '$key'", e)
                properties.remove(key)
            }
        }
        property.addListener { _ ->
            val current = property.value
            if (current == null)
                properties.remove(key)
            else
                properties.setProperty(key, serializer.invoke(current))
        }
    }

    fun bindPath(key: String, property: ObjectProperty<Path?>) =
        bind(key, property) { Paths.get(it) }

    /**
     * Enums are stored under their constant name, because [Enum.toString] is often overridden to
     * return a human readable label that will not parse back.
     */
    inline fun <reified T : Enum<T>> bindEnum(key: String, property: ObjectProperty<T>) =
        bind(key, property, serializer = { it.name }) { stored ->
            enumValues<T>().firstOrNull { it.name == stored || it.toString() == stored }
                ?: property.value
        }

    fun bindColor(key: String, property: ObjectProperty<Color>) =
        bind(key, property) { Color.valueOf(it) }

    fun bindBoolean(key: String, property: BooleanProperty) =
        bind(key, property) { java.lang.Boolean.parseBoolean(it) }

    fun bindDouble(key: String, property: DoubleProperty) =
        bind(key, property) { java.lang.Double.parseDouble(it) }

    fun bindString(key: String, property: StringProperty) =
        bind(key, property) { it }

}

