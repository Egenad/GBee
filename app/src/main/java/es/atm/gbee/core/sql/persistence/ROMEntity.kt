package es.atm.gbee.core.sql.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "roms")
data class ROMEntity (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val imageRes: String?,
    val filePath: String,
    val saveFilePath: String?,
    val addedDate: Long,
    val type: String
)