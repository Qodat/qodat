class Node(val state: Int, var value: Double, val terminal: Boolean = false) {

    val links = mutableListOf<Link>()

    fun createLink(node: Node, p: Double, r: Int) {
        links += Link(node, p, r)
    }
}

class Link(val node: Node, val p: Double, val r: Int)

fun main() {

    val n1 = Node(1, 0.5)
    val n2 = Node(2, 0.5)
    val n3 = Node(3, 0.5)
    val n4 = Node(4, 0.5)
    val n5 = Node(5, 0.0, terminal = true)

    n1.createLink(n2, p = 0.8, r = 2)
    n1.createLink(n3, p = 0.2, r = 3)

    n2.createLink(n1, p = 0.5, r = -3)
    n2.createLink(n4, p = 0.5, r = 4)

    n3.createLink(n1, p = 0.2, r = -3)
    n3.createLink(n5, p = 0.8, r = 10)

    n4.createLink(n2, p = 0.4, r = -1)
    n4.createLink(n5, p = 0.6, r = 20)

    val states = arrayOf(n1, n2, n3, n4, n5)
    for (s in states) {
        if (!s.terminal) {
            println("V_{0}(${s.state}) = \\mathbf{${s.value.format(1)}} \\\\")
        }
    }
    repeat(200) {

        for (s in states) {
            if (!s.terminal) {
                s.value = s.links.sumOf { link -> link.p * (link.r + link.node.value) }
                print("V_{${it+1}}(${s.state}) &= ")
                for (link in s.links) {
                    print("${link.p} * (${link.r} +  ${link.node.value.format(1)})")
                    if (link != s.links.last())
                        print(" + ")
                }
//                println(" \\\\")
                println(" = \\mathbf{${s.value.format(1)}} \\\\")
            }
            else
                break
        }
        println("\\\\")
    }
}

fun Double.format(digits: Int) = "%.${digits}f".format(this)