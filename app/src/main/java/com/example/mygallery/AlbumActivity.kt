package com.example.mygallery

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
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

    private var albumTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupToolbar()
        setupAdapter()
        setupRecycler()
        setupBackButton()
        loadAlbum()
    }

    // ---------------------------------------------------------
    // WINDOW INSETS
    // ---------------------------------------------------------

    private fun setupInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }

    // ---------------------------------------------------------
    // TOOLBAR
    // ---------------------------------------------------------

    private fun setupToolbar() {

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
    }

    // ---------------------------------------------------------
    // ADAPTER
    // ---------------------------------------------------------

    private fun setupAdapter() {

        adapter = MediaGridAdapter(

            onClick = { item: MediaItem ->

                openMedia(item)
            },

            onLongClick = { _: MediaItem ->
                // Selection is handled inside MediaGridAdapter.
            },

            onSelectionChanged = { count: Int ->

                if (count > 0) {

                    binding.toolbar.title =
                        "$count selected"

                } else {

                    binding.toolbar.title =
                        albumTitle
                }
            }
        )
    }

    // ---------------------------------------------------------
    // RECYCLER VIEW
    // ---------------------------------------------------------

    private fun setupRecycler() {

        val columns =
            if (
                resources.configuration
                    .smallestScreenWidthDp >= 600
            ) {
                5
            } else {
                3
            }

        binding.recycler.layoutManager =
            GridLayoutManager(
                this,
                columns
            )

        binding.recycler.adapter = adapter

        binding.recycler.itemAnimator = null
    }

    // ---------------------------------------------------------
    // BACK BUTTON
    // ---------------------------------------------------------

    private fun setupBackButton() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    if (adapter.isSelectionMode) {

                        adapter.clearSelection()

                    } else {

                        isEnabled = false

                        onBackPressedDispatcher
                            .onBackPressed()
                    }
                }
            }
        )
    }

    // ---------------------------------------------------------
    // LOAD ALBUM
    // ---------------------------------------------------------

    private fun loadAlbum() {

        val albumId =
            intent.getStringExtra(EXTRA_ID)

        lifecycleScope.launch {

            val repository =
                MediaRepository(
                    this@AlbumActivity
                )

            val albumItems: List<MediaItem> =

                if (
                    albumId ==
                    FAVORITES_ALBUM_ID
                ) {

                    val allMedia: List<MediaItem> =
                        repository.images() +
                            repository.videos()

                    allMedia.filter { media ->
                        media.isFavorite
                    }

                } else {

                    repository.images(albumId) +
                        repository.videos(albumId)
                }

            val sortedItems: List<MediaItem> =
                albumItems.sortedByDescending { media ->
                    media.dateAddedSeconds
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

    // ---------------------------------------------------------
    // OPEN MEDIA
    // ---------------------------------------------------------

    private fun openMedia(
        item: MediaItem
    ) {

        if (item.type == MediaType.IMAGE) {

            val imageItems: List<MediaItem> =
                adapter.currentList.filter { media ->
                    media.type == MediaType.IMAGE
                }

            val imageUris =
                ArrayList(
                    imageItems.map { media ->
                        media.uri.toString()
                    }
                )

            val position =
                imageItems.indexOfFirst { media ->
                    media.uri == item.uri
                }

            if (position < 0) {
                return
            }

            val intent =
                Intent(
                    this,
                    ImageViewerActivity::class.java
                )

            intent.putStringArrayListExtra(
                ImageViewerActivity.EXTRA_IMAGES,
                imageUris
            )

            intent.putExtra(
                ImageViewerActivity.EXTRA_POSITION,
                position
            )

            startActivity(intent)

        } else {

            val videoItems: List<MediaItem> =
                adapter.currentList.filter { media ->
                    media.type == MediaType.VIDEO
                }

            val videoUris =
                ArrayList(
                    videoItems.map { media ->
                        media.uri.toString()
                    }
                )

            val position =
                videoItems.indexOfFirst { media ->
                    media.uri == item.uri
                }

            if (position < 0) {
                return
            }

            val intent =
                Intent(
                    this,
                    VideoPlayerActivity::class.java
                )

            intent.putStringArrayListExtra(
                VideoPlayerActivity.EXTRA_VIDEOS,
                videoUris
            )

            intent.putExtra(
                VideoPlayerActivity.EXTRA_POSITION,
                position
            )

            startActivity(intent)
        }
    }
}
