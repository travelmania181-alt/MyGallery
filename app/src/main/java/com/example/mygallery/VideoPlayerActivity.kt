package com.example.mygallery

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.mygallery.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null

    private lateinit var binding:
        ActivityVideoPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding =
            ActivityVideoPlayerBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.close.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                audioAttributes,
                true
            )
            .build()
            .also { exoPlayer ->

                exoPlayer.volume = 1f

                binding.playerView.player =
                    exoPlayer

                intent.data?.let { uri ->

                    exoPlayer.setMediaItem(
                        MediaItem.fromUri(uri)
                    )

                    exoPlayer.prepare()

                    exoPlayer.playWhenReady = true
                }
            }
    }

    override fun onStop() {

        binding.playerView.player = null

        player?.release()

        player = null

        super.onStop()
    }
}
