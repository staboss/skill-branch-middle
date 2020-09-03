package ru.skillbranch.skillarticles.viewmodels

import androidx.lifecycle.LiveData
import ru.skillbranch.skillarticles.data.ArticleData
import ru.skillbranch.skillarticles.data.ArticlePersonalInfo

interface IArticleViewModel {

    fun getArticleContent(): LiveData<String?>

    fun getArticleData(): LiveData<ArticleData?>

    fun getArticlePersonalInfo(): LiveData<ArticlePersonalInfo?>

    fun handleNightMode()

    fun handleUpText()

    fun handleDownText()

    fun handleBookmark()

    fun handleLike()

    fun handleShare()

    fun handleToggleMenu()

    fun handleSearchMode(isSearch: Boolean)

    fun handleSearch(query: String?)
}