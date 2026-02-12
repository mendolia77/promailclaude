package com.smartmail.ui.screens.category

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmail.ui.screens.inbox.EmailPreview
import com.smartmail.ui.theme.*
import com.smartmail.workers.EmailSyncWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryDetailScreen(
    folderId: Long,
    categoryName: String,
    categoryIcon: String,
    categoryColor: Color,
    onBack: () -> Unit = {},
    onEmailClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Carica email dal database per questa cartella
    val db = remember { com.smartmail.data.local.database.SmartMailDatabase.getInstance(context) }
    val categoryEmails = remember(folderId) {
        mutableStateOf<List<EmailPreview>>(emptyList())
    }

    // Osserva le email per questa cartella
    LaunchedEffect(folderId) {
        db.emailDao().getEmailsBySmartFolder(folderId).collect { emails ->
            categoryEmails.value = emails.map { email ->
                EmailPreview(
                    id = email.id,
                    sender = email.fromName ?: email.fromAddress.substringBefore("@"),
                    senderEmail = email.fromAddress,
                    subject = email.subject ?: "(Nessun oggetto)",
                    preview = email.bodyText?.take(100) ?: "",
                    time = formatTime(email.receivedDate.time),
                    hasAttachment = email.hasAttachments,
                    isRead = email.isRead,
                    isImportant = email.isStarred,
                    category = categoryIcon
                )
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                EmailSyncWorker.syncNow(context)
                delay(2000)
                isRefreshing = false
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Icona categoria
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(categoryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = categoryIcon,
                                fontSize = 20.sp
                            )
                        }

                        // Nome categoria
                        Column {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${categoryEmails.value.size} email",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Filtri */ }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filtri")
                    }
                    IconButton(onClick = { /* TODO: Altro */ }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Altro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            if (categoryEmails.value.isEmpty()) {
                // Stato vuoto
                EmptyState(categoryName = categoryName, categoryIcon = categoryIcon)
            } else {
                // Lista email
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(
                        items = categoryEmails.value,
                        key = { it.id } // Usa ID unico come key
                    ) { email ->
                        CategoryEmailCard(
                            email = email,
                            categoryColor = categoryColor,
                            onEmailClick = onEmailClick,
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        )
                    }
                }
            }

            // Pull refresh indicator
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = categoryColor
            )
        }
    }
}

@Composable
fun EmptyState(
    categoryName: String,
    categoryIcon: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icona grande
        Text(
            text = categoryIcon,
            fontSize = 80.sp,
            modifier = Modifier.alpha(0.3f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Testo
        Text(
            text = "Nessuna email in $categoryName",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Le email di questa categoria appariranno qui",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEmailCard(
    email: EmailPreview,
    categoryColor: Color,
    onEmailClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onEmailClick(email.id) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (email.isRead) 1.dp else 3.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Barra laterale colorata se importante
            if (email.isImportant) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(categoryColor)
                )
            }

            // Avatar iniziale mittente
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(categoryColor, categoryColor.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = email.sender.first().uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Contenuto email
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = email.sender,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = email.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = email.subject,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = email.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Badges
                if (email.hasAttachment || email.isImportant) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (email.hasAttachment) {
                            AssistChip(
                                onClick = { },
                                label = { Text("📎 Allegato", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        if (email.isImportant) {
                            AssistChip(
                                onClick = { },
                                label = { Text("⭐ Importante", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = categoryColor.copy(alpha = 0.15f),
                                    labelColor = categoryColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// Funzione per formattare il tempo
private fun formatTime(date: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - date

    return when {
        diff < 60_000 -> "Ora"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 604_800_000 -> "${diff / 86_400_000}g"
        else -> {
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = date
            "${calendar.get(java.util.Calendar.DAY_OF_MONTH)}/${calendar.get(java.util.Calendar.MONTH) + 1}"
        }
    }
}
