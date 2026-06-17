package com.fydualcamera.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_files ORDER BY timestamp DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_files WHERE type LIKE :typePrefix ORDER BY timestamp DESC")
    fun getMediaByType(typePrefix: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_files WHERE id = :id")
    suspend fun getMediaById(id: Long): MediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MediaEntity): Long

    @Update
    suspend fun update(media: MediaEntity)

    @Delete
    suspend fun delete(media: MediaEntity)

    @Query("DELETE FROM media_files WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_files")
    suspend fun deleteAll()

    @Query("SELECT SUM(sizeBytes) FROM media_files")
    fun getTotalSize(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM media_files")
    fun getCount(): Flow<Int>
}
