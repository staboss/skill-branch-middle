package ru.skillbranch.skillarticles.viewmodels

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import ru.skillbranch.skillarticles.data.ArticleData
import ru.skillbranch.skillarticles.data.ArticlePersonalInfo
import ru.skillbranch.skillarticles.data.repositories.ArticleRepository
import ru.skillbranch.skillarticles.extensions.data.toAppSettings
import ru.skillbranch.skillarticles.extensions.data.toArticlePersonalInfo
import ru.skillbranch.skillarticles.extensions.format
import ru.skillbranch.skillarticles.extensions.indexesOf
import ru.skillbranch.skillarticles.markdown.MarkdownParser
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Notify

class ArticleViewModel(
    private val articleId: String
) : BaseViewModel<ArticleState>(ArticleState()), IArticleViewModel {

    private val repository: ArticleRepository = ArticleRepository
    private var clearContent: String? = null

    init {
        subscribeOnDataSource(getArticleData()) { article, state ->
            article ?: return@subscribeOnDataSource null
            state.copy(
                shareLink = article.shareLink,
                title = article.title,
                author = article.author,
                category = article.category,
                categoryIcon = article.categoryIcon,
                date = article.date.format()
            )
        }

        subscribeOnDataSource(getArticleContent()) { content, state ->
            content ?: return@subscribeOnDataSource null
            state.copy(
                isLoadingContent = false,
                content = content
            )
        }

        subscribeOnDataSource(getArticlePersonalInfo()) { info, state ->
            info ?: return@subscribeOnDataSource null
            state.copy(
                isBookmark = info.isBookmark,
                isLike = info.isLike
            )
        }

        subscribeOnDataSource(repository.getAppSettings()) { settings, state ->
            state.copy(
                isDarkMode = settings.isDarkMode,
                isBigText = settings.isBigText
            )
        }
    }

    override fun getArticleContent(): LiveData<String?> {
        return repository.loadArticleContent(articleId)
    }

    override fun getArticleData(): LiveData<ArticleData?> {
        return repository.getArticle(articleId)
    }

    override fun getArticlePersonalInfo(): LiveData<ArticlePersonalInfo?> {
        return repository.loadArticlePersonalInfo(articleId)
    }

    override fun handleNightMode() {
        val settings = currentState.toAppSettings()
        repository.updateSettings(settings.copy(isDarkMode = !settings.isDarkMode))
    }

    override fun handleUpText() {
        repository.updateSettings(currentState.toAppSettings().copy(isBigText = true))
    }

    override fun handleDownText() {
        repository.updateSettings(currentState.toAppSettings().copy(isBigText = false))
    }

    override fun handleBookmark() {
        val info = currentState.toArticlePersonalInfo()
        repository.updateArticlePersonalInfo(info.copy(isBookmark = !info.isBookmark))

        val msg = if (currentState.isBookmark) "Add to bookmarks" else "Remove from bookmarks"
        notify(Notify.TextMessage(msg))
    }

    override fun handleLike() {
        val isLiked = currentState.isLike
        val toggleLike = {
            val info = currentState.toArticlePersonalInfo()
            repository.updateArticlePersonalInfo(info.copy(isLike = !info.isLike))
        }

        toggleLike()

        val msg = when {
            !isLiked -> Notify.TextMessage("Mark is liked")
            else -> Notify.ActionMessage("Don`t like it anymore", "No, still like it", toggleLike)
        }

        notify(msg)
    }

    override fun handleShare() {
        val msg = "Share is not implemented"
        notify(Notify.ErrorMessage(msg, "OK", null))
    }

    override fun handleToggleMenu() {
        updateState { state ->
            state.copy(isShowMenu = !state.isShowMenu)
        }
    }

    override fun handleSearchMode(isSearch: Boolean) {
        updateState { state ->
            state.copy(isSearch = isSearch, isShowMenu = false, searchPosition = 0)
        }
    }

    override fun handleSearch(query: String?) {
        query ?: return

        if (clearContent == null) {
            clearContent = MarkdownParser.clear(currentState.content)
        }

        val result = clearContent
            .indexesOf(query)
            .map { it to it + query.length }

        updateState { state ->
            state.copy(searchQuery = query, searchResults = result, searchPosition = 0)
        }
    }

    fun handleUpResult() {
        updateState { state ->
            state.copy(searchPosition = state.searchPosition.dec())
        }
    }

    fun handleDownResult() {
        updateState { state ->
            state.copy(searchPosition = state.searchPosition.inc())
        }
    }
}

data class ArticleState(
    val isAuth: Boolean = false,
    val isLoadingContent: Boolean = true,
    val isLoadingReviews: Boolean = true,
    val isLike: Boolean = false,
    val isBookmark: Boolean = false,
    val isShowMenu: Boolean = false,
    val isBigText: Boolean = false,
    val isDarkMode: Boolean = false,
    val isSearch: Boolean = false,
    val searchQuery: String? = null,
    val searchResults: List<Pair<Int, Int>> = emptyList(),
    val searchPosition: Int = 0,
    val shareLink: String? = null,
    val title: String? = null,
    val category: String? = null,
    val categoryIcon: Any? = null,
    val date: String? = null,
    val author: Any? = null,
    val poster: String? = null,
    val content: String? = null,
    val reviews: List<Any> = emptyList()
) : IViewModelState {

    override fun save(outState: Bundle) {
        outState.putAll(
            bundleOf(
                "isSearch" to isSearch,
                "searchQuery" to searchQuery,
                "searchResults" to searchResults,
                "searchPosition" to searchPosition
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun restore(savedState: Bundle): ArticleState {
        return copy(
            isSearch = savedState["isSearch"] as Boolean,
            searchQuery = savedState["searchQuery"] as? String,
            searchResults = savedState["searchResults"] as List<Pair<Int, Int>>,
            searchPosition = savedState["searchPosition"] as Int
        )
    }
}