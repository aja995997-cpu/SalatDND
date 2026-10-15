
package com.example.salatdnd

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(
            NotificationManager::class.java
        )
        val prefs = context.getSharedPreferences(
            "salat_dnd",
            Context.MODE_PRIVATE
        )

        when (intent.action) {
            ACTION_OFF -> {
                if (nm.isNotificationPolicyAccessGranted) {
                    val previous = prefs.getInt(
                        "previous_filter",
                        NotificationManager.INTERRUPTION_FILTER_ALL
                    )

                    try {
                        nm.setInterruptionFilter(previous)
                    } catch (_: SecurityException) {
                        // Izin bisa dicabut setelah alarm dijadwalkan.
                    }
                }
            }

            ACTION_PRAYER -> {
                if (!prefs.getBoolean("enabled", false)) return
                if (!nm.isNotificationPolicyAccessGranted) return

                try {
                    val current = nm.currentInterruptionFilter

                    // Simpan filter sebelumnya agar bisa dipulihkan.
                    prefs.edit()
                        .putInt("previous_filter", current)
                        .apply()

                    nm.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_NONE
                    )

                    val duration = prefs.getInt("duration", 20)
                        .coerceIn(1, 240)

                    val offIntent = Intent(
                        context,
                        PrayerAlarmReceiver::class.java
                    ).apply {
                        action = ACTION_OFF
                    }

                    val offPending = PendingIntent.getBroadcast(
                        context,
                        OFF_REQUEST_CODE,
                        offIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                    )

                    val alarmManager =
                        context.getSystemService(AlarmManager::class.java)

                    val offTime = System.currentTimeMillis() +
                        duration * 60_000L

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            !alarmManager.canScheduleExactAlarms()
                        ) {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                offTime,
                                offPending
                            )
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                offTime,
                                offPending
                            )
                        }
                    } catch (_: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            offTime,
                            offPending
                        )
                    }
                } catch (_: SecurityException) {
                    // Tidak dapat mengubah DND jika izin tidak tersedia.
                }
            }
        }
    }

    companion object {
        const val ACTION_PRAYER =
            "com.example.salatdnd.PRAYER"

        const val ACTION_OFF =
            "com.example.salatdnd.DND_OFF"

        const val OFF_REQUEST_CODE = 9001
    }
}
