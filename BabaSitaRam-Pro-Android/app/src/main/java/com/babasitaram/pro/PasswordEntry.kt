package com.babasitaram.pro

/** Stored vault entry shared by the UI, audit and autofill code. */
data class PasswordEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    var site: String = "",
    var username: String = "",
    var password: String = "",
    var url: String = "",
    var type: String = "login",
    var category: String = "Other",
    var mobile: String = "",
    var notes: String = "",
    var isFavorite: Boolean = false,
    var updatedAt: Long = System.currentTimeMillis(),
    var cardNumber: String = "",
    var cardExpiry: String = "",
    var cardCvv: String = "",
    var cardholder: String = "",
    var fullName: String = "",
    var email: String = "",
    var phone: String = "",
    var address: String = "",
    var idNumber: String = "",
    var idExpiry: String = "",
    var totp: String = "",
    var folder: String = "",
    var tags: List<String>? = emptyList(),
    var fields: List<CustomField?>? = emptyList(),
    var history: List<PasswordHistory>? = emptyList()
)

data class CustomField(val k: String = "", val v: String = "")
data class PasswordHistory(val at: Long = 0L, val pw: String = "")
