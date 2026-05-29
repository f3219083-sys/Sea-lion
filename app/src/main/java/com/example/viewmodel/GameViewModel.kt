package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.GameDatabase
import com.example.database.LeaderboardEntry
import com.example.database.PlayerState
import com.example.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

sealed class GameUiEvent {
    data class LevelUp(val newLevel: Int, val animalName: String) : GameUiEvent()
    data class SkinUnlocked(val skinName: String, val skinId: String) : GameUiEvent()
    data class GeneralError(val message: String) : GameUiEvent()
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    
    // UI Event channels
    private val _uiEvents = MutableSharedFlow<GameUiEvent>()
    val uiEvents: SharedFlow<GameUiEvent> = _uiEvents.asSharedFlow()

    // Current player state flow directly from Room
    val playerState: StateFlow<PlayerState?>

    // Leaderboard flow directly from Room
    val rawLeaderboard: StateFlow<List<LeaderboardEntry>>

    // Live display values to make tap responsive without waiting for DB writes
    private val _liveClicks = MutableStateFlow<Long?>(null)
    val liveClicks: StateFlow<Long?> = _liveClicks.asStateFlow()

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())

        playerState = repository.playerState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        rawLeaderboard = repository.leaderboard.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize state, DB and start auto-clicking loops
        viewModelScope.launch(Dispatchers.IO) {
            repository.setupDefaultLeaderboardIfEmpty()
            val initial = repository.getPlayerStateSync()
            _liveClicks.value = initial.currentClicks

            // Start clock loop for auto-clicking and AI/leaderboard challenge updates
            launch { startAutoClickerLoop() }
            launch { startLeaderboardSimulationLoop() }
        }
    }

    // Helper math to calculate upgrade cost
    fun getUpgradeCost(upgradeId: String, currentLevelOrCount: Int): Long {
        return when (upgradeId) {
            "click_power" -> {
                // Click power level starts at 1
                (15 * 1.5.pow(currentLevelOrCount - 1)).roundToLong()
            }
            "hamster_wheel" -> {
                (50 * 1.6.pow(currentLevelOrCount)).roundToLong()
            }
            "cat_scratch" -> {
                (250 * 1.6.pow(currentLevelOrCount)).roundToLong()
            }
            "dog_bone" -> {
                (1200 * 1.65.pow(currentLevelOrCount)).roundToLong()
            }
            "dragon_flame" -> {
                (6000 * 1.7.pow(currentLevelOrCount)).roundToLong()
            }
            "elephant_stampede" -> {
                (35000 * 1.7.pow(currentLevelOrCount)).roundToLong()
            }
            "cheetah_nitro" -> {
                (200000 * 1.75.pow(currentLevelOrCount)).roundToLong()
            }
            "phoenix_flight" -> {
                (1500000 * 1.8.pow(currentLevelOrCount)).roundToLong()
            }
            "black_hole" -> {
                (12000000 * 1.85.pow(currentLevelOrCount)).roundToLong()
            }
            else -> Long.MAX_VALUE
        }
    }

    // Calculate current Clicks Per Second
    fun calculateCPS(state: PlayerState): Double {
        return (state.hamsterWheelCount * 0.5) +
               (state.catScratchCount * 2.0) +
               (state.dogBoneCount * 10.0) +
               (state.dragonFlameCount * 50.0) +
               (state.elephantStampedeCount * 200.0) +
               (state.cheetahNitroCount * 1000.0) +
               (state.phoenixFlightCount * 5000.0) +
               (state.blackHoleCount * 30000.0)
    }

    // Calculate clicks per tap
    fun calculateClicksPerTap(state: PlayerState): Long {
        val skinBonus = when (state.equippedSkinId) {
            "cyberpunk" -> 2
            "pirate" -> 5
            "astronaut" -> 15
            "god" -> 50
            "steampunk" -> 100
            "retro" -> 250
            "shadow" -> 750
            "royal" -> 2500
            "cosmic" -> 10000
            else -> 0
        }
        val levelFactor = when (state.currentLevel) {
            2 -> 2
            3 -> 3
            4 -> 5
            5 -> 10
            6 -> 25
            7 -> 75
            8 -> 200
            9 -> 600
            10 -> 2000
            11 -> 7000
            12 -> 25000
            13 -> 100000
            14 -> 400000
            15 -> 1500000
            else -> 1
        }
        return (state.clickPowerLevel * levelFactor + skinBonus).toLong()
    }

    // Handles an user click
    fun onAnimalClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getPlayerStateSync()
            val tapBonus = calculateClicksPerTap(state)

            val nextTotalClicks = state.totalClicks + tapBonus
            val nextCurrentClicks = state.currentClicks + tapBonus

            _liveClicks.value = nextCurrentClicks

            updateStateAndCheckMilestones(state, nextTotalClicks, nextCurrentClicks)
        }
    }

    private suspend fun updateStateAndCheckMilestones(
        state: PlayerState,
        nextTotal: Long,
        nextCurrent: Long
    ) {
        // Find next level
        val nextLevel = when {
            nextTotal >= 200000000000L -> 15 // Supreme Omnipresent
            nextTotal >= 40000000000L -> 14  // Infinite Singularity
            nextTotal >= 8000000000L -> 13   // Galaxian Overlord
            nextTotal >= 1500000000L -> 12  // Chrono Leviathan
            nextTotal >= 300000000L -> 11   // Cosmic Behemoth
            nextTotal >= 60000000L -> 10    // Mystic Phoenix
            nextTotal >= 12000000L -> 9     // Celestial Dragon
            nextTotal >= 2500000L -> 8      // Majestic Lion
            nextTotal >= 500000L -> 7       // Colorful Parrot
            nextTotal >= 100000L -> 6       // Tiny Turtle
            nextTotal >= 25000L -> 5        // Cheeky Monkey
            nextTotal >= 5000L -> 4         // Fluffy Bunny
            nextTotal >= 1000L -> 3         // Alert Doggy
            nextTotal >= 200L -> 2          // Playful Kitty
            else -> 1                       // Cute Hamster
        }

        // Find unlocked skins
        val baseSkins = mutableListOf("standard")
        if (nextTotal >= 200) baseSkins.add("cyberpunk")
        if (nextTotal >= 1000) baseSkins.add("pirate")
        if (nextTotal >= 5000) baseSkins.add("astronaut")
        if (nextTotal >= 25000) baseSkins.add("god")
        if (nextTotal >= 50000) baseSkins.add("steampunk")
        if (nextTotal >= 150000) baseSkins.add("retro")
        if (nextTotal >= 500000) baseSkins.add("shadow")
        if (nextTotal >= 2000000) baseSkins.add("royal")
        if (nextTotal >= 10000000) baseSkins.add("cosmic")

        val currentUnlockedList = state.unlockedSkins.split(",").map { it.trim() }.toSet()
        val newlyUnlockedSkins = baseSkins.filter { !currentUnlockedList.contains(it) }

        // Send UI events for milestones
        if (nextLevel > state.currentLevel) {
            _uiEvents.emit(GameUiEvent.LevelUp(nextLevel, getAnimalNameForLevel(nextLevel)))
        }

        newlyUnlockedSkins.forEach { skinId ->
            _uiEvents.emit(GameUiEvent.SkinUnlocked(getSkinNameForId(skinId), skinId))
        }

        val updatedState = state.copy(
            totalClicks = nextTotal,
            currentClicks = nextCurrent,
            currentLevel = nextLevel,
            unlockedSkins = baseSkins.joinToString(",")
        )

        repository.updatePlayerState(updatedState)
        syncUserWithLeaderboard(updatedState)
    }

    fun buyUpgrade(upgradeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getPlayerStateSync()
            val currentVal = when (upgradeId) {
                "click_power" -> state.clickPowerLevel
                "hamster_wheel" -> state.hamsterWheelCount
                "cat_scratch" -> state.catScratchCount
                "dog_bone" -> state.dogBoneCount
                "dragon_flame" -> state.dragonFlameCount
                "elephant_stampede" -> state.elephantStampedeCount
                "cheetah_nitro" -> state.cheetahNitroCount
                "phoenix_flight" -> state.phoenixFlightCount
                "black_hole" -> state.blackHoleCount
                else -> 0
            }

            val cost = getUpgradeCost(upgradeId, currentVal)
            if (state.currentClicks >= cost) {
                val nextClicks = state.currentClicks - cost
                _liveClicks.value = nextClicks

                val updatedState = when (upgradeId) {
                    "click_power" -> state.copy(clickPowerLevel = state.clickPowerLevel + 1, currentClicks = nextClicks)
                    "hamster_wheel" -> state.copy(hamsterWheelCount = state.hamsterWheelCount + 1, currentClicks = nextClicks)
                    "cat_scratch" -> state.copy(catScratchCount = state.catScratchCount + 1, currentClicks = nextClicks)
                    "dog_bone" -> state.copy(dogBoneCount = state.dogBoneCount + 1, currentClicks = nextClicks)
                    "dragon_flame" -> state.copy(dragonFlameCount = state.dragonFlameCount + 1, currentClicks = nextClicks)
                    "elephant_stampede" -> state.copy(elephantStampedeCount = state.elephantStampedeCount + 1, currentClicks = nextClicks)
                    "cheetah_nitro" -> state.copy(cheetahNitroCount = state.cheetahNitroCount + 1, currentClicks = nextClicks)
                    "phoenix_flight" -> state.copy(phoenixFlightCount = state.phoenixFlightCount + 1, currentClicks = nextClicks)
                    "black_hole" -> state.copy(blackHoleCount = state.blackHoleCount + 1, currentClicks = nextClicks)
                    else -> state
                }

                repository.updatePlayerState(updatedState)
            } else {
                _uiEvents.emit(GameUiEvent.GeneralError("Not enough clicks to purchase!"))
            }
        }
    }

    fun equipSkin(skinId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getPlayerStateSync()
            val unlockedSet = state.unlockedSkins.split(",").map { it.trim() }.toSet()
            if (unlockedSet.contains(skinId)) {
                val updated = state.copy(equippedSkinId = skinId)
                repository.updatePlayerState(updated)
            }
        }
    }

    private suspend fun syncUserWithLeaderboard(state: PlayerState) {
        // Sync user's statistics in leaderboard DB
        val currentLeaderboard = repository.getLeaderboardSync().toMutableList()
        val userIdx = currentLeaderboard.indexOfFirst { it.isUser }

        val emoji = getAnimalEmoji(state.currentLevel)

        val userEntry = LeaderboardEntry(
            name = "You 👑",
            clicks = state.totalClicks,
            isUser = true,
            avatarEmoji = emoji,
            clicksPerSecond = calculateCPS(state)
        )

        if (userIdx >= 0) {
            currentLeaderboard[userIdx] = userEntry
        } else {
            currentLeaderboard.add(userEntry)
        }

        repository.updateLeaderboardEntries(currentLeaderboard)
    }

    // Auto Clicker Clock Loop (Ticks every 100ms for extra fluid visual responsiveness)
    private suspend fun startAutoClickerLoop() {
        var accumulatedTicksTime = 0L
        var lastState = repository.getPlayerStateSync()
        var currentDouble = lastState.currentClicks.toDouble()
        var totalDouble = lastState.totalClicks.toDouble()
        val mutex = Mutex()

        // Sync local double values when DB updates (e.g. from users clicking manually or buying upgrades)
        viewModelScope.launch {
            playerState.collect { newState ->
                if (newState != null) {
                    mutex.withLock {
                        val diffCurrent = Math.abs(newState.currentClicks - currentDouble.toLong())
                        val diffTotal = Math.abs(newState.totalClicks - totalDouble.toLong())
                        // If a purchase occurred, sync exactly to prevent desyncing
                        if (diffCurrent > 5 || diffTotal > 5 || newState.clickPowerLevel != lastState.clickPowerLevel) {
                            currentDouble = newState.currentClicks.toDouble()
                            totalDouble = newState.totalClicks.toDouble()
                            lastState = newState
                            _liveClicks.value = newState.currentClicks
                        }
                    }
                }
            }
        }

        while (true) {
            delay(100)
            accumulatedTicksTime += 100

            mutex.withLock {
                val cps = calculateCPS(lastState)
                if (cps > 0) {
                    val increment = cps * 0.1
                    currentDouble += increment
                    totalDouble += increment

                    val finalClicks = currentDouble.toLong()
                    _liveClicks.value = finalClicks

                    // Persist the accumulated difference to database every 1 second
                    if (accumulatedTicksTime >= 1000) {
                        accumulatedTicksTime = 0
                        val stateBeforeCommit = repository.getPlayerStateSync()
                        
                        // The difference accumulated over this 1-second window:
                        val deltaCurrent = (currentDouble - lastState.currentClicks.toDouble()).coerceAtLeast(0.0)
                        val deltaTotal = (totalDouble - lastState.totalClicks.toDouble()).coerceAtLeast(0.0)

                        if (deltaCurrent >= 1.0 || deltaTotal >= 1.0) {
                            val nextTotal = stateBeforeCommit.totalClicks + deltaTotal.toLong()
                            val nextCurrent = stateBeforeCommit.currentClicks + deltaCurrent.toLong()

                            updateStateAndCheckMilestones(stateBeforeCommit, nextTotal, nextCurrent)

                            lastState = repository.getPlayerStateSync()
                            currentDouble = lastState.currentClicks.toDouble()
                            totalDouble = lastState.totalClicks.toDouble()
                        }
                    }
                } else {
                    if (accumulatedTicksTime >= 1000) {
                        accumulatedTicksTime = 0
                        val freshState = repository.getPlayerStateSync()
                        lastState = freshState
                        currentDouble = freshState.currentClicks.toDouble()
                        totalDouble = freshState.totalClicks.toDouble()
                        _liveClicks.value = freshState.currentClicks
                    }
                }
            }
        }
    }

    // Leaderboard Live Simulation Loop (Competitors tick periodically)
    private suspend fun startLeaderboardSimulationLoop() {
        while (true) {
            delay(2500) // update competitor progress every 2.5 seconds
            val currentLeaderboard = repository.getLeaderboardSync()
            val state = repository.getPlayerStateSync()
            val userTotal = state.totalClicks

            val updatedList = currentLeaderboard.map { entry ->
                if (entry.isUser) {
                    val emoji = getAnimalEmoji(state.currentLevel)
                    entry.copy(clicks = userTotal, avatarEmoji = emoji, clicksPerSecond = calculateCPS(state))
                } else {
                    // Competitors advance randomly based on their clicksPerSecond
                    val fluctuationMultiplier = Random.nextDouble(0.5, 1.5)
                    val addedClicks = (entry.clicksPerSecond * 2.5 * fluctuationMultiplier).roundToLong()
                    // ensure a minimum of 1 click occasionally or keep it simple
                    val finalAdd = if (addedClicks <= 0 && Random.nextFloat() < 0.25) 1L else addedClicks
                    entry.copy(clicks = entry.clicks + finalAdd)
                }
            }
            repository.updateLeaderboardEntries(updatedList)
        }
    }

    fun getAnimalNameForLevel(level: Int): String {
        return when (level) {
            1 -> "Cute Hamster"
            2 -> "Playful Kitty"
            3 -> "Alert Doggy"
            4 -> "Fluffy Bunny"
            5 -> "Cheeky Monkey"
            6 -> "Tiny Turtle"
            7 -> "Colorful Parrot"
            8 -> "Majestic Lion"
            9 -> "Celestial Dragon"
            10 -> "Mystic Phoenix"
            11 -> "Cosmic Behemoth"
            12 -> "Chrono Leviathan"
            13 -> "Galaxian Overlord"
            14 -> "Infinite Singularity"
            15 -> "Supreme Omnipresent"
            else -> "Special Animal"
        }
    }

    fun getAnimalEmoji(level: Int): String {
        return when (level) {
            1 -> "🐹"
            2 -> "🐱"
            3 -> "🐶"
            4 -> "🐰"
            5 -> "🐵"
            6 -> "🐢"
            7 -> "🦜"
            8 -> "🦁"
            9 -> "🐲"
            10 -> "🦅"
            11 -> "🌌"
            12 -> "🐋"
            13 -> "👾"
            14 -> "🌀"
            15 -> "🌟"
            else -> "🐾"
        }
    }

    fun getSkinNameForId(skinId: String): String {
        return when (skinId) {
            "standard" -> "Classic Cutie"
            "cyberpunk" -> "Cyber Neon"
            "pirate" -> "Pirate Cap'n"
            "astronaut" -> "Space Astro"
            "god" -> "Divine Aura"
            "steampunk" -> "Steam Brass"
            "retro" -> "8-Bit Retro"
            "shadow" -> "Shadow Ninja"
            "royal" -> "Royal Crown"
            "cosmic" -> "Cosmic Nebula"
            else -> "Mysterious Look"
        }
    }

    fun getSkinBadgeCountRequirement(skinId: String): Long {
        return when (skinId) {
            "standard" -> 0
            "cyberpunk" -> 200
            "pirate" -> 1000
            "astronaut" -> 5000
            "god" -> 25000
            "steampunk" -> 50000
            "retro" -> 150000
            "shadow" -> 500000
            "royal" -> 2000000
            "cosmic" -> 10000000
            else -> 0
        }
    }
}
