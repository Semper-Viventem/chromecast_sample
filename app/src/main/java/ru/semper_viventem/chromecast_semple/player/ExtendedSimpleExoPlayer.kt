package ru.semper_viventem.chromecast_semple.player

import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.TrackSelector

class ExtendedSimpleExoPlayer constructor(
    renderersFactory: RenderersFactory,
    trackSelector: TrackSelector,
    loadControl: LoadControl
) : SimpleExoPlayer(renderersFactory, trackSelector, loadControl) {

    private val seekListeners = mutableSetOf<SeekListener>()

    override fun seekTo(positionMs: Long) {
        val realTargetTime = getTargetSeekTime(positionMs)
        seekListeners.forEach { it.onSeek(currentPosition, realTargetTime) }
        super.seekTo(positionMs)
    }

    override fun seekTo(windowIndex: Int, positionMs: Long) {
        val realTargetTime = getTargetSeekTime(positionMs)
        if (currentWindowIndex == windowIndex && currentPosition != realTargetTime) { // notify listeners for seek inside the same track
            seekListeners.forEach { it.onSeek(currentPosition, realTargetTime) }
        }
        super.seekTo(windowIndex, positionMs)
    }

    fun addSeekListener(listener: SeekListener): Boolean {
        return seekListeners.add(listener)
    }

    fun removeSeekListener(listener: SeekListener): Boolean {
        return seekListeners.remove(listener)
    }

    /**
     * Return real target seek time
     */
    private fun getTargetSeekTime(positionMs: Long): Long {
        return if (positionMs < duration) {
            positionMs
        } else {
            duration
        }
    }

    interface SeekListener {
        fun onSeek(fromTimeInMillis: Long, toTimeInMillis: Long)
    }
}