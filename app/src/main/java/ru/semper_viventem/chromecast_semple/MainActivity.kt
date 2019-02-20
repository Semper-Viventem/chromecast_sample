package ru.semper_viventem.chromecast_semple

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import ru.semper_viventem.chromecast_semple.player.MainPlayerImpl
import ru.semper_viventem.chromecast_semple.player.MediaContent
import ru.semper_viventem.chromecast_semple.player.MediaMetadata
import ru.semper_viventem.chromecast_semple.player.Player

class MainActivity : AppCompatActivity() {

    private companion object {
        private const val FORWARD_TIME = 10_000L
        private const val REWIND_TIME = 10_000L
    }

    private lateinit var player: Player

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
            // do nothing
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
            // do nothing
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initPlayer()
        initUi()

        val mediaContent = MediaContent(
            type = MediaContent.Type.AUDIO,
            contentUri = Uri.parse("https://www.youtube.com/audiolibrary_download?vid=dd349ceb26f97c13"),
            metadata = MediaMetadata(
                author = "Dan Lebowitz",
                title = "March On",
                posterUrl = "https://jacobsmedia.com/wp-content/uploads/2016/05/rock-n-roll.jpg"
            )
        )

        prepare(mediaContent)
    }

    private fun initPlayer() {
        player = MainPlayerImpl(this)
        player.addListener(playerCallback)
    }

    private fun initUi() {
        playButton.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }

        forwardButton.setOnClickListener {
            val newTime = player.positionInMillis + FORWARD_TIME
            player.positionInMillis = if (newTime > player.duration) player.duration else newTime
        }

        rewindButton.setOnClickListener {
            val newTime = player.positionInMillis - REWIND_TIME
            player.positionInMillis = if (newTime < 0) 0 else newTime
        }
    }

    private fun prepare(mediaContent: MediaContent) {

        toolbar.title = mediaContent.metadata?.title
        toolbar.subtitle = mediaContent.metadata?.author

        Glide.with(this)
            .load(mediaContent.metadata?.posterUrl)
            .into(albumImage)

        player.prepare(mediaContent)
    }
}
