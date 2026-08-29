package com.example.mygallery

import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private val onLongClick: (MediaItem) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<MediaGridAdapter.Holder>() {

    private val items = mutableListOf<MediaItem>()

    private val selectedUris = mutableSetOf<String>()

    val currentItems: List<MediaItem>
        get() = items.toList()

    val selectedItems: List<MediaItem>
        get() = items.filter {
            it.uri.toString() in selectedUris
        }

    val isSelectionMode: Boolean
        get() = selectedUris.isNotEmpty()

    inner class Holder(
        private val binding: ItemMediaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var thumbnailJob: Job? = null

        fun bind(item: MediaItem) {

            thumbnailJob?.cancel()

            binding.thumbnail.tag =
                item.uri.toString()

            binding.thumbnail.setImageDrawable(null)

            if (item.type == MediaType.VIDEO) {

                thumbnailJob =
                    CoroutineScope(
                        Dispatchers.Main
                    ).launch {

                        val bitmap =
                            withContext(Dispatchers.IO) {

                                try {

                                    binding.thumbnail
                                        .context
                                        .contentResolver
                                        .loadThumbnail(
                                            item.uri,
                                            Size(512, 512),
                                            null
                                        )

                                } catch (
                                    e: Exception
                                ) {

                                    null
                                }
                            }

                        if (
                            binding.thumbnail.tag ==
                            item.uri.toString() &&
                            bitmap != null
                        ) {

                            binding.thumbnail
                                .setImageBitmap(bitmap)
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

            val selected =
                item.uri.toString() in selectedUris

            binding.root.isSelected =
                selected

            binding.root.alpha =
                if (selected) {
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

                toggleSelection(item)

                onLongClick(item)

                true
            }
        }

        fun clear() {

            thumbnailJob?.cancel()
            thumbnailJob = null

            binding.thumbnail.tag = null
            binding.thumbnail.setImageDrawable(null)

            binding.root.alpha = 1f
            binding.root.isSelected = false
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder {

        val binding =
            ItemMediaBinding.inflate(
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

        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onViewRecycled(
        holder: Holder
    ) {

        holder.clear()

        super.onViewRecycled(holder)
    }

    /*
     * Main method used by MainActivity.
     *
     * The list is replaced immediately and RecyclerView
     * is explicitly told to redraw in the new order.
     */
    fun replaceItemsImmediately(
        newItems: List<MediaItem>
    ) {

        selectedUris.retainAll { selectedUri ->

            newItems.any {
                it.uri.toString() == selectedUri
            }
        }

        items.clear()
        items.addAll(newItems)

        notifyDataSetChanged()
    }

    fun submitList(
        newItems: List<MediaItem>
    ) {

        replaceItemsImmediately(newItems)
    }

    fun toggleSelection(
        item: MediaItem
    ) {

        val uri =
            item.uri.toString()

        if (uri in selectedUris) {

            selectedUris.remove(uri)

        } else {

            selectedUris.add(uri)
        }

        onSelectionChanged(
            selectedUris.size
        )

        val position =
            items.indexOfFirst {
                it.uri == item.uri
            }

        if (position >= 0) {

            notifyItemChanged(position)
        }
    }

    fun clearSelection() {

        if (selectedUris.isEmpty()) {
            return
        }

        selectedUris.clear()

        onSelectionChanged(0)

        notifyDataSetChanged()
    }

    fun selectItem(
        item: MediaItem
    ) {

        val uri =
            item.uri.toString()

        if (uri !in selectedUris) {

            selectedUris.add(uri)

            onSelectionChanged(
                selectedUris.size
            )

            val position =
                items.indexOfFirst {
                    it.uri == item.uri
                }

            if (position >= 0) {

                notifyItemChanged(position)
            }
        }
    }

    private fun formatDuration(
        ms: Long
    ): String {

        val totalSeconds =
            ms / 1000

        return if (
            totalSeconds >= 3600
        ) {

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
