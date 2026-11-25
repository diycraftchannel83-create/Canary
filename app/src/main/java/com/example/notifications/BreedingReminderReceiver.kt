package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BreedingReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            BreedingNotificationHelper.ensureChannels(context)
            return
        }
        val title = intent.getStringExtra(BreedingNotificationHelper.EXTRA_TITLE)
            ?: "Pengingat Kenari Tasikmalaya"
        val body = intent.getStringExtra(BreedingNotificationHelper.EXTRA_BODY)
            ?: "Periksa jadwal pengeraman dan suplemen harian ternak Anda."
        val channel = intent.getStringExtra(BreedingNotificationHelper.EXTRA_CHANNEL)
            ?: BreedingNotificationHelper.CHANNEL_SUPPLEMENT
        val notifId = intent.getIntExtra(BreedingNotificationHelper.EXTRA_NOTIF_ID, 1001)

        BreedingNotificationHelper.showImmediateNotification(
            context = context,
            channelId = channel,
            notificationId = notifId,
            title = title,
            body = body
        )
    }
}
