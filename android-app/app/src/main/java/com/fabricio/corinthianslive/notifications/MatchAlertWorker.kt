package com.fabricio.corinthianslive.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fabricio.corinthianslive.data.CorinthiansRepository

class MatchAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!NotificationPreferences.isEnabled(applicationContext)) return Result.success()
        val matchId = inputData.getLong(GameNotificationManager.INPUT_MATCH_ID, Long.MIN_VALUE)
        val alert = inputData.getString(GameNotificationManager.INPUT_ALERT_TYPE).orEmpty()
        if (matchId == Long.MIN_VALUE || alert.isBlank()) return Result.failure()

        return runCatching {
            val repository = CorinthiansRepository(applicationContext)
            val match = repository.fixtures().data.firstOrNull { it.id == matchId }
                ?: return@runCatching Result.retry()
            when (alert) {
                GameNotificationManager.ALERT_PRE_GAME -> {
                    GameNotificationManager.showPreGame(applicationContext, match)
                    val live = repository.live().data
                    if (live.match?.id == matchId && (live.homeSquad != null || live.awaySquad != null)) {
                        GameNotificationManager.showLineup(
                            applicationContext,
                            matchId,
                            live.lineupStatus,
                            live.homeSquad,
                            live.awaySquad
                        )
                    }
                }
                GameNotificationManager.ALERT_KICKOFF -> {
                    GameNotificationManager.showKickoff(applicationContext, match)
                    GameNotificationManager.startLiveTracking(applicationContext, matchId)
                }
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }
}

