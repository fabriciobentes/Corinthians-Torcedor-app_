package com.fabricio.corinthianslive.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fabricio.corinthianslive.MainActivity
import com.fabricio.corinthianslive.R
import com.fabricio.corinthianslive.data.model.EventType
import com.fabricio.corinthianslive.data.model.Match
import com.fabricio.corinthianslive.data.model.MatchEvent
import com.fabricio.corinthianslive.data.model.LiveMatch
import com.fabricio.corinthianslive.data.model.TeamSquad
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

object GameNotificationManager {
    const val CHANNEL_ID = "corinthians_game_events_v2"
    const val LIVE_CHANNEL_ID = "corinthians_live_tracking_v1"
    const val ALERT_PRE_GAME = "pre_game"
    const val ALERT_KICKOFF = "kickoff"
    const val ALERT_LINEUP = "lineup"
    const val INPUT_MATCH_ID = "match_id"
    const val INPUT_ALERT_TYPE = "alert_type"
    private const val PERIODIC_WORK = "corinthians_game_checks"
    private const val IMMEDIATE_WORK = "corinthians_game_check_now"
    private const val LIVE_TRACKING_NOTIFICATION_ID = 19101

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val eventsChannel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.game_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.game_notification_channel_description)
            enableVibration(true)
        }
        val trackingChannel = NotificationChannel(
            LIVE_CHANNEL_ID,
            context.getString(R.string.live_tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.live_tracking_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(eventsChannel, trackingChannel))
    }

    fun scheduleChecks(context: Context) {
        val periodic = PeriodicWorkRequestBuilder<GameDayWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .build()
        val immediate = OneTimeWorkRequestBuilder<GameDayWorker>()
            .setConstraints(networkConstraint)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context.applicationContext).apply {
            enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, periodic)
            enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, immediate)
        }
    }

    fun scheduleMatchAlerts(context: Context, match: Match) {
        val kickoff = runCatching { OffsetDateTime.parse(match.kickoff).toInstant() }.getOrNull() ?: return
        if (match.kickoff.contains("T00:00:00")) return
        scheduleAlert(context, match.id, ALERT_PRE_GAME, kickoff.minusSeconds(30 * 60))
        scheduleAlert(context, match.id, ALERT_KICKOFF, kickoff)
    }

    private fun scheduleAlert(context: Context, matchId: Long, type: String, target: Instant) {
        val delay = Duration.between(Instant.now(), target).toMillis()
        if (delay < -10 * 60 * 1000) return
        val request = OneTimeWorkRequestBuilder<MatchAlertWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(INPUT_MATCH_ID, matchId)
                    .putString(INPUT_ALERT_TYPE, type)
                    .build()
            )
            .setInitialDelay(delay.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "corinthians_${type}_${matchId}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun startLiveTracking(context: Context, matchId: Long) {
        val request = OneTimeWorkRequestBuilder<LiveGameWorker>()
            .setInputData(Data.Builder().putLong(INPUT_MATCH_ID, matchId).build())
            .addTag("corinthians_live")
            .setConstraints(networkConstraint)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "corinthians_live_${matchId}",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancelChecks(context: Context) {
        WorkManager.getInstance(context.applicationContext).apply {
            cancelUniqueWork(PERIODIC_WORK)
            cancelUniqueWork(IMMEDIATE_WORK)
            cancelAllWorkByTag("corinthians_live")
        }
    }

    fun canNotify(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun liveForegroundInfo(context: Context, matchId: Long, match: LiveMatch?): ForegroundInfo {
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            LIVE_TRACKING_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = if (match == null) {
            "Preparando o acompanhamento lance a lance."
        } else {
            val clock = if (match.minute > 0) "${match.minute}' • " else ""
            "$clock${match.home} ${match.scoreHome} x ${match.scoreAway} ${match.away}"
        }
        val notification = NotificationCompat.Builder(context, LIVE_CHANNEL_ID)
            .setSmallIcon(R.drawable.corinthians_logo)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.corinthians_crest))
            .setContentTitle("Corinthians em tempo real")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, R.color.corinthians_black))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                LIVE_TRACKING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(LIVE_TRACKING_NOTIFICATION_ID, notification)
        }
    }

    fun showPreGame(context: Context, match: Match): Boolean {
        val body = "${match.home} x ${match.away} começa às ${match.time} (horário de Manaus). " +
            match.broadcasters.joinToString(" • ").ifBlank { "Transmissão a confirmar." }
        return show(context, match.id, ALERT_PRE_GAME, "Faltam 30 minutos!", body)
    }

    fun showKickoff(context: Context, match: Match): Boolean =
        show(
            context,
            match.id,
            ALERT_KICKOFF,
            "Começou! ${match.home} x ${match.away}",
            "${match.competition} • Acompanhe todos os lances no app."
        )

    fun showLineup(context: Context, matchId: Long, status: String, home: TeamSquad?, away: TeamSquad?): Boolean {
        val corinthians = listOfNotNull(home, away).firstOrNull {
            it.teamName.contains("Corinthians", ignoreCase = true)
        }
        val players = corinthians?.starters?.joinToString(", ") { it.name }.orEmpty()
        val body = if (players.isBlank()) {
            "Veja os relacionados e a formação tática no app."
        } else {
            players
        }
        val title = status.ifBlank { "Escalação oficial" }
        val alertKey = ALERT_LINEUP + "_" + title
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return show(context, matchId, alertKey, title, body)
    }

    fun showEvent(context: Context, matchId: Long, event: MatchEvent): Boolean {
        if (NotificationPreferences.wasEventSent(context, matchId, event.id)) return false
        val title = when (event.type) {
            EventType.Goal -> "Gol!"
            EventType.YellowCard -> "Cartão amarelo"
            EventType.RedCard -> "Cartão vermelho"
            EventType.Substitution -> "Substituição"
            EventType.Shot -> "Finalização"
            EventType.Foul -> "Falta"
            EventType.Corner -> "Escanteio"
            EventType.Offside -> "Impedimento"
            EventType.Save -> "Defesa"
            EventType.Penalty -> "Pênalti"
            EventType.Var -> "VAR"
            EventType.Kickoff -> "Bola rolando"
            EventType.Other -> "Novo lance"
        }
        val clock = listOf(event.clock, event.period).filter { it.isNotBlank() }.joinToString(" ")
        val body = listOf(clock, event.team, event.description).filter { it.isNotBlank() }.joinToString(" • ")
        val shown = notify(context, (matchId.toString() + event.id).hashCode(), title, body)
        if (shown) NotificationPreferences.markEventSent(context, matchId, event.id)
        return shown
    }

    private fun show(
        context: Context,
        matchId: Long,
        key: String,
        title: String,
        body: String
    ): Boolean {
        if (NotificationPreferences.wasAlertSent(context, matchId, key)) return false
        val shown = notify(context, (matchId.toString() + key).hashCode(), title, body)
        if (shown) NotificationPreferences.markAlertSent(context, matchId, key)
        return shown
    }

    fun showTest(context: Context): Boolean =
        notify(
            context,
            1910,
            "Notificações ativadas",
            "Você receberá os avisos pré-jogo e todos os eventos publicados durante a partida."
        )

    @SuppressLint("MissingPermission")
    private fun notify(context: Context, id: Int, title: String, body: String): Boolean {
        if (!NotificationPreferences.isEnabled(context) || !canNotify(context)) return false
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
            .setSmallIcon(R.drawable.corinthians_logo)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.corinthians_crest))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setColor(ContextCompat.getColor(context, R.color.corinthians_black))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
        return true
    }
}
