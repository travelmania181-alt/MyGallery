package com.example.mygallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)
    val images = MutableLiveData<List<MediaItem>>(emptyList())
    val videos = MutableLiveData<List<MediaItem>>(emptyList())
    val albums = MutableLiveData<List<Album>>(emptyList())

    fun refresh() = viewModelScope.launch {
        runCatching { repository.images() }.onSuccess { images.value = it }
        runCatching { repository.videos() }.onSuccess { videos.value = it }
        runCatching { repository.albums() }.onSuccess { albums.value = it }
    }

    fun toggleFavorite(item: MediaItem, callback: (Boolean) -> Unit) = viewModelScope.launch {
        callback(repository.toggleFavorite(item))
        refresh()
    }
}
