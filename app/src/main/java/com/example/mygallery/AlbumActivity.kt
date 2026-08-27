package com.example.mygallery

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
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

    private var albumTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { v, insets ->

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

        albumTitle =
            intent.getStringExtra(EXTRA_NAME)
                ?: getString(R.string.album)

        binding.toolbar.title = albumTitle

        binding.toolbar.setNavigationOnClickListener {

            if (adapter.isSelectionMode) {

                adapter.clearSelection()

            } else {

                finish()
            }
        }

        adapter = MediaGridAdapter(

            onClick = { item ->

                if (item.type == MediaType.IMAGE) {

                    // Get all images currently shown in this album
                    val imageItems =
                        adapter.currentList.filter {
                            it.type == MediaType.IMAGE
                        }

                    val imageUris = ArrayList(
                        imageItems.map {
                            it.uri.toString()
                        }
                    )

                    val position =
                        imageItems.indexOfFirst {
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

                    // Get all videos currently shown in this album
                    val videoItems =
                        adapter.currentList.filter {
                            it.type == MediaType.VIDEO
                        }

                    val videoUris = ArrayList(
                        videoItems.map {
                            it.uri.toString()
                        }
                    )

                    val position =
                        videoItems.indexOfFirst {
                            it.uri == item.uri
                        }

                    val videoIntent = Intent(
                        this,
                        VideoPlayerActivity::class.java
                    )

                    videoIntent.putStringArrayListExtra(
                        VideoPlayerActivity.EXTRA_VIDEOS,
                        videoUris
                    )

                    videoIntent.putExtra(
                        VideoPlayerActivity.EXTRA_POSITION,
                        position
                    )

                    startActivity(videoIntent)
                }
            },

            // Long press is handled by MediaGridAdapter
            // to start multi-selection.
            onLongClick = { },

            // Update the toolbar when selection changes
            onSelectionChanged = { count ->

                if (count > 0) {

                    binding.toolbar.title =
                        "$count selected"

                } else {

                    binding.toolbar.title =
                        albumTitle
                }
            }
        )

        binding.recycler.layoutManager =
            GridLayoutManager(
                this,
                if (
                    resources.configuration
                        .smallestScreenWidthDp >= 600
                ) {
                    5
                } else {
                    3
                }
            )

        binding.recycler.adapter = adapter

        val id =
            intent.getStringExtra(EXTRA_ID)

        lifecycleScope.launch {

            val repo =
                MediaRepository(this@AlbumActivity)

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

            adapter.submitList(
                ArrayList(sortedItems)
            )

            binding.emptyText.visibility =
                if (sortedItems.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    override fun onBackPressed() {

        if (
            ::adapter.isInitialized &&
            adapter.isSelectionMode
        ) {

            adapter.clearSelection()

        } else {

            super.onBackPressed()
        }
    }
}
