package com.example

import android.content.Context
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun GlassWidgetContainer(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(190.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(12.dp)
    ) {
        // Close Button (visible on hover/active or simple indicator)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove Widget",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun AnalogClockWidget() {
    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    val cal = Calendar.getInstance().apply { time = currentTime }
    val hour = cal.get(Calendar.HOUR)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw elegant watchface
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            // Dial ring
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Hour markings
            for (i in 0 until 12) {
                val angle = i * 30 * Math.PI / 180f
                val startX = center.x + (radius - 12.dp.toPx()) * Math.sin(angle).toFloat()
                val startY = center.y - (radius - 12.dp.toPx()) * Math.cos(angle).toFloat()
                val endX = center.x + (radius - 4.dp.toPx()) * Math.sin(angle).toFloat()
                val endY = center.y - (radius - 4.dp.toPx()) * Math.cos(angle).toFloat()

                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Hour hand
            val hrAngle = (hour * 30 + minute * 0.5f) * Math.PI / 180f
            val hrLength = radius * 0.5f
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(
                    center.x + hrLength * Math.sin(hrAngle).toFloat(),
                    center.y - hrLength * Math.cos(hrAngle).toFloat()
                ),
                strokeWidth = 4.dp.toPx()
            )

            // Minute hand
            val minAngle = minute * 6 * Math.PI / 180f
            val minLength = radius * 0.75f
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = center,
                end = Offset(
                    center.x + minLength * Math.sin(minAngle).toFloat(),
                    center.y - minLength * Math.cos(minAngle).toFloat()
                ),
                strokeWidth = 2.5f.dp.toPx()
            )

            // Second hand (orange active color)
            val secAngle = second * 6 * Math.PI / 180f
            val secLength = radius * 0.85f
            drawLine(
                color = Color(0xFFFF9500),
                start = center,
                end = Offset(
                    center.x + secLength * Math.sin(secAngle).toFloat(),
                    center.y - secLength * Math.cos(secAngle).toFloat()
                ),
                strokeWidth = 1.5f.dp.toPx()
            )

            // Center pivot
            drawCircle(
                color = Color(0xFFFF9500),
                radius = 3.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
fun CalendarWidget() {
    val cal = Calendar.getInstance()
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString()
    val dayOfWeek = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: ""
    val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Red macOS Header Style
        Text(
            text = monthName.uppercase(),
            color = Color(0xFFFF453A),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Huge dynamic day number
        Text(
            text = dayOfMonth,
            color = Color.White,
            fontSize = 46.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 46.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Day of week
        Text(
            text = dayOfWeek,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SystemStatusWidget() {
    val context = LocalContext.current
    var ramUsage by remember { mutableStateOf(52f) } // Simulated fallback or loaded values
    var batteryUsage by remember { mutableStateOf(85f) }

    // Read real Battery Status dynamically
    LaunchedEffect(Unit) {
        while (true) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryUsage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toFloat()

            // Fetch simulated RAM usage variations for visualization dynamic update
            ramUsage = (40..75).random().toFloat()
            delay(5000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Memory Progress Ring
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawArc(
                            color = Color(0xFF34C759),
                            startAngle = -90f,
                            sweepAngle = (ramUsage / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                    Text(
                        text = "${ramUsage.toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "RAM Usage", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
            }

            // Battery Progress Ring
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawArc(
                            color = Color(0xFF0A84FF),
                            startAngle = -90f,
                            sweepAngle = (batteryUsage / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                    Text(
                        text = "${batteryUsage.toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Battery", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun NotesWidget(
    id: String,
    initialContent: String,
    onSaveContent: (String) -> Unit
) {
    var contentText by remember { mutableStateOf(initialContent.ifEmpty { "Click here to scribble quick notes..." }) }
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable { isEditing = true }
                .padding(vertical = 4.dp)
        ) {
            if (isEditing) {
                BasicTextField(
                    value = contentText,
                    onValueChange = {
                        contentText = it
                        onSaveContent(it)
                    },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = contentText,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 6
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditing) {
                Text(
                    text = "Done",
                    color = Color(0xFF0A84FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { isEditing = false }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Note",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
