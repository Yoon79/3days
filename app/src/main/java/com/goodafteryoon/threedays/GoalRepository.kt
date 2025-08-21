package com.goodafteryoon.threedays

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull

private val Context.dataStore by preferencesDataStore(name = "threedays_prefs")

data class GoalState(
    val goal: String,
    val dueEpochMillis: Long
)

class GoalRepository(private val context: Context) {

    private object Keys {
        val GOAL: Preferences.Key<String> = stringPreferencesKey("goal")
        val DUE: Preferences.Key<Long> = longPreferencesKey("due")
    }

    fun observeGoalState(): Flow<GoalState?> = context.dataStore.data.map { prefs ->
        val goal = prefs[Keys.GOAL] ?: return@map null
        val due = prefs[Keys.DUE] ?: return@map null
        GoalState(goal, due)
    }

    suspend fun setGoalAndResetTimer(goal: String): GoalState {
        val due = System.currentTimeMillis() + THREE_DAYS_MS
        context.dataStore.edit { prefs ->
            prefs[Keys.GOAL] = goal
            prefs[Keys.DUE] = due
        }
        return GoalState(goal, due)
    }

    suspend fun getCurrentState(): GoalState? = context.dataStore.data.map { prefs ->
        val goal = prefs[Keys.GOAL] ?: return@map null
        val due = prefs[Keys.DUE] ?: return@map null
        GoalState(goal, due)
    }.firstOrNull()

    companion object {
        private const val THREE_DAYS_MS: Long = 3L * 24 * 60 * 60 * 1000
    }
}
