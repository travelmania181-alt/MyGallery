package com.example.mygallery

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.mygallery.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var binding: ActivityVideoPlayerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.close.setOnClickListener { finish() }
    }
    override fun onStart() {
        super.onStart()
        player = ExoPlayer.Builder(this).build().also {
            binding.playerView.player = it
            intent.data?.let { uri -> it.setMediaItem(MediaItem.fromUri(uri)); it.prepare(); it.playWhenReady = true }
        }
    }
    override fun onStop() {
        binding.playerView.player = null
        player?.release()
        player = null
        super.onStop()
    }
}
