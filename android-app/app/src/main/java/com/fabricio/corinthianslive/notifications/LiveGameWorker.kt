package com.fabricio.corinthianslive.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fabricio.corinthianslive.data.CorinthiansRepository
import com.fabricio.corinthianslive.data.model.APP_ZONE_ID
import java.time.OffsetDateTime
import java.time.ZonedDateTime

class LiveGameWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!NotificationPreferences.isEnabled(applicationContext)) return Result.success()
        val matchId = inputData.getLong(GameNotificationManager.INPUT_MATCH_ID, Long.MIN_VALUE)
        if (matchId == Long.MIN_VALUE) return Result.failure()

        return runCatching {
            val live = CorinthiansRepository(applicationContext).live().data
            val match = live.match
            if (match == null || match.id != matchId) return@runCatching Result.retry()

            if (live.homeSquad != null || live.awaySquad != null) {
                GameNotificationManager.showLineup(
                    applicationContext,
                    matchId,
                    live.lineupStatus,
                    live.homeSquad,
                    live.awaySquad
                )
            }
            live.events.forEach { event ->
                GameNotificationManager.showEvent(applicationContext, matchId, event)
            }

            val finished = match.statusShort in setOf("FT", "AET", "PEN", "WO", "CANC")
            val kickoff = runCatching {
                OffsetDateTime.parse(match.kickoff).atZoneSameInstant(APP_ZONE_ID)
            }.getOrNull()
            val now = ZonedDateTime.now(APP_ZONE_ID)
            val insideGameWindow = kickoff != null &&
                now.isAfter(kickoff.minusHours(1)) &&
                now.isBefore(kickoff.plusHours(5))
            if (!finished && insideGameWindow) {
                GameNotificationManager.scheduleNextLivePoll(applicationContext, matchId)
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }
}

