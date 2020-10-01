package ru.skillbranch.skillarticles.ui.custom.markdown

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.repositories.Element
import ru.skillbranch.skillarticles.data.repositories.MarkdownElement
import ru.skillbranch.skillarticles.extensions.attrValue
import ru.skillbranch.skillarticles.extensions.dpToPx
import ru.skillbranch.skillarticles.ui.custom.spans.*

class MarkdownBuilder(context: Context) {

    private val colorPrimary: Int = context.attrValue(R.attr.colorPrimary)
    private val colorDivider: Int = context.getColor(R.color.color_divider)
    private val colorSecondary: Int = context.attrValue(R.attr.colorSecondary)
    private val colorOnSurface: Int = context.attrValue(R.attr.colorOnSurface)
    private val opacityColorSurface: Int = context.getColor(R.color.opacity_color_surface)

    private val gap: Float = context.dpToPx(8)
    private val ruleWidth: Float = context.dpToPx(2)
    private val strikeWidth: Float = context.dpToPx(4)
    private val bulletRadius: Float = context.dpToPx(4)
    private val cornerRadius: Float = context.dpToPx(8)

    private val linkIcon: Drawable = ContextCompat.getDrawable(
        context,
        R.drawable.ic_link_black_24dp
    )!!.apply { setTint(colorSecondary) }

    private val headerMarginTop: Float = context.dpToPx(12)
    private val headerMarginBottom: Float = context.dpToPx(8)

    fun markdownToSpan(textContent: MarkdownElement.Text): SpannedString {
        return buildSpannedString {
            textContent.elements.forEach { buildElement(it, this) }
        }
    }

    private fun buildElement(element: Element, builder: SpannableStringBuilder): CharSequence {
        return builder.apply {
            when (element) {
                is Element.Text -> {
                    append(element.text)
                }
                is Element.UnorderedListItem -> {
                    inSpans(UnorderedListSpan(gap, bulletRadius, colorSecondary)) {
                        for (child in element.elements) {
                            buildElement(child, builder)
                        }
                    }
                }
                is Element.Quote -> {
                    inSpans(
                        BlockquotesSpan(gap, strikeWidth, colorSecondary),
                        StyleSpan(Typeface.ITALIC)
                    ) {
                        for (child in element.elements) {
                            buildElement(child, builder)
                        }
                    }
                }
                is Element.Header -> {
                    inSpans(
                        HeaderSpan(
                            element.level,
                            colorPrimary,
                            colorDivider,
                            headerMarginTop,
                            headerMarginBottom
                        )
                    ) {
                        append(element.text)
                    }
                }
                is Element.Italic -> {
                    inSpans(StyleSpan(Typeface.ITALIC)) {
                        for (child in element.elements) {
                            buildElement(child, builder)
                        }
                    }
                }
                is Element.Bold -> {
                    inSpans(StyleSpan(Typeface.BOLD)) {
                        for (child in element.elements) {
                            buildElement(child, builder)
                        }
                    }
                }
                is Element.Strike -> {
                    inSpans(StrikethroughSpan()) {
                        for (child in element.elements) {
                            buildElement(child, builder)
                        }
                    }
                }
                is Element.Rule -> {
                    inSpans(HorizontalRuleSpan(ruleWidth, colorDivider)) {
                        append(element.text)
                    }
                }
                is Element.InlineCode -> {
                    inSpans(
                        InlineCodeSpan(
                            colorOnSurface,
                            opacityColorSurface,
                            cornerRadius,
                            gap
                        )
                    ) {
                        append(element.text)
                    }
                }
                is Element.Link -> {
                    inSpans(
                        IconLinkSpan(linkIcon, gap, colorPrimary, strikeWidth),
                        URLSpan(element.link)
                    ) {
                        append(element.text)
                    }
                }
                is Element.OrderedListItem -> {
                    inSpans(OrderedListSpan(gap, element.order, colorSecondary)) {
                        for (child in element.elements) {
                            buildElement(child, builder)
                        }
                    }
                }
                else -> {
                    append(element.text)
                }
            }
        }
    }
}