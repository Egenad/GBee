package es.atm.gbee.core.sql.persistence.skins

import androidx.room.*

@Dao
interface SkinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSkin(theme: SkinEntity): Long

    @Update
    fun updateSkin(theme: SkinEntity): Int

    @Delete
    fun deleteSkin(theme: SkinEntity): Int

    @Query("SELECT * FROM skins")
    fun getAllSkins(): List<SkinEntity>

    @Query("SELECT * FROM skins WHERE id = :id")
    fun getSkinById(id: Int): SkinEntity?

    @Query("SELECT * FROM skins WHERE title = :title")
    fun getSkinByTitle(title: String): SkinEntity?

    @Query("DELETE FROM skins")
    fun clearTable()
}