package es.atm.gbee.core.sql.persistence.themes

import androidx.room.*

@Dao
interface ThemeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTheme(theme: ThemeEntity): Long

    @Update
    fun updateTheme(theme: ThemeEntity): Int

    @Delete
    fun deleteTheme(theme: ThemeEntity): Int

    @Query("SELECT * FROM themes")
    fun getAllThemes(): List<ThemeEntity>

    @Query("SELECT * FROM themes WHERE id = :id")
    fun getThemeById(id: Int): ThemeEntity?

    @Query("DELETE FROM themes")
    fun clearTable()
}