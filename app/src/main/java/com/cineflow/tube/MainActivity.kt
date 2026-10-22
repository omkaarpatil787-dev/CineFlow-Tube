package com.cineflow.tube

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.data.repository.CineFlowDownloadManager
import com.cineflow.tube.data.repository.StreamingRepository
import com.cineflow.tube.player.CineFlowPlaybackService
import com.cineflow.tube.player.PlaybackController
import com.cineflow.tube.player.QueueManager
import com.cineflow.tube.ui.components.FrostedBottomDock
import com.cineflow.tube.ui.components.FrostedNavRail
import com.cineflow.tube.ui.components.NavDestination
import com.cineflow.tube.ui.player.CineFlowMiniplayerContainer
import com.cineflow.tube.ui.player.MiniplayerMode
import com.cineflow.tube.ui.screens.AppThemeMode
import com.cineflow.tube.ui.screens.SearchScreen
import com.cineflow.tube.ui.screens.ShortsScreen
import com.cineflow.tube.ui.screens.SubscriptionsScreen
import com.cineflow.tube.ui.screens.TrendingFeedScreen
import com.cineflow.tube.ui.screens.YourSpaceScreen
import com.cineflow.tube.ui.splash.GlacierSplashScreen
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.CineFlowTheme
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository = StreamingRepository()
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val serviceIntent = Intent(this, CineFlowPlaybackService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        val sessionToken = SessionToken(this, ComponentName(this, CineFlowPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
            } catch (_: Exception) {
                // Service connected
            }
        }, MoreExecutors.directExecutor())

        setContent {
            var themeMode by rememberSaveable { mutableStateOf(AppThemeMode.DARK) }
            var enableDynamicColor by rememberSaveable { mutableStateOf(false) }
            var reducedMotion by rememberSaveable { mutableStateOf(false) }
            var showSplash by rememberSaveable { mutableStateOf(true) }

            val downloadManager = (application as? CineFlowApplication)?.downloadManager

            CineFlowTheme(
                themeMode = themeMode,
                enableDynamicColor = enableDynamicColor,
                reducedMotion = reducedMotion
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CineFlow.tokens.bg
                ) {
                    if (showSplash) {
                        GlacierSplashScreen(
                            themeMode = themeMode,
                            onFinished = { showSplash = false }
                        )
                    } else {
                        CineFlowAppRoot(
                            repository = repository,
                            downloadManager = downloadManager,
                            themeMode = themeMode,
                            enableDynamicColor = enableDynamicColor,
                            reducedMotion = reducedMotion,
                            onThemeModeChanged = { themeMode = it },
                            onToggleDynamicColor = { enableDynamicColor = it },
                            onToggleReducedMotion = { reducedMotion = it },
                            onCycleTheme = {
                                themeMode = when (themeMode) {
                                    AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
                                    AppThemeMode.LIGHT -> AppThemeMode.DARK
                                    AppThemeMode.DARK -> AppThemeMode.AMOLED
                                    AppThemeMode.AMOLED -> AppThemeMode.SYSTEM
                                }
                            },
                            onReplaySplash = { showSplash = true }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        mediaController?.release()
        super.onDestroy()
    }
}

@Composable
fun CineFlowAppRoot(
    repository: StreamingRepository,
    downloadManager: CineFlowDownloadManager? = null,
    themeMode: AppThemeMode,
    enableDynamicColor: Boolean,
    reducedMotion: Boolean,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onCycleTheme: () -> Unit,
    onReplaySplash: () -> Unit = {}
) {
    var currentDestination by rememberSaveable { mutableStateOf(NavDestination.HOME) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var miniplayerMode by rememberSaveable { mutableStateOf(MiniplayerMode.HIDDEN) }
    val coroutineScope = rememberCoroutineScope()

    // Preserved navigation and list states across destination switches
    val homeListState = rememberLazyListState()
    val shortsGridState = rememberLazyGridState()
    val subscriptionsListState = rememberLazyListState()
    val yourSpaceListState = rememberLazyListState()

    BackHandler(enabled = miniplayerMode == MiniplayerMode.EXPANDED || isSearchActive || currentDestination != NavDestination.HOME) {
        if (miniplayerMode == MiniplayerMode.EXPANDED) {
            miniplayerMode = MiniplayerMode.COLLAPSED
        } else if (isSearchActive) {
            isSearchActive = false
        } else if (currentDestination != NavDestination.HOME) {
            currentDestination = NavDestination.HOME
        }
    }

    fun openAndPlayVideo(video: StreamItem) {
        coroutineScope.launch {
            try {
                miniplayerMode = MiniplayerMode.EXPANDED
                val details = repository.getStreamDetails(video.id)
                PlaybackController.play(details)
            } catch (_: Exception) {
                // Fallback handled in repository
            }
        }
    }

    // Wire queue auto-advance: when ExoPlayer ends, advance to next queued stream
    PlaybackController._onQueueAdvance = { nextItem ->
        coroutineScope.launch {
            try {
                val details = repository.getStreamDetails(nextItem.id)
                PlaybackController.play(details)
            } catch (_: Exception) { }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(CineFlow.tokens.bg)) {
        val isTabletWide = maxWidth >= 760.dp

        if (isTabletWide) {
            // TABLET / WIDE LAYOUT: Navigation Rail on Left + Content on Right
            Row(modifier = Modifier.fillMaxSize()) {
                if (!isSearchActive) {
                    FrostedNavRail(
                        currentDestination = currentDestination,
                        onDestinationSelected = { currentDestination = it }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    DestinationContent(
                        currentDestination = currentDestination,
                        isSearchActive = isSearchActive,
                        repository = repository,
                        downloadManager = downloadManager,
                        themeMode = themeMode,
                        enableDynamicColor = enableDynamicColor,
                        reducedMotion = reducedMotion,
                        homeListState = homeListState,
                        shortsGridState = shortsGridState,
                        subscriptionsListState = subscriptionsListState,
                        yourSpaceListState = yourSpaceListState,
                        onOpenVideo = { openAndPlayVideo(it) },
                        onSearchClick = { isSearchActive = true },
                        onBackFromSearch = { isSearchActive = false },
                        onCycleTheme = onCycleTheme,
                        onThemeModeChanged = onThemeModeChanged,
                        onToggleDynamicColor = onToggleDynamicColor,
                        onToggleReducedMotion = onToggleReducedMotion,
                        onReplaySplash = onReplaySplash,
                        onNavigate = { currentDestination = it }
                    )
                }
            }
        } else {
            // PHONE COMPACT LAYOUT: Full Content + Floating Bottom Dock
            Box(modifier = Modifier.fillMaxSize()) {
                DestinationContent(
                    currentDestination = currentDestination,
                    isSearchActive = isSearchActive,
                    repository = repository,
                    downloadManager = downloadManager,
                    themeMode = themeMode,
                    enableDynamicColor = enableDynamicColor,
                    reducedMotion = reducedMotion,
                    homeListState = homeListState,
                    shortsGridState = shortsGridState,
                    subscriptionsListState = subscriptionsListState,
                    yourSpaceListState = yourSpaceListState,
                    onOpenVideo = { openAndPlayVideo(it) },
                    onSearchClick = { isSearchActive = true },
                    onBackFromSearch = { isSearchActive = false },
                    onCycleTheme = onCycleTheme,
                    onThemeModeChanged = onThemeModeChanged,
                    onToggleDynamicColor = onToggleDynamicColor,
                    onToggleReducedMotion = onToggleReducedMotion,
                    onReplaySplash = onReplaySplash,
                    onNavigate = { currentDestination = it }
                )

                // Floating Frosted Dock (Home, Shorts, Subscriptions, You)
                AnimatedVisibility(
                    visible = miniplayerMode != MiniplayerMode.EXPANDED && !isSearchActive,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    FrostedBottomDock(
                        currentDestination = currentDestination,
                        onDestinationSelected = { currentDestination = it }
                    )
                }
            }
        }

        // Floating Docked Miniplayer Capsule / Expanded Watch Page
        CineFlowMiniplayerContainer(
            mode = miniplayerMode,
            onExpand = { miniplayerMode = MiniplayerMode.EXPANDED },
            onCollapse = { miniplayerMode = MiniplayerMode.COLLAPSED },
            onDismiss = { miniplayerMode = MiniplayerMode.HIDDEN },
            onRelatedVideoClick = { related -> openAndPlayVideo(related) },
            onCycleTheme = onCycleTheme
        )
    }
}

@Composable
private fun DestinationContent(
    currentDestination: NavDestination,
    isSearchActive: Boolean,
    repository: StreamingRepository,
    downloadManager: CineFlowDownloadManager? = null,
    themeMode: AppThemeMode,
    enableDynamicColor: Boolean,
    reducedMotion: Boolean,
    homeListState: androidx.compose.foundation.lazy.LazyListState,
    shortsGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    subscriptionsListState: androidx.compose.foundation.lazy.LazyListState,
    yourSpaceListState: androidx.compose.foundation.lazy.LazyListState,
    onOpenVideo: (StreamItem) -> Unit,
    onSearchClick: () -> Unit,
    onBackFromSearch: () -> Unit,
    onCycleTheme: () -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onReplaySplash: () -> Unit,
    onNavigate: (NavDestination) -> Unit
) {
    if (isSearchActive) {
        SearchScreen(
            repository = repository,
            onBackClick = onBackFromSearch,
            onVideoClick = onOpenVideo
        )
    } else {
        when (currentDestination) {
            NavDestination.HOME -> {
                TrendingFeedScreen(
                    repository = repository,
                    listState = homeListState,
                    onVideoClick = onOpenVideo,
                    onSearchClick = onSearchClick,
                    onCycleTheme = onCycleTheme,
                    onProfileClick = { onNavigate(NavDestination.YOU) }
                )
            }
            NavDestination.SHORTS -> {
                ShortsScreen(
                    gridState = shortsGridState,
                    onShortClick = onOpenVideo
                )
            }
            NavDestination.SUBSCRIPTIONS -> {
                SubscriptionsScreen(
                    listState = subscriptionsListState,
                    onVideoClick = onOpenVideo,
                    onExploreClick = { onNavigate(NavDestination.HOME) }
                )
            }
            NavDestination.YOU -> {
                YourSpaceScreen(
                    repository = repository,
                    listState = yourSpaceListState,
                    downloadManager = downloadManager,
                    currentThemeMode = themeMode,
                    enableDynamicColor = enableDynamicColor,
                    reducedMotion = reducedMotion,
                    onThemeSelected = onThemeModeChanged,
                    onToggleDynamicColor = onToggleDynamicColor,
                    onToggleReducedMotion = onToggleReducedMotion,
                    onVideoClick = onOpenVideo,
                    onReplaySplash = onReplaySplash
                )
            }
        }
    }
}
