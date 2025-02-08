package es.atm.gbee.core.data.skins

object SkinDataSource {

    val skins: MutableList<Skin> = mutableListOf()

    init {
        skins.add(getDefaultSkin())
    }

    fun getSkinById(id: Int): Skin {
        return skins.find { it.id == id } ?: getDefaultSkin()
    }

    fun getPositionById(romId: Int): Int? {
        return skins.indexOfFirst { it.id == romId }.takeIf { it != -1 }
    }

    fun getSkinByTitle(title: String): Skin {
        return skins.find { it.title == title } ?: getDefaultSkin()
    }

    private fun getDefaultSkin(): Skin {
        return Skin(
            title = "Default Skin",
            backgroundColor = "#E2B74F",
            startSelectButtons = "default_start_select",
            aButton = "default_a",
            bButton = "default_b",
            abSameButton = false,
            screenBorder = "default_screen",
            screenOff = "default_screen_off",
            leftHomeImage = "default_logo",
            leftLandscapeImage = "default_logo",
            rightBottomImage = "default_speakers",
            rightLandscapeImage = "default_speakers",
            homeButton = "default_home"
        )
    }

    fun addSkin(rom: Skin) {
        skins.add(rom)
    }

    fun selectAllSkins(selection : Boolean){
        for (skin in skins){
            skin.selected = selection
        }
    }

    fun getSelectedSkinsCount() : Int {

        var count = 0

        for (skin in skins){
            if(skin.selected)
                count++
        }

        return count
    }

    fun deleteSelectedSkins(){

        val newSkinList: MutableList<Skin> = mutableListOf()

        for (skin in skins){
            if(skin.selected)
                newSkinList.add(skin)
        }

        skins.removeAll(newSkinList)
    }
}