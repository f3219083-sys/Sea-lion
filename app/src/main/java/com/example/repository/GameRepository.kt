package com.example.repository

import com.example.database.GameDao
import com.example.database.LeaderboardEntry
import com.example.database.PlayerState
import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {

    val playerState: Flow<PlayerState?> = gameDao.getPlayerStateFlow()
    val leaderboard: Flow<List<LeaderboardEntry>> = gameDao.getLeaderboardFlow()

    suspend fun getPlayerStateSync(): PlayerState {
        var state = gameDao.getPlayerStateSync()
        if (state == null) {
            state = PlayerState()
            gameDao.updatePlayerState(state)
        }
        return state
    }

    suspend fun updatePlayerState(state: PlayerState) {
        gameDao.updatePlayerState(state)
    }

    suspend fun setupDefaultLeaderboardIfEmpty() {
        val current = gameDao.getLeaderboardSync()
        if (current.isEmpty()) {
            val defaults = listOf(
                LeaderboardEntry("CosmicEmperor 🐲", 120000000, false, "🐲", 12000.0),
                LeaderboardEntry("GalacticPanda 🐼", 45000000, false, "🐼", 4500.0),
                LeaderboardEntry("ZenMaster 🦉", 12000000, false, "🦉", 1100.0),
                LeaderboardEntry("DragonLord 🐲", 3000000, false, "🐲", 250.0),
                LeaderboardEntry("HyperChimp 🐒", 600000, false, "🐒", 60.0),
                LeaderboardEntry("RoaringPride 🦁", 130000, false, "🦁", 15.0),
                LeaderboardEntry("ZenCapybara 🦦", 28000, false, "🦦", 3.0),
                LeaderboardEntry("WiggleButt 🐶", 6500, false, "🐶", 0.7),
                LeaderboardEntry("FluffyPurrs 🐱", 1200, false, "🐱", 0.1),
                LeaderboardEntry("TappyCheeks 🐹", 250, false, "🐹", 0.02)
            )
            gameDao.insertLeaderboard(defaults)
        }
    }

    suspend fun getLeaderboardSync(): List<LeaderboardEntry> {
        return gameDao.getLeaderboardSync()
    }

    suspend fun updateLeaderboardEntries(entries: List<LeaderboardEntry>) {
        gameDao.insertLeaderboard(entries)
    }
}
