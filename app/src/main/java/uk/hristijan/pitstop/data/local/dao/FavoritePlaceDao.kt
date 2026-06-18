package uk.hristijan.pitstop.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import uk.hristijan.pitstop.core.model.FavoritePlaceType
import uk.hristijan.pitstop.data.local.entity.FavoritePlace

@Dao
interface FavoritePlaceDao {
    @Query("SELECT * FROM favorite_places ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<FavoritePlace>>

    @Query("SELECT * FROM favorite_places WHERE type = :type ORDER BY name COLLATE NOCASE")
    fun observeByType(type: FavoritePlaceType): Flow<List<FavoritePlace>>

    @Query("SELECT * FROM favorite_places WHERE id = :id")
    suspend fun getById(id: Long): FavoritePlace?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(place: FavoritePlace): Long

    @Update
    suspend fun update(place: FavoritePlace)

    @Delete
    suspend fun delete(place: FavoritePlace)
}
