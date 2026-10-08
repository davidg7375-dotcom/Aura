package com.aura.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import java.time.ZoneId

object Reminders {
    const val CHANNEL_ID = "aura_reminders"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL_ID, "תזכורות", NotificationManager.IMPORTANCE_DEFAULT)
        ch.description = "תזכורות לפגישות ולסימון תשלומים"
        nm.createNotificationChannel(ch)
    }

    private fun pendingIntent(ctx: Context, sessionId: String, type: Int, text: String): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java)
        i.data = Uri.parse("aura://reminder/$sessionId/$type")
        i.putExtra("text", text)
        return PendingIntent.getBroadcast(
            ctx, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancel(ctx: Context, sessionId: String) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        for (t in 0..2) {
            am.cancel(pendingIntent(ctx, sessionId, t, ""))
        }
    }

    private fun setAlarm(ctx: Context, sessionId: String, type: Int, at: Long, text: String) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(ctx, sessionId, type, text))
    }

    fun schedule(ctx: Context, s: Session) {
        cancel(ctx, s.id)
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val start = s.dateTime.atZone(zone).toInstant().toEpochMilli()
        val whenText = "${fmtDayMonth(s.date)} בשעה ${fmtTime(s.time)}"

        s.remindBeforeMin?.let { m ->
            val at = start - m * 60_000L
            if (at > now) setAlarm(ctx, s.id, 0, at, "פגישה ב־$whenText")
        }
        if (s.remindPay && !s.paid) {
            val at = s.date.plusDays(1).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
            if (at > now) {
                setAlarm(ctx, s.id, 1, at, "לא סומן תשלום על הפגישה מ־${fmtDayMonth(s.date)}")
            }
        }
        s.customRemindAt?.let { at ->
            if (at > now) setAlarm(ctx, s.id, 2, at, "תזכורת לפגישה ב־$whenText")
        }
    }

    fun rescheduleAll(ctx: Context, snapshot: Repo.Snapshot) {
        snapshot.sessions.forEach { s ->
            schedule(ctx, s)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra("text") ?: return
        Reminders.ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, Reminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_leaf)
            .setContentTitle("BREATHE")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.notify(System.currentTimeMillis().toInt(), n)
        } catch (e: SecurityException) {
            // אין הרשאת התראות – מתעלמים
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Reminders.rescheduleAll(context, Repo.load(context))
        }
    }
}
