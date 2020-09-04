package ru.skillbranch.skillarticles.markdown

import java.util.regex.Pattern

object MarkdownParser {

    private val LINE_SEPARATOR = System.getProperty("line.separator") ?: "\n"

    // Group regex
    private const val UNORDERED_LIST_ITEM_GROUP = "(^[*+-] .+)"
    private const val HEADER_GROUP = "(^#{1,6} .+?$)"
    private const val QUOTE_GROUP = "(^> .+?$)"
    private const val ITALIC_GROUP = "((?<!\\*)\\*[^*].*?[^*]?\\*(?!\\*)|(?<!_)_[^_].*?[^_]?_(?!_))"
    private const val BOLD_GROUP = "((?<!\\*)\\*{2}[^*].*?[^*]?\\*{2}(?!\\*)|(?<!_)_{2}[^_].*?[^_]?_{2}(?!_))"
    private const val STRIKE_GROUP = "((?<!~)~{2}[^~].*?[^~]?~{2}(?!~))"
    private const val RULE_GROUP = "(^[-_*]{3}$)"
    private const val INLINE_GROUP = "((?<!`)`[^`\\s].*?[^`\\s]?`(?!`))"
    private const val LINK_GROUP = "(\\[[^\\[\\]]*?]\\(.+?\\)|^[*?]\\(.*?\\))"
    private const val ORDERED_LIST_ITEM_GROUP = "(^\\d+\\. .+\$)"
    private const val BLOCK_CODE_GROUP = "(^```[\\s\\S]+?```$)"

    // Result regex
    private const val MARKDOWN_GROUPS = "$UNORDERED_LIST_ITEM_GROUP|$HEADER_GROUP|$QUOTE_GROUP" +
            "|$ITALIC_GROUP|$BOLD_GROUP|$STRIKE_GROUP|$RULE_GROUP" +
            "|$INLINE_GROUP|$LINK_GROUP|$ORDERED_LIST_ITEM_GROUP" +
            "|$BLOCK_CODE_GROUP"

    private val elementsPattern by lazy {
        Pattern.compile(MARKDOWN_GROUPS, Pattern.MULTILINE)
    }

    /**
     * Parse text with markdown characters to text elements
     *
     * @param string text with markdown characters
     * @return text elements
     */
    fun parse(string: String): MarkDownText {
        val elements = mutableListOf<Element>()
        elements.addAll(findElements(string))
        return MarkDownText(elements)
    }

    /**
     * Clear markdown text to string without markdown characters
     *
     * @param string markdown text
     * @return text elements
     */
    fun clear(string: String?): String? {
        string ?: return null
        var clearedString = ""
        findElements(string).forEach { clearedString += getSimple(it) }
        return clearedString
    }

    private fun getSimple(element: Element): String {
        var bufferString = ""
        element.elements.forEach { bufferString += getSimple(it) }
        return if (element.elements.isEmpty()) element.text.toString() else bufferString
    }

