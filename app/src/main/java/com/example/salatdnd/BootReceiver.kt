
package com.example.salatdnd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(
                "salat_dnd",
                Context.MODE_PRIVATE
            )

            // Tandai agar aplikasi dapat memuat ulang jadwal
            // ketika dibuka kembali setelah perangkat menyala.
            prefs.edit()
                .putBoolean("needs_reschedule", true)
                .apply()
        }
    }
}
