package com.carpark.android.data.local

import android.content.Context
import org.json.JSONArray

class SearchHistoryPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getRecentSearches(): List<String> {
        val raw = prefs.getString(KEY_RECENT_SEARCHES, null) ?: return emptyList()
        return runCatching {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val query = jsonArray.optString(index).trim()
                    if (query.isNotEmpty()) {
                        add(query)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveSearch(query: String, limit: Int = DEFAULT_LIMIT) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val updated = buildList {
            add(trimmed)
            addAll(
                getRecentSearches().filterNot { saved ->
                    saved.equals(trimmed, ignoreCase = true)
                }
            )
        }.take(limit)

        persist(updated)
    }

    fun removeSearches(queries: Set<String>) {
        if (queries.isEmpty()) return
        val normalized = queries.map { it.trim().lowercase() }.toSet()
        val updated = getRecentSearches().filterNot { query ->
            query.trim().lowercase() in normalized
        }
        persist(updated)
    }

    fun clearRecentSearches() {
        prefs.edit().remove(KEY_RECENT_SEARCHES).apply()
    }

    private fun persist(queries: List<String>) {
        val jsonArray = JSONArray()
        queries.forEach(jsonArray::put)
        prefs.edit().putString(KEY_RECENT_SEARCHES, jsonArray.toString()).apply()
    }

    companion object {
        private const val PREF_NAME = "carpark"
        private const val KEY_RECENT_SEARCHES = "recentSearches"
        private const val DEFAULT_LIMIT = 10
    }
}
