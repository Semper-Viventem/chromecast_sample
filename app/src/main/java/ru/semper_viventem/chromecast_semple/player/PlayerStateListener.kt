package ru.semper_viventem.chromecast_semple.player

interface PlayerStateListener {

    fun onInit() {
        // do nothing
    }

    fun onPreparing(mediaContent: MediaContent) {
        // do nothing
    }

    fun onPrepared(mediaContent: MediaContent) {
        // do nothing
    }

    fun onPlay() {
        // do nothing
    }

    fun onPause() {
        // do nothing
    }

    fun onError(exception: String) {
        // do nothing
    }

    fun onNetworkWaiting() {
        // do nothing
    }

    fun onStop() {
        // do nothing
    }
}