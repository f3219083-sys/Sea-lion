package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.LeaderboardEntry
import com.example.database.PlayerState
import com.example.viewmodel.GameUiEvent
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.text.font.FontStyle

// Translator and Localizer dictionary
fun getLocalizedString(lang: String, key: String): String {
    val isGr = lang == "el"
    return when (key) {
        "tab_play" -> if (isGr) "Παιχνίδι" else "Play"
        "tab_shop" -> if (isGr) "Μαγαζί" else "Shop"
        "tab_skins" -> if (isGr) "Εμφανίσεις" else "Skins"
        "tab_unlocks" -> if (isGr) "Εξέλιξη" else "Unlocks"
        "settings_title" -> if (isGr) "Ρυθμίσεις Παιχνιδιού ⚙️" else "Game Settings ⚙️"
        "settings_language" -> if (isGr) "Γλώσσα / Language" else "Language / Γλώσσα"
        "settings_music" -> if (isGr) "🔊 Ένταση Μουσικής" else "🔊 Music Volume"
        "settings_sfx" -> if (isGr) "🎵 Ένταση Ηχητικών Εφέ" else "🎵 Sound Effects Volume"
        "settings_save" -> if (isGr) "Αποθήκευση & Κλείσιμο" else "Save & Close"
        "settings_autoclicker" -> if (isGr) "Ασφάλεια Auto Clicker:" else "Auto Clicker Safety:"
        "settings_status_enabled" -> if (isGr) "ΕΝΕΡΓΟΠΟΙΗΜΕΝΗ (Ασφαλής)" else "ENABLED (Protected)"
        "settings_status_disabled" -> if (isGr) "ΑΠΕΝΕΡΓΟΠΟΙΗΜΕΝΗ (Ελεύθερο)" else "DISABLED (Free Play)"
        "ban_title" -> if (isGr) "⚠️ ΑΠΟΚΛΕΙΣΜΟΣ (BAN)" else "⚠️ TEMPORARY BAN ACTIVE"
        "ban_desc" -> if (isGr) "Ανιχνεύτηκαν υπερβολικά γρήγορα κλικ (Auto Clicker)!" else "Excessively fast clicks detected (Auto Clicker)!"
        "ban_penalty" -> if (isGr) "Ποινή: -1.000 κλικ!" else "Penalty: -1,000 Clicks!"
        "ban_warning" -> if (isGr) "Παρακαλώ παίξτε καθαρά με τα δάχτυλά σας! Μην το ξανακάνετε." else "Please play clean using your fingers! Do not do it again."
        "ban_remaining" -> if (isGr) "Χρόνος αποκλεισμού:" else "Time remaining:"
        "welcome_back" -> if (isGr) "Καλώς ήρθες πίσω! 🎉" else "Welcome Back! 🎉"
        "welcome_desc" -> if (isGr) "Κέρδισες %s κλικ (για %s δευτερόλεπτα απουσίας) όσο ήσουν εκτός!" else "You collected %s clicks (during %s seconds of offline absence)!"
        "first_run_title" -> "Zoo Clicker 🦁"
        "first_run_subtitle" -> "Διάλεξε τη γλώσσα σου / Choose your language"
        else -> key
    }
}

