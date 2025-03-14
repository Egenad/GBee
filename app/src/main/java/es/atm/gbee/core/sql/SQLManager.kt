package es.atm.gbee.core.sql

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import es.atm.gbee.core.sql.persistence.roms.ROMDao
import es.atm.gbee.core.sql.persistence.roms.ROMEntity
import es.atm.gbee.core.sql.persistence.skins.SkinDao
import es.atm.gbee.core.sql.persistence.skins.SkinEntity

const val ROOM_DB_NAME = "gbee_sqlite"

@Database(entities = [ROMEntity::class, SkinEntity::class], version = 1, exportSchema = false)
abstract class SQLManager : RoomDatabase(){

    abstract fun romDAO(): ROMDao
    abstract fun skinDAO(): SkinDao

    companion object {

        private var _instance: SQLManager? = null

        @Synchronized
        fun getDatabase(context: Context): SQLManager {
            if (_instance == null) {
                try{
                    val builder = databaseBuilder(
                        context.applicationContext,
                        SQLManager::class.java, ROOM_DB_NAME
                    ).allowMainThreadQueries()
                    _instance = builder.build()
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
            return _instance!!
        }
    }
}