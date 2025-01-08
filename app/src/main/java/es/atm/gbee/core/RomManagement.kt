package es.atm.gbee.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import es.atm.gbee.core.fragments.COVER_KEY
import es.atm.gbee.core.fragments.TITLE_KEY
import es.atm.gbee.core.sql.SQLManager
import es.atm.gbee.core.sql.persistence.ROMEntity
import java.io.File
import java.io.FileOutputStream

object RomManagement {

    fun addROM(context: Context, romPath: String, romTitle: String) {

        val title = romTitle.replace(Regex("\\.gbc?"), "").replace(Regex("_"), " ")
        val type = if(romTitle.contains(".gbc")) "gbc" else "gb"

        val newROM = ROMEntity(
            filePath = romPath,
            title = title,
            imageRes = null,
            saveFilePath = null,
            addedDate = System.currentTimeMillis(),
            type = type
        )

        saveROMToDatabaseAndDataSource(newROM, context)
    }

    private fun saveROMToDatabaseAndDataSource(rom: ROMEntity, context: Context) {
        val db = SQLManager.getDatabase(context)
        val id = db.romDAO().insertROM(rom)

        ROMDataSource.addROM(ROM(title = rom.title, id = id.toInt()))
    }

    fun getGBFilesFromFolder(context: Context, folderUri: Uri): List<File> {
        val files = mutableListOf<File>()

        val treeDocument = DocumentFile.fromTreeUri(context, folderUri)

        treeDocument?.listFiles()?.forEach { document ->
            if (document.isFile && document.name?.endsWith(".gb") == true && document.uri.path != null) {
                val file = File(document.uri.path!!)
                files.add(file)
            }
        }

        return files
    }

    fun fileAlreadyExists(context: Context, uri: Uri): Boolean {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                it.moveToFirst()
                val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val fileName = it.getString(columnIndex)

                val privateFile = File(context.filesDir, fileName)

                if(privateFile.exists()){
                    return true
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun saveFileToPrivateStorage(context: Context, uri: Uri): File? {
        var file: File? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                it.moveToFirst()
                val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val fileName = it.getString(columnIndex)

                val privateFile = File(context.filesDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val outputStream = FileOutputStream(privateFile)
                    inputStream.copyTo(outputStream)
                    file = privateFile
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }

    fun deleteROM(context: Context, rom: ROMEntity?) : Boolean {
        val romDao = SQLManager.getDatabase(context).romDAO()

        // Delete file from private storage
        if(rom != null) {
            val filepath = rom.filePath

            if (filepath.isNotEmpty()) {
                val file = File(filepath)
                if (file.exists()) {
                    file.delete()
                }
            }
            // Delete from DB
            romDao.deleteROM(rom)
            return true
        }

        return false
    }

    fun loadROMSFromDBIfNeeded(context: Context){
        val romDao = SQLManager.getDatabase(context).romDAO()
        val allRoms = romDao.getAllROMs()
        if(ROMDataSource.roms.size != allRoms.size){
            ROMDataSource.roms.clear()
            allRoms.forEach {
                val preferences = context.getSharedPreferences(it.id.toString(), Context.MODE_PRIVATE)
                val prefTitle = preferences.getString(TITLE_KEY, it.title)
                val prefImage = preferences.getString(COVER_KEY, it.imageRes)
                ROMDataSource.addROM(ROM(id = it.id, title = prefTitle, imageRes = prefImage))
            }
        }
    }
}