@Composable
fun LanguageSelectionScreen(
    onSelectLanguage: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF310E5F), Color(0xFF0D0221)))),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp)
                .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1E1A3C))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Zoo Clicker 🦁",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Choose your language\nΕπιλέξτε τη γλώσσα σας",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = { onSelectLanguage("el") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🇬🇷 ΕΛΛΗΝΙΚΑ", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSelectLanguage("en") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🇬🇧 ENGLISH", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    currentLanguage: String,
    currentMusicVol: Float,
    currentSfxVol: Float,
    isAutoclickerDisabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Float, Float) -> Unit
) {
    var lang by remember { mutableStateOf(currentLanguage) }
    var music by remember { mutableFloatStateOf(currentMusicVol) }
    var sfx by remember { mutableFloatStateOf(currentSfxVol) }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, Color(0xFF6750A4), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF3EDF7))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = getLocalizedString(lang, "settings_title"),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1C1B1F)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Language
                Text(
                    text = getLocalizedString(lang, "settings_language"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF49454F),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { lang = "el" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (lang == "el") Color(0xFF6750A4) else Color(0xFFE8DEF8),
                            contentColor = if (lang == "el") Color.White else Color(0xFF1D192B)
                        )
                    ) {
                        Text("🇬🇷 Ελληνικά")
                    }
                    Button(
                        onClick = { lang = "en" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (lang == "en") Color(0xFF6750A4) else Color(0xFFE8DEF8),
                            contentColor = if (lang == "en") Color.White else Color(0xFF1D192B)
                        )
                    ) {
                        Text("🇬🇧 English")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Music Volume Slider
                Text(
                    text = "${getLocalizedString(lang, "settings_music")} (${(music * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF49454F),
                    modifier = Modifier.align(Alignment.Start)
                )
                Slider(
                    value = music,
                    onValueChange = { music = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // SFX Volume Slider
                Text(
                    text = "${getLocalizedString(lang, "settings_sfx")} (${(sfx * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF49454F),
                    modifier = Modifier.align(Alignment.Start)
                )
                Slider(
                    value = sfx,
                    onValueChange = { sfx = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Auto clicker status box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFCAC4D0).copy(alpha = 0.3f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getLocalizedString(lang, "settings_autoclicker"),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1C1B1F)
                    )
                    Text(
                        text = if (isAutoclickerDisabled) {
                            getLocalizedString(lang, "settings_status_disabled")
                        } else {
                            getLocalizedString(lang, "settings_status_enabled")
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = if (isAutoclickerDisabled) Color(0xFFB3261E) else Color(0xFF386A20)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onSave(lang, music, sfx) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text(
                        text = getLocalizedString(lang, "settings_save"),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun BanWarningModal(
    lang: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(3.dp, Color(0xFFB3261E), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFF1F0))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️ " + getLocalizedString(lang, "ban_title"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFFB3261E)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = getLocalizedString(lang, "ban_desc"),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getLocalizedString(lang, "ban_penalty"),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                    color = Color(0xFFB3261E),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = getLocalizedString(lang, "ban_warning"),
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                ) {
                    Text("OK", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

fun Modifier.multiTouchClickable(
    enabled: Boolean = true,
    onClick: (Offset) -> Unit
): Modifier = this.pointerInput(enabled) {
    if (!enabled) return@pointerInput
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { change ->
                if (change.changedToDown()) {
                    change.consume()
                    onClick(change.position)
                }
            }
        }
    }
}

// Class representing floating point indicator on animal click
data class ClickIndicator(
    val id: Long,
    val text: String,
    val xOffset: Float,
    val yOffset: Float,
    val creationTime: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val liveClicks by viewModel.liveClicks.collectAsStateWithLifecycle()
    val leaderboard by viewModel.rawLeaderboard.collectAsStateWithLifecycle()
    val isClickerLocked by viewModel.isClickerLocked.collectAsStateWithLifecycle()
    val playTabPressCount by viewModel.playTabPressCount.collectAsStateWithLifecycle()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.onAppResume()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                viewModel.onAppPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Clicker, 1: Shop, 2: Skins, 3: Leaderboard

    // Local animated float indicators
    var floatingIndicators by remember { mutableStateOf(emptyList<ClickIndicator>()) }
    val coroutineScope = rememberCoroutineScope()

    // Alert states
    var levelUpAlert by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var skinUnlockedAlert by remember { mutableStateOf<Pair<String, String>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Settings & security overlays
    var showSettings by remember { mutableStateOf(false) }
    var showSecurityDisclaimer by remember { mutableStateOf(false) }
    var showPostBanWarning by remember { mutableStateOf(false) }

    val isProtectionDisabled by viewModel.isAutoclickerProtectionDisabled.collectAsStateWithLifecycle()
    val autoclickerBanTimeRemaining by viewModel.autoclickerBanTimeRemaining.collectAsStateWithLifecycle()

    var lastBanState by remember { mutableStateOf(false) }
    LaunchedEffect(isClickerLocked) {
        if (!isClickerLocked && lastBanState) {
            // Ban ended!
            showPostBanWarning = true
        }
        lastBanState = isClickerLocked
    }

    LaunchedEffect(isProtectionDisabled) {
        if (isProtectionDisabled) {
            showSecurityDisclaimer = true
        }
    }

    // Floating text decay loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            if (floatingIndicators.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val updated = floatingIndicators.filter { now - it.creationTime < 1000 }
                if (updated.size != floatingIndicators.size) {
                    floatingIndicators = updated
                }
            }
        }
    }

    // Capture ViewModel side effects
    LaunchedEffect(viewModel.uiEvents) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is GameUiEvent.LevelUp -> {
                    levelUpAlert = Pair(event.newLevel, event.animalName)
                }
                is GameUiEvent.SkinUnlocked -> {
                    skinUnlockedAlert = Pair(event.skinName, event.skinId)
                }
                is GameUiEvent.GeneralError -> {
                    errorMessage = event.message
                }
            }
        }
    }

    val safeStateForFirstRun = playerState
    if (safeStateForFirstRun != null && safeStateForFirstRun.selectedLanguage.isEmpty()) {
        LanguageSelectionScreen(
            onSelectLanguage = { lang ->
                viewModel.updateSettings(lang, 0.4f, 0.5f)
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            val stateForGear = playerState
            if (stateForGear != null) {
                val isGearDarkBg = stateForGear.equippedSkinId in listOf("cyberpunk", "astronaut", "pirate", "steampunk", "retro", "shadow", "royal", "lava_fire", "cosmic", "neon_cyber", "magic_aurora")
                val gearColor = when (stateForGear.equippedSkinId) {
                    "cyberpunk" -> Color(0xFF00FFCC)
                    "astronaut" -> Color(0xFF2196F3)
                    "god" -> Color(0xFFFFD700)
                    "pirate" -> Color(0xFFFFC107)
                    "steampunk" -> Color(0xFFCD853F)
                    "retro" -> Color(0xFF00FF00)
                    "shadow" -> Color(0xFFFF0000)
                    "royal" -> Color(0xFFFFD700)
                    "lava_fire" -> Color(0xFFFF4500)
                    "cosmic" -> Color(0xFF00FFFF)
                    "neon_cyber" -> Color(0xFF00F0FF)
                    "magic_aurora" -> Color(0xFF00FFCC)
                    else -> if (isGearDarkBg) Color.White else Color(0xFF6750A4)
                }
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .statusBarsPadding()
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                        .testTag("settings_gear_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = gearColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Scaffold(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("game_scaffold"),
            bottomBar = {
                val safeStateForNav = playerState
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .border(width = 1.dp, color = Color(0xFFCAC4D0).copy(alpha = 0.3f))
                        .testTag("app_navigation_bar"),
                    containerColor = if (safeStateForNav == null || safeStateForNav.equippedSkinId == "standard") Color(0xFFF3EDF7) else Color.Black.copy(alpha = 0.45f),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = {
                            if (isClickerLocked) {
                                viewModel.incrementPlayTabPress()
                            } else {
                                activeTab = 0
                            }
                        },
                        icon = { Icon(if (activeTab == 0) Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow, contentDescription = "Play") },
                        label = { Text(if (safeStateForNav != null) getLocalizedString(safeStateForNav.selectedLanguage, "tab_play") else "Play") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D192B),
                            selectedTextColor = Color(0xFF1D192B),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F),
                            indicatorColor = Color(0xFFE8DEF8)
                        ),
                        modifier = Modifier.testTag("tab_clicker")
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = {
                            activeTab = 1
                            viewModel.incrementShopTabPress()
                        },
                        icon = { Icon(if (activeTab == 1) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart, contentDescription = "Shop") },
                        label = { Text(if (safeStateForNav != null) getLocalizedString(safeStateForNav.selectedLanguage, "tab_shop") else "Shop") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D192B),
                            selectedTextColor = Color(0xFF1D192B),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F),
                            indicatorColor = Color(0xFFE8DEF8)
                        ),
                        modifier = Modifier.testTag("tab_shop")
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        icon = { Icon(if (activeTab == 2) Icons.Filled.Checkroom else Icons.Outlined.Checkroom, contentDescription = "Skins") },
                        label = { Text(if (safeStateForNav != null) getLocalizedString(safeStateForNav.selectedLanguage, "tab_skins") else "Skins") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D192B),
                            selectedTextColor = Color(0xFF1D192B),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F),
                            indicatorColor = Color(0xFFE8DEF8)
                        ),
                        modifier = Modifier.testTag("tab_skins")
                    )
                    NavigationBarItem(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        icon = { Icon(if (activeTab == 3) Icons.Filled.Pets else Icons.Outlined.Pets, contentDescription = "Unlocks") },
                        label = { Text(if (safeStateForNav != null) getLocalizedString(safeStateForNav.selectedLanguage, "tab_unlocks") else "Unlocks") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D192B),
                            selectedTextColor = Color(0xFF1D192B),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F),
                            indicatorColor = Color(0xFFE8DEF8)
                        ),
                        modifier = Modifier.testTag("tab_unlocks")
                    )
                }
            }
        ) { paddingValues ->
            val safeState = playerState

            if (safeState == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Apply a nice lively cosmic dynamic background brush
                val backgroundBrush = when (safeState.equippedSkinId) {
                    "cyberpunk" -> Brush.verticalGradient(listOf(Color(0xFF03001e), Color(0xFF7303c0), Color(0xFFec38bc)))
                    "astronaut" -> Brush.verticalGradient(listOf(Color(0xFF0B192C), Color(0xFF1E3E62)))
                    "god" -> Brush.verticalGradient(listOf(Color(0xFFFFEEEE), Color(0xFFDED9E2), Color(0xFFFFEEEE)))
                    "pirate" -> Brush.verticalGradient(listOf(Color(0xFF141E30), Color(0xFF243B55)))
                    "steampunk" -> Brush.verticalGradient(listOf(Color(0xFF2B1B17), Color(0xFF4A3B32), Color(0xFF8B6508)))
                    "retro" -> Brush.verticalGradient(listOf(Color(0xFF0D0D0D), Color(0xFF1B4D3E), Color(0xFF0D0D0D)))
                    "shadow" -> Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF141414), Color(0xFF2C0A0A)))
                    "royal" -> Brush.verticalGradient(listOf(Color(0xFF3B0066), Color(0xFF4B0082), Color(0xFF800080)))
                    "cosmic" -> Brush.verticalGradient(listOf(Color(0xFF050515), Color(0xFF1A0B2E), Color(0xFF490B5E)))
                    else -> Brush.verticalGradient(listOf(Color(0xFFFEF7FF), Color(0xFFF3EDF7)))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundBrush)
                ) {
                // Background particle drawings for Cosmic skins
                if (safeState.equippedSkinId == "god") {
                    BackgroundAmbientAura()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .then(
                            if (activeTab != 0) {
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            } else {
                                Modifier
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Global Stats Panel Headers (Hidden on Play Tab for immersive UX)
                    if (activeTab != 0) {
                        StatsHeaderWidget(
                            liveClicksValue = liveClicks ?: safeState.currentClicks,
                            totalClicksValue = safeState.totalClicks,
                            cps = viewModel.calculateCPS(safeState),
                            currentLevel = safeState.currentLevel,
                            skinId = safeState.equippedSkinId,
                            activeTab = activeTab,
                            viewModel = viewModel
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Central view based on Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = activeTab,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> -width } + fadeOut())
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> width } + fadeOut())
                                }
                            },
                            label = "tab_content_animation"
                        ) { targetTab ->
                            when (targetTab) {
                                0 -> {
                                    ClickerPlayground(
                                        state = safeState,
                                        viewModel = viewModel,
                                        floatingIndicators = floatingIndicators,
                                        onFloatingIndicatorAdded = { text, x, y ->
                                            val newId = System.currentTimeMillis() + Random.nextInt(1000)
                                            floatingIndicators = floatingIndicators + ClickIndicator(newId, text, x, y)
                                        }
                                    )
                                }
                                1 -> {
                                    ShopUpgradesPanel(
                                        state = safeState,
                                        viewModel = viewModel
                                    )
                                }
                                2 -> {
                                    SkinsWardrobePanel(
                                        state = safeState,
                                        viewModel = viewModel
                                    )
                                }
                                3 -> {
                                    UnlocksProgressionPanel(
                                        state = safeState,
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val autoclickerBanTimeRemaining by viewModel.autoclickerBanTimeRemaining.collectAsStateWithLifecycle()
    if (isClickerLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable(enabled = true, onClick = {}), // consumes clicks to block screen
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(2.dp, Color.Red, RoundedCornerShape(24.dp)),
                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF250202)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🤖 ΑΝΙΧΝΕΥΤΗΚE AUTO CLICKER! 🤖",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Έχετε αποκλειστεί προσωρινά διότι ανιχνεύτηκε εξαιρετικά γρήγορο clicking. Παρακαλώ παίξτε καθαρά!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Ποινή: -1,000 κλικ!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color(0xFFFFB4AB),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    val minutes = autoclickerBanTimeRemaining / 60
                    val seconds = autoclickerBanTimeRemaining % 60
                    val countdownFormatted = "%02d:%02d".format(minutes, seconds)

                    Text(
                        text = "Το παιχνίδι θα ξεκλειδωθεί αυτόματα σε:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Countdown banner box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Red.copy(alpha = 0.15f))
                            .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countdownFormatted,
                            style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
  }
}

    // Dialog: Level up
    levelUpAlert?.let { (level, name) ->
        CelebratingAlert(
            title = "🎉 Level Unlocked!",
            subtitle = "You reached Level $level!",
            description = "You can now click the $name! Tap power and level coefficients increased!",
            emoji = viewModel.getAnimalEmoji(level),
            onDismiss = { levelUpAlert = null }
        )
    }

    // Dialog: Skin Unlock
    skinUnlockedAlert?.let { (name, id) ->
        CelebratingAlert(
            title = "✨ Skin Unlocked!",
            subtitle = "Unlocked: $name!",
            description = "You unlocked the spectacular $name skin style! Equip it in the Skins Wardrobe tab right now to get powerful point bonuses.",
            emoji = when (id) {
                "cyberpunk" -> "⚡"
                "pirate" -> "🏴‍☠️"
                "astronaut" -> "👨‍🚀"
                "god" -> "👑"
                else -> "🎨"
            },
            onDismiss = { skinUnlockedAlert = null }
        )
    }

    // Dialog: Error Alert
    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            title = { Text("Information") },
            text = { Text(msg) }
        )
    }

    // Dialog: Settings dialog
    val stateForSettings = playerState
    if (showSettings && stateForSettings != null) {
        SettingsDialog(
            currentLanguage = stateForSettings.selectedLanguage,
            currentMusicVol = stateForSettings.musicVolume,
            currentSfxVol = stateForSettings.sfxVolume,
            isAutoclickerDisabled = isProtectionDisabled,
            onDismiss = { showSettings = false },
            onSave = { newLang, newMusic, newSfx ->
                viewModel.updateSettings(newLang, newMusic, newSfx)
                showSettings = false
            }
        )
    }

    // Dialog: Post Ban warning
    if (showPostBanWarning) {
        val currentLang = playerState?.selectedLanguage ?: "en"
        BanWarningModal(
            lang = currentLang,
            onDismiss = { showPostBanWarning = false }
        )
    }

    // Dialog: Offline Earnings
    val offlineEarningsCheck by viewModel.offlineEarnings.collectAsStateWithLifecycle()
    offlineEarningsCheck?.let { (clicks, secs) ->
        val currentLang = playerState?.selectedLanguage ?: "en"
        Dialog(onDismissRequest = { viewModel.offlineEarnings.value = null }) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1F1B2C))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🐾 " + getLocalizedString(currentLang, "welcome_back"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = getLocalizedString(currentLang, "welcome_desc")
                            .format("%,d".format(clicks), secs.toString()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.offlineEarnings.value = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) {
                        Text("Awesome!", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                    }
                }
            }
        }
    }

    // Dialog: Security disclaimer deactivation dialog
    if (showSecurityDisclaimer) {
        Dialog(onDismissRequest = { showSecurityDisclaimer = false }) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(2.dp, Color.Red, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️ SECURITY DISABLED ⚠️",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Auto-Clicker Detection has been successfully deactivated!\n\nYou can now tap freely without triggering penalty screens.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showSecurityDisclaimer = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                    ) {
                        Text("UNDERSTOOD")
                    }
                }
            }
        }
    }
}

