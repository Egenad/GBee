package es.atm.gbee.core.data.skins

data class Skin(
    var id: Int = -1,
    var title: String? = null,
    var backgroundColor: String,
    var startSelectButtons: ByteArray,
    var aButton: ByteArray,
    var bButton: ByteArray,
    var screenOn: ByteArray? = null,
    var screenOff: ByteArray? = null,
    var dpad: ByteArray,
    var homeButton: ByteArray,
    var leftHomeImage: ByteArray? = null,
    var rightHomeImage: ByteArray? = null,
    var leftBottomImage: ByteArray? = null,
    var rightBottomImage: ByteArray? = null,
    var selected: Boolean = false,
    var deletable: Boolean = true,
    var editable: Boolean = true,
    var previewRes: String? = null
)