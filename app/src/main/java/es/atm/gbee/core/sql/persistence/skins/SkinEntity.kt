package es.atm.gbee.core.sql.persistence.skins

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skins")
data class SkinEntity (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val background: String,
    val landscapeBackground: String,
    val ssbuttons: String,
    val aButton: String,
    val bButton: String?,
    val abSameButton: Boolean,
    val screenBorder: String?
)