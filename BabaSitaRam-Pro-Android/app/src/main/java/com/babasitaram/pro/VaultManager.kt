package com.babasitaram.pro

import android.content.Context
import java.util.concurrent.CopyOnWriteArrayList

/** Minimal in-process vault store used by the Android UI and autofill service. */
object VaultManager {
    private val entries = CopyOnWriteArrayList<PasswordEntry>()
    @Volatile var isUnlocked: Boolean = false

    fun getPasswords(): List<PasswordEntry> = entries.toList()
    fun search(q: String): List<PasswordEntry> = entries.filter {
        listOf(it.site, it.username, it.url, it.category, it.notes).any { value -> value.contains(q, true) }
    }
    fun getFavorites() = entries.filter { it.isFavorite }
    fun getByCategory(category: String) = entries.filter { it.category == category }
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
