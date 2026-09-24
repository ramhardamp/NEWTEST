package com.babasitaram.pro

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SystemDiagnosticsActivity : AppCompatActivity() {

    private lateinit var tv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_diagnostics)

        tv = findViewById(R.id.tvSystemDiagnostics)

        findViewById<Button>(R.id.btnSystemDiagRefresh).setOnClickListener {
            refresh()
        }
        findViewById<Button>(R.id.btnSystemDiagCopy).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(
                ClipData.newPlainText(
                    "BSR System Diagnostics",
                    tv.text.toString()
                )
            )
            Toast.makeText(
                this,
                "System diagnostics copy ho gaya",
                Toast.LENGTH_LONG
            ).show()
        }
        findViewById<Button>(R.id.btnSystemDiagClear).setOnClickListener {
            SystemLog.clear(this)
            refresh()
        }
        findViewById<Button>(R.id.btnSystemDiagClose).setOnClickListener {
            finish()
        }

        SystemLog.add(this, "System Diagnostics opened")
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        tv.text = SystemLog.buildReport(this)
    }
}
