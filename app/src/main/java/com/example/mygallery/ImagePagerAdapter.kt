package com.example.mygallery

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ImagePagerAdapter(
    private val images: List<String>
) : RecyclerView.Adapter<ImagePagerAdapter.ImageHolder>() {

    class ImageHolder(
        val imageView: ImageView
    ) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageHolder {

        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        return ImageHolder(imageView)
    }

    override fun onBindViewHolder(
        holder: ImageHolder,
        position: Int
    ) {

        holder.imageView.load(images[position]) {
            crossfade(true)
        }
    }

    override fun getItemCount(): Int = images.size
}
