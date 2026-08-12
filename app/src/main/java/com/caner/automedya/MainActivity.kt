package com.caner.automedya

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.caner.automedya.core.PlaybackStateManager
import com.caner.automedya.data.AppInfo
import com.caner.automedya.data.AppPreferences
import com.caner.automedya.data.MediaAppProvider
import com.caner.automedya.ui.MediaControllerViewManager
import android.media.MediaMetadata
import android.media.session.PlaybackState

class MainActivity : Activity(), PlaybackStateManager.StateListener {

    private lateinit var statusTextView: TextView
    private lateinit var actionButton: Button
    private lateinit var globalSwitch: Switch
    private lateinit var appListView: ListView
    private lateinit var prefs: AppPreferences
    private lateinit var appProvider: MediaAppProvider
    private lateinit var mediaControllerViewManager: MediaControllerViewManager
    private val appList = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)
        appProvider = MediaAppProvider(this)
        mediaControllerViewManager = MediaControllerViewManager(this) { pkg -> appProvider.getAppColor(pkg) }

        val rootLayout = RelativeLayout(this).apply {
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(48, 48, 48, 48)
        }

        val topBar = RelativeLayout(this).apply {
            id = View.generateViewId()
        }

        val titleText = TextView(this).apply {
            text = "Auto Medya"
            textSize = 24f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val titleParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            addRule(RelativeLayout.CENTER_VERTICAL)
        }
        topBar.addView(titleText, titleParams)

        globalSwitch = Switch(this).apply {
            isChecked = prefs.isActive
            setOnCheckedChangeListener { _, isChecked ->
                prefs.isActive = isChecked
                PlaybackStateManager.requestRefresh()
            }
        }
        val switchParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            addRule(RelativeLayout.CENTER_VERTICAL)
        }
        topBar.addView(globalSwitch, switchParams)
        
        val topBarParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
        }
        rootLayout.addView(topBar, topBarParams)

        statusTextView = TextView(this).apply {
            id = View.generateViewId()
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(0, 48, 0, 32)
        }
        val statusParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.BELOW, topBar.id)
        }
        rootLayout.addView(statusTextView, statusParams)

        actionButton = Button(this).apply {
            id = View.generateViewId()
            text = "İzin Ayarlarına Git"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        val actionBtnParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.BELOW, statusTextView.id)
        }
        rootLayout.addView(actionButton, actionBtnParams)

        val mediaCtrlParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.BELOW, actionButton.id)
            setMargins(0, 32, 0, 0)
        }
        rootLayout.addView(mediaControllerViewManager.view, mediaCtrlParams)

        appListView = ListView(this).apply {
            divider = android.graphics.drawable.ColorDrawable(Color.DKGRAY)
            dividerHeight = 1
        }
        val listParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        ).apply {
            addRule(RelativeLayout.BELOW, mediaControllerViewManager.view.id)
            setMargins(0, 32, 0, 0)
        }
        rootLayout.addView(appListView, listParams)

        setContentView(rootLayout)
        
        loadApps()
    }

    private fun loadApps() {
        appList.clear()
        appList.addAll(appProvider.getSupportedApps())

        val adapter = AppAdapter(this, appList, prefs)
        appListView.adapter = adapter
        
        appListView.setOnItemClickListener { _, _, position, _ ->
            prefs.selectedApp = appList[position].pkg
            adapter.notifyDataSetChanged()
            PlaybackStateManager.requestRefresh()
        }
    }

    override fun onResume() {
        super.onResume()
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val isGranted = !TextUtils.isEmpty(flat) && flat.contains(pkgName)
        
        if (isGranted) {
            statusTextView.text = "Bildirim izni tamam. Aşağıdan kontrol edilecek uygulamayı seçebilirsiniz:"
            actionButton.visibility = View.GONE
        } else {
            statusTextView.text = "Uygulamaları kontrol etmek için bildirim okuma izni (Notification Access) gerekiyor."
            actionButton.visibility = View.VISIBLE
        }

        PlaybackStateManager.addStateListener(this)
        PlaybackStateManager.requestRefresh()
    }

    override fun onPause() {
        super.onPause()
        PlaybackStateManager.removeStateListener(this)
    }

    override fun onStateChanged(state: PlaybackState?, metadata: MediaMetadata?, packageName: String?) {
        runOnUiThread {
            mediaControllerViewManager.updateUI(state, metadata, packageName)
        }
    }

    inner class AppAdapter(context: Context, private val apps: List<AppInfo>, private val prefs: AppPreferences) : ArrayAdapter<AppInfo>(context, 0, apps) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: LinearLayout = if (convertView == null) {
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(16, 48, 16, 48)
                    
                    val radio = RadioButton(context).apply {
                        id = android.R.id.button1
                        isClickable = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                    }
                    addView(radio)

                    val iconView = ImageView(context).apply {
                        id = android.R.id.icon
                        layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                            setMargins(32, 0, 32, 0)
                        }
                    }
                    addView(iconView)

                    val nameView = TextView(context).apply {
                        id = android.R.id.text1
                        textSize = 17f
                        setTextColor(Color.WHITE)
                    }
                    addView(nameView)
                }
            } else {
                convertView as LinearLayout
            }

            val app = apps[position]
            val radio = view.findViewById<RadioButton>(android.R.id.button1)
            val iconView = view.findViewById<ImageView>(android.R.id.icon)
            val nameView = view.findViewById<TextView>(android.R.id.text1)

            radio.isChecked = (app.pkg == prefs.selectedApp)

            iconView.setImageDrawable(app.icon)
            nameView.text = app.name

            return view
        }
    }
}