    /**
     * Find markdown elements in markdown text
     *
     * @param string markdown text
     * @return markdown elements
     */
    private fun findElements(string: CharSequence): List<Element> {
        val parents = mutableListOf<Element>()
        val matcher = elementsPattern.matcher(string)
        var lastStartIndex = 0

        loop@
        while (matcher.find(lastStartIndex)) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            if (lastStartIndex < startIndex) {
                parents.add(Element.Text(string.subSequence(lastStartIndex, startIndex)))
            }

            var text: CharSequence
            val groups = 1..11

            when (groups.firstOrNull { matcher.group(it) != null } ?: -1) {
                // NOT FOUND -> break
                -1 -> break@loop

                // UNORDERED LIST -> text without "* " or ". " or "  "
                1 -> {
                    text = string.subSequence(startIndex.plus(2), endIndex)

                    val subElements = findElements(text)
                    val element = Element.UnorderedListItem(text, subElements)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // HEADER -> text without "#{1,6} "
                2 -> {
                    val reg = "^#{1,6}".toRegex().find(string.subSequence(startIndex, endIndex))
                    val level = reg!!.value.length

                    text = string.subSequence(startIndex.plus(level.inc()), endIndex)
                    val element = Element.Header(level, text)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // QUOTE -> text without "> "
                3 -> {
                    text = string.subSequence(startIndex.plus(2), endIndex)

                    val subElements = findElements(text)
                    val element = Element.Quote(text, subElements)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // ITALIC -> text without "*{}*" or "_{}_"
                4 -> {
                    text = string.subSequence(startIndex.inc(), endIndex.dec())

                    val subElements = findElements(text)
                    val element = Element.Italic(text, subElements)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // BOLD -> text without "**{}**" or "__{}__"
                5 -> {
                    text = string.subSequence(startIndex.plus(2), endIndex.minus(2))

                    val subElements = findElements(text)
                    val element = Element.Bold(text, subElements)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // STRIKE -> text without "~~{}~~"
                6 -> {
                    text = string.subSequence(startIndex.plus(2), endIndex.minus(2))

                    val subElements = findElements(text)
                    val element = Element.Strike(text, subElements)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // RULE -> text without "---" or "***" or "___"
                7 -> {
                    val element = Element.Rule()
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // INLINE -> text without "`{}`"
                8 -> {
                    text = string.subSequence(startIndex.inc(), endIndex.dec())

                    val element = Element.InlineCode(text)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // LINK -> full text for regex
                9 -> {
                    text = string.subSequence(startIndex, endIndex)

                    val (title, link) = "\\[(.*)]\\((.*)\\)".toRegex().find(text)!!.destructured
                    val element = Element.Link(link, title)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // ORDERED LIST -> text without "N. "
                10 -> {
                    val reg = "^(^\\d+\\.)".toRegex().find(string.subSequence(startIndex, endIndex))
                    val order = reg!!.value
                    text = string.subSequence(startIndex.plus(order.length.inc()), endIndex)

                    val subs = findElements(text)
                    val element = Element.OrderedListItem(order, text.toString(), subs)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                // BLOCK CODE
                11 -> {
                    text = string.subSequence(startIndex.plus(3), endIndex.plus(-3))

                    if (text.contains(LINE_SEPARATOR)) {
                        for ((index, line) in text.lines().withIndex()) {
                            when (index) {
                                text.lines().lastIndex ->
                                    parents.add(
                                        Element.BlockCode(
                                            Element.BlockCode.Type.END,
                                            line
                                        )
                                    )
                                0 ->
                                    parents.add(
                                        Element.BlockCode(
                                            Element.BlockCode.Type.START,
                                            line + LINE_SEPARATOR
                                        )
                                    )
                                else ->
                                    parents.add(
                                        Element.BlockCode(
                                            Element.BlockCode.Type.MIDDLE,
                                            line + LINE_SEPARATOR
                                        )
                                    )
                            }
                        }
                    } else {
                        parents.add(Element.BlockCode(Element.BlockCode.Type.SINGLE, text))
                    }

                    lastStartIndex = endIndex
                }
            }
        }

        if (lastStartIndex < string.length) {
            val text = string.subSequence(lastStartIndex, string.length)
            parents.add(Element.Text(text))
        }

        return parents
    }
}

data class MarkDownText(
    val elements: List<Element>
)

sealed class Element {

    abstract val text: CharSequence

    abstract val elements: List<Element>

    data class Text(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class UnorderedListItem(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Header(
        val level: Int = 1,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Quote(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Italic(
        override val text: CharSequence,
        override val elements: List<Element>
    ) : Element()

    data class Bold(
        override val text: CharSequence,
        override val elements: List<Element>
    ) : Element()

    data class Strike(
        override val text: CharSequence,
        override val elements: List<Element>
    ) : Element()

    data class Rule(
        override val text: CharSequence = " ",
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class InlineCode(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Link(
        val link: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class OrderedListItem(
        val order: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class BlockCode(
        val type: Type = Type.MIDDLE,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element() {
        enum class Type { START, END, MIDDLE, SINGLE }
    }
}