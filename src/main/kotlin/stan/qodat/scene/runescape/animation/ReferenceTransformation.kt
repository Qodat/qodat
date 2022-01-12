package stan.qodat.scene.runescape.animation

import org.joml.Vector3f

class ReferenceTransformation(transformation: Transformation) : Transformation(transformation) {

    var parent : ReferenceTransformation? = null
    var children = LinkedHashMap<TransformationType, Transformation>()
    var position = Vector3f(0f, 0f, 0f)

    init {
        if (transformation is ReferenceTransformation) {
            parent = transformation.parent
            position = transformation.position
            for (child in transformation.children.values)
                children[child.getType()] = Transformation(child)
        }
    }

    fun trySetParent(node: ReferenceTransformation) {

        if (getName() == node.getName() || getName() == node.parent?.getName() || getRotation() == null)
            return

        // Parent only if its rotation frame map is a superset
        val rotation = node.getRotation() ?: return

        val nodeIndices = rotation.groupIndices.toArray(null).toSet()
        if (!nodeIndices.containsAll(groupIndices.toArray(null).toSet())) {
            return
        }

        // Set if does not exist or closer relation (indicated by a smaller superset)
        val parentRotation = parent?.getRotation()
        if (parentRotation == null || rotation.groupIndices.size() < parentRotation.groupIndices.size()) {
            parent = node
        }
    }

    fun getRotation(): Transformation? {
        return children[TransformationType.ROTATE]
    }
}
