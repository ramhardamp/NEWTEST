package com.babasitaram.pro

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** Settings → Autofill Diagnostics. Refresh / Copy / Clear. */
class AutofillDiagnosticsActivity : AppCompatActivity() {

    private lateinit var tv: TextView

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_autofill_diagnostics)
        tv = findViewById(R.id.tvDiagnostics)

        findViewById<Button>(R.id.btnDiagRefresh).setOnClickListener { refresh() }
        findViewById<Button>(R.id.btnDiagCopy).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("BSR diagnostics", tv.text.toString()))
            Toast.makeText(this, "Copy ho gaya — chat mein paste karein", Toast.LENGTH_LONG).show()
        }
        findViewById<Button>(R.id.btnDiagClear).setOnClickListener {
            AutofillLog.clear(this)
            refresh()
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        tv.text = AutofillLog.buildDiagnostics(this)
    }
}