@Composable
fun StatsHeaderWidget(
    liveClicksValue: Long,
    totalClicksValue: Long,
    cps: Double,
    currentLevel: Int,
    skinId: String,
    activeTab: Int,
    viewModel: GameViewModel
) {
    val isDarkBackground = skinId in listOf("cyberpunk", "astronaut", "pirate", "steampunk", "retro", "shadow", "royal", "lava_fire", "cosmic", "neon_cyber", "magic_aurora") || skinId == ""
    val titleColor = if (isDarkBackground) Color.White else Color(0xFF1C1B1F)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isDarkBackground) Color.White.copy(alpha = 0.2f) else Color(0xFFCAC4D0).copy(alpha = 0.6f),
                RoundedCornerShape(16.dp)
            )
            .testTag("stats_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDarkBackground) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = viewModel.getAnimalTierName(currentLevel).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkBackground) Color(0xFFFFD700) else Color(0xFF6750A4),
                    letterSpacing = 1.5.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "🐾 $liveClicksValue",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 36.sp,
                    color = titleColor,
                    shadow = Shadow(Color.Black.copy(alpha = 0.2f), blurRadius = 3f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("clicks_display")
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${viewModel.getAnimalEmoji(currentLevel)} Level $currentLevel: ${viewModel.getAnimalNameForLevel(currentLevel)}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = titleColor.copy(alpha = 0.9f)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isDarkBackground) Color.White.copy(alpha = 0.12f) else Color(0x1F6750A4))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ Automatics: %.1f/sec".format(cps),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkBackground) Color.White else Color(0xFF21005D)
                    )
                )
            }
        }
    }
}

data class FallingAnimal(
    val id: Long,
    val emoji: String,
    val xPercent: Float,
    val y: Float,
    val speed: Float,
    val rotationSpeed: Float,
    val scale: Float,
    val rotation: Float,
    val creationTime: Long,
    val xSpeed: Float = 0f
)

@Composable
fun SunburstBackground(skinId: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "sunburst_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunburst_angle"
    )

    val backgroundBrush = when (skinId) {
        "cyberpunk" -> Brush.verticalGradient(listOf(Color(0xFF0C001C), Color(0xFF2B0054)))
        "astronaut" -> Brush.verticalGradient(listOf(Color(0xFF0B192C), Color(0xFF1E3E62)))
        "god" -> Brush.verticalGradient(listOf(Color(0xFFFFF2D3), Color(0xFFFFFBEA)))
        "pirate" -> Brush.verticalGradient(listOf(Color(0xFF1A100C), Color(0xFF381F15)))
        else -> Brush.verticalGradient(listOf(Color(0xFF00B4DB), Color(0xFF0083B0))) // Classic Cookie Clicker blue sky gradient!
    }

    val rayColor = when (skinId) {
        "cyberpunk" -> Color(0xFFEC38BC).copy(alpha = 0.12f)
        "astronaut" -> Color(0xFF2196F3).copy(alpha = 0.15f)
        "god" -> Color(0xFFFFF200).copy(alpha = 0.18f)
        "pirate" -> Color(0xFFD4AF37).copy(alpha = 0.12f)
        else -> Color(0xFF63D2FF).copy(alpha = 0.22f) // Soft sky beam
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val numSpokes = 16
            val maxRadius = size.maxDimension * 1.2f
            
            rotate(angle, center) {
                val path = Path()
                val angleStep = 360f / numSpokes
                for (i in 0 until numSpokes step 2) {
                    val startAngleRad = Math.toRadians((i * angleStep).toDouble())
                    val endAngleRad = Math.toRadians(((i + 1) * angleStep).toDouble())
                    
                    path.reset()
                    path.moveTo(center.x, center.y)
                    path.lineTo(
                        center.x + (maxRadius * Math.cos(startAngleRad)).toFloat(),
                        center.y + (maxRadius * Math.sin(startAngleRad)).toFloat()
                    )
                    path.lineTo(
                        center.x + (maxRadius * Math.cos(endAngleRad)).toFloat(),
                        center.y + (maxRadius * Math.sin(endAngleRad)).toFloat()
                    )
                    path.close()
                    
                    drawPath(
                        path = path,
                        color = rayColor
                    )
                }
            }
        }
    }
}

