package stan.qodat.util

import javafx.beans.property.*
import javafx.scene.paint.Color
import javafx.scene.paint.Material
import javafx.scene.paint.PhongMaterial
import java.lang.Exception
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class PropertiesManager(
    private val saveFilePath: Path = Paths.get("session.properties")
) {
    private val properties = Properties()
    private lateinit var saveThread : Thread

    fun loadFromFile() {

        if (!saveFilePath.toFile().exists())
            return

        try {
            properties.load(saveFilePath.toFile().reader())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!this::saveThread.isInitialized){
            saveThread = Thread {
                saveToFile()
                Thread.sleep(2500L)
            }
            saveThread.start()
        }
    }

    fun saveToFile() {
        try {
            properties.store(saveFilePath.toFile().writer(), "Contains properties for the Qodat application.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun<T> bind(key: String, property: Property<T>, transformer: (String) -> T){
        val value = properties.getProperty(key)
        if (value != null)
            property.value = transformer.invoke(value)
        property.addListener { _ ->
            properties.setProperty(key, property.value.toString())
        }
    }

    /**
     * TODO: finish
     */
    fun bindMaterial(key: String, property: ObjectProperty<Material>) = bind(key, property)
    { PhongMaterial() }

    fun bindColor(key: String, property: ObjectProperty<Color>) = bind(key, property)
    { Color.valueOf(it)}

    fun bindBoolean(key: String, property: BooleanProperty) = bind(key, property)
    { java.lang.Boolean.parseBoolean(it) }

    fun bindDouble(key: String, property: DoubleProperty) = bind(key, property)
    { java.lang.Double.parseDouble(it) }

    fun bindInt(key: String, property: IntegerProperty) = bind(key, property)
    { java.lang.Integer.parseInt(it) }

    fun bindString(key: String, property: StringProperty) = bind(key, property)
    { it }
}