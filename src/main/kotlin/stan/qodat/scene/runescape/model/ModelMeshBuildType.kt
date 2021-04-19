package stan.qodat.scene.runescape.model

/**
 * Represents how the [ModelMesh] should be constructed.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   01/02/2021
 */
enum class ModelMeshBuildType {

    /**
     * Creates a texture material that contains all colors of the model.
     *
     * This way a single mesh can be used to construct the model.
     */
    ATLAS,

    SKELETON_ATLAS,

    /**
     * Creates a mesh for each triangle in the model.
     */
    MESH_PER_FACE,
}