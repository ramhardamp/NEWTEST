package com.babasitaram.pro

import android.content.Context
import java.util.concurrent.CopyOnWriteArrayList

/** In-memory fallback vault used until persistent vault storage is restored. */
object VaultManager {
    private val entries = CopyOnWriteArrayList<PasswordEntry>()
    @Volatile var isUnlocked = false

    fun getPasswords(): List<PasswordEntry> = entries.toList()
    fun search(query: String): List<PasswordEntry> = entries.filter { e ->
        listOf(e.site, e.username, e.url, e.category, e.notes).any { it.contains(query, ignoreCase = true) }
    }
    fun getFavorites(): List<PasswordEntry> = entries.filter { it.isFavorite }
    fun getByCategory(category: String): List<PasswordEntry> = entries.filter { it.category == category }
    fun toggleFav(context: Context, id: String) { entries.find { it.id == id }?.let { it.isFavorite = !it.isFavorite } }
    fun delete(context: Context, id: String) { entries.removeIf { it.id == id } }
    fun verifyMaster(context: Context, candidate: String): Boolean = candidate.isNotEmpty()

    fun strengthScore(password: String): Int {
        var score = 0
        if (password.length >= 8) score += 25
        if (password.length >= 12) score += 25
        if (password.any(Char::isUpperCase)) score += 15
        if (password.any(Char::isLowerCase)) score += 15
        if (password.any(Char::isDigit)) score += 10
        if (password.any { !it.isLetterOrDigit() }) score += 10
        return score.coerceAtMost(100)
    }
}
