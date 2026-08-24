package com.example.mygallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mygallery.databinding.ItemAlbumBinding

class AlbumAdapter(private val onClick: (Album) -> Unit) :
    ListAdapter<Album, AlbumAdapter.Holder>(DIFF) {
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Album>() {
            override fun areItemsTheSame(a: Album, b: Album) = a.id == b.id
            override fun areContentsTheSame(a: Album, b: Album) = a == b
        }
    }
    inner class Holder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(album: Album) {
            binding.cover.load(album.coverUri) { crossfade(true) }
            binding.title.text = album.name
            binding.count.text = binding.root.context.getString(R.string.items_count, album.count)
            binding.root.setOnClickListener { onClick(album) }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))
}
