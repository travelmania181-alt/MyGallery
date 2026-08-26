package com.example.mygallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mygallery.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val vm: GalleryViewModel by viewModels()

    // Separate adapters: this prevents Photos and Videos from mixing.
    private lateinit var photoAdapter: MediaGridAdapter
    private lateinit var videoAdapter: MediaGridAdapter
    private lateinit var albumAdapter: AlbumAdapter

    private var currentTab = R.id.photos

    private lateinit var photoLayoutManager: GridLayoutManager
    private lateinit var videoLayoutManager: GridLayoutManager
    private lateinit var albumLayoutManager: GridLayoutManager

    private var sortMode = SortMode.NEWEST

    private enum class SortMode {
        NEWEST,
        OLDEST,
        NAME_ASC,
        NAME_DESC
    }

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            updatePermissionUi()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            v.setPadding(
                bars.left,
                bars.top,
                bars.right,
                0
            )

            insets
        }

        // Photos adapter
        photoAdapter = MediaGridAdapter(
            onClick = ::openMedia,
            onLongClick = ::showActions
        )

        // Videos adapter
        videoAdapter = MediaGridAdapter(
            onClick = ::openMedia,
            onLongClick = ::showActions
        )

        albumAdapter = AlbumAdapter { album ->
            startActivity(
                Intent(
                    this,
                    AlbumActivity::class.java
                ).apply {
                    putExtra(
                        AlbumActivity.EXTRA_ID,
                        album.id
                    )

                    putExtra(
                        AlbumActivity.EXTRA_NAME,
                        album.name
                    )
                }
            )
        }

        binding.recycler.itemAnimator = null

        photoLayoutManager = GridLayoutManager(
            this,
            calculateColumns()
        )

        videoLayoutManager = GridLayoutManager(
            this,
            calculateColumns()
        )

        albumLayoutManager = GridLayoutManager(
            this,
            if (
                resources.configuration.smallestScreenWidthDp >= 600
            ) {
                3
            } else {
                2
            }
        )

        binding.bottomNav.setOnItemSelectedListener { menuItem ->

            currentTab = menuItem.itemId

            renderTab()

            true
        }

        binding.retryPermission.setOnClickListener {
            requestMediaPermission()
        }

        binding.openSettings.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts(
                        "package",
                        packageName,
                        null
                    )
                )
            )
        }

        binding.swipeRefresh.setOnRefreshListener {
            vm.refresh()
            binding.swipeRefresh.isRefreshing = false
        }

        vm.images.observe(this) { items ->
            if (currentTab == R.id.photos) {
                submitPhotos(items)
            }
        }

        vm.videos.observe(this) { items ->
            if (currentTab == R.id.videos) {
                submitVideos(items)
            }
        }

        vm.albums.observe(this) { items ->
            if (currentTab == R.id.albums) {
                submitAlbums(items)
            }
        }

        updatePermissionUi()
    }

    override fun onResume() {
        super.onResume()

        if (hasMediaPermission()) {
            vm.refresh()
        }
    }

    private fun renderTab() {

        binding.emptyText.text =
            when (currentTab) {

                R.id.photos ->
                    getString(R.string.no_photos)

                R.id.videos ->
                    getString(R.string.no_videos)

                else ->
                    getString(R.string.no_albums)
            }

        when (currentTab) {

            R.id.photos -> {
                submitPhotos(
                    vm.images.value.orEmpty()
                )
            }

            R.id.videos -> {
                submitVideos(
                    vm.videos.value.orEmpty()
                )
            }

            R.id.albums -> {
                submitAlbums(
                    vm.albums.value.orEmpty()
                )
            }
        }
    }

    private fun submitPhotos(
        items: List<MediaItem>
    ) {

        if (binding.recycler.adapter !== photoAdapter) {
            binding.recycler.adapter = photoAdapter
        }

        if (
            binding.recycler.layoutManager !== photoLayoutManager
        ) {
            binding.recycler.layoutManager =
                photoLayoutManager
        }

        val photos = items
            .filter {
                it.type == MediaType.IMAGE
            }

        photoAdapter.submitList(
            ArrayList(sortMedia(photos))
        )

        binding.emptyGroup.visibility =
            if (photos.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
    }

    private fun submitVideos(
        items: List<MediaItem>
    ) {

        if (binding.recycler.adapter !== videoAdapter) {
            binding.recycler.adapter = videoAdapter
        }

        if (
            binding.recycler.layoutManager !== videoLayoutManager
        ) {
            binding.recycler.layoutManager =
                videoLayoutManager
        }

        val videos = items
            .filter {
                it.type == MediaType.VIDEO
            }

        videoAdapter.submitList(
            ArrayList(sortMedia(videos))
        )

        binding.emptyGroup.visibility =
            if (videos.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
    }

    private fun sortMedia(
    items: List<MediaItem>
): List<MediaItem> {

    val result = when (sortMode) {

        SortMode.NEWEST ->
            items.sortedByDescending {
                it.dateAddedSeconds
            }

        SortMode.OLDEST ->
            items.sortedBy {
                it.dateAddedSeconds
            }

        SortMode.NAME_ASC ->
            items.sortedWith(
                compareBy<MediaItem> {
                    it.name.lowercase()
                }
            )

        SortMode.NAME_DESC ->
            items.sortedWith(
                compareByDescending<MediaItem> {
                    it.name.lowercase()
                }
            )
    }

    return result.toList()
}

    private fun submitAlbums(
        items: List<Album>
    ) {

        if (
            binding.recycler.adapter !== albumAdapter
        ) {
            binding.recycler.adapter = albumAdapter
        }

        if (
            binding.recycler.layoutManager !== albumLayoutManager
        ) {
            binding.recycler.layoutManager =
                albumLayoutManager
        }

        albumAdapter.submitList(items)

        binding.emptyGroup.visibility =
            if (items.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
    }

    private fun showSortDialog() {

    val options = arrayOf(
        "Newest first",
        "Oldest first",
        "Name A–Z",
        "Name Z–A"
    )

    val selected = when (sortMode) {
        SortMode.NEWEST -> 0
        SortMode.OLDEST -> 1
        SortMode.NAME_ASC -> 2
        SortMode.NAME_DESC -> 3
    }

    AlertDialog.Builder(this)
        .setTitle("Sort by")
        .setSingleChoiceItems(
            options,
            selected
        ) { dialog, which ->

            sortMode = when (which) {
                0 -> SortMode.NEWEST
                1 -> SortMode.OLDEST
                2 -> SortMode.NAME_ASC
                else -> SortMode.NAME_DESC
            }

            dialog.dismiss()

            if (currentTab == R.id.photos) {

                val sortedItems =
                    sortMedia(
                        vm.images.value.orEmpty()
                            .filter {
                                it.type == MediaType.IMAGE
                            }
                    )

                photoAdapter.submitList(
                    null
                )

                photoAdapter.submitList(
                    ArrayList(sortedItems)
                )

            } else if (currentTab == R.id.videos) {

                val sortedItems =
                    sortMedia(
                        vm.videos.value.orEmpty()
                            .filter {
                                it.type == MediaType.VIDEO
                            }
                    )

                videoAdapter.submitList(
                    null
                )

                videoAdapter.submitList(
                    ArrayList(sortedItems)
                )
            }
        }
        .show()
}

    private fun calculateColumns(): Int {

        val widthDp =
            resources.displayMetrics.widthPixels /
                resources.displayMetrics.density

        return when {
            widthDp >= 900 -> 6
            widthDp >= 700 -> 5
            widthDp >= 500 -> 4
            else -> 3
        }
    }

    private fun hasMediaPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED ||

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED

        } else {

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestMediaPermission() {

        val permissions =
            if (Build.VERSION.SDK_INT >= 33) {

                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )

            } else {

                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

        permissionLauncher.launch(permissions)
    }

    private fun updatePermissionUi() {

        val granted = hasMediaPermission()

        binding.permissionGroup.visibility =
            if (granted) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }

        binding.contentGroup.visibility =
            if (granted) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

        if (granted) {
            vm.refresh()
        }
    }

    private fun openMedia(
        item: MediaItem
    ) {

        if (item.type == MediaType.IMAGE) {

            val images =
                sortMedia(
                    vm.images.value.orEmpty()
                )

            val imageUris = ArrayList(
                images.map {
                    it.uri.toString()
                }
            )

            val position =
                images.indexOfFirst {
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
    }

    private fun showActions(
        item: MediaItem
    ) {

        val choices = arrayOf(
            getString(R.string.share),
            if (item.isFavorite) {
                getString(R.string.remove_favorite)
            } else {
                getString(R.string.add_favorite)
            },
            getString(R.string.info),
            getString(R.string.delete)
        )

        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(choices) { _, which ->

                when (which) {
                    0 -> share(item)

                    1 -> vm.toggleFavorite(item) {
                        renderTab()
                    }

                    2 -> MediaInfoDialog.show(
                        this,
                        item
                    )

                    3 -> delete(item)
                }
            }
            .show()
    }

    private fun share(
        item: MediaItem
    ) {

        startActivity(
            Intent.createChooser(
                Intent(
                    Intent.ACTION_SEND
                ).apply {

                    type =
                        item.mimeType.ifBlank {
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

    private fun delete(
        item: MediaItem
    ) {

        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(
                getString(
                    R.string.delete_confirm,
                    item.name
                )
            )
            .setNegativeButton(
                R.string.cancel,
                null
            )
            .setPositiveButton(
                R.string.delete
            ) { _, _ ->

                runCatching {

                    if (Build.VERSION.SDK_INT >= 30) {

                        startIntentSenderForResult(
                            android.provider.MediaStore
                                .createDeleteRequest(
                                    contentResolver,
                                    listOf(item.uri)
                                )
                                .intentSender,
                            42,
                            null,
                            0,
                            0,
                            0
                        )

                    } else {

                        contentResolver.delete(
                            item.uri,
                            null,
                            null
                        )
                    }

                }.onFailure {

                    Snackbar.make(
                        binding.root,
                        R.string.delete_failed,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == 42) {
            vm.refresh()
        }
    }

    override fun onCreateOptionsMenu(
        menu: Menu
    ): Boolean {

        menuInflater.inflate(
            R.menu.main_menu,
            menu
        )

        return true
    }

    override fun onOptionsItemSelected(
        item: MenuItem
    ): Boolean {

        when (item.itemId) {

            R.id.search -> {

                SearchDialog.show(
                    this,
                    vm.images.value.orEmpty() +
                        vm.videos.value.orEmpty(),
                    ::openMedia
                )

                return true
            }

            R.id.sort -> {

                if (
                    currentTab == R.id.photos ||
                    currentTab == R.id.videos
                ) {
                    showSortDialog()
                }

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
