package stan.qodat.scene.runescape.entity

import javafx.beans.property.SimpleStringProperty
import javafx.scene.Node
import javafx.scene.layout.HBox
import stan.qodat.cache.Cache
import stan.qodat.cache.definition.ItemDefinition
import stan.qodat.util.ViewNodeProvider
import stan.qodat.cache.CacheEncoder
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.util.SceneNodeProvider
import stan.qodat.util.Searchable
import java.io.UnsupportedEncodingException

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class Item(cache: Cache, definition: ItemDefinition)
    : Entity<ItemDefinition>(cache, definition), CacheEncoder {

    override fun encode(format: Cache) {
        throw UnsupportedEncodingException()
    }
}