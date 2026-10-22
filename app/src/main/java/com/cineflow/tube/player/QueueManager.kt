package com.cineflow.tube.player

import com.cineflow.tube.data.model.StreamItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CineFlow Queue Manager (Phase 3)
 * --------------------------------
 * Manages the playback queue independently of ExoPlayer so that
 * UI state is not lost when the player transitions between streams.
 *
 * Features:
 * - Add/remove/reorder queue items
 * - Auto-advance integration (called by PlaybackController on STATE_ENDED)
 * - "Play Next" (insert at current+1) vs "Add to Queue" (append)
 * - Persistent queue index with safe bounds
 */
object QueueManager {

    private val _queue = MutableStateFlow<List<StreamItem>>(emptyList())
    val queue: StateFlow<List<StreamItem>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    /** Whether to continue playing after the current video ends */
    private val _autoPlay = MutableStateFlow(true)
    val autoPlay: StateFlow<Boolean> = _autoPlay.asStateFlow()

    // ── Mutation helpers ─────────────────────────────────────────────

    /**
     * Replace the full queue with [items] and reset index.
     * Used when "playing all" from a playlist or channel.
     */
    fun setQueue(items: List<StreamItem>, startIndex: Int = 0) {
        _queue.value = items
        _queueIndex.value = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    }

    /**
     * Append [item] to the end of the queue.
     */
    fun addToQueue(item: StreamItem) {
        _queue.value = _queue.value + item
    }

    /**
     * Insert [item] immediately after the current index ("Play Next").
     */
    fun playNext(item: StreamItem) {
        val current = _queue.value.toMutableList()
        val insertAt = (_queueIndex.value + 1).coerceAtMost(current.size)
        current.add(insertAt, item)
        _queue.value = current
    }

    /**
     * Remove item at [index]. Adjusts the current index if needed.
     */
    fun removeFromQueue(index: Int) {
        val current = _queue.value.toMutableList()
        if (index !in current.indices) return
        current.removeAt(index)
        _queue.value = current

        // Shift index back if we removed something before current position
        if (index < _queueIndex.value) {
            _queueIndex.value = (_queueIndex.value - 1).coerceAtLeast(0)
        }
    }

    /**
     * Move item at [from] to [to] position (for drag-reorder in the queue sheet).
     */
    fun moveQueueItem(from: Int, to: Int) {
        val current = _queue.value.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val item = current.removeAt(from)
        current.add(to, item)
        _queue.value = current

        // Keep index pointing at the same logical item after reorder
        val newIndex = when (_queueIndex.value) {
            from -> to
            in (minOf(from, to)..maxOf(from, to)) -> {
                if (from < to) _queueIndex.value - 1
                else _queueIndex.value + 1
            }
            else -> _queueIndex.value
        }
        _queueIndex.value = newIndex.coerceIn(0, current.lastIndex.coerceAtLeast(0))
    }

    /**
     * Advance to the next item in the queue.
     * Called by PlaybackController when ExoPlayer reaches STATE_ENDED.
     * Returns the next StreamItem if available, null if queue is exhausted.
     */
    fun advanceToNext(): StreamItem? {
        if (!_autoPlay.value) return null
        val nextIndex = _queueIndex.value + 1
        if (nextIndex >= _queue.value.size) return null
        _queueIndex.value = nextIndex
        return _queue.value.getOrNull(nextIndex)
    }

    /**
     * Jump to a specific queue index.
     * Returns the StreamItem at that index, or null.
     */
    fun jumpTo(index: Int): StreamItem? {
        if (index !in _queue.value.indices) return null
        _queueIndex.value = index
        return _queue.value[index]
    }

    fun setAutoPlay(enabled: Boolean) {
        _autoPlay.value = enabled
    }

    fun clear() {
        _queue.value = emptyList()
        _queueIndex.value = 0
    }

    /** Peek the next item without advancing the index */
    fun peekNext(): StreamItem? = _queue.value.getOrNull(_queueIndex.value + 1)

    /** Peek the current item */
    fun current(): StreamItem? = _queue.value.getOrNull(_queueIndex.value)
}
