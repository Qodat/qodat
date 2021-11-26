package stan.qodat.scene.controller

import stan.qodat.Properties

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class ViewerController : EntityViewController("viewer-scene") {

    override fun cacheProperty() = Properties.viewerCache
}