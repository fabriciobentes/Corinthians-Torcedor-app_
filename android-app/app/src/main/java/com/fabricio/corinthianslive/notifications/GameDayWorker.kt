package com.fabricio.corinthianslive.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fabricio.corinthianslive.data.CorinthiansRepository
import com.fabricio.corinthianslive.data.model.APP_ZONE_ID
import com.fabricio.corinthianslive.data.model.kickoffAtManaus
import com.fabricio.corinthianslive.data.model.resolvedStatus
import java.time.Duration
import java.time.ZonedDateTime

class GameDayWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!NotificationPreferences.isEnabled(applicationContext)) return Result.success()

        return runCatching {
            val repository = CorinthiansRepository(applicationContext)
            val fixtures = repository.fixtures().data
            val now = ZonedDateTime.now(APP_ZONE_ID)
            val candidates = fixtures.filter { match ->
                val kickoff = match.kickoffAtManaus()
                kickoff != null &&
                    kickoff.isBefore(now.plusDays(8)) &&
                    kickoff.isAfter(now.minusHours(5)) &&
                    match.resolvedStatus(now) !in setOf("FT", "AET", "PEN", "WO", "CANC", "PST")
            }

            candidates.forEach { match ->
                GameNotificationManager.scheduleMatchAlerts(applicationContext, match)
                val kickoff = match.kickoffAtManaus() ?: return@forEach
                val minutesToStart = Duration.between(now, kickoff).toMinutes()
                if (minutesToStart in 20..35) {
                    GameNotificationManager.showPreGame(applicationContext, match)
                }
                if (minutesToStart in -10..2) {
                    GameNotificationManager.showKickoff(applicationContext, match)
                }
                if (match.resolvedStatus(now) == "LIVE" || minutesToStart in -240..30) {
                    GameNotificationManager.startLiveTracking(applicationContext, match.id)
                }
            }

            val nearGame = candidates.firstOrNull { match ->
                val kickoff = match.kickoffAtManaus() ?: return@firstOrNull false
                Duration.between(now, kickoff).toMinutes() in -240..90
            }
            if (nearGame != null) {
                val live = repository.liveRealtime(nearGame.id).data
                if (live.match?.id == nearGame.id && (live.homeSquad != null || live.awaySquad != null)) {
                    GameNotificationManager.showLineup(
                        applicationContext,
                        nearGame.id,
                        live.lineupStatus,
                        live.homeSquad,
                        live.awaySquad
                    )
                }
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
