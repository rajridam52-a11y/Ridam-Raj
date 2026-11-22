package com.example.data.prefs

enum class ThemePreset(
    val label: String,
    val defaultAccentHex: Long,
    val defaultSecondaryHex: Long
) {
    FUTURISTIC_BLUE("Futuristic Blue", 0xFF00E5FF, 0xFF8B5CF6),
    IOS_TITANIUM("iOS Titanium", 0xFF0A84FF, 0xFF5E5CE6),
    NOTHING_MONO("Nothing Mono", 0xFFFFFFFF, 0xFFFF3B30),
    COSMIC_PURPLE("Purple Nebula", 0xFF8B5CF6, 0xFFD946EF),
    BLACK_GOLD("Black & Gold", 0xFFFFD166, 0xFFF59E0B),
    GLASSMORPHISM("Glassmorphism", 0xFF38BDF8, 0xFF8B5CF6),
    NEON_CYBER("Neon Cyber", 0xFFFF2E93, 0xFF00E5FF),
    EMERALD_AURORA("Emerald Aurora", 0xFF10B981, 0xFF2DD4BF),
    CRIMSON_PULSE("Crimson Pulse", 0xFFF43F5E, 0xFFFB923C),
    LIGHT_FROST("Light Frost", 0xFF2563EB, 0xFF8B5CF6)
}

enum class BackgroundStyle(val label: String) {
    NEBULA_ART("Nebula Waves"),
    AURORA_GRADIENT("Aurora Gradient"),
    STARFIELD_GRID("Starfield Grid"),
    MIDNIGHT_SOLID("Midnight Pure")
}

enum class CardStylePreset(val label: String) {
    GLASS_FROST("Aurora Glass"),
    IOS_ELEVATED("Elevated Soft"),
    NOTHING_OUTLINE("Nothing Outline"),
    NEON_GRADIENT("Neon Rim")
}

enum class FontStylePreset(val label: String) {
    FUTURISTIC("Space Grotesk"),
    OUTFIT_MODERN("Outfit Clean"),
    SORA_TECH("Sora Precision"),
    NOTHING_MONO("JetBrains Mono"),
    LUXURY_SERIF("Editorial Serif")
}

enum class IconStyle(val label: String) {
    FILLED("Filled"),
    OUTLINED("Outlined"),
    ROUNDED("Rounded")
}

enum class DashboardCardType(val id: String, val label: String) {
    CLOCK_GREETING("clock_greeting", "Date, Time & Greeting"),
    WEATHER("weather", "Live Weather"),
    NEXT_REMINDER("next_reminder", "Next Reminder"),
    TODAY_SCHEDULE("today_schedule", "Today's Schedule"),
    GOALS_PROGRESS("goals_progress", "Today's Goals"),
    QUICK_TOOLS("quick_tools", "Alarm, Clock & Stopwatch"),
    CALENDAR_PREVIEW("calendar_preview", "Calendar Preview"),
    BATTERY_STATUS("battery_status", "Battery & Device")
}

data class DesignSettings(
    val isDarkMode: Boolean = true,
    val themePreset: ThemePreset = ThemePreset.FUTURISTIC_BLUE,
    val customAccentHex: Long = 0xFF00E5FF,
    val customSecondaryHex: Long = 0xFF8B5CF6,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.NEBULA_ART,
    val cardStyle: CardStylePreset = CardStylePreset.GLASS_FROST,
    val fontStyle: FontStylePreset = FontStylePreset.FUTURISTIC,
    val cardTransparency: Float = 0.24f,
    val cardCornerRadiusDp: Int = 24,
    val borderGlowEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val blurEffectsEnabled: Boolean = true,
    val animationSpeed: Float = 1.0f,
    val fontScale: Float = 1.0f,
    val iconStyle: IconStyle = IconStyle.ROUNDED,
    val compactWidgets: Boolean = false,
    val dashboardOrder: List<String> = DashboardCardType.entries.map { it.id },
    val hiddenCards: Set<String> = emptySet()
)

data class AppUserPreferences(
    val design: DesignSettings = DesignSettings(),
    val use24HourClock: Boolean = false,
    val defaultSnoozeMinutes: Int = 10,
    val notificationReaderEnabled: Boolean = false,
    val appLockEnabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val biometricEnabled: Boolean = false,
    val offlineModePreferred: Boolean = false,
    val autoCloudSync: Boolean = true,
    val lastCloudBackupMillis: Long = 0L,
    val weatherCityName: String = "San Francisco",
    val weatherLat: Double = 37.7749,
    val weatherLon: Double = -122.4194
)
