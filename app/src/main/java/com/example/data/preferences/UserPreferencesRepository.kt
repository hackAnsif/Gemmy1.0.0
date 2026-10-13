package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gemmy_user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private val fastPrefs = context.getSharedPreferences("gemmy_fast_prefs", Context.MODE_PRIVATE)

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode") // SYSTEM, LIGHT, DARK
        val APP_LANGUAGE = stringPreferencesKey("app_language") // en, ml
        val SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val SMART_INTERVALS = stringPreferencesKey("smart_review_intervals")
        val GEMINI_API_KEY = stringPreferencesKey("user_gemini_api_key")
        val USE_ONLINE_AI = booleanPreferencesKey("use_online_ai")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATE_ENABLED = booleanPreferencesKey("vibrate_enabled")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val GITHUB_REPO = stringPreferencesKey("github_repo") // owner/repo format e.g. "username/repository"
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
    }

    /**
     * Synchronous check on cold start to immediately know if onboarding was completed,
     * avoiding the initial StateFlow `false` flicker on every app open.
     */
    fun hasSeenOnboardingSync(): Boolean {
        return fastPrefs.getBoolean("has_seen_onboarding", false)
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "en"
    }

    val defaultSnoozeMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SNOOZE_MINUTES] ?: 10
    }

    val smartReviewIntervals: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SMART_INTERVALS] ?: "1,3,7,14,30"
    }

    val userGeminiApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GEMINI_API_KEY] ?: ""
    }

    val useOnlineAi: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USE_ONLINE_AI] ?: true
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SOUND_ENABLED] ?: true
    }

    val vibrateEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.VIBRATE_ENABLED] ?: true
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_SEEN_ONBOARDING] ?: fastPrefs.getBoolean("has_seen_onboarding", false)
    }

    val githubRepo: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GITHUB_REPO] ?: ""
    }

    val autoCheckUpdates: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_CHECK_UPDATES] ?: true
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
    }

    suspend fun setAppLanguage(lang: String) {
        context.dataStore.edit { it[PreferencesKeys.APP_LANGUAGE] = lang }
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[PreferencesKeys.SNOOZE_MINUTES] = minutes }
    }

    suspend fun setSmartReviewIntervals(intervals: String) {
        context.dataStore.edit { it[PreferencesKeys.SMART_INTERVALS] = intervals }
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { it[PreferencesKeys.GEMINI_API_KEY] = key }
    }

    suspend fun setUseOnlineAi(useOnline: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_ONLINE_AI] = useOnline }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrateEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VIBRATE_ENABLED] = enabled }
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        fastPrefs.edit().putBoolean("has_seen_onboarding", seen).apply()
        context.dataStore.edit { it[PreferencesKeys.HAS_SEEN_ONBOARDING] = seen }
    }

    suspend fun setGithubRepo(repo: String) {
        context.dataStore.edit { it[PreferencesKeys.GITHUB_REPO] = repo.trim() }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_CHECK_UPDATES] = enabled }
    }
}
