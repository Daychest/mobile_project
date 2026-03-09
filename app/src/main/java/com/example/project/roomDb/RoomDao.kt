package com.example.project.roomDb

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Upsert
    suspend fun upsertUser(user: User)

    @Query("SELECT * FROM User")
    fun getAllUsers(): Flow<List<User>>
}