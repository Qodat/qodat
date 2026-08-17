package stan.qodat.scene.state

import stan.qodat.util.Searchable

interface ViewStateRestorable<S> {
    fun snapshotViewState(): S
    fun restoreViewState(state: S)
}

data class NamedIdentity(
    val id: String? = null,
    val name: String? = null
) {
    fun isEmpty(): Boolean = id.isNullOrBlank() && name.isNullOrBlank()
}

data class CameraViewState(
    val translateX: Double,
    val translateY: Double,
    val positionZ: Double,
    val xRotate: Double,
    val yRotate: Double,
    val pivotX: Double,
    val pivotY: Double,
    val pivotZ: Double
)

data class EntityViewState(
    val selectedTab: String? = null,
    val searches: Map<String, String> = emptyMap(),
    val selections: Map<String, NamedIdentity?> = emptyMap(),
    val animationSearch: String = "",
    val selectedAnimation: NamedIdentity? = null,
    val modelSearch: String = "",
    val selectedModelName: String? = null,
    val materialSearch: String = "",
    val animationPlaying: Boolean = false,
    val animationFrameIndex: Int = 0
)

data class AppViewState(
    val camera: CameraViewState,
    val viewer: EntityViewState,
    val editor: EntityViewState,
    val selectedScene: String? = null,
    val selectedRightTab: String? = null,
    val selectedLeftTab: String? = null,
    val selectedBottomTab: String? = null,
    val playButtonSelected: Boolean = false
)

fun <T> Iterable<T>.findByIdentity(
    identity: NamedIdentity?,
    idOf: (T) -> String?,
    nameOf: (T) -> String?
): T? {
    if (identity == null || identity.isEmpty())
        return null
    identity.id?.takeIf { it.isNotBlank() }?.let { id ->
        firstOrNull { idOf(it) == id }?.let { return it }
    }
    identity.name?.takeIf { it.isNotBlank() }?.let { name ->
        firstOrNull { nameOf(it) == name }?.let { return it }
    }
    return null
}

fun <T : Searchable> Iterable<T>.findByIdentity(
    identity: NamedIdentity?,
    idOf: (T) -> String?
): T? = findByIdentity(identity, idOf) { it.getName() }
