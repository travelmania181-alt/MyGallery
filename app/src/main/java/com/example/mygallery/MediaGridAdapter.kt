package com.example.mygallery

import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mygallery.databinding.ItemMediaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MediaGridAdapter(
    private val onClick: (MediaItem) -> Unit,
    private val onLongClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, MediaGridAdapter.Holder>(DIFF) {

    private val selectedUris = mutableSetOf<String>()

    val selectedItems: List<MediaItem>
        get() = currentList.filter {
            it.uri.toString() in selectedUris
        }

    val isSelectionMode: Boolean
        get() = selectedUris.isNotEmpty()

    companion object {

        private val DIFF = object : DiffUtil.ItemCallback<MediaItem>() {

            override fun areItemsTheSame(
                oldItem: MediaItem,
                newItem: MediaItem
            ): Boolean {
                return oldItem.uri == newItem.uri
            }

            override fun areContentsTheSame(
                oldItem: MediaItem,
                newItem: MediaItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class Holder(
        private val binding: ItemMediaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var thumbnailJob: Job? = null

        fun bind(item: MediaItem) {

            thumbnailJob?.cancel()

            binding.thumbnail.tag = item.uri.toString()
            binding.thumbnail.setImageDrawable(null)

            if (item.type == MediaType.VIDEO) {

                thumbnailJob = CoroutineScope(
                    Dispatchers.Main
                ).launch {

                    val bitmap = withContext(Dispatchers.IO) {
                        try {
                            binding.thumbnail.context.contentResolver.loadThumbnail(
                                item.uri,
                                Size(512, 512),
                                null
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (
                        binding.thumbnail.tag == item.uri.toString() &&
                        bitmap != null
                    ) {
                        binding.thumbnail.setImageBitmap(bitmap)
                    }
                }

            } else {

                binding.thumbnail.load(item.uri) {
                    crossfade(false)
                }
            }

            binding.videoOverlay.visibility =
                if (item.type == MediaType.VIDEO) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            binding.duration.visibility =
                if (item.type == MediaType.VIDEO) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            if (item.type == MediaType.VIDEO) {
                binding.duration.text =
                    formatDuration(item.duration)
            }

            binding.favorite.visibility =
                if (item.isFavorite) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            // Show selected state
            binding.root.isSelected =
                item.uri.toString() in selectedUris

            binding.root.alpha =
                if (binding.root.isSelected) {
                    0.65f
                } else {
                    1f
                }

            binding.root.setOnClickListener {

                if (isSelectionMode) {

                    toggleSelection(item)

                } else {

                    onClick(item)
                }
            }

            binding.root.setOnLongClickListener {

                if (!isSelectionMode) {
                    toggleSelection(item)
                }

                onLongClick(item)

                true
            }
        }

        fun clear() {
            thumbnailJob?.cancel()
            thumbnailJob = null
            binding.thumbnail.tag = null
            binding.thumbnail.setImageDrawable(null)
        }
    }

    fun toggleSelection(item: MediaItem) {

        val uri = item.uri.toString()

        if (uri in selectedUris) {
            selectedUris.remove(uri)
        } else {
            selectedUris.add(uri)
        }

        notifyItemChanged(
            currentList.indexOfFirst {
                it.uri == item.uri
            }
        )
    }

    fun clearSelection() {

        if (selectedUris.isEmpty()) {
            return
        }

        selectedUris.clear()

        notifyDataSetChanged()
    }

    fun selectItem(item: MediaItem) {

        selectedUris.add(item.uri.toString())

        notifyItemChanged(
            currentList.indexOfFirst {
                it.uri == item.uri
            }
        )
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder {

        val binding = ItemMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return Holder(binding)
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: Holder) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    private fun formatDuration(ms: Long): String {

        val totalSeconds = ms / 1000

        return if (totalSeconds >= 3600) {

            String.format(
                Locale.US,
                "%d:%02d:%02d",
                totalSeconds / 3600,
                (totalSeconds % 3600) / 60,
                totalSeconds % 60
            )

        } else {

            String.format(
                Locale.US,
                "%02d:%02d",
                totalSeconds / 60,
                totalSeconds % 60
            )
        }
    }
}
