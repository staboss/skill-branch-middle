package ru.skillbranch.skillarticles.extensions

import androidx.appcompat.widget.SearchView

inline fun SearchView.setOnQueryListener(crossinline action: (String?) -> Unit) {
    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            action.invoke(query)
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            action.invoke(newText)
            return true
        }
    })
}