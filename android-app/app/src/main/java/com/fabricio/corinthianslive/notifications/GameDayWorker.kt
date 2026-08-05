package com.fabricio.corinthianslive.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fabricio.corinthianslive.data.CorinthiansRepository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

class GameDayWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!NotificationPreferences.isEnabled(applicationContext)) return Result.success()

        return runCatching {
            val zone = ZoneId.of("America/Sao_Paulo")
            val today = LocalDate.now(zone)
            val finished = setOf("FT", "AET", "PEN", "WO")
            val game = CorinthiansRepository(applicationContext).fixtures().data.firstOrNull { match ->
                match.statusShort !in finished && runCatching {
                    OffsetDateTime.parse(match.kickoff).atZoneSameInstant(zone).toLocalDate() == today
                }.getOrDefault(false)
            }

            if (game != null && NotificationPreferences.lastNotifiedMatch(applicationContext) != game.id) {
                if (GameNotificationManager.showGameDay(applicationContext, game)) {
                    NotificationPreferences.markMatchNotified(applicationContext, game.id)
                }
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
