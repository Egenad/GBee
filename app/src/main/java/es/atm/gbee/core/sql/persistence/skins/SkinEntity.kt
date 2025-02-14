package es.atm.gbee.core.sql.persistence.skins

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skins")
data class SkinEntity (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var title: String?,
    var backgroundColor: String,
    var startSelectButtons: ByteArray,
    var aButton: ByteArray,
    var bButton: ByteArray,
    var screenOn: ByteArray?,
    var screenOff: ByteArray?,
    var dpad: ByteArray,
    var homeButton: ByteArray,
    var leftHomeImage: ByteArray?,
    var rightHomeImage: ByteArray?,
    var leftBottomImage: ByteArray?,
    var rightBottomImage: ByteArray?,
    var editable: Boolean,
    var deletable: Boolean,
    var selected: Boolean
)