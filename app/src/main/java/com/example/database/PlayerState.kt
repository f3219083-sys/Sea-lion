package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_state")
data class PlayerState(
    @PrimaryKey val id: Int = 1,
    val totalClicks: Long = 0,
    val currentClicks: Long = 0,
    val currentLevel: Int = 1,
    // Upgrades:
    val clickPowerLevel: Int = 1, // base clicks per tap is 1
    val hamsterWheelCount: Int = 0, // Click tier 1: +1 click/sec
    val catScratchCount: Int = 0,  // Click tier 2: +5 clicks/sec
    val dogBoneCount: Int = 0,     // Click tier 3: +20 clicks/sec
    val dragonFlameCount: Int = 0, // Click tier 4: +100 clicks/sec
    val elephantStampedeCount: Int = 0, // Click tier 5: +200.0 clicks/sec
    val cheetahNitroCount: Int = 0,     // Click tier 6: +1000.0 clicks/sec
    val phoenixFlightCount: Int = 0,    // Click tier 7: +5000.0 clicks/sec
    val blackHoleCount: Int = 0,        // Click tier 8: +30000.0 clicks/sec
    val equippedSkinId: String = "standard",
    val unlockedSkins: String = "standard", // comma-separated strings
    val lastSavedTime: Long = 0,
    val banExpirationTime: Long = 0,
    val autoclickerOffenseCount: Int = 0
)
