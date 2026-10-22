package com.cineflow.tube

import com.cineflow.tube.player.PlaybackState
import com.cineflow.tube.player.TimelineStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateTest {

    @Test
    fun testDefaultPlaybackState() {
        val state = PlaybackState()

        assertEquals("1080p", state.selectedQuality)
        assertEquals(1.0f, state.playbackSpeed, 0.001f)
        assertEquals(TimelineStyle.WAVY, state.timelineStyle)
        assertTrue(state.autoHideControls)
        assertFalse(state.repeatVideo)
        assertTrue(state.ambientGlow)
        assertFalse(state.subtitlesEnabled)
        assertEquals(0.8f, state.volume, 0.001f)
        assertFalse(state.isFullscreen)
    }

    @Test
    fun testTimelineStyleDisplayNames() {
        assertEquals("Wavy line · reference", TimelineStyle.WAVY.displayName)
        assertEquals("Wavy + circular", TimelineStyle.CIRCULAR_FLOWER.displayName)
        assertEquals("Minimal straight", TimelineStyle.MINIMAL_STRAIGHT.displayName)
    }

    @Test
    fun testStateCopyMutation() {
        val initial = PlaybackState()
        val modified = initial.copy(
            selectedQuality = "2160p (4K)",
            playbackSpeed = 1.5f,
            timelineStyle = TimelineStyle.CIRCULAR_FLOWER,
            repeatVideo = true,
            isFullscreen = true
        )

        assertEquals("2160p (4K)", modified.selectedQuality)
        assertEquals(1.5f, modified.playbackSpeed, 0.001f)
        assertEquals(TimelineStyle.CIRCULAR_FLOWER, modified.timelineStyle)
        assertTrue(modified.repeatVideo)
        assertTrue(modified.isFullscreen)
    }
}
