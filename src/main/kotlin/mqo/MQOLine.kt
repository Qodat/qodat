package mqo

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   2019-07-18
 * @version 1.0
 */
class MQOLine {

    private val words = ArrayList<String>()

    fun isChunkHead() = words.last() == "{"

    fun isChunkFooter() = words.first() == "}"

    fun countStrings() = when {
        isChunkHead() -> words.lastIndex
        isChunkFooter() -> 0
        else -> words.size
    }

    operator fun get(index: Int) = words[index]


    companion object {

        private val parser = MQOLineParser()

        fun create(line: String) : MQOLine? {
            return parser.parse(line)
        }

        private class Separator(private val cutoffChar : Char) {

            fun isSeparationChar(c : Char) = cutoffChar == c
        }

        private class MQOLineParser {

            private lateinit var line : String

            private val separators = listOf(
                Separator('\n'),
                Separator('\r'),
                Separator('\u0000'),
                Separator('\t'),
                Separator(' ')
            )

            private fun isAnySeparationChar(c : Char) = separators.any { it.isSeparationChar(c) }

            fun parse(line: String) : MQOLine? {

                this.line = line

                val result = MQOLine()

                while (true) {
                    result.words.add(nextWord()
                        ?: return if (result.words.isEmpty()) null else result)
                }
            }

            private fun findHead() : Char {
                val chars = line.toCharArray()
                var head = 0

                while (head < chars.size) {

                    val c = chars[head]

                    if (!isAnySeparationChar(c)) {
                        line = line.substring(head)
                        return c
                    }

                    ++head
                }
                return '\u0000'
            }

            fun nextWord(): String? {

                if (line.isEmpty())
                    return null

                val word: String
                var length = 0

                val head = findHead()
                val buffer = line.toCharArray()

                if (head == '\"') {
                    length = 1
                    while (length < buffer.size && buffer[length] != '\"')
                        ++length
                    word = line.substring(1, length)
                } else {
                    while (length < buffer.size && !isAnySeparationChar(buffer[length]))
                        ++length
                    word = line.substring(0, length)
                }

                line = (if (length < buffer.size) line.substring(length + 1) else "")

                return if (word.isEmpty()) null else word
            }
        }
    }

}