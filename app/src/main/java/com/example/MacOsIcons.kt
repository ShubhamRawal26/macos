package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MacOsIcon(
    type: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .background(Color.White, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (type.lowercase()) {
            "finder" -> FinderIconView()
            "safari" -> SafariIconView()
            "notes" -> NotesIconView()
            "settings" -> SettingsIconView()
            "appstore" -> AppStoreIconView()
            "launchpad" -> LaunchpadIconView()
            else -> DefaultAppIconView(type)
        }
    }
}

@Composable
fun FinderIconView() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Left Face (Light Blue)
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF86C1FB), Color(0xFF3398F6))
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w / 2f, h),
            cornerRadius = CornerRadius(0f)
        )

        // Right Face (Deep Blue)
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF3F96F3), Color(0xFF1359C4))
            ),
            topLeft = Offset(w / 2f, 0f),
            size = Size(w / 2f, h),
            cornerRadius = CornerRadius(0f)
        )

        // Center line & smile curve (Classic Finder Face)
        val path = Path().apply {
            // Smile curve
            moveTo(w * 0.25f, h * 0.65f)
            quadraticTo(w * 0.5f, h * 0.88f, w * 0.75f, h * 0.65f)
            // Interlocking nose line
            moveTo(w * 0.5f, h * 0.2f)
            lineTo(w * 0.5f, h * 0.55f)
            lineTo(w * 0.42f, h * 0.55f)
            lineTo(w * 0.42f, h * 0.48f)
            lineTo(w * 0.58f, h * 0.48f)
        }

        drawPath(
            path = path,
            color = Color(0xFF0C2447),
            style = Stroke(width = w * 0.07f)
        )

        // Eyes
        drawCircle(
            color = Color(0xFF0C2447),
            radius = w * 0.08f,
            center = Offset(w * 0.28f, h * 0.38f)
        )
        drawCircle(
            color = Color(0xFF0C2447),
            radius = w * 0.08f,
            center = Offset(w * 0.72f, h * 0.38f)
        )
    }
}

@Composable
fun SafariIconView() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // White dial background
        drawCircle(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFFFFFFF), Color(0xFFF3F3F3))
            ),
            radius = w * 0.46f,
            center = Offset(w / 2f, h / 2f)
        )

        // Blue outer compass ring
        drawCircle(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF3DA3F1), Color(0xFF116FF4))
            ),
            radius = w * 0.4f,
            center = Offset(w / 2f, h / 2f)
        )

        // Inner ticks
        for (i in 0 until 360 step 30) {
            rotate(degrees = i.toFloat(), pivot = Offset(w / 2f, h / 2f)) {
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(w / 2f, h * 0.14f),
                    end = Offset(w / 2f, h * 0.19f),
                    strokeWidth = w * 0.02f
                )
            }
        }

        // Compass needle (pointing NE)
        rotate(degrees = 45f, pivot = Offset(w / 2f, h / 2f)) {
            val needlePathRed = Path().apply {
                moveTo(w / 2f, h * 0.15f)
                lineTo(w * 0.38f, h / 2f)
                lineTo(w / 2f, h / 2f)
                close()
            }
            drawPath(path = needlePathRed, color = Color(0xFFFF453A))

            val needlePathSilver = Path().apply {
                moveTo(w / 2f, h * 0.15f)
                lineTo(w * 0.62f, h / 2f)
                lineTo(w / 2f, h / 2f)
                close()
            }
            drawPath(path = needlePathSilver, color = Color(0xFFCCCFD6))

            val needlePathBottomSilver = Path().apply {
                moveTo(w / 2f, h * 0.85f)
                lineTo(w * 0.38f, h / 2f)
                lineTo(w / 2f, h / 2f)
                close()
            }
            drawPath(path = needlePathBottomSilver, color = Color(0xFFE2E4E9))

            val needlePathBottomDark = Path().apply {
                moveTo(w / 2f, h * 0.85f)
                lineTo(w * 0.62f, h / 2f)
                lineTo(w / 2f, h / 2f)
                close()
            }
            drawPath(path = needlePathBottomDark, color = Color(0xFF909399))
        }

        // Center pivot
        drawCircle(
            color = Color.White,
            radius = w * 0.05f,
            center = Offset(w / 2f, h / 2f)
        )
        drawCircle(
            color = Color(0xFFFF9500),
            radius = w * 0.02f,
            center = Offset(w / 2f, h / 2f)
        )
    }
}

@Composable
fun NotesIconView() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Yellow notepad background
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFFFF6C3), Color(0xFFFFE87C))
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, h),
            cornerRadius = CornerRadius(0f)
        )

        // Orange binder strip at top
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFE5A823), Color(0xFFC78C11))
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, h * 0.22f)
        )

        // Binding stitches
        for (i in 0 until 5) {
            val cx = w * 0.15f + i * (w * 0.175f)
            drawCircle(
                color = Color(0xFF7F5B05),
                radius = w * 0.03f,
                center = Offset(cx, h * 0.11f)
            )
        }

        // Lined paper notes
        for (i in 0 until 4) {
            val cy = h * 0.38f + i * (h * 0.14f)
            drawLine(
                color = Color(0xFFD4C17C),
                start = Offset(w * 0.1f, cy),
                end = Offset(w * 0.9f, cy),
                strokeWidth = w * 0.02f
            )
        }
    }
}

