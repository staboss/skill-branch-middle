@file:Suppress("DEPRECATION")

package ru.skillbranch.skillarticles.ui

import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.text.getSpans
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.layout_bottombar.*
import kotlinx.android.synthetic.main.layout_search_view.*
import kotlinx.android.synthetic.main.layout_submenu.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.setMarginOptionally
import ru.skillbranch.skillarticles.ui.custom.markdown.MarkdownBuilder
import ru.skillbranch.skillarticles.ui.base.BaseActivity
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.custom.spans.SearchFocusSpan
import ru.skillbranch.skillarticles.ui.custom.spans.SearchSpan
import ru.skillbranch.skillarticles.ui.delegates.AttrValue
import ru.skillbranch.skillarticles.ui.delegates.ObserveProp
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.ArticleState
import ru.skillbranch.skillarticles.viewmodels.ArticleViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Notify

class RootActivity : BaseActivity<ArticleViewModel>(), IArticleView {

    override val layout: Int = R.layout.activity_root

    override val viewModel: ArticleViewModel by provideViewModel("0")

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override val binding: ArticleBinding by lazy {
        ArticleBinding()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val bgColor by AttrValue(R.attr.colorSecondary)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val fgColor by AttrValue(R.attr.colorOnSecondary)

    override fun setupViews() {
        setupToolbar()
        setupBottombar()
        setupSubmenu()
    }

    override fun renderSearchResult(searchResult: List<Pair<Int, Int>>) {
        val content = tv_text_content.text as Spannable
        tv_text_content.isVisible

        clearSearchResult()

        searchResult.forEach { (start, end) ->
            content.setSpan(
                SearchSpan(bgColor, fgColor),
                start,
                end,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun renderSearchPosition(searchPosition: Int) {
        val content = tv_text_content.text as Spannable
        val spans = content.getSpans<SearchSpan>()

        content.getSpans<SearchFocusSpan>().forEach {
            content.removeSpan(it)
        }

        if (spans.isNotEmpty()) {
            val result = spans[searchPosition]
            Selection.setSelection(content, content.getSpanStart(result))
            content.setSpan(
                SearchFocusSpan(bgColor, fgColor),
                content.getSpanStart(result),
                content.getSpanEnd(result),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun clearSearchResult() {
        val content = tv_text_content.text as Spannable
        content.getSpans<SearchSpan>().forEach { content.removeSpan(it) }
    }

    override fun showSearchBar() {
        bottombar.setSearchState(true)
        scroll.setMarginOptionally(bottom = dpToIntPx(56))
    }

    override fun hideSearchBar() {
        bottombar.setSearchState(false)
        scroll.setMarginOptionally(bottom = dpToIntPx(0))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val menuItem = menu?.findItem(R.id.action_search)
        val searchView = (menuItem?.actionView as? SearchView)
        searchView?.queryHint = getString(R.string.search)

        if (binding.isSearch) {
            menuItem?.expandActionView()
            searchView?.setQuery(binding.searchQuery, false)

            if (binding.isFocusedSearch) searchView?.requestFocus()
            else searchView?.clearFocus()
        }

        menuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(true)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(false)
                return true
            }
        })

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.handleSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.handleSearch(newText)
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun renderNotification(notify: Notify) {
        val snackbar = Snackbar
            .make(coordinator_container, notify.message, Snackbar.LENGTH_SHORT)
            .setAnchorView(bottombar)

        when (notify) {
            is Notify.ActionMessage -> {
                val (_, label, handler) = notify

                with(snackbar) {
                    setActionTextColor(getColor(R.color.color_accent_dark))
                    setAction(label) { handler.invoke() }
                }
            }
            is Notify.ErrorMessage -> {
                val (_, label, handler) = notify

                with(snackbar) {
                    setBackgroundTint(getColor(R.color.design_default_color_error))
                    setTextColor(getColor(android.R.color.white))
                    setActionTextColor(getColor(android.R.color.white))
                    handler ?: return@with
                    setAction(label) { handler.invoke() }
                }
            }
        }

        snackbar.show()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val logo = if (toolbar.childCount > 2) toolbar.getChildAt(2) as ImageView else null
        logo?.scaleType = ImageView.ScaleType.CENTER_CROP

        val layoutParams = logo?.layoutParams as? Toolbar.LayoutParams
        layoutParams?.let {
            it.width = this.dpToIntPx(40)
            it.height = this.dpToIntPx(40)
            it.marginEnd = this.dpToIntPx(16)
            logo.layoutParams = it
        }
    }

    private fun setupSubmenu() {
        btn_text_up.setOnClickListener { viewModel.handleUpText() }
        btn_text_down.setOnClickListener { viewModel.handleDownText() }
        switch_mode.setOnClickListener { viewModel.handleNightMode() }
    }

    private fun setupBottombar() {
        btn_like.setOnClickListener { viewModel.handleLike() }
        btn_bookmark.setOnClickListener { viewModel.handleBookmark() }
        btn_share.setOnClickListener { viewModel.handleShare() }
        btn_settings.setOnClickListener { viewModel.handleToggleMenu() }

        btn_result_up.setOnClickListener {
            if (search_view.hasFocus()) search_view.clearFocus()
            if (!tv_text_content.hasFocus()) tv_text_content.requestFocus()
            viewModel.handleUpResult()
        }

        btn_result_down.setOnClickListener {
            if (search_view.hasFocus()) search_view.clearFocus()
            if (!tv_text_content.hasFocus()) tv_text_content.requestFocus()
            viewModel.handleDownResult()
        }

        btn_search_close.setOnClickListener {
            viewModel.handleSearchMode(false)
            invalidateOptionsMenu()
        }
    }

    inner class ArticleBinding : Binding() {

        var isFocusedSearch: Boolean = false
        var searchQuery: String? = null

        private var isLoadingContent: Boolean by ObserveProp(true)

        private var isLike: Boolean by RenderProp(false) {
            btn_like.isChecked = it
        }

        private var isBookmark: Boolean by RenderProp(false) {
            btn_bookmark.isChecked = it
        }

        private var isShowMenu: Boolean by RenderProp(false) {
            btn_settings.isChecked = it
            if (it) submenu.open() else submenu.close()
        }

        private var title: String by RenderProp("Loading") {
            toolbar.title = it
        }

        private var category: String by RenderProp("Loading") {
            toolbar.subtitle = it
        }

        private var categoryIcon: Int by RenderProp(R.drawable.logo_placeholder) {
            toolbar.logo = ContextCompat.getDrawable(applicationContext, it)
        }

        private var isBigText: Boolean by RenderProp(false) {
            tv_text_content.textSize = if (it) 18f else 14f
            btn_text_up.isChecked = it
            btn_text_down.isChecked = !it
        }

        private var isDarkMode: Boolean by RenderProp(false, needInit = false) {
            switch_mode.isChecked = it
            delegate.localNightMode = when {
                it -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_NO
            }
        }

        var isSearch: Boolean by ObserveProp(false) {
            if (it) showSearchBar() else hideSearchBar()
        }

        private var searchResults: List<Pair<Int, Int>> by ObserveProp(emptyList())

        private var searchPosition: Int by ObserveProp(0)

        private var content: String by ObserveProp("Loading") {
            MarkdownBuilder(this@RootActivity)
                .markdownToSpan(it)
                .run {
                    tv_text_content.setText(this, TextView.BufferType.SPANNABLE)
                }
            tv_text_content.movementMethod = LinkMovementMethod.getInstance()
        }

        override fun onFinishInflate() {
            dependsOn<Boolean, Boolean, List<Pair<Int, Int>>, Int>(
                ::isLoadingContent,
                ::isSearch,
                ::searchResults,
                ::searchPosition
            ) { _isLoadingContent, _isSearch, _searchResults, _searchPosition ->
                if (!_isLoadingContent && _isSearch) {
                    renderSearchResult(_searchResults)
                    renderSearchPosition(_searchPosition)
                }

                if (!_isLoadingContent && !_isSearch) {
                    clearSearchResult()
                }

                bottombar.bindSearchInfo(_searchResults.size, _searchPosition)
            }
        }

        override fun bind(data: IViewModelState) {
            data as ArticleState

            isLike = data.isLike
            isBookmark = data.isBookmark
            isShowMenu = data.isShowMenu

            if (data.title != null) title = data.title
            if (data.category != null) category = data.category
            if (data.categoryIcon != null) categoryIcon = data.categoryIcon as Int
            if (data.content != null) content = data.content

            isBigText = data.isBigText
            isDarkMode = data.isDarkMode

            isLoadingContent = data.isLoadingContent
            isSearch = data.isSearch

            searchQuery = data.searchQuery
            searchResults = data.searchResults
            searchPosition = data.searchPosition
        }

        override fun saveUi(outState: Bundle) {
            outState.putBoolean(::isFocusedSearch.name, search_view?.hasFocus() ?: false)
        }

        override fun restoreUi(savedState: Bundle) {
            isFocusedSearch = savedState.getBoolean(::isFocusedSearch.name)
        }
    }
}