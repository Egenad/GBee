package es.atm.gbee.core.data.skins

import android.content.Context
import es.atm.gbee.core.data.rom.ROM
import es.atm.gbee.core.data.rom.ROMDataSource
import es.atm.gbee.core.fragments.COVER_KEY
import es.atm.gbee.core.fragments.TITLE_KEY
import es.atm.gbee.core.sql.SQLManager
import es.atm.gbee.core.sql.persistence.skins.SkinEntity

object SkinsManagement {

    fun addSkin(context: Context, skin: Skin) {
        saveSkinToDatabaseAndDataSource(skin, context)
    }

    fun updateSkin(context: Context, skin: Skin){

    }

    private fun saveSkinToDatabaseAndDataSource(skin: Skin, context: Context) {
        val db = SQLManager.getDatabase(context)

        val skinEntity = SkinEntity(
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
            rightBottomImage = skin.rightBottomImage
        )

        val id = db.skinDAO().insertSkin(skinEntity)
        skin.id = id.toInt()

        SkinDataSource.addSkin(skin)
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
                SkinDataSource.addSkin(Skin(
                    id = it.id,
                    title = it.title,
                    backgroundColor = it.backgroundColor,
                    startSelectButtons = it.startSelectButtons,
                    aButton = it.aButton,
                    bButton = it.bButton,
                    screenOn = it.screenOn,
                    screenOff = it.screenOff,
                    dpad = it.dpad,
                    homeButton = it.homeButton,
                    leftHomeImage = it.leftHomeImage,
                    rightHomeImage = it.rightHomeImage,
                    leftBottomImage = it.leftBottomImage,
                    rightBottomImage = it.rightBottomImage))
            }
        }
    }

     private fun convertDataToEntity(skin: Skin): SkinEntity{
        return SkinEntity(
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
            rightBottomImage = skin.rightBottomImage
        )
    }
}