@Composable
fun SettingsIconView() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Dark slate metallic background
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC))
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, h),
            cornerRadius = CornerRadius(0f)
        )

        // Outer metal gear
        drawCircle(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFB0BEC5), Color(0xFF78909C))
            ),
            radius = w * 0.36f,
            center = Offset(w / 2f, h / 2f)
        )

        // Teeth of the gear
        for (i in 0 until 360 step 45) {
            rotate(degrees = i.toFloat(), pivot = Offset(w / 2f, h / 2f)) {
                drawRoundRect(
                    color = Color(0xFF78909C),
                    topLeft = Offset(w * 0.44f, h * 0.07f),
                    size = Size(w * 0.12f, h * 0.18f),
                    cornerRadius = CornerRadius(w * 0.03f)
                )
            }
        }

        // Inner gear core (Dark charcoal)
        drawCircle(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF546E7A), Color(0xFF37474F))
            ),
            radius = w * 0.22f,
            center = Offset(w / 2f, h / 2f)
        )

        // Golden center dot
        drawCircle(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFFFD54F), Color(0xFFFFB300))
            ),
            radius = w * 0.08f,
            center = Offset(w / 2f, h / 2f)
        )
    }
}

@Composable
fun AppStoreIconView() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Deep blue gradient background
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF2FA2F8), Color(0xFF0C60E1))
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, h),
            cornerRadius = CornerRadius(0f)
        )

        // Interlocking rounded sticks forming 'A'
        val stickWidth = w * 0.09f
        val stickLength = h * 0.52f

        // Stick 1: Diagonal Right (Bottom Left to Top Right)
        rotate(degrees = -30f, pivot = Offset(w * 0.32f, h * 0.72f)) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.28f, h * 0.25f),
                size = Size(stickWidth, stickLength),
                cornerRadius = CornerRadius(stickWidth / 2f)
            )
        }

        // Stick 2: Diagonal Left (Bottom Right to Top Left)
        rotate(degrees = 30f, pivot = Offset(w * 0.68f, h * 0.72f)) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.62f, h * 0.25f),
                size = Size(stickWidth, stickLength),
                cornerRadius = CornerRadius(stickWidth / 2f)
            )
        }

        // Stick 3: Horizontal crossing bar (shifted slightly lower)
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.22f, h * 0.56f),
            size = Size(w * 0.56f, stickWidth),
            cornerRadius = CornerRadius(stickWidth / 2f)
        )
    }
}

@Composable
fun LaunchpadIconView() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Silver cosmic radial wallpaper
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE1F5FE), Color(0xFFB3E5FC), Color(0xFF81D4FA)),
                center = Offset(w / 2f, h / 2f),
                radius = w * 0.7f
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, h),
            cornerRadius = CornerRadius(0f)
        )

        // Grid of 9 vibrant capsules
        val colors = listOf(
            Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFCC00),
            Color(0xFF4CD964), Color(0xFF5AC8FA), Color(0xFF007AFF),
            Color(0xFF5856D6), Color(0xFFFF2D55), Color(0xFF8E8E93)
        )

        val cellWidth = w * 0.18f
        val cellHeight = h * 0.18f
        val padding = w * 0.08f

        for (row in 0 until 3) {
            for (col in 0 until 3) {
                val index = row * 3 + col
                val cx = padding + col * (cellWidth + padding)
                val cy = padding + row * (cellHeight + padding)

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(colors[index], colors[index].copy(alpha = 0.7f))
                    ),
                    topLeft = Offset(cx, cy),
                    size = Size(cellWidth, cellHeight),
                    cornerRadius = CornerRadius(cellWidth * 0.3f)
                )
            }
        }
    }
}

@Composable
fun DefaultAppIconView(label: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val firstLetter = label.getOrNull(0)?.uppercaseChar()?.toString() ?: "A"
        // Generate a deterministic color based on the letter to look beautifully coordinated
        val hash = firstLetter.hashCode()
        val hue = (hash % 360).coerceAtLeast(0)
        val brandColor = Color.hsv(hue.toFloat(), 0.75f, 0.85f)

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(brandColor, brandColor.copy(alpha = 0.6f)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, h),
            cornerRadius = CornerRadius(0f)
        )

        // Draw a minimalist glowing overlay
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = w * 0.45f,
            center = Offset(w / 2f, 0f)
        )
    }
    // Render text overlay elegantly
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val firstLetter = label.getOrNull(0)?.uppercaseChar()?.toString() ?: "A"
        androidx.compose.material3.Text(
            text = firstLetter,
            color = Color.White,
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
