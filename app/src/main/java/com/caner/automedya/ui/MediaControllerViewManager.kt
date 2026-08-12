package com.caner.automedya.ui

import android.content.Context
import android.graphics.Color
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.caner.automedya.core.PlaybackStateManager

class MediaControllerViewManager(private val context: Context, private val colorProvider: (String) -> Int) {

    val view: LinearLayout
    private val albumArtView: ImageView
    private val songTitleView: TextView
    private val songArtistView: TextView
    private val btnPlayPause: ImageButton

    init {
        view = LinearLayout(context).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(32, 32, 32, 32)
            background = getBorderDrawable(Color.DKGRAY, 6)
            visibility = View.GONE
        }

        albumArtView = ImageView(context).apply {
            id = View.generateViewId()
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(140, 140).apply {
                setMargins(0, 0, 32, 0)
            }
        }
        view.addView(albumArtView)

        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        songTitleView = TextView(context).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            ellipsize = TextUtils.TruncateAt.END
            setSingleLine()
            text = "Bilinmeyen Şarkı"
        }
        textContainer.addView(songTitleView)
        
        songArtistView = TextView(context).apply {
            textSize = 14f
            setTextColor(Color.LTGRAY)
            ellipsize = TextUtils.TruncateAt.END
            setSingleLine()
            text = "Bilinmeyen Sanatçı"
            setPadding(0, 8, 0, 0)
        }
        textContainer.addView(songArtistView)
        
        view.addView(textContainer)

        val controlsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val btnPrev = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(24, 24, 24, 24)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { PlaybackStateManager.sendCommand(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) }
        }
        
        btnPlayPause = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_media_play)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(32, 24, 32, 24)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { 
                val state = this.tag as? Int
                if (state == android.media.session.PlaybackState.STATE_PLAYING) {
                    PlaybackStateManager.sendCommand(PlaybackStateCompat.ACTION_PAUSE)
                } else {
                    PlaybackStateManager.sendCommand(PlaybackStateCompat.ACTION_PLAY)
                }
            }
        }
        
        val btnNext = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_media_next)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(24, 24, 24, 24)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { PlaybackStateManager.sendCommand(PlaybackStateCompat.ACTION_SKIP_TO_NEXT) }
        }
        
        controlsContainer.addView(btnPrev)
        controlsContainer.addView(btnPlayPause)
        controlsContainer.addView(btnNext)

        view.addView(controlsContainer)
    }

    private fun getBorderDrawable(color: Int, strokeWidth: Int = 4): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#222222"))
            setStroke(strokeWidth, color)
            cornerRadius = 24f
        }
    }

    fun updateUI(state: android.media.session.PlaybackState?, metadata: android.media.MediaMetadata?, packageName: String?) {
        if (state == null && packageName == null) {
            view.visibility = View.GONE
            return
        }
        view.visibility = View.VISIBLE
        
        val isPlaying = state?.state == android.media.session.PlaybackState.STATE_PLAYING
        btnPlayPause.tag = state?.state
        btnPlayPause.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        
        if (metadata != null) {
            val title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Bilinmeyen Şarkı"
            val artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Bilinmeyen Sanatçı"
            var art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            if (art == null) art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
            if (art == null) art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            
            songTitleView.text = title
            songArtistView.text = artist
            if (art != null) {
                albumArtView.setImageBitmap(art)
                albumArtView.visibility = View.VISIBLE
            } else {
                albumArtView.visibility = View.GONE
            }
        } else {
            songTitleView.text = "Bilinmeyen Şarkı"
            songArtistView.text = "Bilinmeyen Sanatçı"
            albumArtView.visibility = View.GONE
        }
        
        val targetColor = if (packageName != null) colorProvider(packageName) else Color.DKGRAY
        view.background = getBorderDrawable(targetColor, 6)
    }
}
