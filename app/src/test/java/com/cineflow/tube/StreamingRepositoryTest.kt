package com.cineflow.tube

import com.cineflow.tube.data.repository.BackendType
import com.cineflow.tube.data.repository.StreamingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingRepositoryTest {

    @Test
    fun testFallbackTrendingItemsNotEmpty() {
        val repo = StreamingRepository()
        val items = repo.getFallbackTrending()

        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.title.contains("Glacier Sessions") })
        assertEquals("Northbound Films", items.first().uploaderName)
    }

    @Test
    fun testFallbackStreamDetailsIntegrity() {
        val repo = StreamingRepository()
        val details = repo.getFallbackStreamDetails("glacier_test")

        assertEquals("glacier_test", details.id)
        assertTrue(details.videoStreams.isNotEmpty())
        assertTrue(details.videoStreams.any { it.quality.contains("1080p") })
        assertTrue(details.chapters.isNotEmpty())
        assertEquals(754L, details.durationSeconds)
    }

    @Test
    fun testBackendTypeSwitching() {
        val repo = StreamingRepository()
        assertEquals(BackendType.PIPED, repo.activeBackend)

        repo.activeBackend = BackendType.INVIDIOUS
        assertEquals(BackendType.INVIDIOUS, repo.activeBackend)
    }
}
