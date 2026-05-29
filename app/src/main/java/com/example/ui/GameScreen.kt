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

    var activeTab by remember { mutableIntStateOf(0) } // 0: Clicker, 1: Shop, 2: Skins, 3: Leaderboard

    // Local animated float indicators
    var floatingIndicators by remember { mutableStateOf(emptyList<ClickIndicator>()) }
    val coroutineScope = rememberCoroutineScope()

    // Alert states
    var levelUpAlert by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var skinUnlockedAlert by remember { mutableStateOf<Pair<String, String>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_scaffold"),
        topBar = {
            val safeState = playerState
            val currentLvl = safeState?.currentLevel ?: 1
            val animalName = safeState?.let { viewModel.getAnimalNameForLevel(it.currentLevel) } ?: ""
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pets,
                            contentDescription = "Critter Icon",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Critter Clicker",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(Color(0xFFEADDFF), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stars,
                            contentDescription = "Level Badge",
                            tint = Color(0xFF21005D),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "LVL $currentLvl",
                            color = Color(0xFF21005D),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF7F2FA)
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(width = 1.dp, color = Color(0xFFCAC4D0).copy(alpha = 0.5f))
                    .testTag("app_navigation_bar"),
                containerColor = Color(0xFFF3EDF7),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(if (activeTab == 0) Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow, contentDescription = "Play") },
                    label = { Text("Play") },
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
                    onClick = { activeTab = 1 },
                    icon = { Icon(if (activeTab == 1) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart, contentDescription = "Shop") },
                    label = { Text("Shop") },
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
                    label = { Text("Skins") },
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
                    label = { Text("Unlocks") },
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
                    .padding(paddingValues)
            ) {
                // Background particle drawings for Cosmic skins
                if (safeState.equippedSkinId == "god") {
                    BackgroundAmbientAura()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Global Stats Panel Headers
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
    val nextLevelTarget = when (currentLevel) {
        1 -> 200L
        2 -> 1000L
        3 -> 5000L
        4 -> 25000L
        5 -> 100000L
        6 -> 500000L
        7 -> 2500000L
        8 -> 12000000L
        9 -> 60000000L
        else -> -1L
    }

    val isDarkBackground = skinId in listOf("cyberpunk", "astronaut", "pirate", "steampunk", "retro", "shadow", "cosmic") || activeTab == 0
    val cardBg = if (isDarkBackground) Color.Black.copy(alpha = 0.45f) else Color(0xFFF7F2FA).copy(alpha = 0.95f)
    val cardBorder = if (isDarkBackground) Color.White.copy(alpha = 0.2f) else Color(0xFFCAC4D0).copy(alpha = 0.6f)
    val labelColor = if (isDarkBackground) Color.White.copy(alpha = 0.7f) else Color(0xFF49454F)
    val valueColor = if (isDarkBackground) Color.White else Color(0xFF1C1B1F)
    val primaryColor = if (isDarkBackground) Color(0xFFFFD700) else Color(0xFF6750A4)
    val secondaryColor = if (isDarkBackground) Color(0xFFEADDFF) else Color(0xFF21005D)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .testTag("stats_header_card"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardBg
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL CLICKS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = labelColor,
                            letterSpacing = 1.3.sp
                        )
                    )
                    Text(
                        text = "🐾 $liveClicksValue",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = valueColor,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.1f),
                                offset = Offset(1f, 3f),
                                blurRadius = 4f
                            )
                        ),
                        modifier = Modifier.testTag("clicks_display")
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "AUTO-CLICKS / SEC",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = labelColor,
                            letterSpacing = 1.3.sp
                        )
                    )
                    Text(
                        text = "⚡ %.1f clicks".format(cps),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar to next Level
            if (nextLevelTarget != -1L) {
                val previousLevelThreshold = when (currentLevel) {
                    1 -> 0L
                    2 -> 200L
                    3 -> 1000L
                    4 -> 5000L
                    5 -> 25000L
                    6 -> 100000L
                    7 -> 500000L
                    8 -> 2500000L
                    9 -> 12000000L
                    else -> 0L
                }
                val levelProgressClicks = totalClicksValue - previousLevelThreshold
                val levelRequiredClicks = nextLevelTarget - previousLevelThreshold
                val fraction = (levelProgressClicks.toFloat() / levelRequiredClicks).coerceIn(0f, 1f)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${viewModel.getAnimalNameForLevel(currentLevel).uppercase()} GUARDIAN",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = labelColor,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "$totalClicksValue / $nextLevelTarget XP",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = labelColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape),
                        color = primaryColor,
                        trackColor = if (isDarkBackground) Color.White.copy(alpha = 0.2f) else Color(0xFFE6E1E5)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "$totalClicksValue / $nextLevelTarget total clicks",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = labelColor.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.WorkspacePremium,
                        contentDescription = "Max",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MAX LEVEL REACHED 👑 Infinite Singularity!",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    )
                }
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
    val creationTime: Long
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
    Box(modifier = Modifier.fillMaxSize()) {
        val list = fallingAnimalsProvider()
        list.forEach { animal ->
            androidx.compose.runtime.key(animal.id) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val widthPx = size.width
                            translationX = (animal.xPercent * widthPx).coerceIn(0f, (widthPx - 45.dp.toPx()).coerceAtLeast(0f))
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

    val isDarkBackground = state.equippedSkinId in listOf("cyberpunk", "astronaut", "pirate", "steampunk", "retro", "shadow", "cosmic") || state.equippedSkinId == ""
    val titleColor = if (isDarkBackground) Color.White.copy(alpha = 0.9f) else Color(0xFF1C1B1F)
    val subColor = if (isDarkBackground) Color(0xFFF3EDF7) else Color(0xFF49454F)

    var fallingAnimals by remember { mutableStateOf(emptyList<FallingAnimal>()) }
    val currentLevel = state.currentLevel
    val cps = viewModel.calculateCPS(state)

    // A coroutine that spawns falling animals based on clicker speed (CPS)
    LaunchedEffect(cps, currentLevel) {
        if (cps > 0) {
            val delayMs = (1000 / cps).toLong().coerceIn(40, 2000)
            while (true) {
                delay(delayMs)
                // Select a random animal up to currentLevel that the user has unlocked
                val randomLevel = if (currentLevel > 1) Random.nextInt(1, currentLevel + 1) else 1
                val emoji = viewModel.getAnimalEmoji(randomLevel)
                val newAnimal = FallingAnimal(
                    id = System.nanoTime(),
                    emoji = emoji,
                    xPercent = Random.nextFloat(),
                    y = -80f,
                    speed = Random.nextFloat() * 5f + 4f,
                    rotationSpeed = Random.nextFloat() * 8f - 4f,
                    scale = Random.nextFloat() * 0.4f + 0.6f,
                    rotation = Random.nextFloat() * 360f,
                    creationTime = System.currentTimeMillis()
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
                    val newRotation = animal.rotation + animal.rotationSpeed
                    if (newY > 1800f) {
                        null
                    } else {
                        animal.copy(y = newY, rotation = newRotation)
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
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "TAP THE ANIMAL!",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = titleColor,
                    letterSpacing = 2.sp,
                    shadow = if (isDarkBackground) Shadow(Color.Black, blurRadius = 4f) else null
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Total Clicks",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = subColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Clicker Animal Card Box with Absolute overlay multiplier badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                // Background Auto-Clicker Finger Ring
                val numPointers = remember(state) {
                    val totalUpgrades = state.hamsterWheelCount + state.catScratchCount + state.dogBoneCount
                    totalUpgrades.coerceAtMost(16)
                }
                AutoClickerRing(numPointers = numPointers)

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                        .testTag("big_animal_clicker")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scale = 0.85f
                            viewModel.onAnimalClicked()
                            val randomX = Random.nextInt(-100, 100).toFloat()
                            val randomY = Random.nextInt(-100, 0).toFloat()
                            onFloatingIndicatorAdded("+${viewModel.calculateClicksPerTap(state)}", randomX, randomY)

                            // Click induced falling animal cloned tap effect
                            val tapAnimal = FallingAnimal(
                                id = System.nanoTime(),
                                emoji = viewModel.getAnimalEmoji(state.currentLevel),
                                xPercent = Random.nextFloat() * 0.5f + 0.25f,
                                y = 250f,
                                speed = Random.nextFloat() * 3f + 6f,
                                rotationSpeed = Random.nextFloat() * 12f - 6f,
                                scale = Random.nextFloat() * 0.3f + 0.7f,
                                rotation = Random.nextFloat() * 360f,
                                creationTime = System.currentTimeMillis()
                            )
                            fallingAnimals = (fallingAnimals + tapAnimal).take(35)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Apply visual skin backgrounds/borders
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
                        "cosmic" -> Modifier
                            .fillMaxSize()
                            .border(6.dp, Brush.linearGradient(listOf(Color(0xFFBA55D3), Color(0xFF4B0082), Color(0xFF00FFFF))), CircleShape)
                            .background(Color(0xFF0C0728), CircleShape)
                        else -> Modifier
                            .fillMaxSize()
                            .border(8.dp, Color.White, CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFD0BCFF), Color(0xFFEADDFF))
                                ),
                                CircleShape
                            )
                    }

                    Box(
                        modifier = skinModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        // Back glows for legendary skins
                        if (state.equippedSkinId == "god") {
                            GodGlowRaysWidget()
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Main Target Emoji
                            Text(
                                text = viewModel.getAnimalEmoji(state.currentLevel),
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 110.sp,
                                    shadow = Shadow(Color.Black.copy(alpha = 0.15f), offset = Offset(4f, 8f), blurRadius = 8f)
                                )
                            )

                            // Cute skin accessories
                            Text(
                                text = when (state.equippedSkinId) {
                                    "cyberpunk" -> "🕶️ Cyber Mode"
                                    "pirate" -> "🏴‍☠️ Pirate Cap'n"
                                    "god" -> "👑 Golden Divine"
                                    "steampunk" -> "⚙️ Steam Brass"
                                    "retro" -> "👾 8-Bit Retro"
                                    "shadow" -> "🥷 Shadow Ninja"
                                    "royal" -> "👑 Royal Crown"
                                    "cosmic" -> "🌌 Cosmic Nebula"
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

            Spacer(modifier = Modifier.height(20.dp))

            // Total statistics label
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .background(if (isDarkBackground) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Stars, 
                    contentDescription = "Total", 
                    tint = if (isDarkBackground) Color(0xFFFFD700) else Color(0xFF6750A4), 
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Lifetime Clicks: ${state.totalClicks}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (isDarkBackground) Color.White else Color(0xFF1C1B1F),
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            // Skin Unlock Tracker
            val nextLockedSkin = when {
                state.totalClicks < 200 -> Pair("cyberpunk", 200L)
                state.totalClicks < 1000 -> Pair("pirate", 1000L)
                state.totalClicks < 5000 -> Pair("astronaut", 5000L)
                state.totalClicks < 25000 -> Pair("god", 25000L)
                state.totalClicks < 50000 -> Pair("steampunk", 50000L)
                state.totalClicks < 150000 -> Pair("retro", 150000L)
                state.totalClicks < 500000 -> Pair("shadow", 500000L)
                state.totalClicks < 2000000 -> Pair("royal", 2000000L)
                state.totalClicks < 10000000 -> Pair("cosmic", 10000000L)
                else -> null
            }

            if (nextLockedSkin != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.testTag("skin_unlock_tracker")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFFF3EDF7), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val milestoneText = when {
                            nextLockedSkin.second >= 1000000000L -> "${nextLockedSkin.second / 1000000000f}B"
                            nextLockedSkin.second >= 1000000L -> "${nextLockedSkin.second / 1000000f}M"
                            nextLockedSkin.second >= 1000L -> "${nextLockedSkin.second / 1000f}k"
                            else -> "${nextLockedSkin.second}"
                        }.replace(".0", "")
                        Text(
                            text = "Next Skin: $milestoneText Clicks",
                            color = Color(0xFF1D192B),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.getSkinNameForId(nextLockedSkin.first).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isDarkBackground) Color.White.copy(alpha = 0.6f) else Color(0xFF49454F).copy(alpha = 0.6f),
                            letterSpacing = 1.5.sp
                        )
                    )
                }
            }
        }

        // Rising indicators layer
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
                            "steampunk" -> Color(0xFFCD853F)
                            "retro" -> Color(0xFF00FF00)
                            "shadow" -> Color.Red
                            "royal" -> Color(0xFFBA55D3)
                            "cosmic" -> Color(0xFF00FFFF)
                            else -> Color.White
                        },
                        shadow = Shadow(Color.Black, offset = Offset(1f, 1f), blurRadius = 4f)
                    )
                )
            }
        }
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
    val itemsList = listOf(
        ShopItemData(
            id = "click_power",
            title = "👉 Heavy Paws",
            description = "Increases clicks gained per tap manually. Strengthens on high Level.",
            multiplierLabel = "+1 Base Click per Level",
            baseCost = 15,
            currentCount = state.clickPowerLevel
        ),
        ShopItemData(
            id = "hamster_wheel",
            title = "🐹 Hamster Wheel",
            description = "Auto-click helper. Simple but stable running speed.",
            multiplierLabel = "+0.5 Click/sec",
            baseCost = 50,
            currentCount = state.hamsterWheelCount
        ),
        ShopItemData(
            id = "cat_scratch",
            title = "😼 Cat Scratch Pad",
            description = "Harness razor-sharp feline motivation to auto-click.",
            multiplierLabel = "+2.0 Clicks/sec",
            baseCost = 250,
            currentCount = state.catScratchCount
        ),
        ShopItemData(
            id = "dog_bone",
            title = "🍖 Dog Bone Squeaker",
            description = "Unleash persistent puppy energy for big click speeds.",
            multiplierLabel = "+10.0 Clicks/sec",
            baseCost = 1200,
            currentCount = state.dogBoneCount
        ),
        ShopItemData(
            id = "dragon_flame",
            title = "🔥 Cosmic Dragon Flame",
            description = "Summon ancient cosmic energy for ultimate click speed.",
            multiplierLabel = "+50.0 Clicks/sec",
            baseCost = 6000,
            currentCount = state.dragonFlameCount
        ),
        ShopItemData(
            id = "elephant_stampede",
            title = "🐘 Elephant Stampede",
            description = "An incredibly heavy and rhythmic stomping machine helper.",
            multiplierLabel = "+200.0 Clicks/sec",
            baseCost = 35000,
            currentCount = state.elephantStampedeCount
        ),
        ShopItemData(
            id = "cheetah_nitro",
            title = "🐆 Cheetah Accelerator",
            description = "Super-charged feline velocity for rapid boost.",
            multiplierLabel = "+1000.0 Clicks/sec",
            baseCost = 200000,
            currentCount = state.cheetahNitroCount
        ),
        ShopItemData(
            id = "phoenix_flight",
            title = "🦅 Mystic Phoenix Flight",
            description = "Regenerative cosmic heat loops automatically click.",
            multiplierLabel = "+5000.0 Clicks/sec",
            baseCost = 1500000,
            currentCount = state.phoenixFlightCount
        ),
        ShopItemData(
            id = "black_hole",
            title = "🕳️ Interstellar Singularity",
            description = "A powerful spatial warp that absorbs matter into clicks.",
            multiplierLabel = "+30000.0 Clicks/sec",
            baseCost = 12000000,
            currentCount = state.blackHoleCount
        )
    )

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
                                        fontSize = 14.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(
                                    containerColor = if (isDarkBackground) Color.White.copy(alpha = 0.25f) else Color(0xFFEADDFF),
                                    contentColor = if (isDarkBackground) Color.White else Color(0xFF21005D)
                                ) {
                                    Text("Owned: ${item.currentCount}", fontSize = 9.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(1.dp))

                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = textSubColor.copy(alpha = 0.85f),
                                maxLines = 1,
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

                        Button(
                            onClick = { viewModel.buyUpgrade(item.id) },
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6750A4),
                                disabledContainerColor = if (isDarkBackground) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
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
                                    text = "🐾 $cost",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    fontSize = 9.sp
                                )
                            }
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
    val listSkins = listOf(
        SkinData("standard", "Classic Cutie", "Classic appearance matching your animal level.", "🐹, 🐱, 🐶, 🐰, 🐵, 🐢...", 0, "🐾 Standard Power"),
        SkinData("cyberpunk", "Cyber Neon", "Pulsating holographic frame and glowing overlays.", "⚡ Neon Accent", 200, "+2 Bonus Clicks per tap"),
        SkinData("pirate", "Pirate Cap'n", "Ahoy! Equipped with a pirate hat and eyepatch details.", "🏴‍☠️ Pirate theme", 1000, "+5 Bonus Clicks per tap"),
        SkinData("astronaut", "Space Astro", "Sleek astronaut cosmic suit with high-tech helmet.", "👨‍🚀 Space Helmet", 5000, "+15 Bonus Clicks per tap"),
        SkinData("god", "Golden Divine", "Blinding divine light and legendary golden sun ray halo.", "👑 God rays halo", 25000, "+50 Bonus Clicks per tap"),
        SkinData("steampunk", "Steam Brass", "Antique brass finish with rotating intricate golden gears.", "⚙️ Brass Gears", 50000, "+100 Bonus Clicks per tap"),
        SkinData("retro", "8-Bit Retro", "Retro arcade monitor border with pixel green CRT glow.", "👾 Green Glow", 150000, "+250 Bonus Clicks per tap"),
        SkinData("shadow", "Shadow Ninja", "Sleek crimson ninja mask wrapped in shadow dust.", "🥷 Stealth Dust", 500000, "+750 Bonus Clicks per tap"),
        SkinData("royal", "Royal Crown", "Velvet royal purple frame seated with shimmering monarch crowns.", "👑 Velvet Crown", 2000000, "+2.5k Bonus Clicks per tap"),
        SkinData("cosmic", "Cosmic Nebula", "A moving planetary outer orbit with glowing neon rings.", "🌌 Starry Rings", 10000000, "+10k Bonus Clicks per tap")
    )

    val unlockedList = state.unlockedSkins.split(",").map { it.trim() }.toSet()

    val isDarkBackground = state.equippedSkinId in listOf("cyberpunk", "astronaut", "pirate", "steampunk", "retro", "shadow", "cosmic")
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
                                modifier = Modifier
                                    .testTag("equip_button_${skin.id}")
                                    .minimumInteractiveComponentSize(),
                                shape = RoundedCornerShape(12.dp)
                             ) {
                                Text(
                                    text = if (isEquipped) "ACTIVE" else "EQUIP",
                                    fontWeight = FontWeight.Bold
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
    val isDarkBackground = state.equippedSkinId in listOf("cyberpunk", "astronaut", "pirate", "steampunk", "retro", "shadow", "cosmic")
    val titleTextColor = if (isDarkBackground) Color.White else Color(0xFF1C1B1F)

    val currentLevel = state.currentLevel
    val totalClicks = state.totalClicks

    val levelsList = listOf(
        UnlockItem(1, "🐹", "Cute Hamster", 0L, "Standard starting hamster wheel buddy! No requirements."),
        UnlockItem(2, "🐱", "Playful Kitty", 200L, "Quick and mischievous feline helper. Unlocks at 200 total clicks."),
        UnlockItem(3, "🐶", "Alert Doggy", 1000L, "Loyal canine companion for increased tap multiplier. Unlocks at 1,000."),
        UnlockItem(4, "🐰", "Fluffy Bunny", 5000L, "A soft hop-along bunny that doubles your tap effectiveness. Unlocks at 5,000."),
        UnlockItem(5, "🐵", "Cheeky Monkey", 25000L, "A highly active monkey that loves to push keys for you. Unlocks at 25,000."),
        UnlockItem(6, "🐢", "Tiny Turtle", 100000L, "A slow but extremely robust protector of multipliers. Unlocks at 100,000."),
        UnlockItem(7, "🦜", "Colorful Parrot", 500000L, "A talkative bird repeating every tap exponentially. Unlocks at 500,000."),
        UnlockItem(8, "🦁", "Majestic Lion", 2500000L, "King of the savanna, bringing ferocious power. Unlocks at 2,500,000."),
        UnlockItem(9, "🐲", "Celestial Dragon", 12000000L, "An ancient sky-soaring beast with fire multiplier powers. Unlocks at 12,000,000."),
        UnlockItem(10, "🦅", "Mystic Phoenix", 60000000L, "Regenerative divine avian bringing bright starlight boost. Unlocks at 60,000,000."),
        UnlockItem(11, "🌌", "Cosmic Behemoth", 300000000L, "A spatial titan that warps your tapping coordinates. Unlocks at 300,000,000."),
        UnlockItem(12, "🐋", "Chrono Leviathan", 1500000000L, "Ancient controller of oceanic current and frequency. Unlocks at 1,500,000,000."),
        UnlockItem(13, "👾", "Galaxian Overlord", 8000000000L, "Extraterrestrial supreme chief bending cybernetic reality. Unlocks at 8,000,000,000."),
        UnlockItem(14, "🌀", "Infinite Singularity", 40000000000L, "Ultimate core vortex that merges timing and clicking. Unlocks at 40,000,000,000."),
        UnlockItem(15, "🌟", "Supreme Omnipresent", 200000000000L, "Absolute final form. Total mastery over clicking dimensions. Unlocks at 200,000,000,000.")
    )

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
                                2 -> 200L
                                3 -> 1000L
                                4 -> 5000L
                                5 -> 25000L
                                6 -> 100000L
                                7 -> 500000L
                                8 -> 2500000L
                                9 -> 12000000L
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
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "AWESOME! TAP TO CONTINUE",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
