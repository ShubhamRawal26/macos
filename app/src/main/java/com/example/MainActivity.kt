package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepo = SettingsRepository(applicationContext)

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DesktopWorkspace(
                        settingsRepo = settingsRepo,
                        onRequestOverlayPermission = {
                            checkAndRequestOverlayPermission()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            settingsRepo.showOverlayDock = true
            toggleDockOverlayService()
        } else {
            settingsRepo.showOverlayDock = false
            Toast.makeText(this, "Overlay permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
                Toast.makeText(this, "Please grant Draw Over Other Apps permission to launch Floating Dock!", Toast.LENGTH_LONG).show()
            } else {
                toggleDockOverlayService()
            }
        } else {
            toggleDockOverlayService()
        }
    }

    private fun toggleDockOverlayService() {
        val intent = Intent(this, DockOverlayService::class.java)
        if (settingsRepo.showOverlayDock) {
            startService(intent)
            Toast.makeText(this, "Floating macOS Dock overlay started!", Toast.LENGTH_SHORT).show()
        } else {
            stopService(intent)
            Toast.makeText(this, "Floating macOS Dock overlay stopped.", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun DesktopWorkspace(
    settingsRepo: SettingsRepository,
    onRequestOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { LauncherViewModel(context.applicationContext as android.app.Application) }

    // Settings flows
    val activeWallpaper by settingsRepo.activeWallpaperFlow.collectAsState()
    val activeTheme by settingsRepo.activeThemeFlow.collectAsState()
    val showOverlayDock by settingsRepo.showOverlayDockFlow.collectAsState()
    val widgetsList by settingsRepo.widgetsFlow.collectAsState()

    // Navigation and drawer states
    var showLaunchpad by remember { mutableStateOf(false) }
    var showSettingsPage by remember { mutableStateOf(false) }
    var activeCategory by remember { mutableStateOf("All") }
    var showAppleMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Start background overlay service automatically if preference is active and allowed
    LaunchedEffect(showOverlayDock) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (showOverlayDock && Settings.canDrawOverlays(context)) {
                context.startService(Intent(context, DockOverlayService::class.java))
            } else if (!showOverlayDock) {
                context.stopService(Intent(context, DockOverlayService::class.java))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Procedural Luxury Wallpaper Background
        WallpaperBackground(wallpaperName = activeWallpaper)

        // 2. Desktop Drag-and-Drop Workspace Area (Containing widgets)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 44.dp) // Offset for top Menu Bar
        ) {
            // Flow Widget Grid
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                widgetsList.forEach { widget ->
                    GlassWidgetContainer(
                        title = when (widget.type) {
                            "clock" -> "Clock"
                            "calendar" -> "Calendar"
                            "system_info" -> "Activity HUD"
                            "notes" -> "Quick Note"
                            else -> "Widget"
                        },
                        onClose = {
                            viewModel.removeWidget(widget.id)
                        }
                    ) {
                        when (widget.type) {
                            "clock" -> AnalogClockWidget()
                            "calendar" -> CalendarWidget()
                            "system_info" -> SystemStatusWidget()
                            "notes" -> NotesWidget(
                                id = widget.id,
                                initialContent = widget.noteContent,
                                onSaveContent = { text ->
                                    viewModel.updateWidgetNoteContent(widget.id, text)
                                }
                            )
                        }
                    }
                }

                // Add Widget Action Placeholder Card
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp))
                        .clickable {
                            // Cycle widget add options
                            val existingTypes = widgetsList.map { it.type }
                            val remaining = listOf("clock", "calendar", "system_info", "notes")
                                .filter { !existingTypes.contains(it) }
                            if (remaining.isNotEmpty()) {
                                viewModel.addWidget(remaining.first())
                            } else {
                                viewModel.addWidget("notes") // Allow duplicate notes
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add widget",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add Glass Widget",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Top macOS Sonoma Menu Bar
        TopMenuBar(
            theme = activeTheme,
            onAppleClick = { showAppleMenu = !showAppleMenu },
            onLaunchpadClick = { showLaunchpad = !showLaunchpad },
            onSettingsClick = { showSettingsPage = !showSettingsPage },
            onAddWidget = {
                viewModel.addWidget("clock")
            }
        )

        // Dropdown menu from Apple Menu Bar icon
        Box(modifier = Modifier.padding(start = 12.dp, top = 32.dp)) {
            DropdownMenu(
                expanded = showAppleMenu,
                onDismissRequest = { showAppleMenu = false },
                modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("About This Tablet", color = Color.White) },
                    onClick = {
                        showAboutDialog = true
                        showAppleMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("System Preferences", color = Color.White) },
                    onClick = {
                        showSettingsPage = true
                        showAppleMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Launchpad (App Drawer)", color = Color.White) },
                    onClick = {
                        showLaunchpad = true
                        showAppleMenu = false
                    }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                DropdownMenuItem(
                    text = { Text("Restart macOS Experience", color = Color.White) },
                    onClick = {
                        viewModel.loadInstalledApps()
                        showAppleMenu = false
                        Toast.makeText(context, "Refreshed Launcher packages", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 4. macOS centered Dock Launcher
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            contentAlignment = Alignment.BottomCenter
        ) {
            FloatingOverlayDockView(
                settingsRepo = settingsRepo,
                onLaunchApp = { packageName ->
                    if (packageName == "com.apple.settings") {
                        showSettingsPage = true
                    } else if (packageName == "com.apple.launchpad") {
                        showLaunchpad = true
                    } else {
                        val app = viewModel.installedApps.value.find { it.packageName == packageName }
                        if (app != null) {
                            viewModel.launchApp(app)
                        } else {
                            Toast.makeText(context, "Opening virtual shortcut", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // 5. App Drawer (Launchpad Screen) Fullscreen Overlay
        AnimatedVisibility(
            visible = showLaunchpad,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 6 })
        ) {
            LaunchpadScreen(
                viewModel = viewModel,
                activeCategory = activeCategory,
                onCategoryChange = { activeCategory = it },
                onClose = { showLaunchpad = false },
                onLaunchApp = { app ->
                    viewModel.launchApp(app)
                    showLaunchpad = false
                }
            )
        }

        // 6. Settings Page Overlay (Stunning Glass Settings)
        AnimatedVisibility(
            visible = showSettingsPage,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
        ) {
            SettingsPage(
                settingsRepo = settingsRepo,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onClose = { showSettingsPage = false }
            )
        }

        // 7. Apple "About This Tablet" System Dialog
        if (showAboutDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { showAboutDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(420.dp)
                        .padding(16.dp)
                        .clickable(enabled = false) {}, // prevent click-through
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Custom macOS smiling Finder logo as splash
                        Box(modifier = Modifier.size(72.dp)) {
                            FinderIconView()
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "macOS Tablet Sonoma",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version 14.5 (Build 23F79)",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Chipset", color = Color.Gray, fontSize = 13.sp)
                            Text(text = Build.HARDWARE.uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Memory (RAM)", color = Color.Gray, fontSize = 13.sp)
                            Text(text = "8 GB LPDDR5X", color = Color.White, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "System OS Core", color = Color.Gray, fontSize = 13.sp)
                            Text(text = "Android ${Build.VERSION.RELEASE}", color = Color.White, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showAboutDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Done", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperBackground(wallpaperName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val brush = when (wallpaperName) {
                    "Sonoma Sunset" -> Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF5E3A), // Vibrant Peach
                            Color(0xFFFF2A68), // Rich Magenta
                            Color(0xFF552586), // Royal Purple
                            Color(0xFF0D1B2A)  // Cosmic Deep Blue
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                    "Aurora Wave" -> Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00FF87), // Solar Green
                            Color(0xFF60EFFF), // Cyan Wave
                            Color(0xFF0061FF), // Ocean Indigo
                            Color(0xFF190634)  // Mystic Purple
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                    "Midnight Obsidian" -> Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E2022), // Deep Charcoal
                            Color(0xFF141517), // Solid Dark Slate
                            Color(0xFF0D0E10), // Midnight Eclipse
                            Color(0xFF1B1B1F)  // Dark Blue Black
                        )
                    )
                    "Ocean Breeze" -> Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF), // Bright Turquoise
                            Color(0xFF0288D1), // Sky Cerulean
                            Color(0xFF006064), // Deep Sea Teal
                            Color(0xFF0A192F)  // Dark Obsidian
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.9f
                    )
                    "Lavender Fields" -> Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE9D5FF), // Warm Lavender
                            Color(0xFFC084FC), // Meadow Orchid
                            Color(0xFF7E22CE), // Rich Violet
                            Color(0xFF2E1065)  // Eclipse Night
                        )
                    )
                    else -> Brush.linearGradient(
                        colors = listOf(Color(0xFF1E2022), Color(0xFF0D0E10))
                    )
                }
                drawRect(brush = brush)
            }
    )
}

@Composable
fun TopMenuBar(
    theme: String,
    onAppleClick: () -> Unit,
    onLaunchpadClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddWidget: () -> Unit
) {
    var dateTimeText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("EEE d MMM  h:mm:ss a", Locale.getDefault())
        while (true) {
            dateTimeText = sdf.format(Date())
            delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(
                color = if (theme == "Midnight") Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.12f)
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Actions: Apple icon, Active Application context
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Apple Glyph Logo
            Text(
                text = "",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onAppleClick() }
                    .padding(horizontal = 6.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Finder",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onAppleClick() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "File",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.clickable { onAddWidget() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Edit",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "View",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Go",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.clickable { onLaunchpadClick() }
            )
        }

        // Right Actions: Status bar indicators, local datetime
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Launchpad Drawer",
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onLaunchpadClick() }
            )
            Spacer(modifier = Modifier.width(14.dp))
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Launcher Preferences",
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onSettingsClick() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Clock & Date string
            Text(
                text = dateTimeText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LaunchpadScreen(
    viewModel: LauncherViewModel,
    activeCategory: String,
    onCategoryChange: (String) -> Unit,
    onClose: () -> Unit,
    onLaunchApp: (AppItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val installedApps by viewModel.installedApps.collectAsState()

    // Filter apps dynamically
    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isEmpty()) {
            installedApps
        } else {
            installedApps.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .blur(30.dp) // Frosted glass blur overlay
            .clickable { onClose() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 48.dp, vertical = 24.dp)
                .clickable(enabled = false) {}, // prevent click-through closures
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // macOS standard launcher search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Launchpad apps...", color = Color.White.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .width(420.dp)
                    .padding(vertical = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.12f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedBorderColor = Color.White.copy(alpha = 0.3f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )

            // Category Filter Badges
            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All Apps", "System", "Third Party").forEach { category ->
                    val isSelected = when (category) {
                        "All Apps" -> activeCategory == "All"
                        "System" -> activeCategory == "System"
                        "Third Party" -> activeCategory == "ThirdParty"
                        else -> false
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                val target = when (category) {
                                    "All Apps" -> "All"
                                    "System" -> "System"
                                    "Third Party" -> "ThirdParty"
                                    else -> "All"
                                }
                                onCategoryChange(target)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = category,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Grid of launchable apps
            val categoryFilteredApps = remember(filteredApps, activeCategory) {
                when (activeCategory) {
                    "System" -> filteredApps.filter { it.isSystem }
                    "ThirdParty" -> filteredApps.filter { !it.isSystem }
                    else -> filteredApps
                }
            }

            if (categoryFilteredApps.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No apps",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No applications match your search query.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(categoryFilteredApps) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onLaunchApp(app) }
                                .padding(8.dp)
                        ) {
                            MacOsIcon(
                                type = app.label,
                                size = 56.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = app.label,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tap empty workspace area to return to Desktop",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsPage(
    settingsRepo: SettingsRepository,
    onRequestOverlayPermission: () -> Unit,
    onClose: () -> Unit
) {
    // Collect active flow values
    val dockHeight by settingsRepo.dockHeightFlow.collectAsState()
    val iconSizePref by settingsRepo.iconSizeFlow.collectAsState()
    val magnifyPref by settingsRepo.magnificationFactorFlow.collectAsState()
    val blurIntensity by settingsRepo.blurIntensityFlow.collectAsState()
    val transparency by settingsRepo.transparencyFlow.collectAsState()
    val cornerRadiusPref by settingsRepo.cornerRadiusFlow.collectAsState()
    val glowIntensity by settingsRepo.glowIntensityFlow.collectAsState()
    val activeTheme by settingsRepo.activeThemeFlow.collectAsState()
    val activeWallpaper by settingsRepo.activeWallpaperFlow.collectAsState()
    val showOverlayDock by settingsRepo.showOverlayDockFlow.collectAsState()

    var showWallpaperDropdown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(520.dp)
                .fillMaxHeight(0.85f)
                .padding(16.dp)
                .clickable(enabled = false) {}, // prevent closing on internal click
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Settings Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "macOS System Preferences",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tweak your custom liquid glass dock layout",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Close", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable preference forms
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Category: Overlay Service
                    Text(
                        text = "FLOATING OVERLAY DOCK",
                        color = Color(0xFF007AFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Float Over Other Applications", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Renders the glass dock system-wide on top of any active application. Requires draw overlay permission.",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = showOverlayDock,
                            onCheckedChange = { checked ->
                                settingsRepo.showOverlayDock = checked
                                onRequestOverlayPermission()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF007AFF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Category: Aesthetics & Personalization
                    Text(
                        text = "WALLPAPER & THEME",
                        color = Color(0xFF34C759),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Wallpaper Select button
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .clickable { showWallpaperDropdown = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Active Wallpaper Background", color = Color.White, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = activeWallpaper, color = Color.Gray, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Choose", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showWallpaperDropdown,
                            onDismissRequest = { showWallpaperDropdown = false },
                            modifier = Modifier.background(Color(0xFF2E2E2E))
                        ) {
                            listOf("Sonoma Sunset", "Aurora Wave", "Midnight Obsidian", "Ocean Breeze", "Lavender Fields").forEach { wall ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = wall, color = Color.White)
                                            if (activeWallpaper == wall) {
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.Green, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    },
                                    onClick = {
                                        settingsRepo.activeWallpaper = wall
                                        showWallpaperDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Category: Dock Dimensions
                    Text(
                        text = "DOCK & APP ICON SCALING",
                        color = Color(0xFFFF9500),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Sliders
                    Text(text = "Dock Height (${dockHeight.toInt()} dp)", color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = dockHeight,
                        onValueChange = { settingsRepo.dockHeight = it },
                        valueRange = 50f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFFF9500))
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Default Icon Size (${iconSizePref.toInt()} dp)", color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = iconSizePref,
                        onValueChange = { settingsRepo.iconSize = it },
                        valueRange = 40f..80f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFFF9500))
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Magnification Zoom Coefficient (${String.format("%.1f", magnifyPref)}x)", color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = magnifyPref,
                        onValueChange = { settingsRepo.magnificationFactor = it },
                        valueRange = 1.1f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFFF9500))
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Glass Corner Radius (${cornerRadiusPref.toInt()} dp)", color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = cornerRadiusPref,
                        onValueChange = { settingsRepo.cornerRadius = it },
                        valueRange = 12f..36f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFFF9500))
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Glass Backdrop Transparency (${(transparency * 100).toInt()}%)", color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = transparency,
                        onValueChange = { settingsRepo.transparency = it },
                        valueRange = 0.05f..0.5f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFFF9500))
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Floating Ambient Glow (${glowIntensity.toInt()} dp)", color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = glowIntensity,
                        onValueChange = { settingsRepo.glowIntensity = it },
                        valueRange = 0f..16f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFFF9500))
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Button(
                        onClick = {
                            settingsRepo.resetAll()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Reset Preferences to Default", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
