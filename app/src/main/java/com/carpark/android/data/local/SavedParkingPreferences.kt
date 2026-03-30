package com.carpark.android.data.local

import android.content.Context
import com.carpark.android.data.model.ParkingLot
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SavedParkingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getSavedLots(): List<ParkingLot> {
        val json = prefs.getString(KEY_SAVED_LOTS, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<ParkingLot>>(
                json,
                object : TypeToken<List<ParkingLot>>() {}.type,
            ) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun isSaved(lotId: Int): Boolean {
        return getSavedLots().any { it.id == lotId }
    }

    fun toggle(lot: ParkingLot): Boolean {
        val current = getSavedLots().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == lot.id }
        val isSaved = existingIndex >= 0

        if (isSaved) {
            current.removeAt(existingIndex)
        } else {
            current.add(0, lot)
        }

        save(current)
        return !isSaved
    }

    fun remove(lotId: Int) {
        val updated = getSavedLots().filterNot { it.id == lotId }
        save(updated)
    }

    private fun save(lots: List<ParkingLot>) {
        prefs.edit().putString(KEY_SAVED_LOTS, gson.toJson(lots)).apply()
    }

    companion object {
        private const val PREF_NAME = "saved_parking"
        private const val KEY_SAVED_LOTS = "saved_lots"
    }
}
