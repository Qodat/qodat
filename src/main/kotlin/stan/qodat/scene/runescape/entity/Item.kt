package stan.qodat.scene.runescape.entity

import stan.qodat.Properties
import stan.qodat.cache.Cache
import stan.qodat.cache.Encoder
import stan.qodat.cache.definition.ItemDefinition
import java.io.File
import java.io.UnsupportedEncodingException

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class Item(cache: Cache, definition: ItemDefinition)
    : Entity<ItemDefinition>(cache, definition), Encoder {

    override fun property() = Properties.selectedItemName
}