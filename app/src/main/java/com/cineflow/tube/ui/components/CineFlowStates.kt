package com.cineflow.tube.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress

/**
 * Content-shaped Skeleton Placeholder (prevent layout shifts, zero harsh shimmer)
 */
@Composable
fun CineFlowSkeletonCard(
    modifier: Modifier = Modifier
) {
    val isReducedMotion = CineFlow.reducedMotion
    val alphaAnim = if (!isReducedMotion) {
        val transition = rememberInfiniteTransition(label = "skeletonPulse")
        val alpha by transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "skeletonAlpha"
        )
        alpha
    } else 0.7f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .alpha(alphaAnim)
    ) {
        // 16:9 Thumbnail Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(CineFlow.tokens.cardRadius))
                .background(CineFlow.tokens.soft)
                .specularBorder(RoundedCornerShape(CineFlow.tokens.cardRadius))
        )

        Spacer(modifier = Modifier.height(11.dp))

        // Metadata Row Placeholder
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar Placeholder (37dp circle)
            Box(
                modifier = Modifier
                    .size(37.dp)
                    .clip(CircleShape)
                    .background(CineFlow.tokens.soft)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title line 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CineFlow.tokens.soft)
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Title line 2
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CineFlow.tokens.soft)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Subtitle metadata
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CineFlow.tokens.soft.copy(alpha = 0.7f))
                )
            }
        }
    }
}

/**
 * Elegant Loading Indicator with Brand Accent
 */
@Composable
fun CineFlowLoadingView(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = CineFlow.tokens.accent,
            strokeWidth = 3.dp,
            modifier = Modifier.size(38.dp)
        )
        if (message != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = CineFlow.tokens.muted
            )
        }
    }
}

/**
 * Contextual Error View with Retry Action
 */
@Composable
fun CineFlowErrorView(
    title: String = "Unable to connect",
    message: String = "Please check your network connection or try switching backends.",
    icon: ImageVector = Icons.Rounded.CloudOff,
    actionLabel: String = "Retry",
    onActionClick: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FrostedSurface(
            shape = RoundedCornerShape(22.dp),
            backgroundColor = CineFlow.tokens.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(CineFlow.tokens.soft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CineFlow.tokens.accent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineFlow.tokens.ink,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = CineFlow.tokens.muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(CineFlow.tokens.buttonRadius))
                                .background(CineFlow.tokens.soft)
                                .springPress(onClick = onSecondaryActionClick)
                                .padding(horizontal = 16.dp, vertical = 11.dp)
                        ) {
                            Text(
                                text = secondaryActionLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CineFlow.tokens.ink
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CineFlow.tokens.buttonRadius))
                            .background(CineFlow.tokens.ink)
                            .springPress(onClick = onActionClick)
                            .padding(horizontal = 20.dp, vertical = 11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = CineFlow.tokens.bg,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = actionLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineFlow.tokens.bg
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable Empty State View
 */
@Composable
fun CineFlowEmptyView(
    title: String,
    message: String,
    icon: ImageVector = Icons.Rounded.Inbox,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        FrostedSurface(
            shape = RoundedCornerShape(22.dp),
            backgroundColor = CineFlow.tokens.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CineFlow.tokens.soft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CineFlow.tokens.accent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineFlow.tokens.ink,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = CineFlow.tokens.muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                if (actionLabel != null && onActionClick != null) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CineFlow.tokens.buttonRadius))
                            .background(CineFlow.tokens.ink)
                            .springPress(onClick = onActionClick)
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineFlow.tokens.bg
                        )
                    }
                }
            }
        }
    }
}
