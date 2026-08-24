package com.example.mygallery

import android.os.Bundle
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
    }
    private lateinit var binding: ActivityAlbumBinding
    private lateinit var adapter: MediaGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, i ->
            val b = i.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(b.left, b.top, b.right, b.bottom); i
        }
        binding.toolbar.title = intent.getStringExtra(EXTRA_NAME) ?: getString(R.string.album)
        binding.toolbar.setNavigationOnClickListener { finish() }
        adapter = MediaGridAdapter(
            { item -> startActivity(android.content.Intent(this, if (item.type == MediaType.IMAGE) ImageViewerActivity::class.java else VideoPlayerActivity::class.java).setData(item.uri)) },
            { MediaInfoDialog.show(this, it) }
        )
        binding.recycler.layoutManager = GridLayoutManager(this, if (resources.configuration.smallestScreenWidthDp >= 600) 5 else 3)
        binding.recycler.adapter = adapter
        val id = intent.getStringExtra(EXTRA_ID)
        lifecycleScope.launch {
            val repo = MediaRepository(this@AlbumActivity)
            val items = repo.images(id) + repo.videos(id)
            adapter.submitList(items.sortedByDescending { it.dateAddedSeconds })
            binding.emptyText.visibility = if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
}
