package es.atm.gbee.core.sql.persistence.skins

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skins")
data class SkinEntity (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var title: String?,
    var backgroundColor: String,
    var startSelectButtons: String,
    var aButton: String,
    var bButton: String?,
    var abSameButton: Boolean,
    var screenBorder: String?,
    var screenOff: String?,
    var homeButton: String,
    var leftHomeImage: String?,
    var rightHomeImage: String?,
    var leftBottomImage: String?,
    var rightBottomImage: String?,
    var rightLandscapeImage: String?,
    var leftLandscapeImage: String?
)