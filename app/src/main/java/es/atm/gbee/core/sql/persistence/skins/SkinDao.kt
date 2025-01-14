package es.atm.gbee.core.sql.persistence.skins

import androidx.room.*

@Dao
interface SkinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTheme(theme: SkinEntity): Long

    @Update
    fun updateTheme(theme: SkinEntity): Int

    @Delete
    fun deleteTheme(theme: SkinEntity): Int

    @Query("SELECT * FROM skins")
    fun getAllThemes(): List<SkinEntity>

    @Query("SELECT * FROM skins WHERE id = :id")
    fun getThemeById(id: Int): SkinEntity?

    @Query("DELETE FROM skins")
    fun clearTable()
}