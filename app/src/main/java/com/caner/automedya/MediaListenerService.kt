package com.caner.automedya

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.caner.automedya.core.PlaybackStateManager
import com.caner.automedya.data.AppPreferences

class MediaListenerService : NotificationListenerService(), PlaybackStateManager.CommandListener {

    private var activeController: MediaController? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private lateinit var prefs: AppPreferences
    private var pendingAction: Long? = null

    private val activeSessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        prefs = AppPreferences(this)
        PlaybackStateManager.commandListener = this
    }

    override fun onRefreshRequested() {
        refreshState()
    }

    private fun refreshState() {
        try {
            val componentName = ComponentName(this, MediaListenerService::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            updateActiveController(controllers)
        } catch ( e: Exception) {
            // Ignore if permission not granted
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshState()
        try {
            val componentName = ComponentName(this, MediaListenerService::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(activeSessionsChangedListener, componentName)
        } catch (e: SecurityException) {
            Log.e("MediaListener", "Permission not fully granted", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (PlaybackStateManager.commandListener == this) {
            PlaybackStateManager.commandListener = null
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(mediaControllerCallback)
        
        if (!prefs.isActive) {
            activeController = null
            syncStateToManager(null, null)
            return
        }

        val selectedApp = prefs.selectedApp
        var targetController: MediaController? = null
        
        if (controllers != null && controllers.isNotEmpty()) {
            if (selectedApp != "auto") {
                for (controller in controllers) {
                    if (controller.packageName == selectedApp) {
                        targetController = controller
                        break
                    }
                }
            } else {
                var playingController: MediaController? = null
                var pausedController: MediaController? = null
                
                for (controller in controllers) {
                    val state = controller.playbackState?.state
                    if (state == PlaybackState.STATE_PLAYING && playingController == null) {
                        playingController = controller
                    } else if (state == PlaybackState.STATE_PAUSED && pausedController == null) {
                        pausedController = controller
                    }
                }
                targetController = playingController ?: pausedController ?: controllers[0]
            }
        }
        
        activeController = targetController
        activeController?.registerCallback(mediaControllerCallback)
        
        if (activeController != null) {
            syncStateToManager(activeController!!.playbackState, activeController!!.metadata)
        } else {
            syncStateToManager(null, null)
        }
    }

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            val st = state?.state
            if (st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_PAUSED) {
                val action = pendingAction
                if (action != null) {
                    pendingAction = null
                    onCommand(action)
                }
            }
            syncStateToManager(state, activeController?.metadata)
        }

        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            syncStateToManager(activeController?.playbackState, metadata)
        }
    }

    private fun syncStateToManager(state: PlaybackState?, metadata: android.media.MediaMetadata?) {
        PlaybackStateManager.updateState(state, metadata, activeController?.packageName)
    }

    override fun onCommand(action: Long) {
        val controller = activeController ?: return
        
        if (action == PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) {
            val currentPos = controller.playbackState?.position ?: 0L
            if (currentPos >= 4000L) {
                controller.transportControls.seekTo(0L)
                return
            }
        }
        
        val state = controller.playbackState?.state
        if (state == PlaybackState.STATE_BUFFERING || state == PlaybackState.STATE_CONNECTING) {
            pendingAction = action
            return
        }

        try {
            when (action) {
                PlaybackStateCompat.ACTION_PLAY -> controller.transportControls.play()
                PlaybackStateCompat.ACTION_PAUSE -> controller.transportControls.pause()
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT -> controller.transportControls.skipToNext()
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS -> controller.transportControls.skipToPrevious()
            }
        } catch (e: Exception) {
            Log.e("AutoMedya", "Action error", e)
        }
    }

    override fun onSeek(position: Long) {
        val controller = activeController ?: return
        val state = controller.playbackState?.state
        if (state == PlaybackState.STATE_BUFFERING || state == PlaybackState.STATE_CONNECTING) {
            return
        }
        controller.transportControls?.seekTo(position)
    }
}
