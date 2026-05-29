package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_state WHERE id = 1 LIMIT 1")
    fun getPlayerStateFlow(): Flow<PlayerState?>

    @Query("SELECT * FROM player_state WHERE id = 1 LIMIT 1")
    suspend fun getPlayerStateSync(): PlayerState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePlayerState(state: PlayerState)

    @Query("SELECT * FROM leaderboard ORDER BY clicks DESC")
    fun getLeaderboardFlow(): Flow<List<LeaderboardEntry>>

    @Query("SELECT * FROM leaderboard")
    suspend fun getLeaderboardSync(): List<LeaderboardEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboard(entries: List<LeaderboardEntry>)

    @Query("DELETE FROM leaderboard")
    suspend fun clearLeaderboard()
}
