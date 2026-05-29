package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey val name: String,
    val clicks: Long,
    val isUser: Boolean = false,
    val avatarEmoji: String,
    val clicksPerSecond: Double = 0.0
)
