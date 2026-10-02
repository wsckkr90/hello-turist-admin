package com.hellokurukshetra.admin.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat

object EmergencyAlertManager {
    private const val CHANNEL_ID = "emergency_alerts"
    private var ringtone: android.media.Ringtone? = null
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audio = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val channel = NotificationChannel(CHANNEL_ID, "Emergency Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Critical rider emergency alerts"; enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 900); setSound(sound, audio)
        }
        manager.createNotificationChannel(channel)
    }
    fun alert(context: Context, emergencyId: String, riderName: String, category: String) {
        ensureChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("EMERGENCY ALERT")
            .setContentText("$riderName • $category")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Emergency triggered by $riderName. Open Emergency to respond immediately."))
            .setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false).setOngoing(true).build()
        manager.notify(emergencyId.hashCode(), notification)
        try {
            ringtone?.stop()
            ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            ringtone?.play()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ ringtone?.stop(); ringtone=null }, 15_000L)
        } catch (_: Exception) {}
    }
}
