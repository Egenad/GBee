package es.atm.gbee.core.data.rom

data class ROM(
    var id: Int = -1,
    var title: String? = null,
    var imageRes: String? = null,
    var selected: Boolean = false
)