package stan.qodat.scene.control.export.blender

import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.util.Searchable

/**
 * Idle / walk clips to bake into a Blender glTF.
 *
 * Role names come from [qodat.cache.definition.AnimatedEntityDefinition.animationRoleLabels]
 * (`Idle`, `Walk`, or a compact `Idle · Walk` when one sequence fills both).
 */
data class GltfAnimationClip(
    val name: String,
    val frames: List<AnimationFrame>,
)

object GltfExportClips {

    const val MAX_FRAMES_PER_CLIP = 180

    private val exportRoles = listOf("Idle", "Walk")

    fun resolve(exportable: Searchable, selected: Animation?): List<GltfAnimationClip> {
        val chosen = LinkedHashMap<Animation, String>()

        if (exportable is AnimatedEntity<*>) {
            val labels = exportable.definition.animationRoleLabels
            try {
                for (animation in exportable.getPrimaryAnimations()) {
                    val id = animation.definition?.id ?: animation.getName()
                    val label = labels[id]
                    when {
                        label != null && isExportRole(label) ->
                            chosen.putIfAbsent(animation, label)
                        labels.isEmpty() && chosen.size < exportRoles.size ->
                            chosen.putIfAbsent(animation, exportRoles[chosen.size])
                    }
                }
            } catch (_: Exception) {
                // Cache miss / Maya load — still try the selected clip below.
            }
        }

        if (selected != null) {
            val label = when (exportable) {
                is AnimatedEntity<*> ->
                    exportable.definition.animationRoleLabels[selected.definition?.id]
                        ?: selected.getName()
                else -> selected.getName()
            }
            chosen.putIfAbsent(selected, label)
        }

        return chosen.mapNotNull { (animation, label) ->
            val frames = try {
                animation.getFrameList().take(MAX_FRAMES_PER_CLIP).toList()
            } catch (_: Exception) {
                emptyList()
            }
            if (frames.isEmpty()) null else GltfAnimationClip(label, frames)
        }
    }

    internal fun isExportRole(label: String): Boolean =
        exportRoles.any { role ->
            label == role || label.startsWith("$role ·") || label.contains(" · $role")
        }
}
