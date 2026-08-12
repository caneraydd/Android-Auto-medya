package com.caner.automedya

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.caner.automedya.core.PlaybackStateManager
import com.caner.automedya.data.AppPreferences
import com.caner.automedya.data.MediaAppProvider
import android.media.session.PlaybackState
import android.media.MediaMetadata

class AutoMediaService : MediaBrowserServiceCompat(), PlaybackStateManager.StateListener {

    private var mediaSession: MediaSessionCompat? = null
    private lateinit var prefs: AppPreferences
    private lateinit var appProvider: MediaAppProvider
    
    companion object {
        private const val ROOT_ID = "root_id"
    }

    override fun onCreate() {
        super.onCreate()
        
        prefs = AppPreferences(this)
        appProvider = MediaAppProvider(this)
        
        mediaSession = MediaSessionCompat(this, "AutoMediaSession").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            
            val state = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
            
            setPlaybackState(state)
            
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    PlaybackStateManager.sendCommand(PlaybackStateCompat.ACTION_PLAY)
                }

                override fun onPause() {
                    PlaybackStateManager.sendCommand(PlaybackStateCompat.ACTION_PAUSE)
                }

                override fun onSkipToNext() {
                    PlaybackStateManager.sendCommand(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                }

                override fun onSkipToPrevious() {
                    PlaybackStateManager.sendCommand(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                }

                override fun onSeekTo(pos: Long) {
                    PlaybackStateManager.sendSeek(pos)
                }

                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    if (mediaId == "CURRENT" || mediaId == "now_playing" || mediaId == null) {
                        onPlay()
                    } else if (mediaId == "TOGGLE_ACTIVE") {
                        prefs.isActive = !prefs.isActive
                        PlaybackStateManager.requestRefresh()
                    } else {
                        prefs.selectedApp = mediaId
                        PlaybackStateManager.requestRefresh()
                    }
                }
            })
            isActive = true
        }
        
        sessionToken = mediaSession?.sessionToken
        PlaybackStateManager.addStateListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        PlaybackStateManager.removeStateListener(this)
        mediaSession?.release()
        mediaSession = null
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        
        val activeText = if (prefs.isActive) "Auto Medya: AKTİF" else "Auto Medya: KAPALI"
        val activeSub = if (prefs.isActive) "Kapatmak için dokunun" else "Açmak için dokunun"
        
        val toggleDesc = android.support.v4.media.MediaDescriptionCompat.Builder()
            .setMediaId("TOGGLE_ACTIVE")
            .setTitle(activeText)
            .setSubtitle(activeSub)
            .build()
        mediaItems.add(MediaBrowserCompat.MediaItem(toggleDesc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
        
        val currentDesc = android.support.v4.media.MediaDescriptionCompat.Builder()
            .setMediaId("CURRENT")
            .setTitle("Şu An Çalanı Aç")
            .setSubtitle("Oynatıcı ekranına gitmek için dokunun")
            .build()
        mediaItems.add(MediaBrowserCompat.MediaItem(currentDesc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))

        val apps = appProvider.getSupportedApps()
        for (app in apps) {
            if (app.pkg == "auto") continue
            val desc = android.support.v4.media.MediaDescriptionCompat.Builder()
                .setMediaId(app.pkg)
                .setTitle(app.name)
                .setSubtitle("Seçmek için dokunun")
                .build()
            mediaItems.add(MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
        }
        
        result.sendResult(mediaItems)
    }

    override fun onStateChanged(state: PlaybackState?, metadata: MediaMetadata?, packageName: String?) {
        val session = mediaSession ?: return
        
        if (state != null) {
            val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO)
                
            val compatState = when (state.state) {
                PlaybackState.STATE_PLAYING -> PlaybackStateCompat.STATE_PLAYING
                PlaybackState.STATE_PAUSED -> PlaybackStateCompat.STATE_PAUSED
                PlaybackState.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
                PlaybackState.STATE_CONNECTING -> PlaybackStateCompat.STATE_CONNECTING
                PlaybackState.STATE_FAST_FORWARDING -> PlaybackStateCompat.STATE_FAST_FORWARDING
                PlaybackState.STATE_REWINDING -> PlaybackStateCompat.STATE_REWINDING
                PlaybackState.STATE_SKIPPING_TO_NEXT -> PlaybackStateCompat.STATE_SKIPPING_TO_NEXT
                PlaybackState.STATE_SKIPPING_TO_PREVIOUS -> PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS
                PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM
                else -> PlaybackStateCompat.STATE_PAUSED
            }
            
            stateBuilder.setState(compatState, state.position, state.playbackSpeed)
            session.setPlaybackState(stateBuilder.build())
        } else {
            val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1f)
            session.setPlaybackState(stateBuilder.build())
        }

        if (metadata != null) {
            val builder = MediaMetadataCompat.Builder()
            val title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Bilinmeyen Şarkı"
            val artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Bilinmeyen Sanatçı"
            
            val duration = metadata.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION)
            if (duration > 0) {
                builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            }
            
            builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            
            var art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            if (art == null) art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
            if (art == null) art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)

            if (art != null) {
                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
            }
            
            session.setMetadata(builder.build())
        } else {
            val builder = MediaMetadataCompat.Builder()
            builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Auto Medya")
            builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Oynatılan bir şey yok")
            session.setMetadata(builder.build())
        }
    }
}
