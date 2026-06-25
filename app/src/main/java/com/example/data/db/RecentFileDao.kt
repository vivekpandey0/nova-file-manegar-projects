package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecentFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(recentFile: RecentFileEntity)

    @Query("SELECT * FROM recent_files ORDER BY lastAccessed DESC LIMIT 10")
    suspend fun getRecentFiles(): List<RecentFileEntity>
    
    @Query("DELETE FROM recent_files WHERE absolutePath = :path")
    suspend fun deleteRecentFile(path: String)
}
