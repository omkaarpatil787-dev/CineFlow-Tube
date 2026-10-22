package com.cineflow.tube.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.CineFlowMotion
import com.cineflow.tube.ui.theme.springPress
import kotlin.math.roundToInt

enum class NavDestination(
    val title: String,
    val symbol: String,
    val icon: ImageVector
) {
    HOME("Home", "⌂", Icons.Rounded.Home),
    SHORTS("Shorts", "▣", Icons.Rounded.PlayCircle),
    SUBSCRIPTIONS("Subscriptions", "▤", Icons.Rounded.Subscriptions),
    YOU("You", "◉", Icons.Rounded.AccountCircle)
}

/**
 * Universal Mobile Floating Frosted Dock (Phone Portrait < 760dp)
 * Integrates Motion Pattern 03 (Spatial Continuity):
 * Active indicator capsule glides with CineFlowMotion.NavGlideTween (420ms cubic bezier with subtle spring settle),
 * while destinations feature Pattern 02 (Spring Response).
 */
@Composable
fun FrostedBottomDock(
    currentDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetIndex = NavDestination.entries.indexOf(currentDestination).toFloat()
    val animatedIndex by animateFloatAsState(
        targetValue = targetIndex,
        animationSpec = CineFlowMotion.NavGlideTween,
        label = "navIndicatorGlide"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        FrostedSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = CineFlow.tokens.glass
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 6.dp)
            ) {
                val itemWidth = maxWidth / NavDestination.entries.size

                // Gliding Active Indicator Capsule (Pattern 03)
                Box(
                    modifier = Modifier
                        .offset { IntOffset((animatedIndex * itemWidth.toPx()).roundToInt(), 0) }
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CineFlow.tokens.soft)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavDestination.entries.forEach { destination ->
                        val isSelected = destination == currentDestination

                        val tintColor by animateColorAsState(
                            targetValue = if (isSelected) CineFlow.tokens.accent else CineFlow.tokens.muted,
                            animationSpec = tween(220),
                            label = "navColorTint"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .springPress { onDestinationSelected(destination) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = destination.symbol,
                                fontSize = 20.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                color = tintColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = destination.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tintColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tablet & Wide Navigation Rail (matching jadzumi_stream_v02.html `.side`):
 * - Sticky on the left of the screen (width 220dp on wide, 80dp on medium)
 * - Brand logo at top: `▶ cineflow tube`
 * - Destination items with active highlight capsule
 * - Bottom footer note
 */
@Composable
fun FrostedNavRail(
    currentDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedSurface(
        modifier = modifier
            .width(220.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(0.dp),
        backgroundColor = CineFlow.tokens.glass
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 18.dp)
        ) {
            // Brand Mark: ▶ cineflow tube
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF9FD4FF), Color(0xFF4B8FF5))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row {
                    Text(
                        text = "cineflow",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineFlow.tokens.ink
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "tube",
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = CineFlow.tokens.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Items
            NavDestination.entries.forEach { destination ->
                val isSelected = destination == currentDestination
                val backgroundColor = if (isSelected) CineFlow.tokens.soft else Color.Transparent
                val contentColor = if (isSelected) CineFlow.tokens.accent else CineFlow.tokens.ink

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(backgroundColor)
                        .springPress { onDestinationSelected(destination) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = destination.symbol,
                        fontSize = 20.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = destination.title,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = contentColor
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Side Foot (from prototype)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "✦ Your videos. Beautifully in motion.",
                    fontSize = 11.sp,
                    color = CineFlow.tokens.muted,
                    lineHeight = 16.sp
                )
                Text(
                    text = "CineFlow Tube · by Jadzumi",
                    fontSize = 10.sp,
                    color = CineFlow.tokens.muted.copy(alpha = 0.7f)
                )
            }
        }
    }
}
