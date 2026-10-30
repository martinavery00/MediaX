package com.mediax.shared.repository

import com.mediax.shared.model.MediaStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryMediaRepositoryTest {
    private val repository = InMemoryMediaRepository(simulatedDelayMillis = 0)

    @Test
    fun dashboardContainsDeterministicSummary() =
        runTest {
            val snapshot = repository.loadDashboard()

            assertEquals(128, snapshot.summary.totalItems)
            assertEquals(94, snapshot.summary.publishedItems)
            assertEquals(2_480_000, snapshot.summary.totalViews)
        }

    @Test
    fun recentMediaUsesUniqueStableIdentifiers() =
        runTest {
            val media = repository.loadDashboard().recentMedia

            assertEquals(3, media.size)
            assertEquals(media.size, media.map { it.id }.distinct().size)
            assertEquals("launch-film", media.first().id)
            assertEquals(MediaStatus.PUBLISHED, media.first().status)
            assertTrue(
                media.zipWithNext().all { (first, second) ->
                    first.publishedAtEpochSeconds >= second.publishedAtEpochSeconds
                },
            )
        }
}
