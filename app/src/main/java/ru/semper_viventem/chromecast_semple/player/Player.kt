package ru.semper_viventem.chromecast_semple.player

interface Player {

    val isPlaying: Boolean

    val isReleased: Boolean

    val duration: Long

    var positionInMillis: Long

    var speed: Float

    var volume: Float

    var loop: Boolean

    fun addListener(listener: PlayerCallback)

    fun removeListener(listener: PlayerCallback): Boolean

    fun getListeners(): MutableSet<PlayerCallback>

    fun prepare(mediaContent: MediaContent)

    fun play()

    fun pause()

    fun release()

    interface PlayerCallback {
        fun onPlaying(currentPosition: Long)
        fun onPaused(currentPosition: Long)
        fun onPreparing()
        fun onPrepared()
        fun onLoadingChanged(isLoading: Boolean)
        fun onDurationChanged(duration: Long)
        fun onSetSpeed(speed: Float)
        fun onSeekTo(fromTimeInMillis: Long, toTimeInMillis: Long)
        fun onWaitingForNetwork()
        fun onError(error: String?)
        fun onReleased()
        fun onPlayerProgress(currentPosition: Long)
    }
}
