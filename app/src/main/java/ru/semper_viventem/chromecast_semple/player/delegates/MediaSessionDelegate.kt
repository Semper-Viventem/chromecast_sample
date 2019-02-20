package ru.semper_viventem.chromecast_semple.player.delegates

import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import ru.semper_viventem.chromecast_semple.player.MediaContent
import ru.semper_viventem.chromecast_semple.player.PlayerStateListener

class MediaSessionDelegate(
    private val context: Context,
    private val mediaSessionCompat: MediaSessionCompat
): PlayerStateListener {

    private var isPlaying: Boolean = false
    private val stateBuilder: PlaybackStateCompat.Builder = PlaybackStateCompat.Builder().setActions(
        PlaybackStateCompat.ACTION_PLAY
            or PlaybackStateCompat.ACTION_PAUSE
            or PlaybackStateCompat.ACTION_PLAY_PAUSE
    )

    override fun onInit() {
        // do nothing
    }

    override fun onPreparing(mediaContent: MediaContent) {
        // do nothing
    }

    override fun onPrepared(mediaContent: MediaContent) {
        if (mediaContent.metadata == null) return

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mediaContent.metadata.title)
            .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, mediaContent.metadata.author)

        Glide.with(context)
            .asBitmap()
            .load(mediaContent.metadata.posterUrl)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                    return true
                }

                override fun onResourceReady(resource: Bitmap, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    startMediaSession(metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, resource).build())
                    return true
                }

            })
            .submit()
        startMediaSession(metadata.build())
    }

    override fun onPlay() {
        isPlaying = true
        updatePlaybackState()
    }

    override fun onPause() {
        isPlaying = false
        updatePlaybackState()
    }

    override fun onStop() {
        stopMediaSession()
    }

    override fun onError(exception: String) {
        stopMediaSession()
    }

    override fun onNetworkWaiting() {
        // do nothing
    }

    private fun startMediaSession(metadataCompat: MediaMetadataCompat) {
        mediaSessionCompat.setMetadata(metadataCompat)
        mediaSessionCompat.isActive = true
        isPlaying = false
        updatePlaybackState()
    }

    private fun updatePlaybackState() {
        val statePlaying = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSessionCompat.setPlaybackState(
            stateBuilder.setState(
                statePlaying,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1F
            ).build()
        )
    }

    private fun stopMediaSession() {
        mediaSessionCompat.setPlaybackState(
            stateBuilder.setState(
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1F
            ).build()
        )
        mediaSessionCompat.isActive = false
    }
}