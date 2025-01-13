package es.atm.gbee.core.sql.persistence.roms

import androidx.room.*

@Dao
interface ROMDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertROM(rom: ROMEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertROMs(vararg roms: ROMEntity)

    @Update
    fun updateROM(user: ROMEntity): Int

    @Query("UPDATE roms SET imageRes = :imageRes WHERE id = :gameId")
    fun updateCoverImage(gameId: Int, imageRes: String)

    @Query("UPDATE roms SET title = :title WHERE id = :gameId")
    fun updateTitle(gameId: Int, title: String)

    @Delete
    fun deleteROM(user: ROMEntity): Int

    @Query("SELECT * FROM roms")
    fun getAllROMs(): List<ROMEntity>

    @Query("SELECT * FROM roms WHERE title = :title")
    fun getROMByTitle(title: String): ROMEntity?

    @Query("SELECT * FROM roms WHERE id = :id")
    fun getROMById(id: Int): ROMEntity?

    @Query("DELETE FROM roms")
    fun clearTable()
}