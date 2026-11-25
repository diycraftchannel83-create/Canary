package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.utils.IncubationCalculator
import java.util.Calendar

object BreedingNotificationHelper {
    const val CHANNEL_INCUBATION = "kenari_incubation_channel"
    const val CHANNEL_SUPPLEMENT = "kenari_supplement_channel"
    const val CHANNEL_HEALTH = "kenari_health_alert_channel"

    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
    const val EXTRA_CHANNEL = "extra_channel"
    const val EXTRA_NOTIF_ID = "extra_notif_id"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_INCUBATION,
                    "Jadwal Pengeraman 21 Hari",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi otomatis jadwal teropong telur (H+7), cek embrio (H+14), dan estimasi menetas (H+21)."
                },
                NotificationChannel(
                    CHANNEL_SUPPLEMENT,
                    "Pengingat Suplemen Harian",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Pengingat pemberian vitamin, kalsium, dan suplemen harian kenari."
                },
                NotificationChannel(
                    CHANNEL_HEALTH,
                    "Peringatan Dini Kesehatan",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi darurat jika skor atau kondisi kesehatan indukan menurun drastis."
                }
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showImmediateNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        ensureChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priority = if (channelId == CHANNEL_HEALTH || channelId == CHANNEL_INCUBATION) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }

    fun scheduleDailySupplementReminder(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        ensureChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BreedingReminderReceiver::class.java).apply {
            putExtra(EXTRA_CHANNEL, CHANNEL_SUPPLEMENT)
            putExtra(EXTRA_NOTIF_ID, 2001)
            putExtra(EXTRA_TITLE, "Waktunya Suplemen Harian Kenari")
            putExtra(EXTRA_BODY, "Cek daftar vitamin, kalsium, dan suplemen harian indukan di Kenari Tasikmalaya.")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        if (!enabled) return

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun scheduleIncubationAlerts(
        context: Context,
        clutchId: String,
        cageCode: String,
        incubationStartEpochMs: Long,
        enabled: Boolean
    ) {
        if (!enabled || incubationStartEpochMs <= 0L) return
        ensureChannels(context)
        val timeline = IncubationCalculator.calculate(incubationStartEpochMs)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        val milestones = listOf(
            Triple(7, timeline.candlingDay7EpochMs + 8 * 3_600_000L, "Hari ke-7 Teropong Telur ($cageCode)"),
            Triple(14, timeline.embryoCheckDay14EpochMs + 8 * 3_600_000L, "Hari ke-14 Cek Embrio ($cageCode)"),
            Triple(20, timeline.hatchPrepDay20EpochMs + 8 * 3_600_000L, "H-1 Persiapan Menetas ($cageCode)"),
            Triple(21, timeline.expectedHatchEpochMs + 7 * 3_600_000L, "Hari ke-21 Estimasi Menetas ($cageCode)!")
        )

        milestones.forEach { (day, triggerAt, title) ->
            if (triggerAt > now) {
                val requestCode = ("$clutchId-$day").hashCode()
                val body = when (day) {
                    7 -> "Kandang $cageCode memasuki hari ke-7 pengeraman. Lakukan peneropongan fertilitas telur."
                    14 -> "Kandang $cageCode hari ke-14. Pastikan kelembapan sarang terjaga."
                    20 -> "Besok adalah estimasi hari ke-21 penetasan di kandang $cageCode. Siapkan pakan lembut/eggfood."
                    else -> "Hari ini (Hari ke-21) adalah estimasi telur menetas di kandang $cageCode!"
                }
                val intent = Intent(context, BreedingReminderReceiver::class.java).apply {
                    putExtra(EXTRA_CHANNEL, CHANNEL_INCUBATION)
                    putExtra(EXTRA_NOTIF_ID, requestCode)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_BODY, body)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        }
    }

    fun triggerHealthEarlyWarning(
        context: Context,
        canaryName: String,
        ringCode: String,
        previousScore: Int,
        newScore: Int,
        symptoms: String
    ) {
        val body = buildString {
            append("$canaryName ($ringCode) mengalami penurunan kesehatan dari $previousScore ke $newScore.")
            if (symptoms.isNotBlank()) {
                append(" Gejala: $symptoms.")
            }
            append(" Segera lakukan pemeriksaan dan isolasi.")
        }
        showImmediateNotification(
            context = context,
            channelId = CHANNEL_HEALTH,
            notificationId = ringCode.hashCode(),
            title = "Peringatan Dini Kesehatan Kenari!",
            body = body
        )
    }
}
