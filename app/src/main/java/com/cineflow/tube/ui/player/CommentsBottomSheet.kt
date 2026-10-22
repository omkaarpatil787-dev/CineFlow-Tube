package com.cineflow.tube.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.cineflow.tube.data.model.CommentItem
import com.cineflow.tube.ui.components.CineFlowEmptyView
import com.cineflow.tube.ui.components.specularBorder
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress

/**
 * Native CineFlow Comments & Community Sheet:
 * - Frosted glass sliding panel matching cineflow_immersive_player.html (#0B2031F5)
 * - Pinned comments with pin badge
 * - Creator heart badge (❤️ by Creator)
 * - Like counts and reply counts
 * - Smooth dismissal and touch gestures
 */
@Composable
fun CommentsBottomSheet(
    comments: List<CommentItem>,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        // Scrim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x73000000))
        )

        // Sheet Container
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xF50B2031))
                .specularBorder(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .pointerInput(Unit) {
                    detectTapGestures { }
                }
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header: Title, Count, Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Comments",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6F5FF)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${comments.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFA4EAFF)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x38102D3C))
                            .springPress(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFF0FBFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (comments.isEmpty()) {
                    CineFlowEmptyView(
                        title = "No Comments",
                        message = "Be the first to share your thoughts on this video."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            CommentRow(comment = comment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: CommentItem
) {
    var isLiked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Author Avatar
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF17384A))
                .specularBorder(CircleShape)
        ) {
            if (!comment.authorAvatarUrl.isNullOrEmpty()) {
                SubcomposeAsyncImage(
                    model = comment.authorAvatarUrl,
                    contentDescription = comment.author,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = comment.author.firstOrNull()?.uppercase() ?: "▶",
                        color = Color(0xFFA4EAFF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Author Name, Pinned Badge, and Timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = comment.author,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6F5FF)
                )

                if (comment.isPinned) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33A4EAFF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = "Pinned",
                            tint = Color(0xFFA4EAFF),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Pinned",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA4EAFF)
                        )
                    }
                }

                Text(
                    text = comment.publishedTime,
                    fontSize = 11.sp,
                    color = Color(0xFF91B4C9)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Comment Body Content
            Text(
                text = comment.content,
                fontSize = 13.sp,
                color = Color(0xFFDCEEF9),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row: Like, Heart, Replies
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Like Button & Count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.springPress { isLiked = !isLiked }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ThumbUp,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFA4EAFF) else Color(0xFF91B4C9),
                        modifier = Modifier.size(14.dp)
                    )
                    val likes = comment.likeCount + if (isLiked) 1 else 0
                    if (likes > 0) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$likes",
                            fontSize = 11.sp,
                            color = if (isLiked) Color(0xFFA4EAFF) else Color(0xFF91B4C9)
                        )
                    }
                }

                // Creator Heart Badge
                if (comment.isHearted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Hearted by creator",
                            tint = Color(0xFFFF5277),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Creator",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5277)
                        )
                    }
                }

                // Reply Count
                if (comment.replyCount > 0) {
                    Text(
                        text = "${comment.replyCount} ${if (comment.replyCount == 1L) "reply" else "replies"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA4EAFF),
                        modifier = Modifier.springPress(onClick = {})
                    )
                }
            }
        }
    }
}
