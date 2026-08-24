package com.example.mygallery

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.mygallery.databinding.ActivityImageViewerBinding

class ImageViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.photo.load(intent.data) { crossfade(true) }
        binding.close.setOnClickListener { finish() }
    }
}
