package com.caner.automedya.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable

data class AppInfo(val name: String, val pkg: String, val icon: Drawable?)

class MediaAppProvider(private val context: Context) {
    val whitelist = listOf(
        "com.spotify.music",
        "deezer.android.app",
        "com.google.android.apps.youtube.music",
        "com.google.android.youtube",
        "com.apple.android.music",
        "com.sec.android.app.music"
    )

    private val colorMap = mapOf(
        "com.spotify.music" to Color.parseColor("#1DB954"),
        "com.google.android.apps.youtube.music" to Color.parseColor("#FF0000"),
        "com.google.android.youtube" to Color.parseColor("#FF0000"),
        "deezer.android.app" to Color.parseColor("#EF5466"),
        "com.apple.android.music" to Color.parseColor("#FA243C"),
        "com.sec.android.app.music" to Color.parseColor("#6C24CC")
    )

    fun getSupportedApps(): List<AppInfo> {
        val pm = context.packageManager
        val appList = mutableListOf<AppInfo>()
        
        val autoIcon = context.getDrawable(android.R.drawable.ic_menu_compass)
        appList.add(AppInfo("Otomatik (Şu An Çalan)", "auto", autoIcon))

        for (pkg in whitelist) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()
                val appIcon = pm.getApplicationIcon(appInfo)
                appList.add(AppInfo(appName, pkg, appIcon))
            } catch (e: PackageManager.NameNotFoundException) {
                // Ignore uninstalled apps
            }
        }
        return appList
    }

    fun getAppColor(packageName: String): Int {
        if (colorMap.containsKey(packageName)) {
            return colorMap[packageName]!!
        }
        return try {
            val icon = context.packageManager.getApplicationIcon(packageName)
            getDominantColor(icon)
        } catch (e: Exception) {
            Color.DKGRAY
        }
    }

    private fun getDominantColor(drawable: Drawable): Int {
        val bitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        val color = bitmap.getPixel(0, 0)
        bitmap.recycle()
        return color
    }
}