@Composable
fun FallingAnimalsRain(
    fallingAnimalsProvider: () -> List<FallingAnimal>
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        val list = fallingAnimalsProvider()
        list.forEach { animal ->
            androidx.compose.runtime.key(animal.id) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = (animal.xPercent * screenWidthPx).coerceIn(0f, (screenWidthPx - 45.dp.toPx()).coerceAtLeast(0f))
                            translationY = animal.y
                            rotationZ = animal.rotation
                            scaleX = animal.scale
                            scaleY = animal.scale
                        }
                        .alpha(0.85f)
                ) {
                    Text(
                        text = animal.emoji,
                        fontSize = 32.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AutoClickerRing(numPointers: Int) {
    if (numPointers <= 0) return

    val infiniteTransition = rememberInfiniteTransition(label = "pointers_transition")
    val clickOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "click_offset"
    )

    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        for (i in 0 until numPointers) {
            val angleDeg = (i * 360f / numPointers)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            
            // Layout radius radiating from center
            val radiusDp = 138.dp - clickOffset.dp

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = (radiusDp.toPx() * Math.cos(angleRad)).toFloat()
                        translationY = (radiusDp.toPx() * Math.sin(angleRad)).toFloat()
                        rotationZ = angleDeg - 90f // Face inward towards center
                    }
            ) {
                Text(
                    text = "☝️",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 24.sp,
                        shadow = Shadow(Color.Black.copy(alpha = 0.25f), offset = Offset(1f, 2f), blurRadius = 3f)
                    )
                )
            }
        }
    }
}

fun formatCompactNumber(number: Long): String {
    return when {
        number >= 1_000_000_000 -> "%.1fB".format(number / 1_000_000_000f).replace(".0", "")
        number >= 1_000_000 -> "%.1fM".format(number / 1_000_000f).replace(".0", "")
        number >= 1_000 -> "%.1fk".format(number / 1_000f).replace(".0", "")
        else -> number.toString()
    }
}

