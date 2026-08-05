package com.fabricio.corinthianslive.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fabricio.corinthianslive.MainActivity
import com.fabricio.corinthianslive.R
import com.fabricio.corinthianslive.data.model.Match
import java.util.concurrent.TimeUnit

object GameNotificationManager {
    const val CHANNEL_ID = "corinthians_game_day"
    private const val PERIODIC_WORK = "corinthians_game_day_periodic"
    private const val IMMEDIATE_WORK = "corinthians_game_day_now"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.game_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.game_notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun scheduleChecks(context: Context) {
        val periodic = PeriodicWorkRequestBuilder<GameDayWorker>(6, TimeUnit.HOURS)
            .build()
        val immediate = OneTimeWorkRequestBuilder<GameDayWorker>()
            .build()
        WorkManager.getInstance(context.applicationContext).apply {
            enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, periodic)
            enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, immediate)
        }
    }

    fun cancelChecks(context: Context) {
        WorkManager.getInstance(context.applicationContext).apply {
            cancelUniqueWork(PERIODIC_WORK)
            cancelUniqueWork(IMMEDIATE_WORK)
        }
    }

    fun canNotify(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun showGameDay(context: Context, match: Match): Boolean {
        if (!NotificationPreferences.isEnabled(context) || !canNotify(context)) return false
        val body = "${match.home} x ${match.away} • ${match.time} • ${match.competition}"
        notify(context, match.id.hashCode(), "Hoje tem Corinthians!", body)
        return true
    }

    @SuppressLint("MissingPermission")
    fun showTest(context: Context): Boolean {
        if (!canNotify(context)) return false
        notify(context, 1910, "Notificações ativadas", "Você receberá um aviso nos dias de jogo do Corinthians.")
        return true
    }

    private fun notify(context: Context, id: Int, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.corinthians_crest))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setColor(ContextCompat.getColor(context, R.color.corinthians_red))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
