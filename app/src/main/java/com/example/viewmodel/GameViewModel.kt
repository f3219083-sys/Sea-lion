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

    // Current player state tracked in memory for 100% lag-free responsiveness
    private val _playerStateMem = MutableStateFlow<PlayerState?>(null)
    val playerState: StateFlow<PlayerState?> = _playerStateMem.asStateFlow()

    // Leaderboard flow directly from Room
    val rawLeaderboard: StateFlow<List<LeaderboardEntry>>

    // Live display values to make tap responsive without waiting for DB writes
    private val _liveClicks = MutableStateFlow<Long?>(null)
    val liveClicks: StateFlow<Long?> = _liveClicks.asStateFlow()

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
            _playerStateMem.value = initial
            _liveClicks.value = initial.currentClicks

            // Start clock loop for auto-clicking, AI, and saving updates
            launch { startAutoClickerLoop() }
            launch { startLeaderboardSimulationLoop() }
            launch { startDatabaseSyncLoop() }
        }
    }

    // Database syncing background loop (persists memory updates periodically)
    private suspend fun startDatabaseSyncLoop() {
        while (true) {
            delay(1500)
            if (isSavePending) {
                _playerStateMem.value?.let { state ->
                    repository.updatePlayerState(state)
                    syncUserWithLeaderboard(state)
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
               (state.catScratchCount * 2.0) +
               (state.dogBoneCount * 10.0) +
               (state.dragonFlameCount * 50.0) +
               (state.elephantStampedeCount * 200.0) +
               (state.cheetahNitroCount * 1000.0) +
               (state.phoenixFlightCount * 5000.0) +
               (state.blackHoleCount * 30000.0)
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
        return (state.clickPowerLevel + skinBonus).toLong()
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

        viewModelScope.launch {
            val currentState = _playerStateMem.value ?: return@launch
            val tapBonus = calculateClicksPerTap(currentState)

            val nextTotalClicks = currentState.totalClicks + tapBonus
            val nextCurrentClicks = currentState.currentClicks + tapBonus

            _liveClicks.value = nextCurrentClicks

            updateStateInMemoryAndCheckMilestones(currentState, nextTotalClicks, nextCurrentClicks)
        }
    }

    private var banTimerJob: kotlinx.coroutines.Job? = null

    private fun triggerAutoclickerPenalty() {
        if (isAutoclickerProtectionDisabled.value) return

        val newOffenseCount = autoclickerOffenses.value + 1
        autoclickerOffenses.value = newOffenseCount

        // 1st offense = 1 min (60s), 2nd = 2 mins (120s), 3rd = 4 mins (240s)...
        val banMinutes = 2.0.pow(newOffenseCount - 1).toLong()
        val banSeconds = banMinutes * 60L

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
                totalClicks = nextTotal
            )
            _playerStateMem.value = updated
            isSavePending = true
            
            repository.updatePlayerState(updated) // Save penalty to DB immediately
            _uiEvents.emit(GameUiEvent.GeneralError("Ανιχνεύτηκε Auto Clicker! Ποινή: -1,000 κλικ. Προσωρινός αποκλεισμός για $banMinutes λεπτά!"))
        }

        // Start real-time countdown timer tick loop
        banTimerJob?.cancel()
        banTimerJob = viewModelScope.launch {
            while (autoclickerBanTimeRemaining.value > 0) {
                delay(1000)
                if (isAutoclickerProtectionDisabled.value) {
                    autoclickerBanTimeRemaining.value = 0
                    isClickerLocked.value = false
                    break
                }
                autoclickerBanTimeRemaining.value = autoclickerBanTimeRemaining.value - 1
            }
            if (!isAutoclickerProtectionDisabled.value) {
                isClickerLocked.value = false
                synchronized(clickTimestamps) {
                    clickTimestamps.clear()
                }
                _uiEvents.emit(GameUiEvent.GeneralError("Ο αποκλεισμός έληξε! Το παιχνίδι ξεκλειδώθηκε. Παρακαλώ παίξτε καθαρά!"))
            }
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
            viewModelScope.launch {
                _uiEvents.emit(GameUiEvent.GeneralError("Το παιχνίδι ξεκλειδώθηκε με επιτυχία! Παρακαλώ παίξτε καθαρά!"))
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
            viewModelScope.launch {
                _uiEvents.emit(GameUiEvent.GeneralError("⚠️ Η ασφάλεια για το Auto Clicker έχει απενεργοποιηθεί επιτυχώς!"))
            }
        }
    }

    private suspend fun updateStateInMemoryAndCheckMilestones(
        state: PlayerState,
        nextTotal: Long,
        nextCurrent: Long
    ) {
        // Find next level based on curation of real-animal categories
        val nextLevel = when {
            nextTotal >= 200000000000L -> 15 // Mythical Unicorn (Mythic Masterclass)
            nextTotal >= 40000000000L -> 14  // Ancient T-Rex (Legendary Category)
            nextTotal >= 8000000000L -> 13   // Soaring Eagle (Legendary Category)
            nextTotal >= 1500000000L -> 12  // Giant Whale (Epic Category)
            nextTotal >= 300000000L -> 11   // Fearsome Tiger (Epic Category)
            nextTotal >= 60000000L -> 10    // Friendly Panda (Rare Category)
            nextTotal >= 12000000L -> 9     // Majestic Lion (Rare Category)
            nextTotal >= 2500000L -> 8      // Clever Fox (Rare Category)
            nextTotal >= 500000L -> 7       // Colorful Parrot (Uncommon Category)
            nextTotal >= 100000L -> 6       // Tiny Turtle (Uncommon Category)
            nextTotal >= 25000L -> 5        // Cheeky Monkey (Uncommon Category)
            nextTotal >= 5000L -> 4         // Fluffy Bunny (Common Category)
            nextTotal >= 1000L -> 3         // Loyal Doggy (Common Category)
            nextTotal >= 200L -> 2          // Playful Kitty (Common Category)
            else -> 1                       // Cute Hamster (Common Category)
        }

        // Find unlocked skins (Classic + Theme list + 3 newly added specialized skins!)
        val baseSkins = mutableListOf("standard")
        if (nextTotal >= 200) baseSkins.add("cyberpunk")
        if (nextTotal >= 1000) baseSkins.add("pirate")
        if (nextTotal >= 5000) baseSkins.add("astronaut")
        if (nextTotal >= 25000) baseSkins.add("god")
        if (nextTotal >= 50000) baseSkins.add("steampunk")
        if (nextTotal >= 150000) baseSkins.add("retro")
        if (nextTotal >= 500000) baseSkins.add("shadow")
        if (nextTotal >= 2000000) baseSkins.add("royal")
        if (nextTotal >= 5000000) baseSkins.add("lava_fire")
        if (nextTotal >= 10000000) baseSkins.add("cosmic")
        if (nextTotal >= 100000000) baseSkins.add("neon_cyber")
        if (nextTotal >= 1000000000) baseSkins.add("magic_aurora")

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

                _playerStateMem.value = updatedState
                repository.updatePlayerState(updatedState) // Persist transaction to DB instantly
                syncUserWithLeaderboard(updatedState)
            } else {
                _uiEvents.emit(GameUiEvent.GeneralError("Δεν έχετε αρκετά 🐾 για αυτή την αναβάθμιση!"))
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
                val nextTotal = state.totalClicks + increment.toLong()

                _liveClicks.value = nextCurrent
                
                // Advance evolution tiers dynamically in local memory as counts ticking
                val currentLevel = calculateLevelForClicks(nextTotal)
                val updated = state.copy(
                    currentClicks = nextCurrent,
                    totalClicks = nextTotal,
                    currentLevel = currentLevel
                )
                _playerStateMem.value = updated
                isSavePending = true
            }
        }
    }

    private fun calculateLevelForClicks(clicks: Long): Int {
        return when {
            clicks >= 200000000000L -> 15 // Mythical Unicorn
            clicks >= 40000000000L -> 14  // Ancient T-Rex
            clicks >= 8000000000L -> 13   // Soaring Eagle
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
            13 -> "Soaring Eagle"
            14 -> "Ancient T-Rex"
            15 -> "Mythical Unicorn"
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
            13 -> "🦅"
            14 -> "🦖"
            15 -> "🦄"
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