@Composable
fun ClickerPlayground(
    state: PlayerState,
    viewModel: GameViewModel,
    floatingIndicators: List<ClickIndicator>,
    onFloatingIndicatorAdded: (String, Float, Float) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }

    // Bounce tap scale animation
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "click_scale_animation",
        finishedListener = {
            if (scale < 1f) {
                scale = 1f
            }
        }
    )

    val isDarkBackground = state.equippedSkinId in listOf("cyberpunk", "astronaut", "pirate", "steampunk", "retro", "shadow", "royal", "lava_fire", "cosmic", "neon_cyber", "magic_aurora") || state.equippedSkinId == ""
    val titleColor = if (isDarkBackground) Color.White else Color(0xFF1C1B1F)
    val subColor = if (isDarkBackground) Color(0xFFF3EDF7).copy(alpha = 0.9f) else Color(0xFF49454F)

    var fallingAnimals by remember { mutableStateOf(emptyList<FallingAnimal>()) }
    val currentLevel = state.currentLevel
    val cps = viewModel.calculateCPS(state)
    val liveClicks by viewModel.liveClicks.collectAsStateWithLifecycle()
    val isClickerLocked by viewModel.isClickerLocked.collectAsStateWithLifecycle()
    val playTabPressCount by viewModel.playTabPressCount.collectAsStateWithLifecycle()
    val autoclickerBanTimeRemaining by viewModel.autoclickerBanTimeRemaining.collectAsStateWithLifecycle()

    // A coroutine that spawns falling animals based on clicker speed (CPS)
    LaunchedEffect(cps, currentLevel) {
        if (cps > 0) {
            val delayMs = (1000 / cps).toLong().coerceIn(40, 2000)
            while (true) {
                delay(delayMs)
                val emoji = viewModel.getAnimalEmoji(currentLevel)
                val newAnimal = FallingAnimal(
                    id = System.nanoTime(),
                    emoji = emoji,
                    xPercent = Random.nextFloat(),
                    y = -80f,
                    speed = Random.nextFloat() * 5f + 4f,
                    rotationSpeed = Random.nextFloat() * 12f - 6f,
                    scale = Random.nextFloat() * 0.2f + 0.35f, // smaller scale
                    rotation = Random.nextFloat() * 360f,
                    creationTime = System.currentTimeMillis(),
                    xSpeed = Random.nextFloat() * 0.04f - 0.02f // random horizontal drift
                )
                fallingAnimals = (fallingAnimals + newAnimal).take(35)
            }
        }
    }

    // Smooth frame tick updater
    LaunchedEffect(Unit) {
        while (true) {
            delay(30)
            if (fallingAnimals.isNotEmpty()) {
                fallingAnimals = fallingAnimals.mapNotNull { animal ->
                    val newY = animal.y + animal.speed
                    val newX = (animal.xPercent + animal.xSpeed).coerceIn(0f, 1f)
                    val newRotation = animal.rotation + animal.rotationSpeed
                    if (newY > 1800f) {
                        null
                    } else {
                        animal.copy(y = newY, xPercent = newX, rotation = newRotation)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("clicker_playground_screen"),
        contentAlignment = Alignment.Center
    ) {
        // 1. Sunburst Background
        SunburstBackground(skinId = state.equippedSkinId)

        // 2. Falling Animal Rain
        FallingAnimalsRain(fallingAnimalsProvider = { fallingAnimals })

        // 3. Main Gameplay Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            // Immersive Top HUD (replaces large StatsHeaderWidget on Tab 0)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .border(
                        1.dp,
                        if (isDarkBackground) Color.White.copy(alpha = 0.2f) else Color(0xFFCAC4D0).copy(alpha = 0.6f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isDarkBackground) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.getAnimalTierName(currentLevel).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkBackground) Color(0xFFFFD700) else Color(0xFF6750A4),
                            letterSpacing = 1.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "🐾 ${liveClicks ?: state.currentClicks}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 36.sp,
                            color = titleColor,
                            shadow = Shadow(Color.Black.copy(alpha = 0.2f), blurRadius = 3f)
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${viewModel.getAnimalEmoji(currentLevel)} Level $currentLevel: ${viewModel.getAnimalNameForLevel(currentLevel)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = titleColor.copy(alpha = 0.9f)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDarkBackground) Color.White.copy(alpha = 0.12f) else Color(0x1F6750A4))
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ Automatics: %.1f/sec".format(cps),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkBackground) Color.White else Color(0xFF21005D)
                            )
                        )
                    }
                }
            }

            // Central Clicker Animal Box with Absolute overlay multiplier badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                val numPointers = remember(state) {
                    val totalUpgrades = state.hamsterWheelCount + state.catScratchCount + state.dogBoneCount
                    totalUpgrades.coerceAtMost(16)
                }
                AutoClickerRing(numPointers = numPointers)

                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                        .testTag("big_animal_clicker")
                        .multiTouchClickable(!isClickerLocked) { offset ->
                            scale = 0.85f
                            viewModel.onAnimalClicked()
                            
                            val xPos = offset.x - 115f
                            val yPos = offset.y - 115f
                            onFloatingIndicatorAdded("+${viewModel.calculateClicksPerTap(state)}", xPos, yPos)

                            val tapAnimal = FallingAnimal(
                                id = System.nanoTime(),
                                emoji = viewModel.getAnimalEmoji(state.currentLevel),
                                xPercent = (offset.x / 230f).coerceIn(0f, 1f),
                                y = 220f,
                                speed = Random.nextFloat() * 4f + 6f,
                                rotationSpeed = Random.nextFloat() * 30f - 15f,
                                scale = Random.nextFloat() * 0.3f + 0.5f,
                                rotation = Random.nextFloat() * 360f,
                                creationTime = System.currentTimeMillis(),
                                xSpeed = Random.nextFloat() * 0.04f - 0.02f
                            )
                            fallingAnimals = (fallingAnimals + tapAnimal).take(35)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val skinModifier = when (state.equippedSkinId) {
                        "cyberpunk" -> Modifier
                            .fillMaxSize()
                            .border(4.dp, Brush.linearGradient(listOf(Color.Cyan, Color.Magenta)), CircleShape)
                            .background(Color.Black, CircleShape)
                        "astronaut" -> Modifier
                            .fillMaxSize()
                            .border(4.dp, Brush.linearGradient(listOf(Color(0xFF3F51B5), Color(0xFF2196F3))), CircleShape)
                            .background(Color(0xFF1A237E), CircleShape)
                        "god" -> Modifier
                            .fillMaxSize()
                            .border(6.dp, Brush.linearGradient(listOf(Color(0xFFFFDF00), Color(0xFFFFBF00))), CircleShape)
                            .background(Color.White, CircleShape)
                        "pirate" -> Modifier
                            .fillMaxSize()
                            .border(4.dp, Color(0xFF8B4513), RoundedCornerShape(24.dp))
                            .background(Color(0xFF3E2723), RoundedCornerShape(24.dp))
                        "steampunk" -> Modifier
                            .fillMaxSize()
                            .border(5.dp, Brush.linearGradient(listOf(Color(0xFF8B6508), Color(0xFFCD853F), Color(0xFF8B6508))), RoundedCornerShape(32.dp))
                            .background(Color(0xFF2B1B17), RoundedCornerShape(32.dp))
                        "retro" -> Modifier
                            .fillMaxSize()
                            .border(6.dp, Color(0xFF00FF00), RoundedCornerShape(0.dp))
                            .background(Color.Black, RoundedCornerShape(0.dp))
                        "shadow" -> Modifier
                            .fillMaxSize()
                            .border(4.dp, Brush.linearGradient(listOf(Color(0xFF1C1C1C), Color(0xFFFF0000))), CircleShape)
                            .background(Color(0xFF0D0D0D), CircleShape)
                        "royal" -> Modifier
                            .fillMaxSize()
                            .border(5.dp, Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFDAA520))), CircleShape)
                            .background(Color(0xFF4B0082), CircleShape)
                        "lava_fire" -> Modifier
                            .fillMaxSize()
                            .border(5.dp, Brush.linearGradient(listOf(Color(0xFF801000), Color(0xFFFF4500))), CircleShape)
                            .background(Color(0xFF2B0F00), CircleShape)
                        "cosmic" -> Modifier
                            .fillMaxSize()
                            .border(6.dp, Brush.linearGradient(listOf(Color(0xFFBA55D3), Color(0xFF4B0082), Color(0xFF00FFFF))), CircleShape)
                            .background(Color(0xFF0C0728), CircleShape)
                        "neon_cyber" -> Modifier
                            .fillMaxSize()
                            .border(5.dp, Brush.linearGradient(listOf(Color(0xFF00334D), Color(0xFF00F0FF))), CircleShape)
                            .background(Color(0xFF00111A), CircleShape)
                        "magic_aurora" -> Modifier
                            .fillMaxSize()
                            .border(5.dp, Brush.linearGradient(listOf(Color(0xFF3A0066), Color(0xFF00FFCC))), CircleShape)
                            .background(Color(0xFF0F001D), CircleShape)
                        else -> Modifier
                            .fillMaxSize()
                            .border(8.dp, Color.White, CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFFD0BCFF), Color(0xFFEADDFF))),
                                CircleShape
                            )
                    }

                    Box(
                        modifier = skinModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.equippedSkinId == "god") {
                            GodGlowRaysWidget()
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = viewModel.getAnimalEmoji(state.currentLevel),
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 110.sp,
                                    shadow = Shadow(Color.Black.copy(alpha = 0.15f), offset = Offset(4f, 8f), blurRadius = 8f)
                                )
                            )

                            Text(
                                text = when (state.equippedSkinId) {
                                    "cyberpunk" -> "🕶️ Cyber Mode"
                                    "pirate" -> "🏴‍☠️ Pirate Cap'n"
                                    "god" -> "👑 Golden Divine"
                                    "steampunk" -> "⚙️ Steam Brass"
                                    "retro" -> "👾 8-Bit Retro"
                                    "shadow" -> "🥷 Shadow Ninja"
                                    "royal" -> "👑 Royal Crown"
                                    "lava_fire" -> "🌋 Volcanic Fury"
                                    "cosmic" -> "🌌 Cosmic Nebula"
                                    "neon_cyber" -> "⚡ Hyper Grid"
                                    "magic_aurora" -> "🔮 Elven Aurora"
                                    else -> "❤️ Level ${state.currentLevel}"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.equippedSkinId == "god") Color(0xFFC5A02B) else if (isDarkBackground) Color.White else Color(0xFF49454F)
                                )
                            )
                        }
                    }
                }

                // Click Multiplier Badge in top-right absolute overlay position
                val tapPower = viewModel.calculateClicksPerTap(state)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-10).dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFB3261E))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+$tapPower per tap",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Skin Unlock Tracker Spacer + Text
            val nextLockedSkin = when {
                state.totalClicks < 200 -> Pair("cyberpunk", 200L)
                state.totalClicks < 1000 -> Pair("pirate", 1000L)
                state.totalClicks < 5000 -> Pair("astronaut", 5000L)
                state.totalClicks < 25000 -> Pair("god", 25000L)
                state.totalClicks < 50000 -> Pair("steampunk", 50000L)
                state.totalClicks < 150000 -> Pair("retro", 150000L)
                state.totalClicks < 500000 -> Pair("shadow", 500000L)
                state.totalClicks < 2000000 -> Pair("royal", 2000000L)
                state.totalClicks < 5000000 -> Pair("lava_fire", 5000000L)
                state.totalClicks < 10000000 -> Pair("cosmic", 10000000L)
                state.totalClicks < 100000000 -> Pair("neon_cyber", 100000000L)
                state.totalClicks < 1000000000 -> Pair("magic_aurora", 1000000000L)
                else -> null
            }

            if (nextLockedSkin != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("skin_unlock_tracker")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (isDarkBackground) Color.Black.copy(alpha = 0.5f) else Color(0xFFF3EDF7),
                                RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, if (isDarkBackground) Color.White.copy(alpha = 0.2f) else Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = if (isDarkBackground) Color(0xFFFFD700) else Color(0xFF6750A4),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val milestoneText = formatCompactNumber(nextLockedSkin.second)
                        Text(
                            text = "Next Skin: $milestoneText Clicks",
                            color = if (isDarkBackground) Color.White else Color(0xFF1D192B),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewModel.getSkinNameForId(nextLockedSkin.first).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isDarkBackground) Color.White.copy(alpha = 0.6f) else Color(0xFF49454F).copy(alpha = 0.6f),
                            letterSpacing = 1.5.sp
                        )
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // 4. Overlaid Rising Indicators Layer
        floatingIndicators.forEach { indicator ->
            val elapsed = System.currentTimeMillis() - indicator.creationTime
            val t = (elapsed.toFloat() / 800f).coerceIn(0f, 1f)
            val easeOutQuadT = t * (2f - t)
            val verticalTranslation = -300f * easeOutQuadT

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = indicator.xOffset.toInt(),
                            y = (indicator.yOffset + verticalTranslation).toInt()
                        )
                    }
                    .graphicsLayer {
                        this.alpha = (1f - t)
                        this.scaleX = 1f + elapsed.toFloat() / 1600f
                        this.scaleY = 1f + elapsed.toFloat() / 1600f
                    }
            ) {
                Text(
                    text = indicator.text,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = when (state.equippedSkinId) {
                            "cyberpunk" -> Color.Cyan
                            "astronaut" -> Color(0xFF2196F3)
                            "god" -> Color(0xFFFFD700)
                            "pirate" -> Color(0xFFCD853F)
                            "steampunk" -> Color(0xFFCD853F)
                            "retro" -> Color(0xFF00FF00)
                            "shadow" -> Color.Red
                            "royal" -> Color(0xFFBA55D3)
                            "lava_fire" -> Color(0xFFFF4500)
                            "cosmic" -> Color(0xFF00FFFF)
                            "neon_cyber" -> Color(0xFF00F0FF)
                            "magic_aurora" -> Color(0xFF00FFCC)
                            else -> Color.White
                        },
                        shadow = Shadow(Color.Black, offset = Offset(1f, 1f), blurRadius = 4f)
                    )
                )
            }
        }

        // Clicker lock overlay removed from here and moved globally to cover all tabs
    }
}

