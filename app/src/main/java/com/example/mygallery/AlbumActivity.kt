package com.example.mygallery

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mygallery.databinding.ActivityAlbumBinding
import kotlinx.coroutines.launch

class AlbumActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "album_id"
        const val EXTRA_NAME = "album_name"

        private const val FAVORITES_ALBUM_ID = "__favorites__"
    }

    private lateinit var binding: ActivityAlbumBinding
    private lateinit var adapter: MediaGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            v.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )

            insets
        }

        binding.toolbar.title =
            intent.getStringExtra(EXTRA_NAME)
                ?: getString(R.string.album)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        adapter = MediaGridAdapter(
            onClick = { item ->

                if (item.type == MediaType.IMAGE) {

                    val currentItems = adapter.currentList

                    val imageItems = currentItems.filter {
                        it.type == MediaType.IMAGE
                    }

                    val imageUris = ArrayList(
                        imageItems.map {
                            it.uri.toString()
                        }
                    )

                    val position = imageItems.indexOfFirst {
                        it.uri == item.uri
                    }

                    val imageIntent = Intent(
                        this,
                        ImageViewerActivity::class.java
                    )

                    imageIntent.putStringArrayListExtra(
                        ImageViewerActivity.EXTRA_IMAGES,
                        imageUris
                    )

                    imageIntent.putExtra(
                        ImageViewerActivity.EXTRA_POSITION,
                        position
                    )

                    startActivity(imageIntent)

                } else {

                    val videoIntent = Intent(
                        this,
                        VideoPlayerActivity::class.java
                    )

                    videoIntent.data = item.uri

                    startActivity(videoIntent)
                }
            },

            onLongClick = { item ->
                showActions(item)
            }
        )

        binding.recycler.layoutManager = GridLayoutManager(
            this,
            if (
                resources.configuration.smallestScreenWidthDp >= 600
            ) {
                5
            } else {
                3
            }
        )

        binding.recycler.adapter = adapter

        val id = intent.getStringExtra(EXTRA_ID)

        lifecycleScope.launch {

            val repo = MediaRepository(this@AlbumActivity)

            val items =
                if (id == FAVORITES_ALBUM_ID) {

                    val allMedia =
                        repo.images() + repo.videos()

                    allMedia.filter {
                        it.isFavorite
                    }

                } else {

                    repo.images(id) + repo.videos(id)
                }

            val sortedItems =
                items.sortedByDescending {
                    it.dateAddedSeconds
                }

            adapter.submitList(sortedItems)

            binding.emptyText.visibility =
                if (sortedItems.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    private fun showActions(item: MediaItem) {

        val isFavoritesAlbum =
            intent.getStringExtra(EXTRA_ID) == FAVORITES_ALBUM_ID

        val choices =
            if (isFavoritesAlbum) {
                arrayOf(
                    getString(R.string.share),
                    getString(R.string.remove_favorite),
                    getString(R.string.info)
                )
            } else {
                arrayOf(
                    getString(R.string.info)
                )
            }

        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(choices) { _, which ->

                if (isFavoritesAlbum) {

                    when (which) {

                        0 -> share(item)

                        1 -> removeFromFavorites(item)

                        2 -> MediaInfoDialog.show(
                            this,
                            item
                        )
                    }

                } else {

                    MediaInfoDialog.show(
                        this,
                        item
                    )
                }
            }
            .show()
    }

    private fun removeFromFavorites(item: MediaItem) {

        lifecycleScope.launch {

            val repo =
                MediaRepository(this@AlbumActivity)

            repo.toggleFavorite(item)

            val updatedItems =
                adapter.currentList.filter {
                    it.uri != item.uri
                }

            adapter.submitList(updatedItems)

            binding.emptyText.visibility =
                if (updatedItems.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    private fun share(item: MediaItem) {

        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {

                    type = item.mimeType.ifBlank {
                        "*/*"
                    }

                    putExtra(
                        Intent.EXTRA_STREAM,
                        item.uri
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                },
                getString(R.string.share)
            )
        )
    }
}
