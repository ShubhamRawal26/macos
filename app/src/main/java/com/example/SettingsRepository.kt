package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "macos_dock_launcher_settings",
        Context.MODE_PRIVATE
    )

    // Configuration Fields and Defaults
    var dockHeight: Float
        get() = prefs.getFloat("dock_height", 72f)
        set(value) {
            prefs.edit().putFloat("dock_height", value).apply()
            _dockHeightFlow.value = value
        }

    var iconSize: Float
        get() = prefs.getFloat("icon_size", 52f)
        set(value) {
            prefs.edit().putFloat("icon_size", value).apply()
            _iconSizeFlow.value = value
        }

    var magnificationFactor: Float
        get() = prefs.getFloat("magnification_factor", 1.4f)
        set(value) {
            prefs.edit().putFloat("magnification_factor", value).apply()
            _magnificationFactorFlow.value = value
        }

    var blurIntensity: Float
        get() = prefs.getFloat("blur_intensity", 20f)
        set(value) {
            prefs.edit().putFloat("blur_intensity", value).apply()
            _blurIntensityFlow.value = value
        }

    var transparency: Float
        get() = prefs.getFloat("transparency", 0.15f) // Opacity of overlay layer (0 to 1)
        set(value) {
            prefs.edit().putFloat("transparency", value).apply()
            _transparencyFlow.value = value
        }

    var cornerRadius: Float
        get() = prefs.getFloat("corner_radius", 24f)
        set(value) {
            prefs.edit().putFloat("corner_radius", value).apply()
            _cornerRadiusFlow.value = value
        }

    var glowIntensity: Float
        get() = prefs.getFloat("glow_intensity", 8f)
        set(value) {
            prefs.edit().putFloat("glow_intensity", value).apply()
            _glowIntensityFlow.value = value
        }

    var animationDuration: Int
        get() = prefs.getInt("animation_duration", 300)
        set(value) {
            prefs.edit().putInt("animation_duration", value).apply()
            _animationDurationFlow.value = value
        }

    var activeTheme: String
        get() = prefs.getString("active_theme", "Aurora") ?: "Aurora"
        set(value) {
            prefs.edit().putString("active_theme", value).apply()
            _activeThemeFlow.value = value
        }

    var activeWallpaper: String
        get() = prefs.getString("active_wallpaper", "Sonoma Sunset") ?: "Sonoma Sunset"
        set(value) {
            prefs.edit().putString("active_wallpaper", value).apply()
            _activeWallpaperFlow.value = value
        }

    var autoHideMode: String
        get() = prefs.getString("auto_hide_mode", "Always Visible") ?: "Always Visible"
        set(value) {
            prefs.edit().putString("auto_hide_mode", value).apply()
            _autoHideModeFlow.value = value
        }

    var showOverlayDock: Boolean
        get() = prefs.getBoolean("show_overlay_dock", false)
        set(value) {
            prefs.edit().putBoolean("show_overlay_dock", value).apply()
            _showOverlayDockFlow.value = value
        }

    var pinnedAppsString: String
        get() = prefs.getString("pinned_apps", "com.android.settings,com.android.chrome,com.google.android.youtube") ?: ""
        set(value) {
            prefs.edit().putString("pinned_apps", value).apply()
            _pinnedAppsFlow.value = getPinnedAppsList()
        }

    var hiddenAppsString: String
        get() = prefs.getString("hidden_apps", "") ?: ""
        set(value) {
            prefs.edit().putString("hidden_apps", value).apply()
            _hiddenAppsFlow.value = getHiddenAppsList()
        }

    // List helpers
    fun getPinnedAppsList(): List<String> {
        val raw = pinnedAppsString
        if (raw.isEmpty()) return emptyList()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun pinApp(packageName: String) {
        val current = getPinnedAppsList().toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            pinnedAppsString = current.joinToString(",")
        }
    }

    fun unpinApp(packageName: String) {
        val current = getPinnedAppsList().toMutableList()
        if (current.remove(packageName)) {
            pinnedAppsString = current.joinToString(",")
        }
    }

    fun getHiddenAppsList(): List<String> {
        val raw = hiddenAppsString
        if (raw.isEmpty()) return emptyList()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun hideApp(packageName: String) {
        val current = getHiddenAppsList().toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            hiddenAppsString = current.joinToString(",")
        }
    }

    fun unhideApp(packageName: String) {
        val current = getHiddenAppsList().toMutableList()
        if (current.remove(packageName)) {
            hiddenAppsString = current.joinToString(",")
        }
    }

    // Live Flow updates for compose state binding
    private val _dockHeightFlow = MutableStateFlow(dockHeight)
    val dockHeightFlow: StateFlow<Float> = _dockHeightFlow.asStateFlow()

    private val _iconSizeFlow = MutableStateFlow(iconSize)
    val iconSizeFlow: StateFlow<Float> = _iconSizeFlow.asStateFlow()

    private val _magnificationFactorFlow = MutableStateFlow(magnificationFactor)
    val magnificationFactorFlow: StateFlow<Float> = _magnificationFactorFlow.asStateFlow()

    private val _blurIntensityFlow = MutableStateFlow(blurIntensity)
    val blurIntensityFlow: StateFlow<Float> = _blurIntensityFlow.asStateFlow()

    private val _transparencyFlow = MutableStateFlow(transparency)
    val transparencyFlow: StateFlow<Float> = _transparencyFlow.asStateFlow()

    private val _cornerRadiusFlow = MutableStateFlow(cornerRadius)
    val cornerRadiusFlow: StateFlow<Float> = _cornerRadiusFlow.asStateFlow()

    private val _glowIntensityFlow = MutableStateFlow(glowIntensity)
    val glowIntensityFlow: StateFlow<Float> = _glowIntensityFlow.asStateFlow()

    private val _animationDurationFlow = MutableStateFlow(animationDuration)
    val animationDurationFlow: StateFlow<Int> = _animationDurationFlow.asStateFlow()

    private val _activeThemeFlow = MutableStateFlow(activeTheme)
    val activeThemeFlow: StateFlow<String> = _activeThemeFlow.asStateFlow()

    private val _activeWallpaperFlow = MutableStateFlow(activeWallpaper)
    val activeWallpaperFlow: StateFlow<String> = _activeWallpaperFlow.asStateFlow()

    private val _autoHideModeFlow = MutableStateFlow(autoHideMode)
    val autoHideModeFlow: StateFlow<String> = _autoHideModeFlow.asStateFlow()

    private val _showOverlayDockFlow = MutableStateFlow(showOverlayDock)
    val showOverlayDockFlow: StateFlow<Boolean> = _showOverlayDockFlow.asStateFlow()

    private val _pinnedAppsFlow = MutableStateFlow(getPinnedAppsList())
    val pinnedAppsFlow: StateFlow<List<String>> = _pinnedAppsFlow.asStateFlow()

    private val _hiddenAppsFlow = MutableStateFlow(getHiddenAppsList())
    val hiddenAppsFlow: StateFlow<List<String>> = _hiddenAppsFlow.asStateFlow()

    // Widgets placement data in format "type:id:x:y:size_cols:size_rows"
    var widgetsString: String
        get() = prefs.getString("desktop_widgets", "clock:1:1:1:2:2,calendar:2:3:1:2:2,system_info:3:1:3:2:2") ?: ""
        set(value) {
            prefs.edit().putString("desktop_widgets", value).apply()
            _widgetsFlow.value = getWidgetsList()
        }

    data class WidgetData(
        val id: String,
        val type: String, // "clock", "calendar", "notes", "system_info", "weather"
        val col: Int,
        val row: Int,
        val widthCells: Int,
        val heightCells: Int,
        val noteContent: String = ""
    )

    fun getWidgetsList(): List<WidgetData> {
        val raw = widgetsString
        if (raw.isEmpty()) return emptyList()
        return raw.split(";").mapNotNull { part ->
            val tokens = part.split(":")
            if (tokens.size >= 6) {
                WidgetData(
                    id = tokens[1],
                    type = tokens[0],
                    col = tokens[2].toIntOrNull() ?: 1,
                    row = tokens[3].toIntOrNull() ?: 1,
                    widthCells = tokens[4].toIntOrNull() ?: 2,
                    heightCells = tokens[5].toIntOrNull() ?: 2,
                    noteContent = if (tokens.size > 6) tokens[6].replace("##COLON##", ":").replace("##SEMICOLON##", ";") else ""
                )
            } else null
        }
    }

    fun saveWidgetsList(list: List<WidgetData>) {
        val str = list.joinToString(";") { w ->
            val cleanNote = w.noteContent.replace(":", "##COLON##").replace(";", "##SEMICOLON##")
            "${w.type}:${w.id}:${w.col}:${w.row}:${w.widthCells}:${w.heightCells}:${cleanNote}"
        }
        widgetsString = str
    }

    private val _widgetsFlow = MutableStateFlow(getWidgetsList())
    val widgetsFlow: StateFlow<List<WidgetData>> = _widgetsFlow.asStateFlow()

    fun resetAll() {
        prefs.edit().clear().apply()
        // Reload all
        _dockHeightFlow.value = dockHeight
        _iconSizeFlow.value = iconSize
        _magnificationFactorFlow.value = magnificationFactor
        _blurIntensityFlow.value = blurIntensity
        _transparencyFlow.value = transparency
        _cornerRadiusFlow.value = cornerRadius
        _glowIntensityFlow.value = glowIntensity
        _animationDurationFlow.value = animationDuration
        _activeThemeFlow.value = activeTheme
        _activeWallpaperFlow.value = activeWallpaper
        _autoHideModeFlow.value = autoHideMode
        _showOverlayDockFlow.value = showOverlayDock
        _pinnedAppsFlow.value = getPinnedAppsList()
        _hiddenAppsFlow.value = getHiddenAppsList()
        _widgetsFlow.value = getWidgetsList()
    }
}
