package es.atm.gbee.core

import android.content.Context
import es.atm.gbee.R

object ROMDataSource {

    val roms: MutableList<ROM> = mutableListOf()

    fun getSelectedROMsCount(): Int {
        return roms.count { it.selected }
    }

    fun deleteSelectedROMs() {
        roms.removeAll { it.selected }
    }

    fun getROMById(id: Int, context: Context): ROM {
        return roms.find { it.id == id } ?: getDefaultROM(context)
    }

    fun getPositionById(romId: Int): Int? {
        return roms.indexOfFirst { it.id == romId }.takeIf { it != -1 }
    }

    fun getROMByTitle(title: String, context: Context): ROM {
        return roms.find { it.title == title } ?: getDefaultROM(context)
    }

    private fun getDefaultROM(context: Context): ROM {
        val resources = context.resources
        return ROM().apply {
            title = resources.getString(R.string.unknown)
        }
    }

    fun addROM(rom: ROM) {
        roms.add(rom)
    }
}