package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingOverlayDockView(
    settingsRepo: SettingsRepository,
    onLaunchApp: (String) -> Unit
) {
    val dockHeight by settingsRepo.dockHeightFlow.collectAsState()
    val iconSizePref by settingsRepo.iconSizeFlow.collectAsState()
    val magnifyPref by settingsRepo.magnificationFactorFlow.collectAsState()
    val blurIntensity by settingsRepo.blurIntensityFlow.collectAsState()
    val transparency by settingsRepo.transparencyFlow.collectAsState()
    val cornerRadiusPref by settingsRepo.cornerRadiusFlow.collectAsState()
    val glowIntensity by settingsRepo.glowIntensityFlow.collectAsState()
    val pinnedPackages by settingsRepo.pinnedAppsFlow.collectAsState()

    // Query virtual + pinned apps
    val context = LocalContext.current
    val launcherViewModel = remember { LauncherViewModel(context.applicationContext as android.app.Application) }
    val dockApps = launcherViewModel.getDockApps(pinnedPackages)

    // Touch positions for magnification
    var pointerX by remember { mutableStateOf<Float?>(null) }
    val itemCenterXList = remember { mutableMapOf<Int, Float>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Dock Container
        Box(
            modifier = Modifier
                .height((dockHeight + 14).dp)
                .shadow(
                    elevation = glowIntensity.dp,
                    shape = RoundedCornerShape(cornerRadiusPref.dp),
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.8f)
                )
                .background(
                    color = Color.Black.copy(alpha = transparency.coerceIn(0.05f, 0.4f)),
                    shape = RoundedCornerShape(cornerRadiusPref.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadiusPref.dp)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
                // Listen to hover/drag touches across the dock for Magnification
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> pointerX = offset.x },
                        onDrag = { change, _ ->
                            pointerX = change.position.x
                            change.consume()
                        },
                        onDragEnd = { pointerX = null },
                        onDragCancel = { pointerX = null }
                    )
                }
                .pointerInput(Unit) {
                    // Track tap coordinates too
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                if (change.pressed) {
                                    pointerX = change.position.x
                                } else {
                                    pointerX = null
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                dockApps.forEachIndexed { index, app ->
                    // State for bounce animation on click
                    val bounceAnim = remember { Animatable(0f) }
                    val scope = rememberCoroutineScope()
                    var showMenu by remember { mutableStateOf(false) }

                    // Calculate real-time magnification scaling
                    val targetScale = remember(pointerX, itemCenterXList[index]) {
                        val currentPointerX = pointerX
                        val centerOfItem = itemCenterXList[index]
                        if (currentPointerX != null && centerOfItem != null) {
                            val distance = abs(centerOfItem - currentPointerX)
                            // 200px radius of magnification
                            val radius = 180f
                            if (distance < radius) {
                                val ratio = 1f - (distance / radius)
                                1f + (magnifyPref - 1f) * ratio
                            } else 1f
                        } else 1f
                    }

                    // Apply smooth spring physics to magnification scaling
                    val smoothScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "magnification_scale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                // Record the mid-point X coordinate of each item in parent dock container
                                val parentOffset = coords.positionInParent()
                                itemCenterXList[index] = parentOffset.x + (coords.size.width / 2f)
                            }
                            .offset { IntOffset(0, bounceAnim.value.toInt()) }
                    ) {
                        // Application Label Tooltip on Magnification hover
                        if (pointerX != null && targetScale > 1.15f) {
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = app.label,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // App Icon Wrapper
                        Box(
                            modifier = Modifier
                                .size((iconSizePref * smoothScale).dp)
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = {
                                        // Bounce Animation like macOS!
                                        scope.launch {
                                            // Bounce up and down twice
                                            repeat(2) {
                                                bounceAnim.animateTo(
                                                    targetValue = -35f,
                                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f)
                                                )
                                                bounceAnim.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f)
                                                )
                                            }
                                            // Launch target
                                            onLaunchApp(app.packageName)
                                        }
                                    },
                                    onLongClick = {
                                        showMenu = true
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            MacOsIcon(
                                type = if (app.isVirtual) app.virtualType else app.label,
                                size = (iconSizePref * smoothScale).dp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Glowing indicator dot below running applications
                        val isAppRunning = app.isRunning || app.isVirtual // Virtual apps always have a soft light
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(
                                    color = if (isAppRunning) {
                                        Color(0xFF00FFCC)
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = CircleShape
                                )
                        )

                        // Context Menu (Pin/Unpin/Hide/Remove)
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = app.label,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            if (app.isPinned && !app.isVirtual) {
                                DropdownMenuItem(
                                    text = { Text("Unpin from Dock", color = Color.White) },
                                    onClick = {
                                        settingsRepo.unpinApp(app.packageName)
                                        showMenu = false
                                    }
                                )
                            } else if (!app.isVirtual) {
                                DropdownMenuItem(
                                    text = { Text("Pin to Dock", color = Color.White) },
                                    onClick = {
                                        settingsRepo.pinApp(app.packageName)
                                        showMenu = false
                                    }
                                )
                            }
                            if (!app.isVirtual) {
                                DropdownMenuItem(
                                    text = { Text("Hide App Shortcut", color = Color.Red) },
                                    onClick = {
                                        settingsRepo.hideApp(app.packageName)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
        }
    }
}
