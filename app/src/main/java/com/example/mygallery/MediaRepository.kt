package com.example.mygallery

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    private val favoriteDao = FavoritesDatabase.get(context).dao()

    suspend fun images(bucketId: String? = null): List<MediaItem> = query(MediaType.IMAGE, bucketId)
    suspend fun videos(bucketId: String? = null): List<MediaItem> = query(MediaType.VIDEO, bucketId)

    private suspend fun query(type: MediaType, bucketId: String?): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val collection = if (type == MediaType.IMAGE)
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT,
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
            )
            val selection = bucketId?.let { "${MediaStore.MediaColumns.BUCKET_ID}=?" }
            val args = bucketId?.let { arrayOf(it) }
            val favorites = favoriteDao.all().toHashSet()
            val result = mutableListOf<MediaItem>()

            resolver.query(
                collection, projection, selection, args,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { c ->
                val idIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (c.moveToNext()) {
                    val id = c.getLong(idIndex)
                    val uri = android.content.ContentUris.withAppendedId(collection, id)
                    result += MediaItem(
                        id = id,
                        uri = uri,
                        name = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)) ?: "Untitled",
                        mimeType = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)) ?: "",
                        dateModifiedSeconds = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)),
                        dateAddedSeconds = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)),
                        size = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                        width = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)),
                        height = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)),
                        duration = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)),
                        bucketId = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)),
                        bucketName = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)),
                        type = type,
                        isFavorite = uri.toString() in favorites
                    )
                }
            }
            result
        }

    suspend fun albums(): List<Album> = withContext(Dispatchers.IO) {
        val all = images() + videos()
        all.groupBy { it.bucketId ?: it.bucketName ?: "unknown" }
            .map { (id, items) ->
                val cover = items.maxByOrNull { it.dateAddedSeconds }!!
                Album(id, cover.bucketName?.ifBlank { null } ?: "Other", cover.uri, items.size)
            }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun toggleFavorite(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        if (favoriteDao.exists(item.uri.toString())) {
            favoriteDao.remove(item.uri.toString())
            false
        } else {
            favoriteDao.add(FavoriteEntity(item.uri.toString()))
            true
        }
    }
}
