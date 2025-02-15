package es.atm.gbee.core.data.skins

import android.content.Context
import android.util.Log
import androidx.room.Delete
import es.atm.gbee.R
import es.atm.gbee.activities.BUTTON_A
import es.atm.gbee.activities.BUTTON_B
import es.atm.gbee.activities.BUTTON_DPAD
import es.atm.gbee.activities.BUTTON_HOME
import es.atm.gbee.activities.BUTTON_SELECT
import es.atm.gbee.activities.BUTTON_START
import es.atm.gbee.activities.LEFT_BOTTOM
import es.atm.gbee.activities.LEFT_HOME
import es.atm.gbee.activities.RIGHT_BOTTOM
import es.atm.gbee.activities.RIGHT_HOME
import es.atm.gbee.activities.SCREEN_OFF
import es.atm.gbee.activities.SCREEN_ON
import es.atm.gbee.core.sql.SQLManager
import es.atm.gbee.core.sql.persistence.skins.SkinEntity

enum class DeleteResult(){
    NOT_DELETED,
    DELETED,
    DEFAULT_UPDATED
}

object SkinsManagement {

    fun addSkin(context: Context, skin: Skin) {
        saveSkinToDatabaseAndDataSource(skin, context)
    }

    fun updateSkin(context: Context, skinData: Skin){
        val skinDao = SQLManager.getDatabase(context).skinDAO()
        skinDao.updateSkin(convertDataToEntity(skinData))
    }

    fun deleteSkin(context: Context, skinPosition: Int): DeleteResult{
        var defaultUpdated = false
        try {
            val skinDao = SQLManager.getDatabase(context).skinDAO()

            if (SkinDataSource.skinIsSelected(skinPosition)) {
                val defaultSkin = skinDao.getSkinByTitle(SkinDataSource.getDefaultSkin().title!!)
                if(defaultSkin != null) {
                    defaultSkin.selected = true
                    updateSkin(context, convertEntityToData(defaultSkin))
                    SkinDataSource.selectSkinByPosition(0)
                    defaultUpdated = true
                }
            }

            // Remove from DataSource DB
            val skin = SkinDataSource.getSkinByPosition(skinPosition)
            if (skin != null) {
                skinDao.deleteSkin(convertDataToEntity(skin))
                SkinDataSource.removeFromPosition(skinPosition)
                if(defaultUpdated)
                    return DeleteResult.DEFAULT_UPDATED
                return DeleteResult.DELETED
            }
        }catch (e: Exception){
            Log.e("DatabaseError", "Error deleting skin: ${e.message}", e)
        }

        return DeleteResult.NOT_DELETED
    }

    private fun saveSkinToDatabaseAndDataSource(skin: Skin, context: Context) {
        try {
            val db = SQLManager.getDatabase(context)

            val skinEntity = convertDataToEntity(skin)

            val id = db.skinDAO().insertSkin(skinEntity)
            skin.id = id.toInt()

            SkinDataSource.addSkin(skin)
        } catch (e: Exception) {
            Log.e("DatabaseError", "Error inserting skin: ${e.message}", e)
        }
    }

    fun loadSkinsFromDBIfNeeded(context: Context){
        val skinDao = SQLManager.getDatabase(context).skinDAO()

        val defaultSkin = SkinDataSource.getDefaultSkin()
        val defaultSkinDB = skinDao.getSkinByTitle(defaultSkin.title!!)

        if(defaultSkinDB == null)
            skinDao.insertSkin(convertDataToEntity(defaultSkin))

        val allSkins = skinDao.getAllSkins()

        if(SkinDataSource.skins.size != allSkins.size){
            SkinDataSource.skins.clear()

            allSkins.forEach {
                SkinDataSource.addSkin(convertEntityToData(it))
            }
        }
    }


    fun convertDataToEntity(skin: Skin): SkinEntity{
        return SkinEntity(
            id = if (skin.id != -1) skin.id else 0,
            title = skin.title,
            backgroundColor = skin.backgroundColor,
            startSelectButtons = skin.startSelectButtons,
            aButton = skin.aButton,
            bButton = skin.bButton,
            screenOn = skin.screenOn,
            screenOff = skin.screenOff,
            dpad = skin.dpad,
            homeButton = skin.homeButton,
            leftHomeImage = skin.leftHomeImage,
            rightHomeImage = skin.rightHomeImage,
            leftBottomImage = skin.leftBottomImage,
            rightBottomImage = skin.rightBottomImage,
            editable = skin.editable,
            deletable = skin.deletable,
            selected = skin.selected
        )
    }

    fun convertEntityToData(entity: SkinEntity): Skin{
        return Skin(
            id = entity.id,
            title = entity.title,
            backgroundColor = entity.backgroundColor,
            startSelectButtons = entity.startSelectButtons,
            aButton = entity.aButton,
            bButton = entity.bButton,
            screenOn = entity.screenOn,
            screenOff = entity.screenOff,
            dpad = entity.dpad,
            homeButton = entity.homeButton,
            leftHomeImage = entity.leftHomeImage,
            rightHomeImage = entity.rightHomeImage,
            leftBottomImage = entity.leftBottomImage,
            rightBottomImage = entity.rightBottomImage,
            selected = entity.selected,
            deletable = entity.deletable,
            editable = entity.editable
        )
    }

    fun getDefaultDrawable(button: String): Int {
        return when (button) {
            BUTTON_A -> R.drawable.default_a
            BUTTON_B -> R.drawable.default_b
            BUTTON_START, BUTTON_SELECT -> R.drawable.default_start_select
            BUTTON_HOME -> R.drawable.default_home
            LEFT_BOTTOM, RIGHT_BOTTOM -> R.drawable.default_speakers
            LEFT_HOME, RIGHT_HOME -> R.drawable.default_logo
            BUTTON_DPAD -> R.drawable.default_dpad
            SCREEN_ON -> R.drawable.default_screen
            SCREEN_OFF -> R.drawable.default_screen_off
            else -> R.drawable.image_vector
        }
    }
}