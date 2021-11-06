package stan.qodat.scene.control.tree

import javafx.scene.control.TreeItem
import javafx.scene.text.TextFlow
import stan.qodat.javafx.menloText
import stan.qodat.javafx.text
import stan.qodat.javafx.treeItem
import stan.qodat.util.BABY_BLUE
import stan.qodat.util.LOG_ERROR
import stan.qodat.util.LOG_VERBOSE

/**
 * Represents a [TreeItem] for the provided [throwable].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class ExceptionTreeItem(
    private val throwable: Throwable
) : TreeItem<TextFlow>() {

    init {
        value = TextFlow().apply {
            menloText("${throwable::class.simpleName}: ${throwable.localizedMessage}" to LOG_ERROR)
        }
        addToTree(throwable.stackTrace.iterator())
    }

    private fun TreeItem<TextFlow>.addToTree(stackTrace: Iterator<StackTraceElement>, lastClassName: String? = null) {
        if (stackTrace.hasNext()) {
            var next : StackTraceElement
            do {
                next = stackTrace.next()
                treeItem {
                    isExpanded = false
                    value = TextFlow().apply {
                        menloText(
                            "${next.className}.${next.methodName}(" to LOG_ERROR,
                            fileName(next) to LOG_VERBOSE,
                            ":${next.lineNumber}" to LOG_VERBOSE,
                            ")" to LOG_ERROR,
                        )
                    }
                    if (stackTrace.hasNext()) {
                        if (next.className != lastClassName) {
                            addToTree(stackTrace, next.className)
                            return@treeItem
                        }
                    }
                }
            } while (stackTrace.hasNext())
        }
    }

    private fun fileName(next: StackTraceElement) = next.fileName?:"???"
}