package com.fabricio.corinthianslive.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fabricio.corinthianslive.data.CorinthiansRepository
import com.fabricio.corinthianslive.data.model.APP_ZONE_ID
import kotlinx.coroutines.delay
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

        val repository = CorinthiansRepository(applicationContext)
        val trackingStartedAt = System.currentTimeMillis()
        setForeground(GameNotificationManager.liveForegroundInfo(applicationContext, matchId, null))

        while (
            NotificationPreferences.isEnabled(applicationContext) &&
            System.currentTimeMillis() - trackingStartedAt < MAX_TRACKING_DURATION_MS
        ) {
            runCatching { repository.liveRealtime(matchId).data }
                .onSuccess { live ->
                    val match = live.match
                    if (match != null && match.id == matchId) {
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
                        setForeground(
                            GameNotificationManager.liveForegroundInfo(applicationContext, matchId, match)
                        )

                        if (match.statusShort in FINISHED_STATUSES || matchWindowEnded(match.kickoff)) {
                            return Result.success()
                        }
                    }
                }
            delay(LIVE_POLL_INTERVAL_MS)
        }
        return Result.success()
    }

    private fun matchWindowEnded(kickoff: String): Boolean {
        val start = runCatching {
            OffsetDateTime.parse(kickoff).atZoneSameInstant(APP_ZONE_ID)
        }.getOrNull() ?: return false
        return ZonedDateTime.now(APP_ZONE_ID).isAfter(start.plusHours(5))
    }

    private companion object {
        const val LIVE_POLL_INTERVAL_MS = 10_000L
        const val MAX_TRACKING_DURATION_MS = 6 * 60 * 60 * 1_000L
        val FINISHED_STATUSES = setOf("FT", "AET", "PEN", "WO", "CANC", "PST", "AWAITING_RESULT")
    }
}