@Composable
fun BackgroundAmbientAura() {
    val infiniteTransition = rememberInfiniteTransition(label = "aura_transition")
    val pulseRatio by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ratio"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.08f)
    ) {
        drawCircle(
            color = Color.White,
            radius = size.minDimension * 0.4f * pulseRatio,
            center = Offset(size.width * 0.5f, size.height * 0.4f)
        )
    }
}

@Composable
fun GodGlowRaysWidget() {
    val infiniteTransition = rememberInfiniteTransition(label = "rays_transition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rays_rotation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.2f)
            .graphicsLayer {
                rotationZ = rotation
            }
    ) {
        val count = 12
        val length = size.maxDimension * 0.6f
        val center = Offset(size.width / 2, size.height / 2)
        for (i in 0 until count) {
            val angle = i * (360f / count)
            val rad = Math.toRadians(angle.toDouble())
            val endX = center.x + length * sin(rad).toFloat()
            val endY = center.y + length * Math.cos(rad).toFloat()
            drawLine(
                color = Color(0xFFFFD700),
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 14f
            )
        }
    }
}

@Composable
fun ShopUpgradesPanel(
    state: PlayerState,
    viewModel: GameViewModel
) {
    val itemsList = remember(
        state.clickPowerLevel,
        state.hamsterWheelCount,
        state.catScratchCount,
        state.dogBoneCount,
        state.dragonFlameCount,
        state.elephantStampedeCount,
        state.cheetahNitroCount,
        state.phoenixFlightCount,
        state.blackHoleCount
    ) {
        listOf(
            ShopItemData(
                id = "click_power",
                title = "👉 Heavy Paws (Tap Up 1)",
                description = "Increases clicks gained per tap manually. Strengthens on high Level.",
                multiplierLabel = "+1 Clicks/Tap",
                baseCost = 15,
                currentCount = state.clickPowerLevel
            ),
            ShopItemData(
                id = "hamster_wheel",
                title = "🐹 Hamster Wheel (CPS Up 1)",
                description = "Auto-click helper. Simple but stable running speed.",
                multiplierLabel = "+0.5 Clicks/sec",
                baseCost = 50,
                currentCount = state.hamsterWheelCount
            ),
            ShopItemData(
                id = "cat_scratch",
                title = "😼 Cat Scratch (Tap Up 2)",
                description = "Harness razor-sharp feline motivation to increase Tap power.",
                multiplierLabel = "+5 Clicks/Tap",
                baseCost = 250,
                currentCount = state.catScratchCount
            ),
            ShopItemData(
                id = "dog_bone",
                title = "🍖 Dog Bone (CPS Up 2)",
                description = "Unleash persistent puppy energy for big click speeds.",
                multiplierLabel = "+20.0 Clicks/sec",
                baseCost = 1200,
                currentCount = state.dogBoneCount
            ),
            ShopItemData(
                id = "dragon_flame",
                title = "🔥 Dragon Flame (Tap Up 3)",
                description = "Summon ancient dragon energy for ultimate manual Tap power.",
                multiplierLabel = "+50 Clicks/Tap",
                baseCost = 6000,
                currentCount = state.dragonFlameCount
            ),
            ShopItemData(
                id = "elephant_stampede",
                title = "🐘 Elephant Stampede (CPS Up 3)",
                description = "An incredibly heavy and rhythmic stomping machine helper.",
                multiplierLabel = "+200.0 Clicks/sec",
                baseCost = 35000,
                currentCount = state.elephantStampedeCount
            ),
            ShopItemData(
                id = "cheetah_nitro",
                title = "🐆 Cheetah Speed (Tap Up 4)",
                description = "Super-charged feline velocity for rapid boost on Tap.",
                multiplierLabel = "+1000 Clicks/Tap",
                baseCost = 200000,
                currentCount = state.cheetahNitroCount
            ),
            ShopItemData(
                id = "phoenix_flight",
                title = "🦅 Phoenix Flight (CPS Up 4)",
                description = "Regenerative cosmic heat loops automatically click.",
                multiplierLabel = "+5000.0 Clicks/sec",
                baseCost = 1500000,
                currentCount = state.phoenixFlightCount
            ),
            ShopItemData(
                id = "black_hole",
                title = "🕳️ Singularity (Tap Up 5)",
                description = "A powerful spatial warp that boosts manual clicks heavily.",
                multiplierLabel = "+30000 Clicks/Tap",
                baseCost = 12000000,
                currentCount = state.blackHoleCount
            )
        )
    }

    val isDarkBackground = state.equippedSkinId in listOf("cyberpunk", "astronaut", "pirate")
    val titleTextColor = if (isDarkBackground) Color.White else Color(0xFF1C1B1F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shop_panel"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🛒 PET BOOST SHOP",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = titleTextColor,
                letterSpacing = 1.5.sp
            ),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(itemsList) { item ->
                val cost = viewModel.getUpgradeCost(item.id, item.currentCount)
                val canAfford = state.currentClicks >= cost

                val cardBg = if (isDarkBackground) Color.White.copy(alpha = 0.15f) else Color.White
                val cardBorder = if (isDarkBackground) Color.White.copy(alpha = 0.2f) else Color(0xFFE6E1E5)
                val textMainColor = if (isDarkBackground) Color.White else Color(0xFF1C1B1F)
                val textSubColor = if (isDarkBackground) Color.White.copy(alpha = 0.7f) else Color(0xFF49454F)

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .testTag("shop_item_${item.id}"),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = cardBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Soft colored icon chip matching HTML specifications
                        val chipBg = when (item.id) {
                            "click_power" -> Color(0xFFFFD8E4) // VibrantSoftPink
                            "hamster_wheel" -> Color(0xFFC2E7FF) // VibrantSoftBlue
                            "cat_scratch" -> Color(0xFFFFD8E4)
                            "dog_bone" -> Color(0xFFC2E7FF)
                            "dragon_flame" -> Color(0xFFEADDFF) // Purple container
                            "elephant_stampede" -> Color(0xFFE3E2E6) // Grey container
                            "cheetah_nitro" -> Color(0xFFFFDCC0) // Orange container
                            "phoenix_flight" -> Color(0xFFFFDAD6) // Red container
                            "black_hole" -> Color(0xFFE8DDFF) // Dark purple container
                            else -> Color(0xFFEADDFF)
                        }
                        val chipIconColor = when (item.id) {
                            "click_power" -> Color(0xFF88014F)
                            "hamster_wheel" -> Color(0xFF004A77)
                            "cat_scratch" -> Color(0xFF88014F)
                            "dog_bone" -> Color(0xFF004A77)
                            "dragon_flame" -> Color(0xFF21005D)
                            "elephant_stampede" -> Color(0xFF43474E)
                            "cheetah_nitro" -> Color(0xFF863400)
                            "phoenix_flight" -> Color(0xFFBA1A1A)
                            "black_hole" -> Color(0xFF320099)
                            else -> Color(0xFF21005D)
                        }
                        val chipIcon = when (item.id) {
                            "click_power" -> Icons.Filled.TouchApp
                            "hamster_wheel" -> Icons.Filled.DirectionsRun
                            "cat_scratch" -> Icons.Filled.Pets
                            "dog_bone" -> Icons.Filled.Favorite
                            "dragon_flame" -> Icons.Filled.Whatshot
                            "elephant_stampede" -> Icons.Filled.FitnessCenter
                            "cheetah_nitro" -> Icons.Filled.Speed
                            "phoenix_flight" -> Icons.Filled.LocalFireDepartment
                            "black_hole" -> Icons.Filled.Loop
                            else -> Icons.Filled.Stars
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = chipIcon,
                                contentDescription = item.title,
                                tint = chipIconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = textMainColor,
                                        fontSize = 13.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(1.dp))

                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = textSubColor.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(1.dp))

                            Text(
                                text = item.multiplierLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6750A4),
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { viewModel.buyUpgrade(item.id) },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6750A4),
                                    disabledContainerColor = if (isDarkBackground) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .testTag("buy_button_${item.id}"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "BUY",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "🐾 ${formatCompactNumber(cost)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Owned: ${item.currentCount}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (isDarkBackground) Color.White.copy(alpha = 0.9f) else Color(0xFF6750A4)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ShopItemData(
    val id: String,
    val title: String,
    val description: String,
    val multiplierLabel: String,
    val baseCost: Long,
    val currentCount: Int
)

