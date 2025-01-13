package es.atm.gbee.core.sql.persistence.themes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "themes")
data class ThemeEntity (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val background: String,
    val ssbuttons: String,
    val aButton: String,
    val bButton: String?,
    val abSameButton: Boolean,
    val screenBorder: String?
)