package es.atm.gbee.core.data.skins

data class Skin(
    var id: Int = -1,
    var title: String? = null,
    var backgroundColor: String,
    var startSelectButtons: String,
    var aButton: String,
    var bButton: String? = null,
    var abSameButton: Boolean,
    var screenBorder: String? = null,
    var screenOff: String? = null,
    var homeButton: String,
    var leftHomeImage: String? = null,
    var rightHomeImage: String? = null,
    var leftBottomImage: String? = null,
    var rightBottomImage: String? = null,
    var rightLandscapeImage: String? = null,
    var leftLandscapeImage: String? = null,
    var selected: Boolean = false,
    var deletable: Boolean = true,
    var editable: Boolean = true,
    var previewRes: String? = null
)