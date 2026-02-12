package com.smartmail.ui.screens.inbox

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.smartmail.ui.theme.*
import com.smartmail.workers.EmailSyncWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onCompose: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    var selectedFolder by remember { mutableStateOf("Posta in arrivo") }

    Scaffold(
        topBar = {
            InboxTopBar()
        },
        bottomBar = {
            InboxBottomBar(onSettings = onSettings)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCompose,
                containerColor = DeepPurple,
                contentColor = Color.White,
                modifier = Modifier.shadow(8.dp, CircleShape)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Scrivi email")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Smart Folders Row
            SmartFoldersRow()
            
            // Email List
            EmailList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTopBar() {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "SmartMail",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        actions = {
            IconButton(onClick = { /* TODO: Search */ }) {
                Icon(Icons.Filled.Search, contentDescription = "Cerca")
            }
            IconButton(onClick = { /* TODO: Profile */ }) {
                Icon(Icons.Filled.AccountCircle, contentDescription = "Profilo")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun SmartFoldersRow() {
    val folders = remember {
        listOf(
            FolderItem(0L, "📥", "Posta in arrivo", 12, FolderWork),
            FolderItem(0L, "⭐", "Importanti", 3, CoralOrange),
            FolderItem(0L, "🏢", "Lavoro", 25, FolderWork),
            FolderItem(0L, "👥", "Personale", 8, FolderPersonal),
            FolderItem(0L, "📎", "Allegati", 5, FolderAttachments)
        )
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(folders) { folder ->
            FolderCard(folder)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderCard(folder: FolderItem) {
    val scale by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )
    
    Card(
        onClick = { /* TODO: Navigate to folder */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icona folder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(folder.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = folder.icon,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Nome folder
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            
            // Badge count
            if (folder.count > 0) {
                Badge(
                    containerColor = folder.color,
                    contentColor = Color.White
                ) {
                    Text(
                        text = folder.count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EmailList() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Lista vuota - le email reali vengono mostrate solo in InboxScreenEnhanced
    val emails = remember { emptyList<EmailPreview>() }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                // Trigger immediate sync
                EmailSyncWorker.syncNow(context)
                // Show refreshing for 2 seconds minimum
                delay(2000)
                isRefreshing = false
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (emails.isEmpty()) {
            // Mostra messaggio quando non ci sono email
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📧",
                    fontSize = 80.sp,
                    modifier = Modifier.alpha(0.3f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Nessuna email",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Configura un account per iniziare",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(emails) { email ->
                    EmailCard(email)
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = DeepPurple
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailCard(email: EmailPreview) {
    Card(
        onClick = { /* TODO: Open email */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (email.isRead) 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepPurple, IndigoBlue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = email.sender.first().toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = email.sender,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = email.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = email.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = email.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (email.hasAttachment) {
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

@Composable
fun InboxBottomBar(onSettings: () -> Unit = {}) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Inbox, contentDescription = null) },
            label = { Text("Inbox") },
            selected = true,
            onClick = { }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Email, contentDescription = null) },
            label = { Text("Email") },
            selected = false,
            onClick = { }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text("Impostazioni") },
            selected = false,
            onClick = onSettings
        )
    }
}

// Data classes
data class FolderItem(
    val id: Long = 0L,
    val icon: String,
    val name: String,
    val count: Int,
    val color: Color
)

data class EmailPreview(
    val id: Long = 0L,  // ID unico dell'email dal database
    val sender: String,
    val senderEmail: String = "",
    val subject: String,
    val preview: String,
    val time: String,
    val hasAttachment: Boolean,
    val isRead: Boolean,
    val isImportant: Boolean = false,
    val category: String = "📧"
)
