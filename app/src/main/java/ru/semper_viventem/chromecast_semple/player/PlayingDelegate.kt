package ru.semper_viventem.chromecast_semple.player

abstract class PlayingDelegate : Player {

    private var isLeadingProvider: IsLeadingProvider? = null
        get() = field ?: throw IllegalStateException("Delegate must be attached to use this method!")

    protected var playerCallback: Player.PlayerCallback? = null
        get() = field ?: throw IllegalStateException("Delegate must be attached to use this method!")

    protected var isAttached: Boolean = false

    protected var leadingCallback: LeadingCallback? = null

    protected val isLeading: Boolean
        get() = isLeadingProvider?.isLeading(this)
            ?: throw IllegalStateException("Delegate must be attached to use this method!")

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

    fun attache(
        leadingCallback: LeadingCallback,
        playerCallback: Player.PlayerCallback,
        isLeadingProvider: IsLeadingProvider
    ) {
        this.leadingCallback = leadingCallback
        this.playerCallback = playerCallback
        this.isLeadingProvider = isLeadingProvider
        isAttached = true
    }

    fun detach() {
        isAttached = false
        leadingCallback = null
        playerCallback = null
        isLeadingProvider = null
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