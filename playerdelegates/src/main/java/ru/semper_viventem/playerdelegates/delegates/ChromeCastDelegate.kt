package ru.semper_viventem.playerdelegates.delegates

import android.content.Context
import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import ru.semper_viventem.playerdelegates.MediaContent
import ru.semper_viventem.playerdelegates.PlayingDelegate
import timber.log.Timber

class ChromeCastDelegate(
    context: Context
) : PlayingDelegate() {

    companion object {
        private const val CONTENT_TYPE_VIDEO = "videos/mp4"
        private const val CONTENT_TYPE_AUDIO = "audio/mp3"
        private const val PROGRESS_DELAY_MILLS = 500L
        private const val DEFAULT_SPEED = 1F
    }

    private var sessionManager: SessionManager? = null
    private var currentSession: CastSession? = null
    private var mediaContent: MediaContent? = null

    private var currentPosition: Long = 0

    private val mediaSessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            currentSession = session
            leadingCallback?.onStartLeading(this@ChromeCastDelegate)
        }

        override fun onSessionEnding(session: CastSession) {
            currentPosition = session.remoteMediaClient?.approximateStreamPosition
                ?: currentPosition
            stopCasting()
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            currentSession = session
            leadingCallback?.onStartLeading(this@ChromeCastDelegate)
        }

        override fun onSessionStartFailed(session: CastSession, p1: Int) {
            stopCasting()
        }

        override fun onSessionEnded(session: CastSession, p1: Int) {
            // do nothing
        }

        override fun onSessionResumeFailed(session: CastSession, p1: Int) {
            // do nothing
        }

        override fun onSessionSuspended(session: CastSession, p1: Int) {
            // do nothing
        }

        override fun onSessionStarting(session: CastSession) {
            // do nothing
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {
            // do nothing
        }
    }

    private val castStatusCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            if (currentSession == null) return
            val playerState = currentSession!!.remoteMediaClient.playerState

            when (playerState) {
                MediaStatus.PLAYER_STATE_PLAYING -> playerCallback?.onPlaying(positionInMillis)
                MediaStatus.PLAYER_STATE_PAUSED -> playerCallback?.onPaused(positionInMillis)
            }
        }
    }

    private val progressListener = RemoteMediaClient.ProgressListener { progressMs, durationMs ->
        playerCallback?.onPlayerProgress(progressMs)
    }

    // Playing delegate

    override val isReleased: Boolean = false
    override var loop: Boolean = false

    override val isPlaying: Boolean
        get() = currentSession?.remoteMediaClient?.isPlaying ?: false

    override val duration: Long
        get() = currentSession?.remoteMediaClient?.streamDuration ?: 0

    override var positionInMillis: Long
        get() {
            currentPosition = currentSession?.remoteMediaClient?.approximateStreamPosition
                ?: currentPosition
            return currentPosition
        }
        set(value) {
            currentPosition = value
            checkAndStartCasting()
            Timber.d("On set position")
        }

    override var speed: Float = DEFAULT_SPEED
        set(value) {
            field = value
            checkAndStartCasting()
            Timber.d("On set speed")
        }

    override var volume: Float
        get() = currentSession?.volume?.toFloat() ?: 0F
        set(value) {
            currentSession?.volume = value.toDouble()
        }

    init {
        sessionManager = CastContext.getSharedInstance(context).sessionManager
        sessionManager?.addSessionManagerListener(mediaSessionListener, CastSession::class.java)
        currentSession = sessionManager?.currentCastSession
    }

    override fun prepare(mediaContent: MediaContent) {

        this.mediaContent = mediaContent

        playerCallback?.onPrepared()

        Timber.d("On prepared")
    }

    override fun play() {
        currentSession?.remoteMediaClient?.play()

        Timber.d("On play")
    }

    override fun pause() {
        currentSession?.remoteMediaClient?.pause()

        Timber.d("On pause")
    }

    override fun release() {
        stopCasting(true)

        Timber.d("On stop")
    }

    override fun onLeading(leadingParams: LeadingParams?) {
        leadingParams?.let {
            prepare(it.mediaContent)
            currentPosition = it.positionMills
        }
        checkAndStartCasting()
    }

    override fun onIdle() {
        // TODO
    }

    override fun readyForLeading(): Boolean {
        return currentSession != null
    }

    // internal
    private fun checkAndStartCasting() {
        if (currentSession != null && mediaContent?.metadata != null && isLeading) {

            val mediaMetadata = MediaMetadata(getMetadataType(mediaContent!!.type)).apply {
                putString(MediaMetadata.KEY_TITLE, mediaContent?.metadata?.title.orEmpty())
                putString(MediaMetadata.KEY_ARTIST, mediaContent?.metadata?.author.orEmpty())
                mediaContent?.metadata?.posterUrl?.let { poster ->
                    addImage(WebImage(Uri.parse(poster)))
                }
            }

            val mediaInfo = MediaInfo.Builder(mediaContent!!.contentUri.toString())
                .setContentType(getContentType(mediaContent!!.type))
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build()

            val mediaLoadOptions = MediaLoadOptions.Builder()
                .setPlayPosition(currentPosition)
                .setAutoplay(true)
                .setPlaybackRate(speed.toDouble())
                .build()

            val remoteMediaClient = currentSession!!.remoteMediaClient
            remoteMediaClient.unregisterCallback(castStatusCallback)
            remoteMediaClient.load(mediaInfo, mediaLoadOptions)
            remoteMediaClient.registerCallback(castStatusCallback)
            remoteMediaClient.addProgressListener(progressListener, PROGRESS_DELAY_MILLS)
        }
    }

    private fun stopCasting(andRelease: Boolean = false) {

        val leadingParams = LeadingParams(
            mediaContent!!,
            positionInMillis,
            duration,
            isPlaying,
            speed,
            volume
        )

        if (andRelease) {
            sessionManager?.removeSessionManagerListener(mediaSessionListener, CastSession::class.java)
        }

        currentSession?.remoteMediaClient?.unregisterCallback(castStatusCallback)
        currentSession?.remoteMediaClient?.removeProgressListener(progressListener)
        currentSession?.remoteMediaClient?.stop()
        currentSession = null

        if (isLeading && !andRelease) {
            leadingCallback?.onStopLeading(this, leadingParams)
        }
    }

    private fun getContentType(mediaType: MediaContent.Type) = when (mediaType) {
        MediaContent.Type.AUDIO -> CONTENT_TYPE_AUDIO
        MediaContent.Type.VIDEO -> CONTENT_TYPE_VIDEO
    }

    private fun getMetadataType(mediaType: MediaContent.Type) = when (mediaType) {
        MediaContent.Type.AUDIO -> MediaMetadata.MEDIA_TYPE_MUSIC_TRACK
        MediaContent.Type.VIDEO -> MediaMetadata.MEDIA_TYPE_MOVIE
    }
}