package es.atm.gbee.core.data.skins

import android.content.Context

object SkinDataSource {

    val roms: MutableList<Skin> = mutableListOf()

    fun getSelectedSkinsCount(): Int {
        return roms.count { it.selected }
    }

    fun deleteSelectedSkins() {
        roms.removeAll { it.selected }
    }

    fun getSkinById(id: Int, context: Context): Skin {
        return roms.find { it.id == id } ?: getDefaultSkin(context)
    }

    fun getPositionById(romId: Int): Int? {
        return roms.indexOfFirst { it.id == romId }.takeIf { it != -1 }
    }

    fun getSkinByTitle(title: String, context: Context): Skin {
        return roms.find { it.title == title } ?: getDefaultSkin(context)
    }

    private fun getDefaultSkin(context: Context): Skin {
        val resources = context.resources

        return Skin(background = "default_background",
            landscapeBackground = "default_landscape_background",
            ssbuttons = "default_ssbuttons",
            aButton = "default_a_button",
            abSameButton = true,
            screenBorder = null,
            selected = false)

    }

    fun addROM(rom: Skin) {
        roms.add(rom)
    }
}