package ru.semper_viventem.playerdelegates

import android.net.Uri

data class MediaContent(
    val type: Type,
    val contentUris: List<Uri>,
    val metadata: MediaMetadata? = null
) {
    enum class Type {
        VIDEO,
        AUDIO
    }
}

data class MediaMetadata(
    val author: String = "",
    val posterUrl: String = "",
    val title: String = ""
)