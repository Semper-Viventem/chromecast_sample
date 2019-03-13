package ru.semper_viventem.chromecast_semple.player

abstract class PlayingDelegate(
    protected val playerCallback: Player.PlayerCallback,
    private val isLeadingProvider: IsLeadingProvider
) : Player {

    protected var leadingCallback: LeadingCallback? = null
    protected val isLeading: Boolean get() = isLeadingProvider.isLeading(this)

    fun setIsLeading(isLeading: Boolean, leadingParams: LeadingParams? = null) {
        if (isLeading) {
            onLeading(leadingParams)
        } else {
            onIdle()
        }
    }

    final override fun addListener(listener: Player.PlayerCallback) {
        // do nothing
    }

    final override fun removeListener(listener: Player.PlayerCallback): Boolean {
        return false
    }

    final override fun getListeners(): MutableSet<Player.PlayerCallback> {
        return mutableSetOf()
    }

    fun setOnLeadingCallback(leadingCallback: LeadingCallback?) {
        this.leadingCallback = leadingCallback
    }

    /**
     * Если сеть вернулась
     */
    open fun networkIsRestored() {
        // do nothing
    }

    /**
     * Делегат переведен в ведущее состояние
     */
    abstract fun onLeading(leadingParams: LeadingParams?)

    /**
     * Делегат переведен в состояние бездействия
     */
    abstract fun onIdle()

    /**
     * Вызывается на этапе инициализации плеера.
     * Если делегат готов к ведению воспроизведения,
     * то плеер может передать эту ответственность ему.
     */
    abstract fun readyForLeading(): Boolean

    data class LeadingParams(
        val mediaContent: MediaContent,
        val positionMills: Long,
        val duration: Long,
        val isPlaying: Boolean,
        val speed: Float,
        val volume: Float
    )

    interface LeadingCallback {

        fun onStartLeading(delegate: PlayingDelegate)

        fun onStopLeading(delegate: PlayingDelegate, leadingParams: LeadingParams)
    }

    interface IsLeadingProvider {
        fun isLeading(playingDelegate: PlayingDelegate): Boolean
    }
}