package com.studionails.beauty.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.studionails.beauty.R
import com.studionails.beauty.data.BookStore
import com.studionails.beauty.model.BookState
import com.studionails.beauty.model.noticesFor

private const val ChannelId = "citas_v3"
private const val Prefs = "studio_reminders"

object Reminders {
    fun selectedSound(context: Context): Uri? {
        val saved = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString("sound_uri", null)
        return saved?.let(Uri::parse) ?: Settings.System.DEFAULT_NOTIFICATION_URI
    }

    fun soundName(context: Context): String {
        val uri = selectedSound(context)
        return if (uri == null) "Silencio" else RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Predeterminado"
    }

    fun saveSound(context: Context, uri: Uri?) {
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit()
            .putString("sound_uri", uri?.toString() ?: Settings.System.DEFAULT_NOTIFICATION_URI.toString())
            .apply()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(ChannelId)
        ensureChannel(context)
    }

    fun sync(context: Context, state: BookState) {
        ensureChannel(context)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val prefs = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet("ids", emptySet()) ?: emptySet()
        previous.forEach { id -> alarm.cancel(pending(context, id, "", "")) }
        val now = System.currentTimeMillis()
        val horizon = now + 21L * 24 * 60 * 60 * 1000
        val upcoming = noticesFor(state).filter { it.at in (now + 1)..horizon }
        val ids = mutableSetOf<String>()
        upcoming.forEach { notice ->
            val intent = pending(context, notice.id, notice.title, notice.body)
            try {
                val exactAllowed = Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()
                if (exactAllowed) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notice.at, intent)
                else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notice.at, intent)
            } catch (_: SecurityException) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notice.at, intent)
            }
            ids += notice.id
        }
        prefs.edit().putStringSet("ids", ids).apply()
    }

    fun flashNew(context: Context, before: BookState, after: BookState) {
        val old = noticesFor(before).map { it.id }.toSet()
        val now = System.currentTimeMillis()
        noticesFor(after)
            .filter { it.id !in old && it.at <= now + 2_000 }
            .forEach { show(context, it.id.hashCode(), it.title, it.body) }
    }

    fun show(context: Context, id: Int, title: String, body: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_nail)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            /* permiso de notificaciones todavía no concedido */
        }
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(ChannelId) != null) return
        val channel = NotificationChannel(ChannelId, "Citas", NotificationManager.IMPORTANCE_HIGH)
        channel.description = "Avisos cuando se agenda o cancela una cita y recordatorios."
        channel.enableVibration(true)
        channel.vibrationPattern = longArrayOf(0, 250, 120, 250)
        channel.setSound(selectedSound(context), AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        manager.createNotificationChannel(channel)
    }

    private fun pending(context: Context, id: String, title: String, body: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra("title", title)
            .putExtra("body", body)
            .putExtra("nid", id.hashCode())
        return PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: return
        val body = intent.getStringExtra("body") ?: ""
        Reminders.show(context, intent.getIntExtra("nid", title.hashCode()), title, body)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Reminders.sync(context, BookStore(context).load())
    }
}
