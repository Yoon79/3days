package com.goodafteryoon.threedays

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "threedays_prefs")

data class GoalItem(
    val id: String,
    val text: String,
    val dueEpochMillis: Long
)

class GoalRepository(private val context: Context) {

    private object Keys {
        val GOALS_JSON: Preferences.Key<String> = stringPreferencesKey("goals_json")
    }

    fun observeGoals(): Flow<List<GoalItem>> = context.dataStore.data.map { prefs ->
        parseGoalsJson(prefs[Keys.GOALS_JSON])
    }

    suspend fun getGoals(): List<GoalItem> = context.dataStore.data.map { prefs ->
        parseGoalsJson(prefs[Keys.GOALS_JSON])
    }.firstOrNull() ?: emptyList()

    suspend fun getGoal(id: String): GoalItem? = getGoals().firstOrNull { it.id == id }

    suspend fun addGoal(text: String): GoalItem {
        val now = System.currentTimeMillis()
        val item = GoalItem(
            id = UUID.randomUUID().toString(),
            text = text,
            dueEpochMillis = now + THREE_DAYS_MS
        )
        val updated = getGoals().toMutableList().apply { add(0, item) }
        saveGoals(updated)
        return item
    }

    suspend fun updateGoal(id: String, newText: String): GoalItem? {
        val now = System.currentTimeMillis()
        val updated = getGoals().map { g ->
            if (g.id == id) g.copy(text = newText, dueEpochMillis = now + THREE_DAYS_MS) else g
        }
        saveGoals(updated)
        return updated.firstOrNull { it.id == id }
    }

    suspend fun resetGoalTimer(id: String): GoalItem? {
        val now = System.currentTimeMillis()
        val updated = getGoals().map { g ->
            if (g.id == id) g.copy(dueEpochMillis = now + THREE_DAYS_MS) else g
        }
        saveGoals(updated)
        return updated.firstOrNull { it.id == id }
    }

    suspend fun deleteGoal(id: String) {
        val updated = getGoals().filterNot { it.id == id }
        saveGoals(updated)
    }

    private suspend fun saveGoals(list: List<GoalItem>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOALS_JSON] = toJson(list)
        }
    }

    private fun parseGoalsJson(json: String?): List<GoalItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val result = ArrayList<GoalItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(
                    GoalItem(
                        id = o.optString("id"),
                        text = o.optString("text"),
                        dueEpochMillis = o.optLong("due")
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun toJson(list: List<GoalItem>): String {
        val arr = JSONArray()
        list.forEach { g ->
            val o = JSONObject()
            o.put("id", g.id)
            o.put("text", g.text)
            o.put("due", g.dueEpochMillis)
            arr.put(o)
        }
        return arr.toString()
    }

    companion object {
        private const val THREE_DAYS_MS: Long = 3L * 24 * 60 * 60 * 1000
    }
}
