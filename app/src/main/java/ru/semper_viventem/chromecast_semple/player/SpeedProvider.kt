package ru.semper_viventem.chromecast_semple.player

object SpeedProvider {

    private val values = listOf(1F, 1.25F, 1.5F, 1.75F)

    fun default() = values.first()

    fun nextOf(current: Float): Float {
        return values[(values.indexOf(current) + 1) % values.size]
    }
}