package net.finiasz.lecompte

import android.content.Context
import androidx.compose.runtime.mutableStateOf

class SettingsManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val alwaysSolution = mutableStateOf(getBoolean(ALWAYS_SOLUTION_KEY, true))

    fun setAlwaysSolution(alwaysSolution : Boolean) {
        saveBoolean(ALWAYS_SOLUTION_KEY, alwaysSolution)
        this.alwaysSolution.value = alwaysSolution
    }

    private fun getBoolean(key: String, default : Boolean) : Boolean {
        return sharedPreferences.getBoolean(key, default)
    }

    private fun saveBoolean(key : String, value : Boolean?) {
        val editor = sharedPreferences.edit()
        if (value == null) {
            editor.remove(key)
        } else {
            editor.putBoolean(key, value)
        }
        editor.apply()
    }

    companion object {
        val ALWAYS_SOLUTION_KEY = "always_solution"
    }
}