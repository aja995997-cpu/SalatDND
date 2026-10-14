package com.example.salatdnd

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var notificationManager: NotificationManager
    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var permissionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationManager = getSystemService(NotificationManager::class.java)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "Salat DND"
            textSize = 28f
            gravity = Gravity.CENTER
        }
        statusText = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }
        permissionButton = Button(this).apply {
            text = "Beri Izin DND"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        }
        toggleButton = Button(this).apply {
            text = "Aktifkan DND"
            setOnClickListener { toggleDnd() }
        }
        val refreshButton = Button(this).apply {
            text = "Periksa Status"
            setOnClickListener { refreshStatus() }
        }
        root.addView(title)
        root.addView(statusText)
        root.addView(permissionButton)
        root.addView(toggleButton)
        root.addView(refreshButton)
        setContentView(root)
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::notificationManager.isInitialized) refreshStatus()
    }

    private fun toggleDnd() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Berikan izin DND terlebih dahulu", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }

        try {
            val current = notificationManager.currentInterruptionFilter
            val currentlyOn = current != NotificationManager.INTERRUPTION_FILTER_ALL &&
                current != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
            val target = if (currentlyOn) {
                NotificationManager.INTERRUPTION_FILTER_ALL
            } else {
                NotificationManager.INTERRUPTION_FILTER_NONE
            }
            notificationManager.setInterruptionFilter(target)
            refreshStatus()
            Toast.makeText(
                this,
                if (target == NotificationManager.INTERRUPTION_FILTER_NONE) "Permintaan DND ON dikirim" else "Permintaan DND OFF dikirim",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Gagal: izin DND belum tersedia", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal mengubah DND: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshStatus() {
        val granted = notificationManager.isNotificationPolicyAccessGranted
        permissionButton.isEnabled = !granted
        permissionButton.text = if (granted) "Izin DND sudah diberikan" else "Beri Izin DND"
        if (!granted) {
            statusText.text = "Izin: belum diberikan\nStatus DND belum dapat dikontrol"
            toggleButton.text = "Aktifkan DND"
            toggleButton.isEnabled = true
            return
        }
        val filter = notificationManager.currentInterruptionFilter
        val enabled = filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        statusText.text = "Izin: diberikan\nDND: ${if (enabled) "AKTIF" else "NONAKTIF"}"
        toggleButton.text = if (enabled) "Matikan DND" else "Aktifkan DND"
        toggleButton.isEnabled = true
    }
}
