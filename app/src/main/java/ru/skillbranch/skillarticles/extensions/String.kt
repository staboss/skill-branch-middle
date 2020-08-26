package ru.skillbranch.skillarticles.extensions

import java.util.Locale

fun String?.indexesOf(substring: String, ignoreCase: Boolean = true): List<Int> {
    if (this == null || substring.isEmpty()) return emptyList()

    val regexString = if (ignoreCase) substring.toLowerCase(Locale.getDefault()) else substring
    val sourceString = if (ignoreCase) this.toLowerCase(Locale.getDefault()) else this

    return Regex(regexString).findAll(sourceString).map { it.range.first }.toList()
}