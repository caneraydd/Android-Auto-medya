package com.caner.automedya.core

import android.media.MediaMetadata
import android.media.session.PlaybackState

object PlaybackStateManager {
    var currentState: PlaybackState? = null
        private set
    var currentMetadata: MediaMetadata? = null
        private set
    var currentPackageName: String? = null
        private set

    private val stateListeners = mutableListOf<StateListener>()
    
    interface StateListener {
        fun onStateChanged(state: PlaybackState?, metadata: MediaMetadata?, packageName: String?)
    }

    interface CommandListener {
        fun onCommand(action: Long)
        fun onSeek(position: Long)
        fun onRefreshRequested()
    }

    var commandListener: CommandListener? = null

    fun updateState(state: PlaybackState?, metadata: MediaMetadata?, packageName: String?) {
        currentState = state
        currentMetadata = metadata
        currentPackageName = packageName
        
        val iterator = stateListeners.iterator()
        while (iterator.hasNext()) {
            iterator.next().onStateChanged(state, metadata, packageName)
        }
    }

    fun addStateListener(listener: StateListener) {
        if (!stateListeners.contains(listener)) {
            stateListeners.add(listener)
        }
        listener.onStateChanged(currentState, currentMetadata, currentPackageName)
    }

    fun removeStateListener(listener: StateListener) {
        stateListeners.remove(listener)
    }

    fun sendCommand(action: Long) {
        commandListener?.onCommand(action)
    }

    fun sendSeek(position: Long) {
        commandListener?.onSeek(position)
    }

    fun requestRefresh() {
        commandListener?.onRefreshRequested()
    }
}
