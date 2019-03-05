package ru.semper_viventem.chromecast_semple

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.android.synthetic.main.activity_main.*
import ru.semper_viventem.chromecast_semple.player.MainPlayerImpl
import ru.semper_viventem.chromecast_semple.player.MediaContent
import ru.semper_viventem.chromecast_semple.player.MediaMetadata
import ru.semper_viventem.chromecast_semple.player.Player

class MainActivity : AppCompatActivity() {

    private companion object {
        private const val FORWARD_TIME = 10_000L
        private const val REWIND_TIME = 10_000L
        private const val MEDIA_SESSION_TAG = "media_session_audio"
    }

    private var player: Player? = null
    private lateinit var mediaSession: MediaSessionCompat

    private val playerCallback = object : Player.PlayerCallback {
        override fun onPlaying(currentPosition: Long) {
            playButton.setImageResource(R.drawable.ic_pause)
        }

        override fun onPaused(currentPosition: Long) {
            playButton.setImageResource(R.drawable.ic_play)
        }

        override fun onPreparing() {
            // do nothing
        }

        override fun onPrepared() {
            // do nothing
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            // do nothing
        }

        override fun onDurationChanged(duration: Long) {
            seekBar.max = duration.toInt()
        }

        override fun onSetSpeed(speed: Float) {
            // do nothing
        }

        override fun onSeekTo(fromTimeInMillis: Long, toTimeInMillis: Long) {
            // do nothing
        }

        override fun onWaitingForNetwork() {
            // do nothing
        }

        override fun onError(error: String?) {
            // do nothing
        }

        override fun onReleased() {
            // do nothing
        }

        override fun onPlayerProgress(currentPosition: Long) {
            seekBar.progress = currentPosition.toInt()
        }

    }

    private val mediaSessionCallback = object: MediaSessionCompat.Callback() {
        override fun onPlay() {
            player!!.play()
        }

        override fun onPause() {
            player!!.pause()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initPlayer()
        initUi()

        val mediaContent = MediaContent(
            type = MediaContent.Type.AUDIO,
            contentUri = Uri.parse("http://semper-viventem.ru/danse_macabre_metal.mp3"),
            metadata = MediaMetadata(
                author = "Camille Saint-Saens",
                title = "Danse macabre",
                posterUrl = "https://images-na.ssl-images-amazon.com/images/I/61SUsgRlbqL._SX373_BO1,204,203,200_.jpg"
            )
        )

        prepare(mediaContent)
    }

    override fun onResume() {
        super.onResume()
        mediaSession.setCallback(mediaSessionCallback)
        player!!.addListener(playerCallback)
    }

    override fun onPause() {
        mediaSession.setCallback(null)
        player!!.removeListener(playerCallback)
        super.onPause()
    }

    override fun onDestroy() {
        player?.release()
        player?.removeListener(playerCallback)
        player = null
        super.onDestroy()
    }

    private fun initPlayer() {
        mediaSession = MediaSessionCompat(this, MEDIA_SESSION_TAG)

        player = MainPlayerImpl(this, mediaSession)
        CastButtonFactory.setUpMediaRouteButton(applicationContext, mediaRouterButton)
    }

    private fun initUi() {
        if (player == null) throw IllegalStateException("Player must be initialized!")

        playButton.setOnClickListener {
            if (player!!.isPlaying) {
                player!!.pause()
            } else {
                player!!.play()
            }
        }

        forwardButton.setOnClickListener {
            val newTime = player!!.positionInMillis + FORWARD_TIME
            player!!.positionInMillis = if (newTime > player!!.duration) player!!.duration else newTime
        }

        rewindButton.setOnClickListener {
            val newTime = player!!.positionInMillis - REWIND_TIME
            player!!.positionInMillis = if (newTime < 0) 0 else newTime
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player!!.positionInMillis = progress.toLong()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // do nothing
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // do nothing
            }

        })
    }

    private fun prepare(mediaContent: MediaContent) {
        if (player == null) throw IllegalStateException("Player must be initialized!")

        toolbar.title = mediaContent.metadata?.title
        toolbar.subtitle = mediaContent.metadata?.author

        Glide.with(this)
            .load(mediaContent.metadata?.posterUrl)
            .into(albumImage)

        player!!.prepare(mediaContent)
    }
}
