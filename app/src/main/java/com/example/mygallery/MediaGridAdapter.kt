package com.example.mygallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.videoFrameMillis
import com.example.mygallery.databinding.ItemMediaBinding
import java.util.Locale

class MediaGridAdapter(
    private val onClick: (MediaItem) -> Unit,
    private val onLongClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, MediaGridAdapter.Holder>(DIFF) {

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

        fun bind(item: MediaItem) {

            binding.thumbnail.load(item.uri) {
                crossfade(true)

                if (item.type == MediaType.VIDEO) {
                    videoFrameMillis(1000)
                }
            }

            binding.videoOverlay.visibility =
                if (item.type == MediaType.VIDEO) View.VISIBLE
                else View.GONE

            binding.duration.visibility =
                if (item.type == MediaType.VIDEO) View.VISIBLE
                else View.GONE

            if (item.type == MediaType.VIDEO) {
                binding.duration.text = formatDuration(item.duration)
            }

            binding.favorite.visibility =
                if (item.isFavorite) View.VISIBLE
                else View.GONE

            binding.root.setOnClickListener {
                onClick(item)
            }

            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
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
