 
package com.example.salatdnd

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var nm: NotificationManager
    private lateinit var cityInput: EditText
    private lateinit var durationInput: EditText
    private lateinit var statusText: TextView
    private lateinit var scheduleText: TextView

    private val prefs by lazy {
        getSharedPreferences("salat_dnd", MODE_PRIVATE)
    }

    private val prayers = listOf(
        "Subuh" to "Fajr",
        "Zuhur" to "Dhuhr",
        "Asar" to "Asr",
        "Magrib" to "Maghrib",
        "Isya" to "Isha"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nm = getSystemService(NotificationManager::class.java)

        cityInput = EditText(this).apply {
            hint = "Kota (contoh: Jakarta)"
            setSingleLine(true)
            setText(prefs.getString("city", "Jakarta"))
        }

        durationInput = EditText(this).apply {
            hint = "Durasi DND dalam menit"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setText(prefs.getInt("duration", 20).toString())
        }

        statusText = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
        }

        scheduleText = TextView(this).apply {
            textSize = 16f
            setPadding(8, 16, 8, 16)
        }

        val permissionButton = Button(this).apply {
            text = "Beri Izin DND"
            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                )
            }
        }

        val exactAlarmButton = Button(this).apply {
            text = "Izinkan Alarm Tepat Waktu"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    )
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            }
        }

        val scheduleButton = Button(this).apply {
            text = "Simpan & Aktifkan Jadwal Otomatis"
            setOnClickListener { saveAndSchedule() }
        }

        val refreshButton = Button(this).apply {
            text = "Perbarui Jadwal Salat"
            setOnClickListener { loadSchedule() }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 24, 24, 24)
        }

        root.addView(TextView(this).apply {
            text = "Salat DND"
            textSize = 28f
            gravity = Gravity.CENTER
        })
        root.addView(statusText)
        root.addView(cityInput)
        root.addView(durationInput)
        root.addView(permissionButton)
        root.addView(exactAlarmButton)
        root.addView(scheduleButton)
        root.addView(refreshButton)
        root.addView(scheduleText)

        setContentView(ScrollView(this).apply {
            addView(root, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        })

        updateStatus()
        loadSchedule()
    }

    override fun onResume() {
        super.onResume()
        if (::nm.isInitialized) updateStatus()
    }

    private fun updateStatus() {
        if (!::statusText.isInitialized) return

        val permission = nm.isNotificationPolicyAccessGranted
        val dnd = if (permission) {
            nm.currentInterruptionFilter !=
                NotificationManager.INTERRUPTION_FILTER_ALL &&
                nm.currentInterruptionFilter !=
                NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        } else false

        statusText.text = buildString {
            append("Izin DND: ")
            append(if (permission) "Diberikan" else "Belum diberikan")
            append("\nStatus DND sekarang: ")
            append(if (dnd) "AKTIF" else "NONAKTIF")
        }
    }

    private fun saveAndSchedule() {
        val city = cityInput.text.toString().trim()
        val duration = durationInput.text.toString().toIntOrNull()

        if (city.isBlank()) {
            cityInput.error = "Masukkan nama kota"
            return
        }

        if (duration == null || duration !in 1..240) {
            durationInput.error = "Masukkan durasi 1–240 menit"
            return
        }

        prefs.edit()
            .putString("city", city)
            .putInt("duration", duration)
            .putBoolean("enabled", true)
            .apply()

        if (!nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(
                this,
                "Berikan izin DND terlebih dahulu",
                Toast.LENGTH_LONG
            ).show()
            startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            )
            return
        }

        loadSchedule()
    }

    private fun loadSchedule() {
        val city = cityInput.text.toString().trim().ifBlank {
            prefs.getString("city", "Jakarta") ?: "Jakarta"
        }

        scheduleText.text = "Mengambil jadwal salat..."

        Thread {
            try {
                val result = mutableListOf<String>()
                val now = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

                for (offset in 0..1) {
                    val calendar = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, offset)
                    }
                    val date = dateFormat.format(calendar.time)

                    val encodedCity = Uri.encode(city)
                    val url = URL(
                        "https://api.aladhan.com/v1/timingsByCity/" +
                            "$date?city=$encodedCity&country=Indonesia&method=20"
                    )

                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.requestMethod = "GET"

                    val body = connection.inputStream.bufferedReader().use {
                        it.readText()
                    }
                    connection.disconnect()

                    val json = JSONObject(body)
                    if (json.optInt("code") != 200) {
                        throw Exception("API tidak mengembalikan jadwal")
                    }

                    val timings = json.getJSONObject("data")
                        .getJSONObject("timings")

                    result.add(displayFormat.format(calendar.time))

                    for ((name, key) in prayers) {
                        val time = timings.getString(key)
                            .substringBefore(" ")
                        result.add("$name: $time")

                        val parts = time.split(":")
                        val prayerCalendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                            set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH,
                                calendar.get(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                            set(Calendar.MINUTE, parts[1].toInt())
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val trigger = prayerCalendar.timeInMillis
                        if (trigger > now) {
                            scheduleAlarm(
                                trigger,
                                name,
                                key.hashCode()
                            )
                        }
                    }
                    result.add("")
                }

                runOnUiThread {
                    scheduleText.text = result.joinToString("\n")
                    Toast.makeText(
                        this,
                        "Jadwal berhasil diperbarui",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    scheduleText.text =
                        "Gagal mengambil jadwal: ${e.message}\n" +
                        "Periksa internet dan nama kota."
                }
            }
        }.start()
    }

    private fun scheduleAlarm(
        timeMillis: Long,
        prayerName: String,
        requestCode: Int
    ) {
        val intent = Intent(this, PrayerAlarmReceiver::class.java).apply {
            action = "com.example.salatdnd.PRAYER"
            putExtra("prayer", prayerName)
        }

        val pending = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(AlarmManager::class.java)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pending
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pending
                )
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeMillis,
                pending
            )
        }
    }
}
