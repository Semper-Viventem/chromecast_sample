package ru.semper_viventem.playerdelegates.delegates

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.DynamicConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import ru.semper_viventem.playerdelegates.ExtendedSimpleExoPlayer
import ru.semper_viventem.playerdelegates.MediaContent
import ru.semper_viventem.playerdelegates.PlayingDelegate
import timber.log.Timber

class ExoPlayerDelegate(
    private val context: Context,
    appName: String
) : PlayingDelegate() {

    companion object {
        private const val PROGRESS_DELAY_MILLS = 500L
        private const val DEFAULT_SPEED = 1F
    }

    var simpleExoPlayer: ExtendedSimpleExoPlayer? = null
        private set

    private val applicationName: String = appName
    private lateinit var playlist: DynamicConcatenatingMediaSource

    private lateinit var checkProgressRunnable: Runnable
    private val progressHandler = Handler()

    // playing delegate

    override var loop: Boolean = false
    override val isReleased: Boolean = false

    override val isPlaying: Boolean
        get() = simpleExoPlayer?.playWhenReady ?: false

    override val duration: Long
        get() = simpleExoPlayer!!.duration

    override var positionInMillis: Long
        get() = simpleExoPlayer?.currentPosition ?: 0
        set(value) {
            simpleExoPlayer!!.seekTo(value)
        }

    override var speed: Float
        get() = simpleExoPlayer?.playbackParameters?.speed ?: DEFAULT_SPEED
        set(value) {
            simpleExoPlayer!!.playbackParameters = PlaybackParameters(value, simpleExoPlayer!!.playbackParameters.pitch)
        }

    override var volume: Float
        get() = simpleExoPlayer?.volume ?: 0F
        set(value) {
            simpleExoPlayer!!.volume = value
        }

    init {
        checkProgressRunnable = Runnable {
            playerCallback?.onPlayerProgress(positionInMillis)
            progressHandler.postDelayed(checkProgressRunnable, PROGRESS_DELAY_MILLS)
        }
    }

    override fun prepare(mediaContent: MediaContent) {
        val extractorsFactory = DefaultExtractorsFactory()

        playlist = DynamicConcatenatingMediaSource()

        val dataFactory = DefaultDataSourceFactory(context, applicationName)
        val audioSource = ExtractorMediaSource(mediaContent.contentUri, dataFactory, extractorsFactory, null, null)
        playlist.addMediaSource(audioSource)

        simpleExoPlayer = ExtendedSimpleExoPlayer(
            DefaultRenderersFactory(context),
            DefaultTrackSelector(),
            DefaultLoadControl()
        )

        with(simpleExoPlayer!!) {
            addListener(ExoPlayerEventListener())
            addSeekListener(SeekPlayerEventListener())
            prepare(playlist)
            repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        }
    }

    override fun play() {
        simpleExoPlayer!!.playWhenReady = true
        startProgressHandler()
    }

    override fun pause() {
        simpleExoPlayer!!.playWhenReady = false
        stopProgressHandler()
    }

    override fun release() {
        stopProgressHandler()
        simpleExoPlayer?.apply {
            stop()
            release()
        }
        simpleExoPlayer = null
        getListeners().forEach { it.onReleased() }
    }

    override fun networkIsRestored() {
        simpleExoPlayer!!.prepare(playlist, false, true)
    }

    override fun onLeading(leadingParams: LeadingParams?) {
        if (leadingParams == null) return

        if (simpleExoPlayer == null) {
            prepare(leadingParams.mediaContent)
        }

        this.positionInMillis = leadingParams.positionMills
        if (leadingParams.isPlaying) {
            play()
        } else {
            pause()
        }
    }

    override fun onIdle() {
        simpleExoPlayer?.playWhenReady = false
        stopProgressHandler()
    }

    override fun readyForLeading(): Boolean {
        return true
    }

    private fun startProgressHandler() {
        stopProgressHandler()
        progressHandler.post(checkProgressRunnable)
    }

    private fun stopProgressHandler() {
        progressHandler.removeCallbacks(checkProgressRunnable)
    }

    private inner class ExoPlayerEventListener : com.google.android.exoplayer2.Player.EventListener {

        private var isPrepared = false
        private var isTrackChanged = true
        private var isStateEnded = false

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            playerCallback?.onSetSpeed(playbackParameters.speed)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            //do nothing
        }

        override fun onTimelineChanged(timeline: Timeline, manifest: Any?) {
            // do nothing
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
            // do nothing
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            playerCallback?.onLoadingChanged(isLoading)
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Timber.d("onPlayerStateChanged playbackState: $playbackState; isPrepared: $isPrepared")
            when (playbackState) {
                com.google.android.exoplayer2.Player.STATE_READY -> {
                    isStateEnded = false
                    if (!isPrepared) {
                        isPrepared = true
                        playerCallback?.onPrepared()
                    }
                    if (isTrackChanged) {
                        isTrackChanged = false
                        playerCallback?.onDurationChanged(duration)
                        playerCallback?.onPlayerProgress(positionInMillis)
                    }
                }
                com.google.android.exoplayer2.Player.STATE_ENDED -> {
                    if (!isStateEnded) {
                        isStateEnded = true
                        playerCallback?.onPaused(simpleExoPlayer!!.currentPosition)
                    }
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            Timber.e(error.cause)
            isPrepared = false
            if (error.type == ExoPlaybackException.TYPE_SOURCE && error.cause is HttpDataSource.HttpDataSourceException) {
                playerCallback?.onWaitingForNetwork()
            } else {
                playerCallback?.onError(error.message)
            }
        }

        override fun onPositionDiscontinuity() {
            try {
                isTrackChanged = true
                playerCallback?.onDurationChanged(duration)
                playerCallback?.onPlayerProgress(positionInMillis)
            } catch (exception: IllegalStateException) {
                Timber.e(exception) // ignore video player seek actions when error occurred
            }
        }
    }

    //endregion player states

    private open inner class SeekPlayerEventListener : ExtendedSimpleExoPlayer.SeekListener {

        override fun onSeek(fromTimeInMillis: Long, toTimeInMillis: Long) {
            playerCallback?.onSeekTo(fromTimeInMillis, toTimeInMillis)
        }
    }
}