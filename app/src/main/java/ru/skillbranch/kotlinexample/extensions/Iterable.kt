package ru.skillbranch.kotlinexample.extensions

inline fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    if (isNotEmpty()) {
        with(toMutableList()) {
            while (!predicate(last())) remove(last())
            return this.also { remove(last()) }
        }
    }
    return emptyList()
}