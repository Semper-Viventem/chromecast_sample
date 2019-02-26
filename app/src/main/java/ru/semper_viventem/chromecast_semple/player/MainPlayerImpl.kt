package ru.semper_viventem.chromecast_semple.player

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import ru.semper_viventem.chromecast_semple.player.delegates.ChromeCastDelegate
import ru.semper_viventem.chromecast_semple.player.delegates.ExoPlayerDelegate
import ru.semper_viventem.chromecast_semple.player.delegates.MediaSessionListener
import timber.log.Timber

class MainPlayerImpl(
    context: Context,
    mediaSession: MediaSessionCompat
) : Player {

    private val castCallback = object : ChromeCastDelegate.ChromeCastListener {

        override fun onCastStarted() {
            val playing = currentState is Playing
            playingDelegates.forEach {
                it.setIsLeading(it is ChromeCastDelegate, positionInMillis, playing)
            }
        }

        override fun onCastStopped() {
            val playing = currentState is Playing
            playingDelegates.forEach {
                it.setIsLeading(it is ExoPlayerDelegate, positionInMillis, playing)
            }
        }
    }

    private val stateListeners = mutableListOf<PlayerStateListener>()
    private val playingDelegates = mutableListOf<PlayingDelegate>()

    private val leadingDelegate: PlayingDelegate
        get() = playingDelegates.firstOrNull { it.isLeading } ?: playingDelegates.first()

    private var currentState: State = Empty()
        set(value) {
            field = value
            field.notifyListeners()
        }

    val listeners = hashSetOf<Player.PlayerCallback>()

    //region player interface
    override var volume
        get() = currentState.volume
        set(value) {
            currentState.volume = value
        }

    override var loop = false
    override val isPlaying
        get() = currentState.isPlaying

    override var positionInMillis
        get() = currentState.positionInMillis
        set(value) {
            currentState.positionInMillis = value
        }

    override var speed: Float
        get() = currentState.speed
        set(value) {
            currentState.speed = value
        }

    override val duration get() = currentState.duration
    override val isReleased get() = currentState.isReleased

    init {
        val playerCallbackInternal = PlayerCallbackInternal()

        val mediaSessionListener = MediaSessionListener(context, mediaSession)
        val chromeCastDelegate = ChromeCastDelegate(context, castCallback, playerCallbackInternal)
        val exoPlayerDelegate = ExoPlayerDelegate(context, playerCallbackInternal)

        with(stateListeners) {
            add(mediaSessionListener)
        }

        with(playingDelegates) {
            add(chromeCastDelegate)
            add(exoPlayerDelegate)
        }
    }

    override fun prepare(mediaContent: MediaContent) = currentState.prepare(mediaContent)

    override fun play() = currentState.play()

    override fun pause() = currentState.pause()

    override fun release() = currentState.release()

    override fun addListener(listener: Player.PlayerCallback) {
        if (listeners.add(listener)) {
            notifyNewListener(listener)
        }
    }

    override fun removeListener(listener: Player.PlayerCallback) = listeners.remove(listener)

    override fun getListeners(): MutableSet<Player.PlayerCallback> = listeners

    //endregion player interface

    /**
     * Send current player state to new listener
     */
    private fun notifyNewListener(listener: Player.PlayerCallback) {
        try {
            if (currentState.isPrepared) {
                listener.onPrepared()
            }
            listener.onDurationChanged(currentState.duration)
            listener.onPlayerProgress(currentState.positionInMillis)
            listener.onSetSpeed(currentState.speed)
            currentState.notifyListeners()
            if (currentState.isPlaying)
                listener.onPlaying(currentState.positionInMillis)
            else
                listener.onPaused(currentState.positionInMillis)
        } catch (exception: IllegalStateException) {
            Timber.i("Can't notify listener. Player not initialized yet.")
        }
    }

    //region player states
    /**
     * Abstract state class for player
     */
    private abstract inner class State {

        open val isPrepared = false
        open var isPlaying = false

        open var positionInMillis: Long
            get() = throw IllegalStateException()
            set(value) {
                // ignore
            }

        open val duration: Long
            get() = throw IllegalStateException()

        open var volume: Float
            get() = throw IllegalStateException()
            set(value) = Timber.e("illegal state for set volume")

        open var speed: Float
            get() = throw IllegalArgumentException()
            set(value) = Timber.e("illegal state for set speed")

        open val isReleased = true

        open fun prepare(mediaContent: MediaContent) {
            currentState = Preparing(mediaContent)
        }

        open fun prepared() = Timber.e("illegal state for prepared")

        open fun play() = Timber.e("illegal state for play")

        open fun pause() = Timber.e("illegal state for pause")

        open fun notifyListeners() = Timber.e("illegal state for notifyListeners")

        fun release() {
            Timber.d("release")
            playingDelegates.forEach { it.release() }
            currentState = Empty()
            stateListeners.forEach { it.onStop() }
        }

        fun waitingNetwork() {
            currentState = WaitingForNetwork()
        }

        fun error(error: String?) {
            currentState = Error(error)
        }
    }

    private inner class Empty : State() {
        init {
            stateListeners.forEach { it.onInit() }
        }
    }

    private inner class Preparing(mediaContent: MediaContent) : State() {

        override val isReleased = false

        private var playWhenReady = false

        init {
            Timber.d("prepare uri: " + mediaContent.contentUri.toString())

            playingDelegates.forEach { it.prepare(mediaContent) }

            playingDelegates.forEach {
                if (it.readyForLeading()) {
                    it.isLeading = true
                    return@forEach
                }
            }

            stateListeners.forEach { it.onPrepared(mediaContent) }
        }

        override fun prepare(mediaContent: MediaContent) {
            release()
            super.prepare(mediaContent)
        }

        override fun play() {
            playWhenReady = true
        }

        override fun pause() {
            playWhenReady = false
        }

        override fun prepared() {
            currentState = Prepared()
            if (playWhenReady) {
                currentState.play()
            }
        }

        override fun notifyListeners() = listeners.forEach { it.onPreparing() }

    }

    private inner class Prepared : State() {

        override val isPrepared = true

        override var positionInMillis: Long
            get() = leadingDelegate.positionInMillis
            set(value) {
                playingDelegates.forEach { it.positionInMillis = value }
            }

        override val duration
            get() = leadingDelegate.duration

        override val isReleased = false

        override var volume
            get() = leadingDelegate.volume
            set(value) {
                playingDelegates.forEach { it.volume = value }
            }

        override var speed: Float
            get() = leadingDelegate.speed
            set(value) {
                Timber.d("Speed selected: $value")
                playingDelegates.forEach { it.speed = value }
            }

        init {
            Timber.d("prepared")
            listeners.forEach { it.onPrepared() }
        }

        override fun prepare(mediaContent: MediaContent) {
            release()
            super.prepare(mediaContent)
        }

        override fun play() {
            currentState = Playing()
        }

        override fun pause() {
            currentState = Paused()
        }

        override fun notifyListeners() = listeners.forEach { it.onPrepared() }
    }

    private inner class Playing : State() {

        override val isPrepared = true

        override var isPlaying = true

        override var positionInMillis: Long
            get() = leadingDelegate.positionInMillis
            set(value) {
                playingDelegates.forEach { it.positionInMillis = value }
            }

        override val duration
            get() = leadingDelegate.duration

        override var speed: Float
            get() = leadingDelegate.speed
            set(value) {
                Timber.d("Speed selected: $value")
                playingDelegates.forEach { it.speed = value }
            }

        override val isReleased = false

        override var volume
            get() = leadingDelegate.volume
            set(value) {
                playingDelegates.forEach { it.volume = value }
            }

        init {
            Timber.d("play")
            leadingDelegate.play()
            stateListeners.forEach { it.onPlay() }
        }

        override fun prepare(mediaContent: MediaContent) {
            release()
            super.prepare(mediaContent)
        }

        override fun pause() {
            currentState = Paused()
        }

        override fun notifyListeners() = listeners.forEach { it.onPlaying(currentState.positionInMillis) }
    }

    private inner class Paused : State() {

        override val isPrepared = true

        override var positionInMillis: Long
            get() = leadingDelegate.positionInMillis
            set(value) {
                playingDelegates.forEach { it.positionInMillis = value }
            }

        override val duration
            get() = leadingDelegate.duration

        override var speed: Float
            get() = leadingDelegate.speed
            set(value) {
                Timber.d("Speed selected: $value")
                playingDelegates.forEach { it.speed = value }
            }

        override val isReleased = false

        override var volume
            get() = leadingDelegate.volume
            set(value) {
                playingDelegates.forEach { it.volume = value }
            }

        init {
            Timber.d("pause")

            leadingDelegate.pause()
            stateListeners.forEach { it.onPause() }
        }

        override fun prepare(mediaContent: MediaContent) {
            release()
            super.prepare(mediaContent)
        }

        override fun play() {
            with(leadingDelegate) {
                if (positionInMillis < duration) {
                    currentState = Playing()
                }
            }
        }

        override fun notifyListeners() = listeners.forEach { it.onPaused(positionInMillis) }
    }

    private inner class WaitingForNetwork : State() {

        override var positionInMillis: Long
            get() = leadingDelegate.positionInMillis
            set(value) {
                playingDelegates.forEach { it.positionInMillis = value }
            }

        override val duration
            get() = leadingDelegate.duration

        override var volume
            get() = leadingDelegate.volume
            set(value) {
                playingDelegates.forEach { it.volume = value }
            }

        override var speed: Float
            get() = leadingDelegate.speed
            set(value) {
                Timber.d("Speed selected: $value")
                playingDelegates.forEach { it.speed = value }
            }

        override val isReleased = false

        override var isPlaying = leadingDelegate.isPlaying

        init {
            Timber.d("waiting for network")

            // TODO show on network state and reload
        }

        override fun prepare(mediaContent: MediaContent) {
            release()
            super.prepare(mediaContent)
        }

        override fun prepared() {
            currentState = Prepared()
            if (leadingDelegate.isPlaying) {
                currentState.play()
            } else {
                currentState.pause()
            }
        }

        override fun play() {
            isPlaying = true
            leadingDelegate.play()
            listeners.forEach { it.onPlaying(positionInMillis) }
        }

        override fun pause() {
            isPlaying = false
            leadingDelegate.pause()
            listeners.forEach { it.onPaused(positionInMillis) }
        }

        override fun notifyListeners() = listeners.forEach { it.onWaitingForNetwork() }
    }

    private inner class Error(val error: String?) : State() {

        override var positionInMillis: Long
            get() = leadingDelegate.positionInMillis
            set(value) {
                playingDelegates.forEach { it.positionInMillis = value }
            }

        override val duration
            get() = leadingDelegate.duration

        override val isReleased = false

        init {
            Timber.d("error")
            stateListeners.forEach { it.onError(error.orEmpty()) }
        }

        override fun prepare(mediaContent: MediaContent) {
            release()
            super.prepare(mediaContent)
        }

        override fun notifyListeners() = listeners.forEach { it.onError(error) }
    }

    //endregion player states

    private inner class PlayerCallbackInternal : Player.PlayerCallback {

        override fun onPlaying(currentPosition: Long) {
            currentState.play()
        }

        override fun onPaused(currentPosition: Long) {
            currentState.pause()
        }

        override fun onPrepared() {
            currentState.prepared()
        }

        override fun onWaitingForNetwork() {
            currentState.waitingNetwork()
        }

        override fun onError(error: String?) {
            currentState.error(error)
        }

        override fun onReleased() {
            currentState.release()
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            listeners.forEach { it.onLoadingChanged(isLoading) }
        }

        override fun onDurationChanged(duration: Long) {
            listeners.forEach { it.onDurationChanged(duration) }
        }

        override fun onSetSpeed(speed: Float) {
            listeners.forEach { it.onSetSpeed(speed) }
        }

        override fun onSeekTo(fromTimeInMillis: Long, toTimeInMillis: Long) {
            listeners.forEach { it.onSeekTo(fromTimeInMillis, toTimeInMillis) }
        }

        override fun onPlayerProgress(currentPosition: Long) {
            listeners.forEach { it.onPlayerProgress(currentPosition) }
        }

        override fun onPreparing() {
            // do nothing
        }
    }
}
