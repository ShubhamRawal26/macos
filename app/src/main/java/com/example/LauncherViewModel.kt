package com.example

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepository = SettingsRepository(application)
    private val packageManager: PackageManager = application.packageManager

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private val _runningPackages = MutableStateFlow<Set<String>>(mutableSetOf())
    val runningPackages: StateFlow<Set<String>> = _runningPackages.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered apps based on search query
    private val _searchResults = MutableStateFlow<List<AppItem>>(emptyList())
    val searchResults: StateFlow<List<AppItem>> = _searchResults.asStateFlow()

    // Virtual default macOS apps
    private val virtualApps = listOf(
        AppItem("com.apple.finder", "FinderActivity", "Finder", isVirtual = true, virtualType = "finder", isPinned = true),
        AppItem("com.apple.safari", "SafariActivity", "Safari", isVirtual = true, virtualType = "safari", isPinned = true),
        AppItem("com.apple.notes", "NotesActivity", "Notes", isVirtual = true, virtualType = "notes", isPinned = true),
        AppItem("com.apple.settings", "SettingsActivity", "Dock Settings", isVirtual = true, virtualType = "settings", isPinned = true),
        AppItem("com.apple.appstore", "AppStoreActivity", "App Store", isVirtual = true, virtualType = "appstore", isPinned = true)
    )

    init {
        loadInstalledApps()
        // Listen to changes in settings or running apps to update search/pin list
        viewModelScope.launch {
            combine(
                _installedApps,
                settingsRepository.pinnedAppsFlow,
                settingsRepository.hiddenAppsFlow,
                _runningPackages
            ) { apps, pinned, hidden, running ->
                apps.map { app ->
                    app.copy(
                        isPinned = pinned.contains(app.packageName),
                        isRunning = running.contains(app.packageName)
                    )
                }.filter { !hidden.contains(it.packageName) }
            }.collect { updatedList ->
                // Sort or handle search query updates
                updateFilteredList(updatedList)
            }
        }

        viewModelScope.launch {
            _searchQuery.collect { query ->
                updateSearch(query)
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            val appList = mutableListOf<AppItem>()

            // First time setup - Pin some default apps if empty
            val firstRun = settingsRepository.getPinnedAppsList().isEmpty()
            val initialPins = mutableListOf<String>()

            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                val label = info.loadLabel(packageManager).toString()
                val actName = info.activityInfo.name
                val isSys = (info.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                // If first run, auto pin browser/settings/maps or similar
                if (firstRun) {
                    if (pkgName.contains("chrome") || pkgName.contains("browser") || pkgName.contains("settings") || pkgName.contains("youtube") || pkgName.contains("camera")) {
                        initialPins.add(pkgName)
                    }
                }

                appList.add(
                    AppItem(
                        packageName = pkgName,
                        activityName = actName,
                        label = label,
                        isSystem = isSys,
                        isPinned = false
                    )
                )
            }

            if (firstRun && initialPins.isNotEmpty()) {
                settingsRepository.pinnedAppsString = initialPins.joinToString(",")
            }

            // Group alphabetically or keep unsorted
            _installedApps.value = appList.sortedBy { it.label }
        }
    }

    private fun updateFilteredList(apps: List<AppItem>) {
        val query = _searchQuery.value
        if (query.isEmpty()) {
            _searchResults.value = apps
        } else {
            _searchResults.value = apps.filter {
                it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    private fun updateSearch(query: String) {
        val apps = _installedApps.value
        if (query.isEmpty()) {
            _searchResults.value = apps
        } else {
            _searchResults.value = apps.filter {
                it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Combine virtual macOS apps and pinned Android apps to construct the complete Dock list
    fun getDockApps(pinnedPackages: List<String>): List<AppItem> {
        val realApps = _installedApps.value
        val pinnedReal = pinnedPackages.mapNotNull { pkg ->
            realApps.find { it.packageName == pkg }?.copy(isPinned = true, isRunning = _runningPackages.value.contains(pkg))
        }

        // Return virtual apps + pinned real apps
        val virtuals = virtualApps.map { v ->
            v.copy(isRunning = _runningPackages.value.contains(v.packageName))
        }
        return virtuals + pinnedReal
    }

    fun launchApp(app: AppItem) {
        viewModelScope.launch {
            // Update running list
            val currentRunning = _runningPackages.value.toMutableSet()
            currentRunning.add(app.packageName)
            _runningPackages.value = currentRunning

            if (app.isVirtual) {
                // Launch special triggers
                when (app.virtualType) {
                    "finder" -> {
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        tryLaunchIntent(intent)
                    }
                    "safari" -> {
                        val chromeIntent = packageManager.getLaunchIntentForPackage("com.android.chrome")
                            ?: packageManager.getLaunchIntentForPackage("com.android.browser")
                            ?: Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        tryLaunchIntent(chromeIntent)
                    }
                    "notes" -> {
                        // Open system notes or default text editor, or just notify
                        val notesIntent = packageManager.getLaunchIntentForPackage("com.google.android.keep")
                            ?: Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                type = "text/plain"
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        tryLaunchIntent(notesIntent)
                    }
                    "settings" -> {
                        // Open our settings page (managed inside MainActivity Compose routing)
                    }
                    "appstore" -> {
                        val playIntent = packageManager.getLaunchIntentForPackage("com.android.vending")
                            ?: Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        tryLaunchIntent(playIntent)
                    }
                }
            } else {
                val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    tryLaunchIntent(intent)
                }
            }
        }
    }

    private fun tryLaunchIntent(intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun closeRunningApp(packageName: String) {
        val currentRunning = _runningPackages.value.toMutableSet()
        currentRunning.remove(packageName)
        _runningPackages.value = currentRunning
    }

    fun pinAppToDock(packageName: String) {
        settingsRepository.pinApp(packageName)
    }

    fun unpinAppFromDock(packageName: String) {
        settingsRepository.unpinApp(packageName)
    }

    fun hideAppFromDrawer(packageName: String) {
        settingsRepository.hideApp(packageName)
    }

    // Widgets placement management
    fun addWidget(type: String) {
        val current = settingsRepository.getWidgetsList().toMutableList()
        val nextId = (current.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0) + 1
        // Try to place it nicely
        val newWidget = SettingsRepository.WidgetData(
            id = nextId.toString(),
            type = type,
            col = if (current.size % 2 == 0) 1 else 3,
            row = (current.size / 2) * 2 + 1,
            widthCells = 2,
            heightCells = 2
        )
        current.add(newWidget)
        settingsRepository.saveWidgetsList(current)
    }

    fun removeWidget(id: String) {
        val current = settingsRepository.getWidgetsList().filter { it.id != id }
        settingsRepository.saveWidgetsList(current)
    }

    fun updateWidgetNoteContent(id: String, content: String) {
        val current = settingsRepository.getWidgetsList().map { w ->
            if (w.id == id) w.copy(noteContent = content) else w
        }
        settingsRepository.saveWidgetsList(current)
    }
}
