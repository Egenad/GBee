package es.atm.gbee.core.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object FileManager {

    fun copyFileFromUriToPrivateStorage(context: Context, uri: Uri, gameId: Int) : String?{

        var filePath : String? = null

        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "cover_image_$gameId.jpg"

            val file = File(context.filesDir, fileName)

            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            filePath = file.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return filePath
    }

    fun deleteFileFromPrivateStorage(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isTitleValid(title: String): Boolean {
        val regex = "^[a-zA-Z0-9_ -]+$"
        return title.matches(regex.toRegex())
    }
}