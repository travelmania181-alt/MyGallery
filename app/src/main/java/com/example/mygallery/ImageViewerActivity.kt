package com.example.mygallery

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mygallery.databinding.ActivityImageViewerBinding

class ImageViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGES = "extra_images"
        const val EXTRA_POSITION = "extra_position"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val binding =
            ActivityImageViewerBinding.inflate(layoutInflater)

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars =
                insets.getInsets(
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

        val images =
            intent.getStringArrayListExtra(EXTRA_IMAGES)
                ?: arrayListOf()

        val position =
            intent.getIntExtra(EXTRA_POSITION, 0)

        binding.imagePager.adapter =
            ImagePagerAdapter(images)

        binding.imagePager.setCurrentItem(
            position,
            false
        )

        binding.close.setOnClickListener {
            finish()
        }
    }
}
