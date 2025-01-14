package es.atm.gbee.core.data.skins

data class Skin(
    var id: Int = -1,
    var title: String? = null,
    var background: String,
    var landscapeBackground: String,
    var ssbuttons: String,
    var aButton: String,
    var bButton: String? = null,
    var abSameButton: Boolean,
    var screenBorder: String? = null,
    var selected: Boolean = false
)