package es.atm.gbee.core.data.skins

import android.content.Context
import es.atm.gbee.R
import es.atm.gbee.core.utils.FileManager

object SkinDataSource {

    private lateinit var appContext: Context
    val skins: MutableList<Skin> = mutableListOf()

    fun init(context: Context) {
        appContext = context.applicationContext
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

    fun getSkinByPosition(position: Int): Skin? {
        if(position > 0 && position < skins.size)
            return skins[position]

        return null
    }

    fun getDefaultSkin(): Skin {
        return Skin(
            title = "Default Skin",
            backgroundColor = "#E2B74F",
            startSelectButtons = FileManager.getDrawableAsByteArray(appContext, R.drawable.default_start_select),
            aButton = FileManager.getDrawableAsByteArray(appContext, R.drawable.default_a),
            bButton = FileManager.getDrawableAsByteArray(appContext, R.drawable.default_b),
            screenOn = FileManager.getDrawableAsByteArray(appContext, R.drawable.default_screen),
            screenOff = FileManager.getDrawableAsByteArray(appContext, R.drawable.default_screen_off),
            dpad = FileManager.getDrawableAsByteArray(appContext, R.drawable.default_dpad),
            leftHomeImage = FileManager.getDrawableAsByteArray(appContext, R.drawable.default_logo),
            rightBottomImage = FileManager.getDrawableAsByteArray(appContext, R.drawable.default_speakers),
            homeButton = FileManager.getDrawableAsByteArray(appContext, R.drawable.default_home),
            editable = false,
            deletable = false
        )
    }

    fun addSkin(skin: Skin) {
        skins.add(skin)
    }

    fun selectSkinByPosition(position : Int){
        skins[position].selected = true
    }

    fun skinIsSelected(position: Int): Boolean{
        if(position >= 0 && position < skins.size)
            return skins[position].selected

        return false
    }

    fun removeFromPosition(position: Int): Boolean{
        try {
            skins.removeAt(position)
            return true
        }catch (e: Exception){
            e.printStackTrace()
        }

        return false
    }

    fun deselectLastSkin(): Int{
        var result = -1
        for ((position, skin) in skins.withIndex()){
            if(skin.selected) {
                skin.selected = false
                result = position
            }
        }
        return result
    }
}