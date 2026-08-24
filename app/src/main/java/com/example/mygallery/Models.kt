package com.example.mygallery

import android.net.Uri

enum class MediaType { IMAGE, VIDEO }

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val dateModifiedSeconds: Long,
    val dateAddedSeconds: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val duration: Long,
    val bucketId: String?,
    val bucketName: String?,
    val type: MediaType,
    val isFavorite: Boolean = false
)

data class Album(
    val id: String,
    val name: String,
    val coverUri: Uri,
    val count: Int
)
