package com.caner.automedya.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AutoMedyaPrefs", Context.MODE_PRIVATE)

    var isActive: Boolean
        get() = prefs.getBoolean("isActive", true)
        set(value) = prefs.edit().putBoolean("isActive", value).apply()

    var selectedApp: String
        get() = prefs.getString("selectedApp", "auto") ?: "auto"
        set(value) = prefs.edit().putString("selectedApp", value).apply()
}
