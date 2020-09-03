package ru.skillbranch.skillarticles.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import ru.skillbranch.skillarticles.data.delegates.PrefDelegate

@SuppressLint("RestrictedApi")
class PrefManager(context: Context) {

    internal val preferences: SharedPreferences by lazy {
        PreferenceManager(context).sharedPreferences
    }

    var storedInt by PrefDelegate(0)
    var storedLong by PrefDelegate(0L)
    var storedFloat by PrefDelegate(0F)
    var storedString by PrefDelegate("string")
    var storedBoolean by PrefDelegate(false)

    fun clearAll() {
        preferences.edit().clear().apply()
    }
}