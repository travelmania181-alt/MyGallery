package com.example.mygallery

import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

object SearchDialog {
    fun show(context: Context, items: List<MediaItem>, onClick: (MediaItem) -> Unit) {
        val input = EditText(context).apply { hint = context.getString(R.string.search_hint) }
        AlertDialog.Builder(context)
            .setTitle(R.string.search)
            .setView(input)
            .setPositiveButton(R.string.search) { _, _ ->
                val q = input.text.toString().trim()
                val match = items.firstOrNull {
                    it.name.contains(q, true) || (it.bucketName?.contains(q, true) == true)
                }
                match?.let(onClick)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
