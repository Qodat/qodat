package mqo

/**
 * TODO: add documentation
 *
 * @author Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @version 1.0
 * @since 2019-07-18
 */
class MQOGroup{

    lateinit var header : MQOLine

    private val children = ArrayList<MQOGroup>()
    private val lines = ArrayList<MQOLine>()

    fun getChild(name: String) = children.find { it.header[0] == name }

    fun getChildren(name: String) = children.filter { it.header[0] == name }.toTypedArray()

    fun getEntries() = lines.size

    operator fun get(index: Int) = lines[index]

    fun addLine(line: MQOLine) {
        lines.add(line)
    }

    fun addChild(child: MQOGroup) {
        children.add(child)
    }

}
