package com.example.mygallery

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val uri: String)

@Dao
interface FavoriteDao {
    @Query("SELECT uri FROM favorites")
    suspend fun all(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE uri = :uri")
    suspend fun remove(uri: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE uri = :uri)")
    suspend fun exists(uri: String): Boolean
}

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class FavoritesDatabase : RoomDatabase() {
    abstract fun dao(): FavoriteDao

    companion object {
        @Volatile private var instance: FavoritesDatabase? = null
        fun get(context: Context): FavoritesDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FavoritesDatabase::class.java,
                    "favorites.db"
                ).build().also { instance = it }
            }
    }
}
