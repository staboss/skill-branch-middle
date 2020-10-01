package ru.skillbranch.skillarticles.ui.custom.markdown

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Selection
import android.text.Spannable
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.core.view.setPadding
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.attrValue
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.dpToPx
import ru.skillbranch.skillarticles.extensions.setPaddingOptionally

@SuppressLint("ViewConstructor")
class MarkdownCodeView private constructor(
    context: Context,
    fontSize: Float
) : ViewGroup(context, null, 0), IMarkdownView {

    var copyListener: ((String) -> Unit)? = null

    private lateinit var codeString: CharSequence

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val iv_copy: ImageView

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val iv_switch: ImageView

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val sv_scroll: HorizontalScrollView

    private val tv_codeView: MarkdownTextView

    @ColorInt
    private val darkSurface: Int = context.attrValue(R.attr.darkSurfaceColor)

    @ColorInt
    private val darkOnSurface: Int = context.attrValue(R.attr.darkOnSurfaceColor)

    @ColorInt
    private val lightSurface: Int = context.attrValue(R.attr.lightSurfaceColor)

    @ColorInt
    private val lightOnSurface: Int = context.attrValue(R.attr.lightOnSurfaceColor)

    private val radius: Float = context.dpToPx(8)
    private val padding: Int = context.dpToIntPx(8)
    private val iconSize: Int = context.dpToIntPx(12)
    private val fadingOffset: Int = context.dpToIntPx(144)
    private val scrollBarHeight: Int = context.dpToIntPx(2)
    private val textExtraPadding: Int = context.dpToIntPx(80)

    private var isSingleLine: Boolean = false
    private var isManual: Boolean = false
    private var isDark: Boolean = false

    private val bgColor: Int
        get() = when {
            !isManual -> context.attrValue(R.attr.colorSurface)
            isDark -> darkSurface
            else -> lightSurface
        }

    private val textColor: Int
        get() = when {
            !isManual -> context.attrValue(R.attr.colorOnSurface)
            isDark -> darkOnSurface
            else -> lightOnSurface
        }

    override var fontSize: Float = fontSize
        set(value) {
            tv_codeView.textSize = value * 0.85f
            field = value
        }

    override val spannableContent: Spannable
        get() = tv_codeView.text as Spannable

    init {
        tv_codeView = MarkdownTextView(context, fontSize * 0.85f).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            setTextColor(textColor)
            setPaddingOptionally(right = textExtraPadding)
            isFocusable = true
            isFocusableInTouchMode = true
        }

        sv_scroll = object : HorizontalScrollView(context) {
            override fun getLeftFadingEdgeStrength(): Float {
                return 0f
            }
        }.apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isHorizontalFadingEdgeEnabled = true
            scrollBarSize = scrollBarHeight
            setFadingEdgeLength(fadingOffset)
            addView(tv_codeView) // add code text to scroll
        }
        addView(sv_scroll)

        iv_copy = ImageView(context).apply {
            setImageResource(R.drawable.ic_content_copy_black_24dp)
            imageTintList = ColorStateList.valueOf(textColor)
            setOnClickListener {
                copyListener?.invoke(codeString.toString())
            }
        }
        addView(iv_copy)

        iv_switch = ImageView(context).apply {
            setImageResource(R.drawable.ic_brightness_medium_black_24dp)
            imageTintList = ColorStateList.valueOf(textColor)
            setOnClickListener { toggleColors() }
        }
        addView(iv_switch)
    }

    constructor(
        context: Context,
        fontSize: Float,
        code: CharSequence
    ) : this(context, fontSize) {
        codeString = code
        isSingleLine = code.lines().size == 1

        tv_codeView.setText(codeString, TextView.BufferType.SPANNABLE)
        setPadding(padding)

        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = FloatArray(8).apply { fill(radius, 0, size) }
            color = ColorStateList.valueOf(bgColor)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var usedHeight = 0
        val width = View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        measureChild(sv_scroll, widthMeasureSpec, heightMeasureSpec)

        usedHeight += sv_scroll.measuredHeight + paddingTop + paddingBottom
        setMeasuredDimension(width, usedHeight)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val usedHeight = paddingTop
        val bodyWidth = r - l - paddingLeft - paddingRight
        val left = paddingLeft
        val right = paddingLeft + bodyWidth

        if (isSingleLine) {
            val iconHeight = (b - t - iconSize) / 2
            iv_copy.layout(
                right - iconSize,
                iconHeight,
                right,
                iconHeight + iconSize
            )
            iv_switch.layout(
                iv_copy.right - (2.5f * iconSize).toInt(),
                iconHeight,
                iv_copy.right - (1.5f * iconSize).toInt(),
                iconHeight + iconSize
            )
        } else {
            iv_copy.layout(
                right - iconSize,
                usedHeight,
                right,
                usedHeight + iconSize
            )
            iv_switch.layout(
                iv_copy.right - (2.5f * iconSize).toInt(),
                usedHeight,
                iv_copy.right - (1.5f * iconSize).toInt(),
                usedHeight + iconSize
            )
        }
        sv_scroll.layout(
            left,
            usedHeight,
            right,
            usedHeight + sv_scroll.measuredHeight
        )
    }

    override fun renderSearchPosition(searchPosition: Pair<Int, Int>, offset: Int) {
        super.renderSearchPosition(searchPosition, offset)
        if ((parent as ViewGroup).hasFocus() && !tv_codeView.hasFocus()) {
            tv_codeView.requestFocus()
        }
        Selection.setSelection(spannableContent, searchPosition.first.minus(offset))
    }

    private fun toggleColors() {
        isManual = true
        isDark = !isDark
        applyColors()
    }

    private fun applyColors() {
        iv_switch.imageTintList = ColorStateList.valueOf(textColor)
        iv_copy.imageTintList = ColorStateList.valueOf(textColor)
        (background as GradientDrawable).color = ColorStateList.valueOf(bgColor)
        tv_codeView.setTextColor(textColor)
    }
}