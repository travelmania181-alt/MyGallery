package com.example.mygallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
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

    companion object {
        private const val DELETE_SINGLE_REQUEST = 42
        private const val DELETE_MULTIPLE_REQUEST = 43
    }

    private lateinit var binding: ActivityMainBinding

    private val vm: GalleryViewModel by viewModels()

    private lateinit var photoAdapter: MediaGridAdapter
    private lateinit var videoAdapter: MediaGridAdapter
    private lateinit var albumAdapter: AlbumAdapter

    private lateinit var photoLayoutManager: GridLayoutManager
    private lateinit var videoLayoutManager: GridLayoutManager
    private lateinit var albumLayoutManager: GridLayoutManager

    private var currentTab = R.id.photos

    private var photoSortMode = SortMode.NEWEST
    private var videoSortMode = SortMode.NEWEST

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
                0
            )

            insets
        }

        setupAdapters()
        setupSelectionToolbar()
        setupRecycler()
        setupBottomNavigation()
        setupPermissionButtons()
        setupSwipeRefresh()
        setupObservers()
        setupBackButton()

        updatePermissionUi()
    }

    private fun setupAdapters() {

        photoAdapter = MediaGridAdapter(
            onClick = ::openMedia,
            onLongClick = {},
            onSelectionChanged = { count ->

                if (currentTab == R.id.photos) {
                    updateSelectionUi(count)
                }
            }
        )

        videoAdapter = MediaGridAdapter(
            onClick = ::openMedia,
            onLongClick = {},
            onSelectionChanged = { count ->

                if (currentTab == R.id.videos) {
                    updateSelectionUi(count)
                }
            }
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
    }

    private fun setupSelectionToolbar() {

        binding.selectionToolbar.setNavigationOnClickListener {

            getCurrentMediaAdapter()
                ?.clearSelection()
        }

        binding.selectionToolbar.setOnMenuItemClickListener { item ->

            when (item.itemId) {

                R.id.action_share_selected -> {
                    shareSelectedMedia()
                    true
                }

                R.id.action_favorite_selected -> {
                    toggleSelectedFavorites()
                    true
                }

                R.id.action_delete_selected -> {
                    deleteSelectedMedia()
                    true
                }

                else -> false
            }
        }
    }

    private fun setupRecycler() {

        binding.recycler.itemAnimator = null

        photoLayoutManager =
            GridLayoutManager(
                this,
                calculateColumns()
            )

        videoLayoutManager =
            GridLayoutManager(
                this,
                calculateColumns()
            )

        albumLayoutManager =
            GridLayoutManager(
                this,
                if (
                    resources.configuration.smallestScreenWidthDp >= 600
                ) {
                    3
                } else {
                    2
                }
            )
    }

    private fun setupBottomNavigation() {

        binding.bottomNav.setOnItemSelectedListener { menuItem ->

            clearAllSelections()

            currentTab = menuItem.itemId

            updateSelectionUi(0)

            renderTab()

            true
        }
    }

    private fun setupPermissionButtons() {

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
    }

    private fun setupSwipeRefresh() {

        binding.swipeRefresh.setOnRefreshListener {

            vm.refresh()

            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupObservers() {

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
    }

    private fun setupBackButton() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    val adapter =
                        getCurrentMediaAdapter()

                    if (
                        adapter != null &&
                        adapter.isSelectionMode
                    ) {

                        adapter.clearSelection()

                    } else {

                        isEnabled = false

                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()

        if (hasMediaPermission()) {
            vm.refresh()
        }
    }

    private fun renderTab() {

        updateSelectionUi(0)

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

            R.id.photos ->
                submitPhotos(
                    vm.images.value.orEmpty()
                )

            R.id.videos ->
                submitVideos(
                    vm.videos.value.orEmpty()
                )

            R.id.albums ->
                submitAlbums(
                    vm.albums.value.orEmpty()
                )
        }
    }

    private fun submitPhotos(
        items: List<MediaItem>
    ) {

        binding.recycler.adapter = photoAdapter
        binding.recycler.layoutManager =
            photoLayoutManager

        val photos =
            items.filter {
                it.type == MediaType.IMAGE
            }

        photoAdapter.submitList(
            ArrayList(
                sortMedia(
                    photos,
                    photoSortMode
                )
            )
        )

        binding.emptyGroup.visibility =
            if (photos.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun submitVideos(
        items: List<MediaItem>
    ) {

        binding.recycler.adapter = videoAdapter
        binding.recycler.layoutManager =
            videoLayoutManager

        val videos =
            items.filter {
                it.type == MediaType.VIDEO
            }

        videoAdapter.submitList(
            ArrayList(
                sortMedia(
                    videos,
                    videoSortMode
                )
            )
        )

        binding.emptyGroup.visibility =
            if (videos.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun submitAlbums(
        items: List<Album>
    ) {

        binding.recycler.adapter = albumAdapter
        binding.recycler.layoutManager =
            albumLayoutManager

        albumAdapter.submitList(
            ArrayList(items)
        )

        binding.emptyGroup.visibility =
            if (items.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun getCurrentMediaAdapter(): MediaGridAdapter? {

        return when (currentTab) {

            R.id.photos -> photoAdapter

            R.id.videos -> videoAdapter

            else -> null
        }
    }

    private fun clearAllSelections() {

        if (::photoAdapter.isInitialized) {
            photoAdapter.clearSelection()
        }

        if (::videoAdapter.isInitialized) {
            videoAdapter.clearSelection()
        }
    }

    private fun updateSelectionUi(count: Int) {

        if (
            count > 0 &&
            (
                currentTab == R.id.photos ||
                    currentTab == R.id.videos
                )
        ) {

            binding.toolbar.visibility =
                View.GONE

            binding.selectionToolbar.visibility =
                View.VISIBLE

            binding.selectionToolbar.title =
                "$count selected"

            updateFavoriteMenuTitle()

        } else {

            binding.selectionToolbar.visibility =
                View.GONE

            binding.toolbar.visibility =
                View.VISIBLE

            binding.toolbar.title =
                getString(R.string.app_name)
        }
    }

    private fun updateFavoriteMenuTitle() {

        val adapter =
            getCurrentMediaAdapter()
                ?: return

        val selectedItems =
            adapter.selectedItems

        if (selectedItems.isEmpty()) {
            return
        }

        val allFavorites =
            selectedItems.all {
                it.isFavorite
            }

        val menuItem =
            binding.selectionToolbar.menu.findItem(
                R.id.action_favorite_selected
            )

        if (allFavorites) {

            menuItem.title =
                getString(
                    R.string.remove_favorite
                )

        } else {

            menuItem.title =
                getString(
                    R.string.add_favorite
                )
        }
    }

    private fun shareSelectedMedia() {

        val adapter =
            getCurrentMediaAdapter()
                ?: return

        val selectedItems =
            adapter.selectedItems

        if (selectedItems.isEmpty()) {
            return
        }

        val uris =
            ArrayList<Uri>()

        selectedItems.forEach { item ->
            uris.add(item.uri)
        }

        val shareIntent =
            if (uris.size == 1) {

                Intent(
                    Intent.ACTION_SEND
                ).apply {

                    type =
                        selectedItems.first()
                            .mimeType
                            .ifBlank {
                                "*/*"
                            }

                    putExtra(
                        Intent.EXTRA_STREAM,
                        uris.first()
                    )
                }

            } else {

                Intent(
                    Intent.ACTION_SEND_MULTIPLE
                ).apply {

                    type = "*/*"

                    putParcelableArrayListExtra(
                        Intent.EXTRA_STREAM,
                        uris
                    )
                }
            }

        shareIntent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        startActivity(
            Intent.createChooser(
                shareIntent,
                getString(R.string.share)
            )
        )
    }

    private fun toggleSelectedFavorites() {

        val adapter =
            getCurrentMediaAdapter()
                ?: return

        val selectedItems =
            adapter.selectedItems.toList()

        if (selectedItems.isEmpty()) {
            return
        }

        val allFavorites =
            selectedItems.all {
                it.isFavorite
            }

        selectedItems.forEach { item ->

            if (allFavorites) {

                if (item.isFavorite) {
                    vm.toggleFavorite(item) {}
                }

            } else {

                if (!item.isFavorite) {
                    vm.toggleFavorite(item) {}
                }
            }
        }

        adapter.clearSelection()

        vm.refresh()
    }

    private fun deleteSelectedMedia() {

        val adapter =
            getCurrentMediaAdapter()
                ?: return

        val selectedItems =
            adapter.selectedItems.toList()

        if (selectedItems.isEmpty()) {
            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                "Delete ${selectedItems.size} items?"
            )
            .setMessage(
                "These items will be moved to the trash."
            )
            .setNegativeButton(
                R.string.cancel,
                null
            )
            .setPositiveButton(
                R.string.delete
            ) { _, _ ->

                deleteMediaItems(
                    selectedItems
                )

                adapter.clearSelection()
            }
            .show()
    }

    private fun deleteMediaItems(
        items: List<MediaItem>
    ) {

        runCatching {

            val uris =
                items.map {
                    it.uri
                }

            if (Build.VERSION.SDK_INT >= 30) {

                startIntentSenderForResult(
                    MediaStore.createDeleteRequest(
                        contentResolver,
                        uris
                    ).intentSender,
                    DELETE_MULTIPLE_REQUEST,
                    null,
                    0,
                    0,
                    0
                )

            } else {

                items.forEach { item ->

                    contentResolver.delete(
                        item.uri,
                        null,
                        null
                    )
                }

                vm.refresh()
            }

        }.onFailure {

            Snackbar.make(
                binding.root,
                R.string.delete_failed,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    // -------------------------
    // SORT
    // -------------------------

    private fun showSortDialog() {

        val options = arrayOf(
            "Newest first",
            "Oldest first",
            "Name A–Z",
            "Name Z–A"
        )

        val currentSortMode =
            when (currentTab) {

                R.id.photos ->
                    photoSortMode

                R.id.videos ->
                    videoSortMode

                else ->
                    return
            }

        val selected =
            when (currentSortMode) {

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

                when (currentTab) {

                    R.id.photos -> {

                        photoSortMode =
                            when (which) {

                                0 -> SortMode.NEWEST
                                1 -> SortMode.OLDEST
                                2 -> SortMode.NAME_ASC
                                else -> SortMode.NAME_DESC
                            }

                        submitPhotos(
                            vm.images.value.orEmpty()
                        )
                    }

                    R.id.videos -> {

                        videoSortMode =
                            when (which) {

                                0 -> SortMode.NEWEST
                                1 -> SortMode.OLDEST
                                2 -> SortMode.NAME_ASC
                                else -> SortMode.NAME_DESC
                            }

                        submitVideos(
                            vm.videos.value.orEmpty()
                        )
                    }
                }

                dialog.dismiss()
            }
            .show()
    }

    private fun sortMedia(
        items: List<MediaItem>,
        sortMode: SortMode
    ): List<MediaItem> {

        return when (sortMode) {

            SortMode.NEWEST ->

                items.sortedByDescending {
                    it.dateAddedSeconds
                }

            SortMode.OLDEST ->

                items.sortedBy {
                    it.dateAddedSeconds
                }

            SortMode.NAME_ASC ->

                items.sortedBy {
                    it.name.lowercase()
                }

            SortMode.NAME_DESC ->

                items.sortedByDescending {
                    it.name.lowercase()
                }
        }
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

    private fun hasMediaPermission(): Boolean {

        return if (Build.VERSION.SDK_INT >= 33) {

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

        val granted =
            hasMediaPermission()

        binding.permissionGroup.visibility =
            if (granted) {
                View.GONE
            } else {
                View.VISIBLE
            }

        binding.contentGroup.visibility =
            if (granted) {
                View.VISIBLE
            } else {
                View.GONE
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
                        .filter {
                            it.type == MediaType.IMAGE
                        },
                    photoSortMode
                )

            val imageUris =
                ArrayList(
                    images.map {
                        it.uri.toString()
                    }
                )

            val position =
                images.indexOfFirst {
                    it.uri == item.uri
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

            val videos =
                sortMedia(
                    vm.videos.value.orEmpty()
                        .filter {
                            it.type == MediaType.VIDEO
                        },
                    videoSortMode
                )

            val videoUris =
                ArrayList(
                    videos.map {
                        it.uri.toString()
                    }
                )

            val position =
                videos.indexOfFirst {
                    it.uri == item.uri
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
                            MediaStore.createDeleteRequest(
                                contentResolver,
                                listOf(item.uri)
                            ).intentSender,
                            DELETE_SINGLE_REQUEST,
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

                        vm.refresh()
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

        if (
            requestCode == DELETE_SINGLE_REQUEST ||
            requestCode == DELETE_MULTIPLE_REQUEST
        ) {

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
