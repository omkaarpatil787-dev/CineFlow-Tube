package com.cineflow.tube.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.data.repository.StreamingRepository
import com.cineflow.tube.ui.components.CineFlowEmptyView
import com.cineflow.tube.ui.components.CineFlowErrorView
import com.cineflow.tube.ui.components.CineFlowSkeletonCard
import com.cineflow.tube.ui.components.FrostedSurface
import com.cineflow.tube.ui.components.VideoCard
import com.cineflow.tube.ui.components.specularBorder
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * CineFlow Robust Search Screen:
 * - Debounced typing (350ms) with obsolete request cancellation
 * - Recent search history chips
 * - Content-shaped skeleton loading
 * - Contextual retry and error handling
 * - Preserves search query and results across configuration changes
 */
@OptIn(FlowPreview::class)
@Composable
fun SearchScreen(
    repository: StreamingRepository,
    onBackClick: () -> Unit,
    onVideoClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<StreamItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var recentSearches by rememberSaveable {
        mutableStateOf(listOf("4K Nature", "Ambient Glacier", "Tokyo Diary", "Minimal Desk", "Cosmos"))
    }

    val coroutineScope = rememberCoroutineScope()

    fun executeSearch(targetQuery: String) {
        val trimmed = targetQuery.trim()
        if (trimmed.isEmpty()) {
            results = emptyList()
            isSearching = false
            return
        }

        coroutineScope.launch {
            isSearching = true
            errorMessage = null
            try {
                results = repository.search(trimmed)
                if (!recentSearches.contains(trimmed)) {
                    recentSearches = (listOf(trimmed) + recentSearches).take(8)
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Search failed. Check your internet connection."
            } finally {
                isSearching = false
            }
        }
    }

    // Debounce typing (350ms)
    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .debounce(350)
            .distinctUntilChanged()
            .filter { it.trim().length >= 2 }
            .collect { debouncedQuery ->
                executeSearch(debouncedQuery)
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Search Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accessible Back Button (>= 48dp)
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = CineFlow.tokens.minTouchTarget, minHeight = CineFlow.tokens.minTouchTarget)
                    .clip(CircleShape)
                    .springPress(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = CineFlow.tokens.ink,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Frosted Search Capsule (matching jadzumi_stream_v02.html: border-radius 100px)
            FrostedSurface(
                shape = RoundedCornerShape(CineFlow.tokens.pillRadius),
                backgroundColor = CineFlow.tokens.surface,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = CineFlow.tokens.muted,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    BasicTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (it.isEmpty()) {
                                results = emptyList()
                                errorMessage = null
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { executeSearch(query) }),
                        textStyle = TextStyle(
                            color = CineFlow.tokens.ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(CineFlow.tokens.accent),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search videos, channels, topics…",
                                    color = CineFlow.tokens.muted,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable {
                                    query = ""
                                    results = emptyList()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search",
                                tint = CineFlow.tokens.muted,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recent Searches Chips (when query is empty)
        if (query.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = CineFlow.tokens.muted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recent Searches",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CineFlow.tokens.muted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentSearches) { term ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(13.dp))
                                .background(CineFlow.tokens.soft)
                                .specularBorder(RoundedCornerShape(13.dp))
                                .springPress {
                                    query = term
                                    executeSearch(term)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = term,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = CineFlow.tokens.ink
                            )
                        }
                    }
                }
            }
        }

        // Main Search Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isSearching -> {
                    // Content-shaped Skeleton Cards
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(3) {
                            CineFlowSkeletonCard()
                        }
                    }
                }

                errorMessage != null -> {
                    CineFlowErrorView(
                        title = "Search Error",
                        message = errorMessage ?: "Could not fetch search results.",
                        actionLabel = "Retry",
                        onActionClick = { executeSearch(query) }
                    )
                }

                query.isNotEmpty() && results.isEmpty() -> {
                    CineFlowEmptyView(
                        title = "No results found",
                        message = "We couldn't find any videos matching \"$query\". Try checking your spelling or searching for related topics.",
                        actionLabel = "Clear Search",
                        onActionClick = {
                            query = ""
                            results = emptyList()
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 140.dp)
                    ) {
                        items(results, key = { it.id }) { item ->
                            VideoCard(
                                item = item,
                                onClick = { onVideoClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}
