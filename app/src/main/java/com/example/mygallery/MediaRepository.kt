package com.example.mygallery

import android.content.ContentResolver
import android.content.Context
import android.content.ContentUris
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    private val resolver: ContentResolver = context.contentResolver
    private val favoriteDao = FavoritesDatabase.get(context).dao()

    suspend fun images(
        bucketId: String? = null
    ): List<MediaItem> {
        return query(MediaType.IMAGE, bucketId)
    }

    suspend fun videos(
        bucketId: String? = null
    ): List<MediaItem> {
        return query(MediaType.VIDEO, bucketId)
    }

    private suspend fun query(
        type: MediaType,
        bucketId: String?
    ): List<MediaItem> = withContext(Dispatchers.IO) {

        val collection =
            if (type == MediaType.IMAGE) {
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            }

        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )

        if (type == MediaType.VIDEO) {
            projection.add(MediaStore.Video.Media.DURATION)
        }

        val selection = bucketId?.let {
            "${MediaStore.MediaColumns.BUCKET_ID}=?"
        }

        val selectionArgs = bucketId?.let {
            arrayOf(it)
        }

        val favorites = favoriteDao.all().toHashSet()
        val result = mutableListOf<MediaItem>()

        resolver.query(
            collection,
            projection.toTypedArray(),
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->

            val idIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns._ID
                )

            val nameIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.DISPLAY_NAME
                )

            val mimeTypeIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.MIME_TYPE
                )

            val dateModifiedIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.DATE_MODIFIED
                )

            val dateAddedIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.DATE_ADDED
                )

            val sizeIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.SIZE
                )

            val widthIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.WIDTH
                )

            val heightIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.HEIGHT
                )

            val bucketIdIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.BUCKET_ID
                )

            val bucketNameIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
                )

            val durationIndex =
                if (type == MediaType.VIDEO) {
                    cursor.getColumnIndex(
                        MediaStore.Video.Media.DURATION
                    )
                } else {
                    -1
                }

            while (cursor.moveToNext()) {

                val id = cursor.getLong(idIndex)

                val uri = ContentUris.withAppendedId(
                    collection,
                    id
                )

                val duration =
                    if (
                        type == MediaType.VIDEO &&
                        durationIndex >= 0
                    ) {
                        cursor.getLong(durationIndex)
                    } else {
                        0L
                    }

                result.add(
                    MediaItem(
                        id = id,
                        uri = uri,
                        name = cursor.getString(nameIndex)
                            ?: "Untitled",
                        mimeType = cursor.getString(mimeTypeIndex)
                            ?: "",
                        dateModifiedSeconds =
                            cursor.getLong(dateModifiedIndex),
                        dateAddedSeconds =
                            cursor.getLong(dateAddedIndex),
                        size =
                            cursor.getLong(sizeIndex),
                        width =
                            cursor.getInt(widthIndex),
                        height =
                            cursor.getInt(heightIndex),
                        duration = duration,
                        bucketId =
                            cursor.getString(bucketIdIndex),
                        bucketName =
                            cursor.getString(bucketNameIndex),
                        type = type,
                        isFavorite =
                            uri.toString() in favorites
                    )
                )
            }
        }

        result
    }

    suspend fun albums(): List<Album> =
    withContext(Dispatchers.IO) {

        val all = images() + videos()

        val favoriteItems = all.filter { it.isFavorite }

        val regularAlbums = all
            .groupBy {
                it.bucketId
                    ?: it.bucketName
                    ?: "unknown"
            }
            .map { (id, items) ->

                val cover = items.maxByOrNull {
                    it.dateAddedSeconds
                }!!

                Album(
                    id = id,
                    name = cover.bucketName
                        ?.ifBlank { null }
                        ?: "Other",
                    coverUri = cover.uri,
                    count = items.size
                )
            }
            .sortedBy {
                it.name.lowercase()
            }

        if (favoriteItems.isEmpty()) {
            regularAlbums
        } else {

            val favoriteCover =
                favoriteItems.maxByOrNull {
                    it.dateAddedSeconds
                }!!

            listOf(
                Album(
                    id = "__favorites__",
                    name = "Favorites",
                    coverUri = favoriteCover.uri,
                    count = favoriteItems.size
                )
            ) + regularAlbums
        }
    }
    suspend fun toggleFavorite(
        item: MediaItem
    ): Boolean = withContext(Dispatchers.IO) {

        if (
            favoriteDao.exists(
                item.uri.toString()
            )
        ) {

            favoriteDao.remove(
                item.uri.toString()
            )

            false

        } else {

            favoriteDao.add(
                FavoriteEntity(
                    item.uri.toString()
                )
            )

            true
        }
    }
}
