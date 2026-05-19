package com.example.klinikku.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "KlinikKuPrefs"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_NIK = "nik"
        private const val KEY_NAMA = "nama"
        private const val KEY_ROLE = "role"
    }

    /**
     * Saves credentials (NIK, Nama, Role) and sets login status to true.
     * Uses editor.apply() to safely write values asynchronously.
     */
    fun saveSession(nik: String, nama: String, role: String) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_NIK, nik)
            putString(KEY_NAMA, nama)
            putString(KEY_ROLE, role)
            apply()
        }
    }

    /**
     * Returns whether the user is logged in.
     * Default value is false if key is not found.
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Returns the stored NIK string. Returns null if not stored or empty.
     */
    fun getNik(): String? {
        return sharedPreferences.getString(KEY_NIK, null)
    }

    /**
     * Returns the stored Name string. Returns null if not stored or empty.
     */
    fun getNama(): String? {
        return sharedPreferences.getString(KEY_NAMA, null)
    }

    /**
     * Returns the stored Role string. Returns null if not stored or empty.
     */
    fun getRole(): String? {
        return sharedPreferences.getString(KEY_ROLE, null)
    }

    /**
     * Flushes all stored credentials and preferences safely using editor.clear().apply().
     */
    fun logout() {
        sharedPreferences.edit().clear().apply()
    }
}
