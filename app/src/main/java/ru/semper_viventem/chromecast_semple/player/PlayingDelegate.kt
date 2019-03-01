package ru.semper_viventem.chromecast_semple.player

abstract class PlayingDelegate(
    protected val playerCallback: Player.PlayerCallback,
    var isLeading: Boolean = false
) : Player {

    fun setIsLeading(isLeading: Boolean, positionMills: Long, isPlaying: Boolean) {
        this.isLeading = isLeading

        if (isLeading) {
            onLeading(positionMills, isPlaying)
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

    /**
     * Если сеть вернулась
     */
    open fun netwarkIsRestored() {
        // do nothing
    }

    /**
     * Делегат переведен в ведущее состояние
     */
    abstract fun onLeading(positionMills: Long, isPlaying: Boolean)

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
}