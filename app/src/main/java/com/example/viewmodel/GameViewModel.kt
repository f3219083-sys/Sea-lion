package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.GameDatabase
import com.example.database.LeaderboardEntry
import com.example.database.PlayerState
import com.example.repository.GameRepository
import com.example.ui.SynthesizedAudioManager
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

    // Current player state tracked in memory for 100% lag-free responsiveness
    private val _playerStateMem = MutableStateFlow<PlayerState?>(null)
    val playerState: StateFlow<PlayerState?> = _playerStateMem.asStateFlow()

    // Leaderboard flow directly from Room
    val rawLeaderboard: StateFlow<List<LeaderboardEntry>>

    // Live display values to make tap responsive without waiting for DB writes
    private val _liveClicks = MutableStateFlow<Long?>(null)
    val liveClicks: StateFlow<Long?> = _liveClicks.asStateFlow()

    // Offline progress tracking state
    val offlineEarnings = MutableStateFlow<Pair<Long, Long>?>(null)

    // Autoclicker detection and locking states
    val isClickerLocked = MutableStateFlow(false)
    val playTabPressCount = MutableStateFlow(0)
    private val clickTimestamps = mutableListOf<Long>()

    // Doubling ban states
    val autoclickerOffenses = MutableStateFlow(0)
    val autoclickerBanTimeRemaining = MutableStateFlow(0L) // active countdown in seconds
    val isAutoclickerProtectionDisabled = MutableStateFlow(false)
    val shopTabPressCount = MutableStateFlow(0)

    private var isSavePending = false

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())

        rawLeaderboard = repository.leaderboard.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize state, DB and start auto-clicking loops
        viewModelScope.launch(Dispatchers.IO) {
            repository.setupDefaultLeaderboardIfEmpty()
            val initial = repository.getPlayerStateSync()
            
            val nowMs = System.currentTimeMillis()
            var stateWithSavedTime = initial
            
            // Check if user is currently banned based on stored banExpirationTime in DB
            if (initial.banExpirationTime > nowMs) {
                val remSeconds = (initial.banExpirationTime - nowMs) / 1000L
                viewModelScope.launch(Dispatchers.Main) {
                    isClickerLocked.value = true
                    autoclickerBanTimeRemaining.value = remSeconds
                    autoclickerOffenses.value = initial.autoclickerOffenseCount
                    startBanCountdownTimer(initial.banExpirationTime)
                }
            } else {
                // Not banned or ban expired. Check offline clicks!
                val cps = calculateCPS(initial)
                if (initial.lastSavedTime > 0 && cps > 0) {
                    val elapsedTimeSec = (nowMs - initial.lastSavedTime) / 1000L
                    if (elapsedTimeSec >= 10) { // minimum 10 seconds of absence to count as offline progress
                        val offlineClicksCollected = (elapsedTimeSec * cps).toLong()
                        if (offlineClicksCollected > 0) {
                            val nextCurrent = initial.currentClicks + offlineClicksCollected
                            val nextTotal = initial.totalClicks + offlineClicksCollected
                            val currentLevel = calculateLevelForClicks(nextCurrent)
                            stateWithSavedTime = initial.copy(
                                currentClicks = nextCurrent,
                                totalClicks = nextTotal,
                                currentLevel = currentLevel,
                                lastSavedTime = nowMs
                            )
                            isSavePending = true
                            
                            viewModelScope.launch(Dispatchers.Main) {
                                offlineEarnings.value = Pair(offlineClicksCollected, elapsedTimeSec)
                                _liveClicks.value = nextCurrent
                            }
                        }
                    }
                }
            }
            
            // Periodically refresh/set lastSavedTime
            if (stateWithSavedTime.lastSavedTime == 0L || stateWithSavedTime.lastSavedTime < nowMs) {
                stateWithSavedTime = stateWithSavedTime.copy(lastSavedTime = nowMs)
                isSavePending = true
            }

            _playerStateMem.value = stateWithSavedTime
            _liveClicks.value = stateWithSavedTime.currentClicks

            // Set media volumes and start ambient synth music
            SynthesizedAudioManager.musicVolume = stateWithSavedTime.musicVolume
            SynthesizedAudioManager.sfxVolume = stateWithSavedTime.sfxVolume
            if (stateWithSavedTime.musicVolume > 0.01f) {
                SynthesizedAudioManager.startBackgroundMusic()
            }

            // Start clock loop for auto-clicking, AI, and saving updates
            launch { startAutoClickerLoop() }
            launch { startLeaderboardSimulationLoop() }
            launch { startDatabaseSyncLoop() }
        }
    }

    fun onAppResume() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _playerStateMem.value ?: return@launch
            val nowMs = System.currentTimeMillis()
            val cps = calculateCPS(state)
            if (state.lastSavedTime > 0 && cps > 0) {
                val elapsedTimeSec = (nowMs - state.lastSavedTime) / 1000L
                if (elapsedTimeSec >= 10) {
                    val offlineClicksCollected = (elapsedTimeSec * cps).toLong()
                    if (offlineClicksCollected > 0) {
                        val nextCurrent = state.currentClicks + offlineClicksCollected
                        val nextTotal = state.totalClicks + offlineClicksCollected
                        val currentLevel = calculateLevelForClicks(nextCurrent)
                        val updated = state.copy(
                            currentClicks = nextCurrent,
                            totalClicks = nextTotal,
                            currentLevel = currentLevel,
                            lastSavedTime = nowMs
                        )
                        _playerStateMem.value = updated
                        _liveClicks.value = nextCurrent
                        offlineEarnings.value = Pair(offlineClicksCollected, elapsedTimeSec)
                        isSavePending = true
                        
                        SynthesizedAudioManager.playPurchase()
                    }
                }
            }
            val refreshed = _playerStateMem.value ?: return@launch
            _playerStateMem.value = refreshed.copy(lastSavedTime = nowMs)
            isSavePending = true
            
            if (refreshed.musicVolume > 0.01f) {
                SynthesizedAudioManager.startBackgroundMusic()
            }
        }
    }

    fun onAppPause() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _playerStateMem.value ?: return@launch
            val updated = state.copy(lastSavedTime = System.currentTimeMillis())
            _playerStateMem.value = updated
            repository.updatePlayerState(updated)
            SynthesizedAudioManager.stopBackgroundMusic()
        }
    }

    fun updateSettings(selectedLanguage: String, musicVolume: Float, sfxVolume: Float) {
        viewModelScope.launch {
            val state = _playerStateMem.value ?: return@launch
            val updated = state.copy(
                selectedLanguage = selectedLanguage,
                musicVolume = musicVolume,
                sfxVolume = sfxVolume
            )
            _playerStateMem.value = updated
            SynthesizedAudioManager.musicVolume = musicVolume
            SynthesizedAudioManager.sfxVolume = sfxVolume
            if (musicVolume > 0.01f) {
                SynthesizedAudioManager.startBackgroundMusic()
            } else {
                SynthesizedAudioManager.stopBackgroundMusic()
            }
            repository.updatePlayerState(updated)
        }
    }

    // Database syncing background loop (persists memory updates periodically)
    private suspend fun startDatabaseSyncLoop() {
        while (true) {
            delay(1500)
            if (isSavePending) {
                _playerStateMem.value?.let { state ->
                    val updatedState = state.copy(lastSavedTime = System.currentTimeMillis())
                    _playerStateMem.value = updatedState
                    repository.updatePlayerState(updatedState)
                    syncUserWithLeaderboard(updatedState)
                    isSavePending = false
                }
            }
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
               (state.dogBoneCount * 20.0) +
               (state.elephantStampedeCount * 200.0) +
               (state.phoenixFlightCount * 5000.0)
    }

    // Calculate clicks per tap - REMOVED Level multiplier to fulfill user's "what you upgrade is what you get" rule
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
            "lava_fire" -> 5000
            "cosmic" -> 10000
            "neon_cyber" -> 50000
            "magic_aurora" -> 250000
            else -> 0
        }
        val tapFromUpgrades = state.clickPowerLevel.toLong() +
                              (state.catScratchCount * 5L) +
                              (state.dragonFlameCount * 50L) +
                              (state.cheetahNitroCount * 1000L) +
                              (state.blackHoleCount * 30000L)
        return tapFromUpgrades + skinBonus
    }

    // Handles an user click
    fun onAnimalClicked() {
        if (isClickerLocked.value) return

        if (!isAutoclickerProtectionDisabled.value) {
            val now = System.currentTimeMillis()
            synchronized(clickTimestamps) {
                clickTimestamps.add(now)
                if (clickTimestamps.size > 12) {
                    clickTimestamps.removeAt(0)
                }
                if (clickTimestamps.size >= 12) {
                    val timeSpan = now - clickTimestamps[0]
                    // 12 taps in less than 500ms means >24 CPS, physically impossible for human fingers
                    if (timeSpan < 500) {
                        triggerAutoclickerPenalty()
                        return
                    }
                }
            }
        }

        SynthesizedAudioManager.playClick()

        viewModelScope.launch {
            val currentState = _playerStateMem.value ?: return@launch
            val tapBonus = calculateClicksPerTap(currentState)

            val nextTotalClicks = currentState.currentClicks + tapBonus
            val nextCurrentClicks = currentState.currentClicks + tapBonus

            _liveClicks.value = nextCurrentClicks

            updateStateInMemoryAndCheckMilestones(currentState, nextTotalClicks, nextCurrentClicks)
        }
    }

    private var banTimerJob: kotlinx.coroutines.Job? = null

    private fun startBanCountdownTimer(expirationMs: Long) {
        banTimerJob?.cancel()
        banTimerJob = viewModelScope.launch(Dispatchers.Main) {
            while (true) {
                val now = System.currentTimeMillis()
                val remSeconds = ((expirationMs - now) / 1000L).coerceAtLeast(0L)
                autoclickerBanTimeRemaining.value = remSeconds
                
                if (isAutoclickerProtectionDisabled.value || remSeconds <= 0L) {
                    autoclickerBanTimeRemaining.value = 0L
                    isClickerLocked.value = false
                    
                    // persist ban clear in db
                    val current = _playerStateMem.value
                    if (current != null) {
                        val cleared = current.copy(banExpirationTime = 0)
                        _playerStateMem.value = cleared
                        repository.updatePlayerState(cleared)
                    }
                    
                    synchronized(clickTimestamps) {
                        clickTimestamps.clear()
                    }
                    break
                }
                delay(1000)
            }
        }
    }

    private fun triggerAutoclickerPenalty() {
        if (isAutoclickerProtectionDisabled.value) return

        val newOffenseCount = autoclickerOffenses.value + 1
        autoclickerOffenses.value = newOffenseCount

        // Ban seconds multiply based on offenses count
        val banMinutes = 2.0.pow(newOffenseCount - 1).toLong()
        val banSeconds = banMinutes * 60L
        val banExpirationMs = System.currentTimeMillis() + (banSeconds * 1000L)

        autoclickerBanTimeRemaining.value = banSeconds
        isClickerLocked.value = true
        playTabPressCount.value = 0

        viewModelScope.launch {
            val currentState = _playerStateMem.value ?: return@launch
            
            // Deduct 1000 clicks unless score is low (e.g. at or below 500) to protect new users
            val nextCurrent = if (currentState.currentClicks > 500L) {
                (currentState.currentClicks - 1000L).coerceAtLeast(0L)
            } else {
                currentState.currentClicks
            }
            val nextTotal = if (currentState.totalClicks > 500L) {
                (currentState.totalClicks - 1000L).coerceAtLeast(0L)
            } else {
                currentState.totalClicks
            }

            _liveClicks.value = nextCurrent
            val updated = currentState.copy(
                currentClicks = nextCurrent,
                totalClicks = nextTotal,
                banExpirationTime = banExpirationMs,
                autoclickerOffenseCount = newOffenseCount
            )
            _playerStateMem.value = updated
            isSavePending = true
            
            repository.updatePlayerState(updated) // Save penalty to DB immediately
            
            // Start real-time countdown timer tick loop
            startBanCountdownTimer(banExpirationMs)
        }
    }

    // Increments bottom navigation tab clicks to bypass the penalty warning (retained legacy support for safety)
    fun incrementPlayTabPress() {
        if (!isClickerLocked.value) return
        val nextVal = playTabPressCount.value + 1
        playTabPressCount.value = nextVal
        if (nextVal >= 10) {
            isClickerLocked.value = false
            playTabPressCount.value = 0
            synchronized(clickTimestamps) {
                clickTimestamps.clear()
            }
        }
    }

    // Increments shop tab clicks, if clicked 10 times disables security
    fun incrementShopTabPress() {
        val nextVal = shopTabPressCount.value + 1
        shopTabPressCount.value = nextVal
        if (nextVal >= 10) {
            isAutoclickerProtectionDisabled.value = true
            isClickerLocked.value = false
            autoclickerBanTimeRemaining.value = 0L
            synchronized(clickTimestamps) {
                clickTimestamps.clear()
            }
        }
    }

    private suspend fun updateStateInMemoryAndCheckMilestones(
        state: PlayerState,
        nextTotal: Long,
        nextCurrent: Long
    ) {
        // Find next level based on current clicks balance
        val nextLevel = when {
            nextCurrent >= 200000000000L -> 15 // Wise Elephant (Mythic Masterclass)
            nextCurrent >= 40000000000L -> 14  // Great White Shark (Legendary Category)
            nextCurrent >= 8000000000L -> 13   // Silverback Gorilla (Legendary Category)
            nextCurrent >= 1500000000L -> 12  // Giant Whale (Epic Category)
            nextCurrent >= 300000000L -> 11   // Fearsome Tiger (Epic Category)
            nextCurrent >= 60000000L -> 10    // Friendly Panda (Rare Category)
            nextCurrent >= 12000000L -> 9     // Majestic Lion (Rare Category)
            nextCurrent >= 2500000L -> 8      // Clever Fox (Rare Category)
            nextCurrent >= 500000L -> 7       // Colorful Parrot (Uncommon Category)
            nextCurrent >= 100000L -> 6       // Tiny Turtle (Uncommon Category)
            nextCurrent >= 25000L -> 5        // Cheeky Monkey (Uncommon Category)
            nextCurrent >= 5000L -> 4         // Fluffy Bunny (Common Category)
            nextCurrent >= 1000L -> 3         // Loyal Doggy (Common Category)
            nextCurrent >= 200L -> 2          // Playful Kitty (Common Category)
            else -> 1                       // Cute Hamster (Common Category)
        }

        // Find unlocked skins
        val baseSkins = mutableListOf("standard")
        if (nextCurrent >= 200) baseSkins.add("cyberpunk")
        if (nextCurrent >= 1000) baseSkins.add("pirate")
        if (nextCurrent >= 5000) baseSkins.add("astronaut")
        if (nextCurrent >= 25000) baseSkins.add("god")
        if (nextCurrent >= 50000) baseSkins.add("steampunk")
        if (nextCurrent >= 150000) baseSkins.add("retro")
        if (nextCurrent >= 500000) baseSkins.add("shadow")
        if (nextCurrent >= 2000000) baseSkins.add("royal")
        if (nextCurrent >= 5000000) baseSkins.add("lava_fire")
        if (nextCurrent >= 10000000) baseSkins.add("cosmic")
        if (nextCurrent >= 100000000) baseSkins.add("neon_cyber")
        if (nextCurrent >= 1000000000) baseSkins.add("magic_aurora")

        val currentUnlockedList = state.unlockedSkins.split(",").map { it.trim() }.toSet()
        val newlyUnlockedSkins = baseSkins.filter { !currentUnlockedList.contains(it) }

        // Send UI events for milestones (only if ascending to protect against noise, but let level decrease go silent)
        if (nextLevel > state.currentLevel) {
            _uiEvents.emit(GameUiEvent.LevelUp(nextLevel, getAnimalNameForLevel(nextLevel)))
        }

        newlyUnlockedSkins.forEach { skinId ->
            _uiEvents.emit(GameUiEvent.SkinUnlocked(getSkinNameForId(skinId), skinId))
        }

        val highestNewSkin = newlyUnlockedSkins.lastOrNull()
        // If highestNewSkin is unlocked, auto-apply it. Otherwise, fallback to standard if currently equipped skin gets locked.
        val finalEquippedSkinId = if (highestNewSkin != null) {
            highestNewSkin
        } else if (baseSkins.contains(state.equippedSkinId)) {
            state.equippedSkinId
        } else {
            "standard"
        }

        val updatedState = state.copy(
            totalClicks = nextCurrent, // totalClicks and currentClicks completely in sync
            currentClicks = nextCurrent,
            currentLevel = nextLevel,
            unlockedSkins = baseSkins.joinToString(","),
            equippedSkinId = finalEquippedSkinId
        )

        _playerStateMem.value = updatedState
        isSavePending = true
    }

    fun buyUpgrade(upgradeId: String) {
        viewModelScope.launch {
            val state = _playerStateMem.value ?: return@launch
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
                    "click_power" -> state.copy(clickPowerLevel = state.clickPowerLevel + 1, currentClicks = nextClicks, totalClicks = nextClicks)
                    "hamster_wheel" -> state.copy(hamsterWheelCount = state.hamsterWheelCount + 1, currentClicks = nextClicks, totalClicks = nextClicks)
                    "cat_scratch" -> state.copy(catScratchCount = state.catScratchCount + 1, currentClicks = nextClicks, totalClicks = nextClicks)
                    "dog_bone" -> state.copy(dogBoneCount = state.dogBoneCount + 1, currentClicks = nextClicks, totalClicks = nextClicks)
                    "dragon_flame" -> state.copy(dragonFlameCount = state.dragonFlameCount + 1, currentClicks = nextClicks, totalClicks = nextClicks)
                    "elephant_stampede" -> state.copy(elephantStampedeCount = state.elephantStampedeCount + 1, currentClicks = nextClicks, totalClicks = nextClicks)
                    "cheetah_nitro" -> state.copy(cheetahNitroCount = state.cheetahNitroCount + 1, currentClicks = nextClicks, totalClicks = nextClicks)
                    "phoenix_flight" -> state.copy(phoenixFlightCount = state.phoenixFlightCount + 1, currentClicks = nextClicks, totalClicks = nextClicks)
                    "black_hole" -> state.copy(blackHoleCount = state.blackHoleCount + 1, currentClicks = nextClicks, totalClicks = nextClicks)
                    else -> state.copy(currentClicks = nextClicks, totalClicks = nextClicks)
                }

                SynthesizedAudioManager.playPurchase()
                updateStateInMemoryAndCheckMilestones(updatedState, nextClicks, nextClicks)
                syncUserWithLeaderboard(updatedState)
            } else {
                // We don't dispatch an English error or Greek hardcoded error, we use simple toast context or let the UI handle affordances
            }
        }
    }

    fun equipSkin(skinId: String) {
        viewModelScope.launch {
            val state = _playerStateMem.value ?: return@launch
            val unlockedSet = state.unlockedSkins.split(",").map { it.trim() }.toSet()
            if (unlockedSet.contains(skinId)) {
                val updated = state.copy(equippedSkinId = skinId)
                _playerStateMem.value = updated
                repository.updatePlayerState(updated) // Persist skin selection instantly
            }
        }
    }

    private suspend fun syncUserWithLeaderboard(state: PlayerState) {
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
        while (true) {
            delay(100)
            val state = _playerStateMem.value ?: continue
            val cps = calculateCPS(state)
            if (cps > 0) {
                val increment = cps * 0.1
                val nextCurrent = state.currentClicks + increment.toLong()

                _liveClicks.value = nextCurrent
                
                // Advance evolution tiers dynamically in local memory as counts ticking
                updateStateInMemoryAndCheckMilestones(state, nextCurrent, nextCurrent)
            }
        }
    }

    private fun calculateLevelForClicks(clicks: Long): Int {
        return when {
            clicks >= 200000000000L -> 15 // Wise Elephant
            clicks >= 40000000000L -> 14  // Great White Shark
            clicks >= 8000000000L -> 13   // Silverback Gorilla
            clicks >= 1500000000L -> 12  // Giant Whale
            clicks >= 300000000L -> 11   // Fearsome Tiger
            clicks >= 60000000L -> 10    // Friendly Panda
            clicks >= 12000000L -> 9     // Majestic Lion
            clicks >= 2500000L -> 8      // Clever Fox
            clicks >= 500000L -> 7       // Colorful Parrot
            clicks >= 100000L -> 6       // Tiny Turtle
            clicks >= 25000L -> 5        // Cheeky Monkey
            clicks >= 5000L -> 4         // Fluffy Bunny
            clicks >= 1000L -> 3         // Loyal Doggy
            clicks >= 200L -> 2          // Playful Kitty
            else -> 1                    // Cute Hamster
        }
    }

    // Leaderboard Live Simulation Loop (Competitors tick periodically)
    private suspend fun startLeaderboardSimulationLoop() {
        while (true) {
            delay(2500) // update competitor progress every 2.5 seconds
            val currentLeaderboard = repository.getLeaderboardSync()
            val state = _playerStateMem.value ?: continue
            val userTotal = state.totalClicks

            val updatedList = currentLeaderboard.map { entry ->
                if (entry.isUser) {
                    val emoji = getAnimalEmoji(state.currentLevel)
                    entry.copy(clicks = userTotal, avatarEmoji = emoji, clicksPerSecond = calculateCPS(state))
                } else {
                    val fluctuationMultiplier = Random.nextDouble(0.5, 1.5)
                    val addedClicks = (entry.clicksPerSecond * 2.5 * fluctuationMultiplier).roundToLong()
                    val finalAdd = if (addedClicks <= 0 && Random.nextFloat() < 0.25) 1L else addedClicks
                    entry.copy(clicks = entry.clicks + finalAdd)
                }
            }
            repository.updateLeaderboardEntries(updatedList)
        }
    }

    fun getAnimalTierName(level: Int): String {
        return when (level) {
            in 1..4 -> "COMMON Tier"
            in 5..7 -> "UNCOMMON Tier"
            in 8..10 -> "RARE Tier"
            in 11..12 -> "EPIC Tier"
            in 13..14 -> "LEGENDARY Tier"
            15 -> "MYTHIC Overwhelming Mastery"
            else -> "Tier Unknown"
        }
    }

    fun getAnimalNameForLevel(level: Int): String {
        return when (level) {
            1 -> "Cute Hamster"
            2 -> "Playful Kitty"
            3 -> "Loyal Doggy"
            4 -> "Fluffy Bunny"
            5 -> "Cheeky Monkey"
            6 -> "Tiny Turtle"
            7 -> "Colorful Parrot"
            8 -> "Clever Fox"
            9 -> "Majestic Lion"
            10 -> "Friendly Panda"
            11 -> "Fearsome Tiger"
            12 -> "Giant Whale"
            13 -> "Silverback Gorilla"
            14 -> "Great White Shark"
            15 -> "Wise Elephant"
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
            8 -> "🦊"
            9 -> "🦁"
            10 -> "🐼"
            11 -> "🐯"
            12 -> "🐋"
            13 -> "🦍"
            14 -> "🦈"
            15 -> "🐘"
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
            "lava_fire" -> "Volcanic Fury"
            "cosmic" -> "Cosmic Nebula"
            "neon_cyber" -> "Hyper Grid"
            "magic_aurora" -> "Elven Aurora"
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
            "lava_fire" -> 5000000
            "cosmic" -> 10000000
            "neon_cyber" -> 100000000
            "magic_aurora" -> 1000000000
            else -> 0
        }
    }
}
