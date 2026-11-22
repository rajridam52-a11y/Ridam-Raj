package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rr_reminder_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val ACCENT_HEX = longPreferencesKey("accent_hex")
        val SECONDARY_HEX = longPreferencesKey("secondary_hex")
        val BG_STYLE = stringPreferencesKey("bg_style")
        val CARD_STYLE = stringPreferencesKey("card_style")
        val FONT_STYLE = stringPreferencesKey("font_style")
        val CARD_TRANSPARENCY = floatPreferencesKey("card_transparency")
        val CARD_CORNER_RADIUS = intPreferencesKey("card_corner_radius")
        val BORDER_GLOW = booleanPreferencesKey("border_glow")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val BLUR_EFFECTS = booleanPreferencesKey("blur_effects")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val ICON_STYLE = stringPreferencesKey("icon_style")
        val COMPACT_WIDGETS = booleanPreferencesKey("compact_widgets")
        val DASHBOARD_ORDER = stringPreferencesKey("dashboard_order")
        val HIDDEN_CARDS = stringPreferencesKey("hidden_cards")

        val USE_24H = booleanPreferencesKey("use_24h")
        val DEFAULT_SNOOZE = intPreferencesKey("default_snooze")
        val NOTIF_READER = booleanPreferencesKey("notif_reader")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val LAST_BACKUP = longPreferencesKey("last_backup")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val WEATHER_LAT = doublePreferencesKey("weather_lat")
        val WEATHER_LON = doublePreferencesKey("weather_lon")
        val DEFAULT_RINGTONE_NAME = stringPreferencesKey("default_ringtone_name")
        val DEFAULT_RINGTONE_URI = stringPreferencesKey("default_ringtone_uri")
        val SAVED_RINGTONES_JSON = stringPreferencesKey("saved_ringtones_json")
    }

    val preferencesFlow: Flow<AppUserPreferences> = context.dataStore.data.map { p ->
        val defaultOrder = DashboardCardType.entries.map { it.id }
        val orderList = p[Keys.DASHBOARD_ORDER]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: defaultOrder
        val mergedOrder = (orderList + defaultOrder).distinct()

        val hiddenSet = p[Keys.HIDDEN_CARDS]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

        AppUserPreferences(
            design = DesignSettings(
                isDarkMode = p[Keys.DARK_MODE] ?: true,
                themePreset = enumValues<ThemePreset>().firstOrNull {
                    it.name == p[Keys.THEME_PRESET]
                } ?: ThemePreset.FUTURISTIC_BLUE,
                customAccentHex = p[Keys.ACCENT_HEX] ?: 0xFF00E5FF,
                customSecondaryHex = p[Keys.SECONDARY_HEX] ?: 0xFF8B5CF6,
                backgroundStyle = enumValues<BackgroundStyle>().firstOrNull {
                    it.name == p[Keys.BG_STYLE]
                } ?: BackgroundStyle.NEBULA_ART,
                cardStyle = enumValues<CardStylePreset>().firstOrNull {
                    it.name == p[Keys.CARD_STYLE]
                } ?: CardStylePreset.GLASS_FROST,
                fontStyle = enumValues<FontStylePreset>().firstOrNull {
                    it.name == p[Keys.FONT_STYLE]
                } ?: FontStylePreset.FUTURISTIC,
                cardTransparency = (p[Keys.CARD_TRANSPARENCY] ?: 0.24f).coerceIn(0.10f, 0.90f),
                cardCornerRadiusDp = (p[Keys.CARD_CORNER_RADIUS] ?: 24).coerceIn(10, 32),
                borderGlowEnabled = p[Keys.BORDER_GLOW] ?: true,
                animationsEnabled = p[Keys.ANIMATIONS_ENABLED] ?: true,
                blurEffectsEnabled = p[Keys.BLUR_EFFECTS] ?: true,
                animationSpeed = (p[Keys.ANIMATION_SPEED] ?: 1.0f).coerceIn(0.5f, 2.0f),
                fontScale = (p[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.30f),
                iconStyle = enumValues<IconStyle>().firstOrNull {
                    it.name == p[Keys.ICON_STYLE]
                } ?: IconStyle.ROUNDED,
                compactWidgets = p[Keys.COMPACT_WIDGETS] ?: false,
                dashboardOrder = mergedOrder,
                hiddenCards = hiddenSet
            ),
            use24HourClock = p[Keys.USE_24H] ?: false,
            defaultSnoozeMinutes = p[Keys.DEFAULT_SNOOZE] ?: 10,
            notificationReaderEnabled = p[Keys.NOTIF_READER] ?: false,
            appLockEnabled = p[Keys.APP_LOCK] ?: false,
            pinHash = p[Keys.PIN_HASH] ?: "",
            pinSalt = p[Keys.PIN_SALT] ?: "",
            biometricEnabled = p[Keys.BIOMETRIC] ?: false,
            offlineModePreferred = p[Keys.OFFLINE_MODE] ?: false,
            autoCloudSync = p[Keys.AUTO_SYNC] ?: true,
            lastCloudBackupMillis = p[Keys.LAST_BACKUP] ?: 0L,
            weatherCityName = p[Keys.WEATHER_CITY] ?: "New Delhi",
            weatherLat = p[Keys.WEATHER_LAT] ?: 28.6139,
            weatherLon = p[Keys.WEATHER_LON] ?: 77.2090,
            defaultRingtoneName = p[Keys.DEFAULT_RINGTONE_NAME] ?: "System Default Alarm",
            defaultRingtoneUri = p[Keys.DEFAULT_RINGTONE_URI]?.takeIf { it.isNotBlank() },
            savedRingtones = decodeRingtones(p[Keys.SAVED_RINGTONES_JSON])
        )
    }

    suspend fun updateDesign(design: DesignSettings) {
        context.dataStore.edit { p ->
            p[Keys.DARK_MODE] = design.isDarkMode
            p[Keys.THEME_PRESET] = design.themePreset.name
            p[Keys.ACCENT_HEX] = design.customAccentHex
            p[Keys.SECONDARY_HEX] = design.customSecondaryHex
            p[Keys.BG_STYLE] = design.backgroundStyle.name
            p[Keys.CARD_STYLE] = design.cardStyle.name
            p[Keys.FONT_STYLE] = design.fontStyle.name
            p[Keys.CARD_TRANSPARENCY] = design.cardTransparency
            p[Keys.CARD_CORNER_RADIUS] = design.cardCornerRadiusDp
            p[Keys.BORDER_GLOW] = design.borderGlowEnabled
            p[Keys.ANIMATIONS_ENABLED] = design.animationsEnabled
            p[Keys.BLUR_EFFECTS] = design.blurEffectsEnabled
            p[Keys.ANIMATION_SPEED] = design.animationSpeed
            p[Keys.FONT_SCALE] = design.fontScale
            p[Keys.ICON_STYLE] = design.iconStyle.name
            p[Keys.COMPACT_WIDGETS] = design.compactWidgets
            p[Keys.DASHBOARD_ORDER] = design.dashboardOrder.joinToString(",")
            p[Keys.HIDDEN_CARDS] = design.hiddenCards.joinToString(",")
        }
    }

    suspend fun resetDesignToDefault() {
        updateDesign(DesignSettings())
    }

    suspend fun setClockFormat(use24h: Boolean) {
        context.dataStore.edit { it[Keys.USE_24H] = use24h }
    }

    suspend fun setDefaultSnooze(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_SNOOZE] = minutes.coerceIn(1, 60) }
    }

    suspend fun setNotificationReader(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_READER] = enabled }
    }

    suspend fun setPinLock(enabled: Boolean, hash: String, salt: String, biometric: Boolean) {
        context.dataStore.edit { p ->
            p[Keys.APP_LOCK] = enabled
            p[Keys.PIN_HASH] = hash
            p[Keys.PIN_SALT] = salt
            p[Keys.BIOMETRIC] = biometric
        }
    }

    suspend fun setOfflineModePreferred(offline: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_MODE] = offline }
    }

    suspend fun setAutoCloudSync(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SYNC] = enabled }
    }

    suspend fun setLastCloudBackup(millis: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP] = millis }
    }

    suspend fun setWeatherLocation(city: String, lat: Double, lon: Double) {
        context.dataStore.edit { p ->
            p[Keys.WEATHER_CITY] = city
            p[Keys.WEATHER_LAT] = lat
            p[Keys.WEATHER_LON] = lon
        }
    }

    suspend fun setDefaultRingtone(name: String, uri: String?) {
        context.dataStore.edit { p ->
            p[Keys.DEFAULT_RINGTONE_NAME] = name.ifBlank { "System Default Alarm" }
            if (uri.isNullOrBlank()) {
                p.remove(Keys.DEFAULT_RINGTONE_URI)
            } else {
                p[Keys.DEFAULT_RINGTONE_URI] = uri
            }
        }
    }

    suspend fun addSavedRingtone(item: CustomRingtoneItem) {
        context.dataStore.edit { p ->
            val current = decodeRingtones(p[Keys.SAVED_RINGTONES_JSON])
                .filterNot { it.id == item.id || it.uriString == item.uriString }
            val updated = listOf(item) + current
            p[Keys.SAVED_RINGTONES_JSON] = encodeRingtones(updated)
        }
    }

    suspend fun removeSavedRingtone(id: String) {
        context.dataStore.edit { p ->
            val current = decodeRingtones(p[Keys.SAVED_RINGTONES_JSON])
            val removed = current.firstOrNull { it.id == id }
            val updated = current.filterNot { it.id == id }
            p[Keys.SAVED_RINGTONES_JSON] = encodeRingtones(updated)
            if (removed != null && p[Keys.DEFAULT_RINGTONE_URI] == removed.uriString) {
                p[Keys.DEFAULT_RINGTONE_NAME] = "System Default Alarm"
                p.remove(Keys.DEFAULT_RINGTONE_URI)
            }
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        fun encodeRingtones(list: List<CustomRingtoneItem>): String {
            val arr = JSONArray()
            list.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("name", item.name)
                        put("artist", item.artist)
                        put("uriString", item.uriString)
                        put("durationMs", item.durationMs)
                        put("sourceType", item.sourceType)
                        put("addedAtMillis", item.addedAtMillis)
                    }
                )
            }
            return arr.toString()
        }

        fun decodeRingtones(raw: String?): List<CustomRingtoneItem> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optString("id")
                        val name = obj.optString("name")
                        val uri = obj.optString("uriString")
                        if (id.isNotBlank() && name.isNotBlank() && uri.isNotBlank()) {
                            add(
                                CustomRingtoneItem(
                                    id = id,
                                    name = name,
                                    artist = obj.optString("artist", ""),
                                    uriString = uri,
                                    durationMs = obj.optLong("durationMs", 0L),
                                    sourceType = obj.optString("sourceType", "SONG"),
                                    addedAtMillis = obj.optLong("addedAtMillis", 0L)
                                )
                            )
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}
