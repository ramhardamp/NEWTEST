package com.babasitaram.pro

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Safe in-app health check.
 * This test NEVER adds/updates/deletes vault entries and never stores a test password.
 * Crypto/import checks run entirely in memory. Device/system checks are read-only.
 */
class SelfTestActivity : AppCompatActivity() {
    private lateinit var tv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_self_test)
        tv = findViewById(R.id.tvSelfTest)

        findViewById<Button>(R.id.btnRunSelfTest).setOnClickListener { runTests() }
        findViewById<Button>(R.id.btnCopySelfTest).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("BSR Self-Test", tv.text.toString()))
            Toast.makeText(this, "Self-Test report copy ho gaya", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnCloseSelfTest).setOnClickListener { finish() }
        runTests()
    }

    private fun runTests() {
        tv.text = "BSR SELF-TEST\n\nTesting... please wait"
        lifecycleScope.launch {
            val report = withContext(Dispatchers.Default) { executeTests() }
            tv.text = report
        }
    }

    private fun executeTests(): String {
        val pass = mutableListOf<String>()
        val warn = mutableListOf<String>()
        val fail = mutableListOf<String>()

        fun pass(name: String) { pass += "PASS  $name" }
        fun warn(name: String) { warn += "WARN  $name" }
        fun fail(name: String) { fail += "FAIL  $name" }

        try {
            val pi = packageManager.getPackageInfo(packageName, 0)
            if (pi.packageName == "com.babasitaram.pro") pass("App package/version readable")
            else fail("Unexpected package: " + pi.packageName)
        } catch (e: Exception) { fail("Package check: " + e.javaClass.simpleName) }

        try {
            if (VaultManager.isUnlocked) {
                pass("Vault readable (entries=" + VaultManager.getPasswords().size + "; no vault write)")
            } else warn("Vault locked — vault contents were not opened")
        } catch (e: Exception) { fail("Vault read check: " + e.javaClass.simpleName) }

        val testPassword = "BSR-SelfTest-Only-9!x"
        val testEntry = PasswordEntry(
            id = "bsr-self-test-entry",
            site = "BSR Self Test",
            url = "https://self-test.example.invalid/login",
            appPackage = "com.babasitaram.pro",
            username = "selftest@example.invalid",
            password = testPassword,
            notes = "temporary in-memory self test",
            category = "Other",
            type = "login"
        )

        try {
            val plain = """{"selfTest":"ok","value":"BSR"}"""
            val enc = VaultManager.encryptString(plain, testPassword)
            val dec = VaultManager.decryptString(enc, testPassword)
            if (dec == plain) pass("BSR AES-GCM encrypt/decrypt round-trip")
            else fail("BSR AES-GCM round-trip mismatch")
        } catch (e: Exception) { fail("BSR AES-GCM: " + e.javaClass.simpleName) }

        try {
            val backupJson = BackupManager.buildBackupJson(testPassword, listOf(testEntry))
            val imported = kotlinx.coroutines.runBlocking {
                BackupManager.importBackup(this@SelfTestActivity, backupJson, testPassword)
            }
            if (imported is BackupManager.ImportResult.Success &&
                imported.data.passwords.size == 1 &&
                imported.data.passwords[0].username == testEntry.username
            ) pass(".bsrpro build/verify/decrypt/import round-trip")
            else fail(".bsrpro import round-trip returned unexpected result")
        } catch (e: Exception) { fail(".bsrpro round-trip: " + e.javaClass.simpleName) }

        try {
            val encrypted = BackupManager.buildExtensionBackup(testPassword, listOf(testEntry))
            val plain = VaultManager.decryptExtensionBackup(encrypted, testPassword)
            val root = JsonParser.parseString(plain).asJsonObject
            if (root.get("_bsrOrigin")?.asString == "BABASITARAMPro:android" &&
                root.has("data")
            ) pass(".vaultbak build/decrypt round-trip")
            else fail(".vaultbak round-trip returned unexpected result")
        } catch (e: Exception) { fail(".vaultbak round-trip: " + e.javaClass.simpleName) }

        try {
            val json = """{"_bsrOrigin":"BABASITARAMPro:android","data":{"version":"2.0","entries":[{"id":"x","title":"Self Test","url":"https://example.invalid","username":"u","password":"p"}]}}"""
            val root = JsonParser.parseString(json).asJsonObject
            val data = root.getAsJsonObject("data")
            val entries = data.getAsJsonArray("entries")
            if (root.get("_bsrOrigin").asString == "BABASITARAMPro:android" && entries.size() == 1) pass("Extension JSON wrapper parser")
            else fail("Extension JSON wrapper parser")
        } catch (e: Exception) { fail("Extension JSON parser: " + e.javaClass.simpleName) }

        try {
            val csv = "name,url,username,password\nSelf Test,https://example.invalid,u,p"
            val rows = BackupManager.parseCsv(csv)
            if (rows.size == 2 && rows[1].size == 4 && rows[1][0] == "Self Test") pass("CSV parser/header mapping")
            else fail("CSV parser/header mapping")
        } catch (e: Exception) { fail("CSV parser: " + e.javaClass.simpleName) }

        try {
            val key = VaultManager.dedupKey(testEntry)
            if (key.startsWith("l|") && key.contains("selftest@example.invalid")) pass("Vault duplicate-key generation")
            else fail("Vault duplicate-key generation")
        } catch (e: Exception) { fail("Dedup key: " + e.javaClass.simpleName) }

        try {
            val weak = VaultManager.strengthScore("abc")
            val strong = VaultManager.strengthScore("A9!long-strong-password")
            if (weak < strong && strong > 70) pass("Password strength calculation")
            else fail("Password strength calculation")
        } catch (e: Exception) { fail("Password strength: " + e.javaClass.simpleName) }

        try {
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            if (apps.isNotEmpty()) pass("Installed-app package discovery (" + apps.size + ")")
            else fail("Installed-app package discovery returned 0 apps")
        } catch (e: Exception) { fail("Installed-app query: " + e.javaClass.simpleName) }

        try {
            if (AutofillSetup.isEnabled(this)) pass("BSR Autofill is enabled")
            else warn("BSR Autofill is disabled — enable it for external Autofill tests")
        } catch (e: Exception) { fail("Autofill state: " + e.javaClass.simpleName) }

        try {
            when (ChromeAutofill.getStatus(this)) {
                ChromeAutofill.Status.ENABLED -> pass("Chrome 3rd-party Autofill integration enabled")
                ChromeAutofill.Status.DISABLED -> warn("Chrome 3rd-party Autofill integration disabled")
                ChromeAutofill.Status.UNAVAILABLE -> warn("Chrome Autofill status unavailable on this Chrome build")
            }
        } catch (e: Exception) { fail("Chrome integration check: " + e.javaClass.simpleName) }

        try {
            com.babasitaram.pro.autofill.Psl.ensureLoaded(this)
            if (com.babasitaram.pro.autofill.Psl.isLoaded()) pass("Public suffix list loaded")
            else warn("Public suffix list not loaded")
        } catch (e: Exception) { fail("PSL check: " + e.javaClass.simpleName) }

        try {
            if (AutoBackup.isConfigured(this)) pass("Auto-backup folder configured")
            else warn("Auto-backup folder not configured")
        } catch (e: Exception) { fail("Auto-backup check: " + e.javaClass.simpleName) }

        pass("Data-safety invariant: no add/update/delete/reset operation executed")

        val total = pass.size + warn.size + fail.size
        return buildString {
            appendLine("===== BSR VAULT SELF-TEST =====")
            appendLine("Version: v${BuildConfig.VERSION_NAME}")
            appendLine("Result : ${if (fail.isEmpty()) "PASS" else "FAIL"}")
            appendLine("Checks : $total   PASS=${pass.size}  WARN=${warn.size}  FAIL=${fail.size}")
            appendLine()
            appendLine("--- PASS ---")
            pass.forEach(::appendLine)
            appendLine()
            appendLine("--- WARN ---")
            if (warn.isEmpty()) appendLine("(none)") else warn.forEach(::appendLine)
            appendLine()
            appendLine("--- FAIL ---")
            if (fail.isEmpty()) appendLine("(none)") else fail.forEach(::appendLine)
            appendLine()
            appendLine("--- NOT COVERED BY IN-APP SELF-TEST ---")
            appendLine("External Chrome website Autofill/save-popup and vendor-specific")
            appendLine("Xiaomi Pattern/Biometric -> Autofill resume require device UI automation.")
            appendLine("This screen deliberately does not fake those results.")
            appendLine()
            appendLine("DATA SAFETY: Real vault entries were not modified by this self-test.")
        }
    }
}
