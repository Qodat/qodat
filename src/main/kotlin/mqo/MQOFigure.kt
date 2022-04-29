package mqo

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   2019-07-18
 * @version 1.0
 */
class MQOFigure(val root: MQOGroup) {

    private val materials = ArrayList<MQOMaterial>()
    private val meshes = ArrayList<MQOMesh>()

    init {

        val material = root.getChild("Material")!!

        for(i in 0 until material.getEntries())
            materials.add(MQOMaterial(material[i]))

        meshes.addAll(root.getChildren("Object").map { MQOMesh(it) })
    }

    operator fun get(index: Int) = meshes[index]

    fun getMaterial(index: Int) = materials[index]

    fun getMeshCount() = meshes.size

    fun getMaterialCount() = materials.size

}