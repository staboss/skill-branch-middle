package ru.skillbranch.skillarticles.markdown.spans

import android.graphics.*
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.VisibleForTesting
import ru.skillbranch.skillarticles.markdown.Element

class BlockCodeSpan(
    @ColorInt
    private val textColor: Int,
    @ColorInt
    private val bgColor: Int,
    @Px
    private val cornerRadius: Float,
    @Px
    private val padding: Float,
    private val type: Element.BlockCode.Type
) : ReplacementSpan() {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var rect = RectF()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var path = Path()

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fontMetrics: Paint.FontMetricsInt?
    ): Int {
        if (fontMetrics != null) {
            when (type) {
                Element.BlockCode.Type.SINGLE -> {
                    fontMetrics.ascent = (paint.ascent() - 2 * padding).toInt()
                    fontMetrics.descent = (paint.descent() + 2 * padding).toInt()
                }

                Element.BlockCode.Type.START -> {
                    fontMetrics.ascent = (paint.ascent() - 2 * padding).toInt()
                    fontMetrics.descent = paint.descent().toInt()
                }

                Element.BlockCode.Type.MIDDLE -> {
                    fontMetrics.ascent = paint.ascent().toInt()
                    fontMetrics.descent = paint.descent().toInt()
                }

                Element.BlockCode.Type.END -> {
                    fontMetrics.ascent = paint.ascent().toInt()
                    fontMetrics.descent = (paint.descent() + 2 * padding).toInt()
                }
            }
            fontMetrics.top = fontMetrics.ascent
            fontMetrics.bottom = fontMetrics.descent
        }
        return 0
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        when (type) {
            Element.BlockCode.Type.SINGLE -> paint.forBackground {
                rect.set(0f, top + padding, canvas.width.toFloat(), bottom - padding)
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            }

            Element.BlockCode.Type.START -> paint.forBackground {
                rect.set(0f, top + padding, canvas.width.toFloat(), bottom.toFloat())
                path.reset()
                path.addRoundRect(
                    rect,
                    floatArrayOf(
                        cornerRadius,
                        cornerRadius,
                        cornerRadius,
                        cornerRadius,
                        0f,
                        0f,
                        0f,
                        0f
                    ),
                    Path.Direction.CW
                )
                canvas.drawPath(path, paint)
            }

            Element.BlockCode.Type.MIDDLE -> paint.forBackground {
                rect.set(0f, top.toFloat(), canvas.width.toFloat(), bottom.toFloat())
                canvas.drawRect(rect, paint)
            }

            Element.BlockCode.Type.END -> paint.forBackground {
                rect.set(0f, top.toFloat(), canvas.width.toFloat(), bottom - padding)
                path.reset()
                path.addRoundRect(
                    rect,
                    floatArrayOf(
                        0f,
                        0f,
                        0f,
                        0f,
                        cornerRadius,
                        cornerRadius,
                        cornerRadius,
                        cornerRadius
                    ),
                    Path.Direction.CW
                )
                canvas.drawPath(path, paint)
            }
        }
        paint.forText {
            canvas.drawText(text, start, end, x + padding, y.toFloat(), paint)
        }
    }

    private inline fun Paint.forText(block: () -> Unit) {
        val oldSize = textSize
        val oldStyle = typeface?.style ?: 0
        val oldFont = typeface
        val oldColor = color

        color = textColor
        typeface = Typeface.create(Typeface.MONOSPACE, oldStyle)
        textSize *= 0.85f

        block()

        color = oldColor
        typeface = oldFont
        textSize = oldSize
    }

    private inline fun Paint.forBackground(block: () -> Unit) {
        val oldStyle = style
        val oldColor = color

        color = bgColor
        style = Paint.Style.FILL

        block()

        color = oldColor
        style = oldStyle
    }
}