@Composable
fun SkinsWardrobePanel(
    state: PlayerState,
    viewModel: GameViewModel
) {
    val listSkins = remember {
        listOf(
            SkinData("standard", "Classic Cutie", "Classic appearance matching your animal level.", "🐹, 🐱, 🐶, 🐰, 🐵, 🐢...", 0, "🐾 Standard Power"),
            SkinData("cyberpunk", "Cyber Neon", "Pulsating holographic frame and glowing overlays.", "⚡ Neon Accent", 200, "+2 Bonus Clicks per tap"),
            SkinData("pirate", "Pirate Cap'n", "Ahoy! Equipped with a pirate hat and eyepatch details.", "🏴‍☠️ Pirate theme", 1000, "+5 Bonus Clicks per tap"),
            SkinData("astronaut", "Space Astro", "Sleek astronaut cosmic suit with high-tech helmet.", "👨‍🚀 Space Helmet", 5000, "+15 Bonus Clicks per tap"),
            SkinData("god", "Golden Divine", "Blinding divine light and legendary golden sun ray halo.", "👑 God rays halo", 25000, "+50 Bonus Clicks per tap"),
            SkinData("steampunk", "Steam Brass", "Antique brass finish with rotating intricate golden gears.", "⚙️ Brass Gears", 50000, "+100 Bonus Clicks per tap"),
            SkinData("retro", "8-Bit Retro", "Retro arcade monitor border with pixel green CRT glow.", "👾 Green Glow", 150000, "+250 Bonus Clicks per tap"),
            SkinData("shadow", "Shadow Ninja", "Sleek crimson ninja mask wrapped in shadow dust.", "🥷 Stealth Dust", 500000, "+750 Bonus Clicks per tap"),
            SkinData("royal", "Royal Crown", "Velvet royal purple frame seated with shimmering monarch crowns.", "👑 Velvet Crown", 2000000, "+2.5k Bonus Clicks per tap"),
            SkinData("lava_fire", "Volcanic Fury", "Molten volcanic magma aura with crackling embers.", "🌋 Volcanic Aura", 5000000, "+5.0k Bonus Clicks per tap"),
            SkinData("cosmic", "Cosmic Nebula", "A moving planetary outer orbit with glowing neon rings.", "🌌 Starry Rings", 10000000, "+10k Bonus Clicks per tap"),
            SkinData("neon_cyber", "Hyper Grid", "A futuristic holographic computing grid with matrix coding.", "⚡ Hyper Grid", 100000000, "+50k Bonus Clicks per tap"),
            SkinData("magic_aurora", "Elven Aurora", "Magical northern lights glowing with enchanted spell circles.", "🔮 Northern Glow", 1000000000, "+250k Bonus Clicks per tap")
        )
    }

    val unlockedList = remember(state.unlockedSkins) {
        state.unlockedSkins.split(",").map { it.trim() }.toSet()
    }

    val isDarkBackground = state.equippedSkinId in listOf("cyberpunk", "astronaut", "pirate", "steampunk", "retro", "shadow", "royal", "lava_fire", "cosmic", "neon_cyber", "magic_aurora")
    val titleTextColor = if (isDarkBackground) Color.White else Color(0xFF1C1B1F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("skins_panel"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎨 SKINS WARDROBE",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = titleTextColor,
                letterSpacing = 1.5.sp
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(listSkins) { skin ->
                val isUnlocked = unlockedList.contains(skin.id)
                val isEquipped = state.equippedSkinId == skin.id

                val cardBg = if (isEquipped) {
                    if (isDarkBackground) Color(0xFFEADDFF).copy(alpha = 0.95f) else Color(0xFFEADDFF)
                } else {
                    if (isDarkBackground) Color.White.copy(alpha = 0.15f) else Color.White
                }
                val cardBorder = if (isEquipped) {
                    Color(0xFF6750A4)
                } else {
                    if (isDarkBackground) Color.White.copy(alpha = 0.2f) else Color(0xFFE6E1E5)
                }
                val textMainColor = if (isEquipped) {
                    Color(0xFF21005D)
                } else {
                    if (isDarkBackground) Color.White else Color(0xFF1C1B1F)
                }
                val textSubColor = if (isEquipped) {
                    Color(0xFF21005D).copy(alpha = 0.8f)
                } else {
                    if (isDarkBackground) Color.White.copy(alpha = 0.7f) else Color(0xFF49454F)
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                        .testTag("skin_item_${skin.id}"),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = cardBg
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = skin.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = textMainColor
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (isEquipped) {
                                    Badge(
                                        containerColor = Color(0xFF6750A4),
                                        contentColor = Color.White
                                    ) {
                                        Text("Active")
                                    }
                                } else if (!isUnlocked) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = "Locked",
                                        tint = if (isDarkBackground) Color.White.copy(alpha = 0.5f) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Unlocked at: ${skin.clicksMilestone} clicks.",
                                style = MaterialTheme.typography.labelSmall,
                                color = textSubColor
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Power: ${skin.bonusDescription}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF6750A4)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isUnlocked) {
                            Button(
                                onClick = { viewModel.equipSkin(skin.id) },
                                enabled = !isEquipped,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6750A4),
                                    disabledContainerColor = if (isDarkBackground) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .testTag("equip_button_${skin.id}")
                                    .minimumInteractiveComponentSize(),
                                shape = RoundedCornerShape(12.dp)
                             ) {
                                Text(
                                    text = if (isEquipped) "ACTIVE" else "EQUIP",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            val remaining = skin.clicksMilestone - state.totalClicks
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LinearProgressIndicator(
                                    progress = { (state.totalClicks.toFloat() / skin.clicksMilestone).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = Color(0xFF6750A4),
                                    trackColor = Color(0xFFE6E1E5)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Needs $remaining 🐾",
                                    color = textSubColor,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SkinData(
    val id: String,
    val name: String,
    val description: String,
    val visualAttributes: String,
    val clicksMilestone: Int,
    val bonusDescription: String
)

@Composable
fun UnlocksProgressionPanel(
    state: PlayerState,
    viewModel: GameViewModel
) {
    val isDarkBackground = state.equippedSkinId in listOf("cyberpunk", "astronaut", "pirate", "steampunk", "retro", "shadow", "royal", "lava_fire", "cosmic", "neon_cyber", "magic_aurora")
    val titleTextColor = if (isDarkBackground) Color.White else Color(0xFF1C1B1F)

    val currentLevel = state.currentLevel
    val totalClicks = state.totalClicks

    val levelsList = remember {
        listOf(
            UnlockItem(1, "🐹", "Cute Hamster", 0L, "Standard starting hamster wheel buddy! No requirements."),
            UnlockItem(2, "🐱", "Playful Kitty", 200L, "Quick and mischievous feline helper. Unlocks at 200 total clicks."),
            UnlockItem(3, "🐶", "Loyal Doggy", 1000L, "Loyal canine companion for increased tap multiplier. Unlocks at 1,000."),
            UnlockItem(4, "🐰", "Fluffy Bunny", 5000L, "A soft hop-along bunny that doubles your tap effectiveness. Unlocks at 5,000."),
            UnlockItem(5, "🐵", "Cheeky Monkey", 25000L, "A highly active monkey that loves to push keys for you. Unlocks at 25,000."),
            UnlockItem(6, "🐢", "Tiny Turtle", 100000L, "A slow but extremely robust protector of multipliers. Unlocks at 100,000."),
            UnlockItem(7, "🦜", "Colorful Parrot", 500000L, "A talkative bird repeating every tap exponentially. Unlocks at 500,000."),
            UnlockItem(8, "🦊", "Clever Fox", 2500000L, "Clever and cunning fox multiplying click speed with intelligence. Unlocks at 2,500,000."),
            UnlockItem(9, "🦁", "Majestic Lion", 12000000L, "King of the savanna bringing loud ferocious roars and absolute click authority. Unlocks at 12,000,000."),
            UnlockItem(10, "🐼", "Friendly Panda", 60000000L, "A lovable soft panda bringing Zen focus and peaceful giant clicks. Unlocks at 60,000,000."),
            UnlockItem(11, "🐯", "Fearsome Tiger", 300000000L, "A wild and fearsome tiger matching rapid strikes of the click engine. Unlocks at 300,000,000."),
            UnlockItem(12, "🐋", "Giant Whale", 1500000000L, "A giant ocean titan singing majestic high frequency waves of auto-clicks. Unlocks at 1,500,000,000."),
            UnlockItem(13, "🦍", "Silverback Gorilla", 8000000000L, "A powerful forest leader drumming immense clicks into existence. Unlocks at 8,000,000,000."),
            UnlockItem(14, "🦈", "Great White Shark", 40000000000L, "Apex ocean tracker charging forward with massive biting click boosts. Unlocks at 40,000,000,000."),
            UnlockItem(15, "🐘", "Wise Elephant", 200000000000L, "A majestic and extremely memory-rich titan with infinite click mastery. Unlocks at 200,000,000,000.")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("unlocks_progression_panel"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🐾 ANIMAL EVOLUTION PROGRESS",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = titleTextColor,
                letterSpacing = 1.5.sp
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Banner showcasing current animal profile
        val bannerBg = if (isDarkBackground) Color(0xFFEADDFF).copy(alpha = 0.95f) else Color(0xFFEADDFF)
        val bannerBorder = if (isDarkBackground) Color(0xFF6750A4) else Color(0xFFCAC4D0)
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, bannerBorder, RoundedCornerShape(12.dp))
                .padding(bottom = 12.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = bannerBg
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.getAnimalEmoji(currentLevel),
                        fontSize = 32.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "CURRENT ANIMAL: LEVEL $currentLevel",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF21005D)
                        )
                    )
                    Text(
                        text = "${viewModel.getAnimalNameForLevel(currentLevel)} (${viewModel.getAnimalEmoji(currentLevel)})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D)
                        )
                    )
                }
            }
        }

        // Evolution items list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(levelsList) { item ->
                val isUnlocked = currentLevel >= item.level
                val isNextUnlock = currentLevel + 1 == item.level

                val cardBg = if (isUnlocked) {
                    if (isDarkBackground) Color(0xFFEADDFF).copy(alpha = 0.95f) else Color(0xFFEADDFF)
                } else if (isNextUnlock) {
                    if (isDarkBackground) Color(0xFF21005D).copy(alpha = 0.6f) else Color(0xFFFFD8E4)
                } else {
                    if (isDarkBackground) Color.White.copy(alpha = 0.08f) else Color(0xFFF3EDF7)
                }

                val cardBorder = if (isUnlocked) {
                    Color(0xFF6750A4)
                } else if (isNextUnlock) {
                    Color(0xFF88014F)
                } else {
                    if (isDarkBackground) Color.White.copy(alpha = 0.15f) else Color(0xFFCAC4D0).copy(alpha = 0.5f)
                }

                val textMainColor = if (isUnlocked) {
                    Color(0xFF21005D)
                } else if (isNextUnlock) {
                    if (isDarkBackground) Color.White else Color(0xFF3B001A)
                } else {
                    if (isDarkBackground) Color.White.copy(alpha = 0.5f) else Color(0xFF1C1B1F).copy(alpha = 0.5f)
                }

                val textSubColor = if (isUnlocked) {
                    Color(0xFF21005D).copy(alpha = 0.8f)
                } else if (isNextUnlock) {
                    if (isDarkBackground) Color.White.copy(alpha = 0.8f) else Color(0xFF3B001A).copy(alpha = 0.8f)
                } else {
                    if (isDarkBackground) Color.White.copy(alpha = 0.35f) else Color(0xFF49454F).copy(alpha = 0.5f)
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .testTag("unlock_item_card_${item.level}"),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = cardBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isUnlocked) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.emoji,
                                        fontSize = 24.sp,
                                        modifier = Modifier.alpha(if (isUnlocked) 1.0f else 0.4f)
                                    )
                                    if (!isUnlocked) {
                                        Icon(
                                            Icons.Filled.Lock,
                                            contentDescription = "Locked",
                                            tint = textMainColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Lv ${item.level}: ${item.name}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = textMainColor
                                    )
                                    Text(
                                        text = if (item.clicksRequired == 0L) "Free Unlock" else "Requires ${item.clicksRequired} Total clicks",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textSubColor
                                    )
                                }
                            }

                            // Badge state
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isUnlocked) Color(0xFF386A20).copy(alpha = 0.15f)
                                        else if (isNextUnlock) Color(0xFFBA1A1A).copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isUnlocked) "UNLOCKED ✅" else if (isNextUnlock) "NEXT UP ⚡" else "LOCKED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isUnlocked) Color(0xFF386A20) else if (isNextUnlock) Color(0xFFBA1A1A) else textMainColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = textSubColor
                        )

                        if (isNextUnlock) {
                            val previousLevelThreshold = when (item.level) {
                                1 -> 0L
                                2 -> 0L
                                3 -> 200L
                                4 -> 1000L
                                5 -> 5000L
                                6 -> 25000L
                                7 -> 100000L
                                8 -> 500000L
                                9 -> 2500000L
                                10 -> 12000000L
                                11 -> 60000000L
                                12 -> 300000000L
                                13 -> 1500000000L
                                14 -> 8000000000L
                                15 -> 40000000000L
                                else -> 0L
                            }
                            val fraction = if (item.clicksRequired > previousLevelThreshold) {
                                ((totalClicks - previousLevelThreshold).toFloat() / (item.clicksRequired - previousLevelThreshold).toFloat()).coerceIn(0f, 1f)
                            } else {
                                1f
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Evolution Progress",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textSubColor
                                    )
                                    Text(
                                        text = "${(fraction * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = textMainColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (isDarkBackground) Color.White else Color(0xFF6750A4),
                                    trackColor = if (isDarkBackground) Color.White.copy(alpha = 0.2f) else Color(0xFFE8DEF8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class UnlockItem(
    val level: Int,
    val emoji: String,
    val name: String,
    val clicksRequired: Long,
    val description: String
)

@Composable
fun CelebratingAlert(
    title: String,
    subtitle: String,
    description: String,
    emoji: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large visual emoji with glow ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 54.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AWESOME! TAP TO CONTINUE",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
