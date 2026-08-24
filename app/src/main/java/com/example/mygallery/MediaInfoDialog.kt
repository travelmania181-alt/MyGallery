package com.example.mygallery

import android.content.Context
import androidx.appcompat.app.AlertDialog
import java.text.DateFormat
import java.util.Date

object MediaInfoDialog {
    fun show(context: Context, item: MediaItem) {
        val size = android.text.format.Formatter.formatFileSize(context, item.size)
        val date = DateFormat.getDateTimeInstance().format(Date(item.dateAddedSeconds * 1000))
        val resolution = if (item.width > 0 && item.height > 0) "${item.width} × ${item.height}" else context.getString(R.string.unknown)
        val duration = if (item.type == MediaType.VIDEO) "\n${context.getString(R.string.duration)}: ${item.duration / 1000}s" else ""
        AlertDialog.Builder(context)
            .setTitle(item.name)
            .setMessage("${context.getString(R.string.file_name)}: ${item.name}\n${context.getString(R.string.date)}: $date\n${context.getString(R.string.file_size)}: $size\n${context.getString(R.string.resolution)}: $resolution$duration")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
