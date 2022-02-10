package stan.qodat.scene.runescape.entity

import stan.qodat.Properties
import qodat.cache.Cache
import qodat.cache.Encoder
import qodat.cache.definition.ItemDefinition

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