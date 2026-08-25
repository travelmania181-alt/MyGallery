package com.example.mygallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
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
            override fun areItemsTheSame(a: MediaItem, b: MediaItem) = a.uri == b.uri
            override fun areContentsTheSame(a: MediaItem, b: MediaItem) = a == b
        }
    }

    inner class Holder(val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MediaItem) {
            binding.thumbnail.load(item.uri) {
                crossfade(true)
                if (item.type == MediaType.VIDEO) videoFrameMillis(0)
            }
            binding.videoOverlay.visibility =
                if (item.type == MediaType.VIDEO) android.view.View.VISIBLE else android.view.View.GONE
            binding.duration.text = formatDuration(item.duration)
            binding.duration.visibility =
                if (item.type == MediaType.VIDEO) android.view.View.VISIBLE else android.view.View.GONE
            binding.favorite.visibility =
                if (item.isFavorite) android.view.View.VISIBLE else android.view.View.GONE
            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnLongClickListener { onLongClick(item); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private fun formatDuration(ms: Long): String {
        val total = ms / 1000
        return if (total >= 3600)
            String.format(Locale.US, "%d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
        else String.format(Locale.US, "%d:%02d", total / 60, total % 60)
    }
}
