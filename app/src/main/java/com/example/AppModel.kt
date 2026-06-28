package com.example

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isSystem: Boolean = false,
    val isPinned: Boolean = false,
    val isRunning: Boolean = false,
    val isVirtual: Boolean = false, // True for custom preloaded macOS styled apps
    val virtualType: String = "" // "finder", "safari", "notes", "settings", "launchpad", "appstore"
)
