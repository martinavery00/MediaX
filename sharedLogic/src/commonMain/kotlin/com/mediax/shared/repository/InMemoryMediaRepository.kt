package com.mediax.shared.repository

import com.mediax.shared.model.DashboardSnapshot
import com.mediax.shared.model.DashboardSummary
import com.mediax.shared.model.MediaItem
import com.mediax.shared.model.MediaKind
import com.mediax.shared.model.MediaStatus
import kotlinx.coroutines.delay

class InMemoryMediaRepository(
    private val simulatedDelayMillis: Long = 180,
) : MediaRepository {
    override suspend fun loadDashboard(): DashboardSnapshot {
        if (simulatedDelayMillis > 0) {
            delay(simulatedDelayMillis)
        }

        return DashboardSnapshot(
            summary =
                DashboardSummary(
                    totalItems = 128,
                    publishedItems = 94,
                    totalViews = 2_480_000,
                ),
            recentMedia = sampleMedia,
        )
    }

    private companion object {
        val sampleMedia =
            listOf(
                MediaItem(
                    id = "launch-film",
                    title = "Autumn product film",
                    kind = MediaKind.VIDEO,
                    status = MediaStatus.PUBLISHED,
                    viewCount = 184_200,
                    publishedAtEpochSeconds = 1_791_936_000,
                ),
                MediaItem(
                    id = "founder-interview",
                    title = "Founder interview",
                    kind = MediaKind.AUDIO,
                    status = MediaStatus.PROCESSING,
                    viewCount = 0,
                    publishedAtEpochSeconds = 1_791_849_600,
                ),
                MediaItem(
                    id = "campaign-stills",
                    title = "Campaign stills",
                    kind = MediaKind.IMAGE,
                    status = MediaStatus.DRAFT,
                    viewCount = 0,
                    publishedAtEpochSeconds = 1_791_763_200,
                ),
            )
    }
}
