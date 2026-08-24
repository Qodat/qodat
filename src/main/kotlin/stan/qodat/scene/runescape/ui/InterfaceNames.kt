package stan.qodat.scene.runescape.ui

import qodat.cache.definition.InterfaceDefinition

/**
 * Best-effort interface titles from widget `name` / title `text` in the cache.
 */
object InterfaceNames {

    fun display(groupId: Int, definitions: List<InterfaceDefinition>): String {
        val derived = derive(definitions) ?: return groupId.toString()
        return "$groupId  $derived"
    }

    fun derive(definitions: List<InterfaceDefinition>): String? {
        val fromName = definitions.asSequence()
            .mapNotNull { clean(it.name) }
            .firstOrNull { it !in GENERIC }
        if (fromName != null)
            return fromName
        return definitions.asSequence()
            .filter { it.type == 4 }
            .mapNotNull { clean(it.text) }
            .filter { it.length in 2..48 && it !in GENERIC && looksLikeTitle(it) }
            .firstOrNull()
    }

    private fun clean(value: String?): String? {
        val text = value
            ?.replace(TAG, "")
            ?.replace('\u00a0', ' ')
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            ?: return null
        return text
    }

    private fun looksLikeTitle(text: String): Boolean =
        text.any { it.isLetter() } && '%' !in text && !text.startsWith("Choose")

    private val TAG = Regex("<[^>]+>")

    private val GENERIC = setOf(
        "Close", "Ok", "OK", "Select", "Continue", "Cancel", "Examine",
        "Walk here", "Use", "Drop", "Take", "Wear", "Wield", "Empty",
    )
}
