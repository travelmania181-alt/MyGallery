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

            val intent = Intent(
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

            val intent = Intent(
                this,
                VideoPlayerActivity::class.java
            )

            intent.data = item.uri

            startActivity(intent)
        }
    },
    onLongClick = { item ->
        MediaInfoDialog.show(this, item)
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
}
