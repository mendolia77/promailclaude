package com.smartmail.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmail.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SwipeAction {
    NONE,
    ARCHIVE,
    DELETE,
    STAR,
    MARK_READ
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToActionEmail(
    sender: String,
    subject: String,
    preview: String,
    time: String,
    isRead: Boolean = false,
    isStarred: Boolean = false,
    hasAttachment: Boolean = false,
    category: String = "📧",
    onArchive: () -> Unit = {},
    onDelete: () -> Unit = {},
    onStar: () -> Unit = {},
    onMarkRead: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    var offsetX by remember { mutableStateOf(0f) }
    var currentAction by remember { mutableStateOf(SwipeAction.NONE) }
    val scope = rememberCoroutineScope()

    // Soglie per attivare le azioni
    val archiveThreshold = 150f
    val deleteThreshold = -150f
    val starThreshold = 300f

    // Animazione di ritorno
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offset"
    )

    // Scala icone in base allo swipe
    val actionScale = remember(offsetX) {
        when {
            offsetX > archiveThreshold -> (offsetX / archiveThreshold).coerceIn(1f, 1.5f)
            offsetX < deleteThreshold -> (abs(offsetX) / abs(deleteThreshold)).coerceIn(1f, 1.5f)
            else -> 1f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Background con azioni (visibile durante lo swipe)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(16.dp)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Azione Sinistra (Archive/Star)
            AnimatedVisibility(
                visible = offsetX > 0,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(
                            if (offsetX > starThreshold) {
                                Brush.horizontalGradient(
                                    colors = listOf(BrightPink, CoralOrange)
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(FolderPersonal, TurquoiseBlue)
                                )
                            }
                        )
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (offsetX > starThreshold) Icons.Filled.Star else Icons.Filled.Archive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .scale(actionScale)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (offsetX > starThreshold) "Importante" else "Archivia",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Azione Destra (Delete)
            AnimatedVisibility(
                visible = offsetX < 0,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFE53935), Color(0xFFC62828))
                            )
                        )
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Elimina",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .scale(actionScale)
                    )
                }
            }
        }

        // Card dell'email (swipeable)
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                offsetX > starThreshold -> {
                                    currentAction = SwipeAction.STAR
                                    onStar()
                                }
                                offsetX > archiveThreshold -> {
                                    currentAction = SwipeAction.ARCHIVE
                                    onArchive()
                                }
                                offsetX < deleteThreshold -> {
                                    currentAction = SwipeAction.DELETE
                                    onDelete()
                                }
                            }
                            // Reset position
                            scope.launch {
                                offsetX = 0f
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-400f, 400f)
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = if (isRead)
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                else
                    MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(DeepPurple, IndigoBlue)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sender.first().toString().uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sender,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isRead) FontWeight.Normal else FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isStarred) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "⭐", fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subject with category
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isRead) FontWeight.Normal else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Preview
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (hasAttachment) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.AttachFile,
                                contentDescription = "Allegato",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Componente di feedback visivo per le azioni completate
@Composable
fun ActionFeedback(
    action: SwipeAction,
    onDismiss: () -> Unit
) {
    val (icon, text, color) = when (action) {
        SwipeAction.ARCHIVE -> Triple(Icons.Filled.Archive, "Email archiviata", TurquoiseBlue)
        SwipeAction.DELETE -> Triple(Icons.Filled.Delete, "Email eliminata", Color(0xFFE53935))
        SwipeAction.STAR -> Triple(Icons.Filled.Star, "Aggiunta agli importanti", CoralOrange)
        SwipeAction.MARK_READ -> Triple(Icons.Filled.MarkEmailRead, "Segnata come letta", FolderPersonal)
        SwipeAction.NONE -> return
    }

    LaunchedEffect(action) {
        kotlinx.coroutines.delay(2000)
        onDismiss()
    }

    Snackbar(
        modifier = Modifier.padding(16.dp),
        containerColor = color,
        contentColor = Color.White,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}
