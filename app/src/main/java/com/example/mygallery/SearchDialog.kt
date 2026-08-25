package com.example.mygallery

import android.content.Context
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

object SearchDialog {

    fun show(
        context: Context,
        items: List<MediaItem>,
        onClick: (MediaItem) -> Unit
    ) {
        val input = EditText(context).apply {
            hint = context.getString(R.string.search_hint)
            setSingleLine(true)
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.search)
            .setView(input)
            .setPositiveButton(R.string.search) { _, _ ->

                val q = input.text.toString().trim()

                if (q.isEmpty()) {
                    Toast.makeText(
                        context,
                        "Please enter something to search",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val match = items.firstOrNull {
                    it.name.contains(q, ignoreCase = true) ||
                    (it.bucketName?.contains(q, ignoreCase = true) == true)
                }

                if (match != null) {
                    onClick(match)
                } else {
                    Toast.makeText(
                        context,
                        "No photo or video found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
