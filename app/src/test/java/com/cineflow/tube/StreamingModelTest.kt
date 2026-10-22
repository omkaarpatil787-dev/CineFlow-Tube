package com.cineflow.tube

import com.cineflow.tube.data.model.Chapter
import com.cineflow.tube.data.model.SponsorSegment
import com.cineflow.tube.data.model.StreamDetails
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.ui.theme.CineFlowMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingModelTest {

    @Test
    fun testStreamItemCreation() {
        val item = StreamItem(
            id = "dQw4w9WgXcQ",
            title = "Never Gonna Give You Up",
            uploaderName = "Rick Astley",
            thumbnailUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            durationSeconds = 213,
            durationString = "3:33",
            views = 1500000000L,
            viewsFormatted = "1.5B views"
        )

        assertEquals("dQw4w9WgXcQ", item.id)
        assertEquals("Never Gonna Give You Up", item.title)
        assertEquals("Rick Astley", item.uploaderName)
        assertEquals("3:33", item.durationString)
        assertEquals("1.5B views", item.viewsFormatted)
    }

    @Test
    fun testSponsorBlockSegment() {
        val segment = SponsorSegment(
            category = "sponsor",
            startSeconds = 45.5f,
            endSeconds = 75.0f,
            actionType = "skip"
        )

        assertEquals("sponsor", segment.category)
        assertTrue(segment.endSeconds > segment.startSeconds)
        assertEquals("skip", segment.actionType)
    }

    @Test
    fun testStreamDetailsWithChapters() {
        val chapters = listOf(
            Chapter(title = "Intro", startSeconds = 0),
            Chapter(title = "The Reveal", startSeconds = 120),
            Chapter(title = "Conclusion", startSeconds = 300)
        )

        val details = StreamDetails(
            id = "test1234",
            title = "CineFlow Architecture",
            description = "Deep dive into 120fps fluid Compose motion",
            uploaderName = "CineFlow Labs",
            uploaderChannelId = "UC12345",
            durationSeconds = 400,
            chapters = chapters
        )

        assertEquals(3, details.chapters.size)
        assertEquals("The Reveal", details.chapters[1].title)
        assertEquals(120L, details.chapters[1].startSeconds)
    }

    @Test
    fun testCineFlowMotionLabPatterns() {
        // Pattern 01: Icon Morph (300ms)
        assertEquals(300, CineFlowMotion.DurationIconMorph)

        // Pattern 02: Spring Response (Damping 0.72, Stiffness 380)
        assertEquals(0.72f, CineFlowMotion.PressDampingRatio)
        assertEquals(380f, CineFlowMotion.PressStiffness)
        assertEquals(0.94f, CineFlowMotion.PressTargetScale)

        // Pattern 03: Spatial Continuity (420ms)
        assertEquals(420, CineFlowMotion.DurationNavGlide)

        // Pattern 04: Container Transform (460ms)
        assertEquals(460, CineFlowMotion.DurationContainerTransform)

        // Pattern 05: Seek Feedback (360ms ripple, 200ms button)
        assertEquals(360, CineFlowMotion.DurationSeekRipple)
        assertEquals(200, CineFlowMotion.DurationSeekButton)

        // Pattern 06: Theme Bloom (580ms)
        assertEquals(580, CineFlowMotion.DurationThemeBloom)
    }
}
