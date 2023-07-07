package com.udacity.project4.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreManager(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "myLocationReminderApp")

    companion object {
        val PREF_KEY_IS_USER_CONNECTED = booleanPreferencesKey("isUserConnected")
        val PREF_KEY_DISPLAY_NAME = stringPreferencesKey("userDisplayName")
    }

    suspend fun saveIntDataStore(key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit { preferences -> preferences[key] = value }
    }

    suspend fun saveBooleanDataStore(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { preferences -> preferences[key] = value }
    }

    suspend fun saveStringDataStore(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { preferences -> preferences[key] = value }
    }

    val getUserDisplayName: Flow<String> = context.dataStore.data.map { preferences -> preferences[PREF_KEY_DISPLAY_NAME] ?: "" }

    val isUserConnected: Flow<Boolean> = context.dataStore.data.map { preferences -> preferences[PREF_KEY_IS_USER_CONNECTED] ?: false }
}
