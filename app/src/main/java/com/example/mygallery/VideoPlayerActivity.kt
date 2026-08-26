package com.example.mygallery

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.example.mygallery.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VIDEOS = "video_list"
        const val EXTRA_POSITION = "video_position"
    }

    private var player: ExoPlayer? = null
    private lateinit var binding: ActivityVideoPlayerBinding

    private var videoUris = arrayListOf<String>()
    private var currentPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoUris = intent.getStringArrayListExtra(EXTRA_VIDEOS)
            ?: arrayListOf()

        currentPosition = intent.getIntExtra(
            EXTRA_POSITION,
            0
        )

        // Backward compatibility: opening a single video
        if (videoUris.isEmpty()) {
            intent.data?.let { uri ->
                videoUris.add(uri.toString())
            }
        }

        currentPosition = currentPosition.coerceIn(
            0,
            (videoUris.size - 1).coerceAtLeast(0)
        )

        binding.close.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()

        if (videoUris.isEmpty()) {
            Toast.makeText(
                this,
                "No video found",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { exoPlayer ->

                exoPlayer.volume = 1f

                binding.playerView.player = exoPlayer

                exoPlayer.addListener(object : Player.Listener {

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {
                        Toast.makeText(
                            this@VideoPlayerActivity,
                            "Cannot play this video: ${error.errorCodeName}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })

                // Add the complete playlist
                val mediaItems = videoUris.map { uriString ->
                    MediaItem.fromUri(Uri.parse(uriString))
                }

                exoPlayer.setMediaItems(
                    mediaItems,
                    currentPosition,
                    0L
                )

                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
    }

    override fun onStop() {

        binding.playerView.player = null

        player?.release()
        player = null

        super.onStop()
    }
}
