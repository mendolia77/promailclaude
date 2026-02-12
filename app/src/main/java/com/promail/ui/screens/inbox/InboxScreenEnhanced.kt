package com.smartmail.ui.screens.inbox

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmail.ui.theme.*
import com.smartmail.workers.EmailSyncWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InboxScreenEnhanced(
    onCompose: () -> Unit = {},
    onSettings: () -> Unit = {},
    onSearch: () -> Unit = {},
    onCategoryClick: (Long, String, String, Color) -> Unit = { _, _, _, _ -> },
    onEmailClick: (Long) -> Unit = {}, // Callback per aprire email
    viewModel: InboxViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFolder by remember { mutableStateOf<FolderItem?>(null) }
    var showFocusMode by remember { mutableStateOf(false) }

    // Animazione FAB
    val fabScale by rememberInfiniteTransition(label = "fab").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_scale"
    )

    Scaffold(
        topBar = {
            InboxTopBarEnhanced(
                showFocusMode = showFocusMode,
                onToggleFocusMode = { showFocusMode = !showFocusMode },
                onSearch = onSearch
            )
        },
        bottomBar = {
            InboxBottomBarAnimated(onSettings = onSettings)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCompose,
                containerColor = DeepPurple,
                contentColor = Color.White,
                modifier = Modifier
                    .scale(fabScale)
                    .shadow(12.dp, CircleShape),
                icon = {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        "Scrivi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Smart Folders Row scrollabile
                SmartFoldersRowEnhanced(
                    selectedFolder = selectedFolder,
                    onFolderSelected = { selectedFolder = it },
                    onCategoryClick = onCategoryClick,
                    smartFolders = uiState.smartFolders,
                    totalEmails = uiState.emails.size,
                    unreadCount = uiState.unreadCount,
                    starredCount = uiState.starredCount,
                    folderUnreadCounts = uiState.folderUnreadCounts,
                    onSelectInbox = { viewModel.selectInbox() },
                    onSelectStarred = { viewModel.selectStarred() },
                    onSelectAttachments = { viewModel.selectAttachments() }
                )

                // Email List con swipe gestures
                EmailListEnhanced(
                    emails = uiState.emails,
                    showFocusMode = showFocusMode,
                    onToggleRead = viewModel::toggleRead,
                    onToggleStar = viewModel::toggleStar,
                    onDelete = viewModel::deleteEmail,
                    onEmailClick = onEmailClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTopBarEnhanced(
    showFocusMode: Boolean,
    onToggleFocusMode: () -> Unit,
    onSearch: () -> Unit = {}
) {
    // Gradient animato per la top bar
    val infiniteTransition = rememberInfiniteTransition(label = "topbar")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Logo animato
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
                        text = "✉️",
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        "SmartMail",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (showFocusMode) {
                        Text(
                            "Focus Mode Attivo",
                            style = MaterialTheme.typography.labelSmall,
                            color = CoralOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        actions = {
            // Toggle Focus Mode
            IconButton(
                onClick = onToggleFocusMode
            ) {
                Icon(
                    if (showFocusMode) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = "Focus Mode",
                    tint = if (showFocusMode) CoralOrange else MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(onClick = onSearch) {
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
fun SmartFoldersRowEnhanced(
    selectedFolder: FolderItem?,
    onFolderSelected: (FolderItem) -> Unit,
    onCategoryClick: (Long, String, String, Color) -> Unit = { _, _, _, _ -> },
    smartFolders: List<com.smartmail.domain.models.SmartFolder> = emptyList(),
    totalEmails: Int = 0,
    unreadCount: Int = 0,
    starredCount: Int = 0,
    folderUnreadCounts: Map<Long, Int> = emptyMap(),
    onSelectInbox: () -> Unit = {},
    onSelectStarred: () -> Unit = {},
    onSelectAttachments: () -> Unit = {}
) {
    // Converti SmartFolder in FolderItem e aggiungi cartelle speciali
    val folders = remember(smartFolders, totalEmails, unreadCount, starredCount, folderUnreadCounts) {
        val specialFolders = listOf(
            FolderItem(0L, "📥", "Tutti", totalEmails, IndigoBlue)
        )

        // Converti le smart folders dal database in FolderItem
        val dbFolders = smartFolders.map { folder ->
            FolderItem(
                id = folder.id,
                icon = folder.icon,
                name = folder.name,
                count = folderUnreadCounts[folder.id] ?: 0,
                color = try {
                    Color(folder.colorHex.toLong(16))
                } catch (e: Exception) {
                    IndigoBlue
                }
            )
        }

        specialFolders + dbFolders
    }

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500)
        isVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Categorie Smart",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy
                )
            ) + fadeIn()
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(folders) { index, folder ->
                    FolderChip(
                        folder = folder,
                        isSelected = selectedFolder == folder,
                        onClick = {
                            onFolderSelected(folder)
                            // Gestisci clic su cartelle speciali
                            when (folder.name) {
                                "Tutti" -> onSelectInbox()
                                "Importanti" -> onSelectStarred()
                                "Allegati" -> onSelectAttachments()
                                else -> onCategoryClick(folder.id, folder.name, folder.icon, folder.color)
                            }
                        },
                        animationDelay = index * 50
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderChip(
    folder: FolderItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    animationDelay: Int = 0
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn()
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.scale(scale),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    folder.color.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(
                if (isSelected) 6.dp else 2.dp
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = folder.icon,
                    fontSize = 20.sp
                )
                Text(
                    text = folder.name,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
                if (folder.count > 0) {
                    Badge(
                        containerColor = folder.color,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = folder.count.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun EmailListEnhanced(
    emails: List<com.smartmail.domain.models.Email>,
    showFocusMode: Boolean,
    onToggleRead: (Long, Boolean) -> Unit = { _, _ -> },
    onToggleStar: (Long, Boolean) -> Unit = { _, _ -> },
    onDelete: (Long) -> Unit = {},
    onEmailClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Converti Email del database in EmailPreview per la UI
    val emailPreviews = remember(emails) {
        emails.map { email ->
            EmailPreview(
                id = email.id, // ID unico dell'email
                sender = email.fromName ?: email.fromAddress,
                senderEmail = email.fromAddress,
                subject = email.subject,
                preview = email.bodyText?.take(100) ?: email.bodyHtml?.take(100) ?: "",
                time = InboxViewModel.formatDate(email.receivedDate),
                hasAttachment = email.hasAttachments,
                isRead = email.isRead,
                isImportant = email.isImportant || email.isStarred,
                category = "📧" // Default, potrebbe essere migliorato con smartFolderId
            )
        }
    }

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
        if (emailPreviews.isEmpty()) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(
                    items = emailPreviews,
                    key = { _, email -> email.id } // Usa ID unico come key
                ) { index, email ->
                    EmailCardSwipeable(
                        email = email,
                        animationDelay = index * 80,
                        onEmailClick = { onEmailClick(email.id) }
                    )
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EmailCardSwipeable(
    email: EmailPreview,
    animationDelay: Int = 0,
    onEmailClick: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    var isDeleted by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible && !isDeleted && !isArchived,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        SwipeableEmailCard(
            email = email,
            onDelete = { isDeleted = true },
            onArchive = { isArchived = true },
            onEmailClick = onEmailClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableEmailCard(
    email: EmailPreview,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onEmailClick: () -> Unit = {}
) {
    // TODO: Implementare swipe gestures quando disponibile in Material3
    // Per ora usiamo un card normale con effetti

    Card(
        onClick = onEmailClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (email.isRead)
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            if (email.isImportant) 6.dp else 3.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (email.isImportant) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                CoralOrange.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar con gradiente
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
                        text = email.sender.first().toString().uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = email.sender,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (email.isImportant) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "⭐", fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = email.time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = email.category,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = email.subject,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

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
}

@Composable
fun InboxBottomBarAnimated(onSettings: () -> Unit = {}) {
    var selectedIndex by remember { mutableStateOf(0) }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.shadow(8.dp)
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Inbox,
                    contentDescription = null,
                    modifier = Modifier.scale(if (selectedIndex == 0) 1.2f else 1f)
                )
            },
            label = { Text("Inbox") },
            selected = selectedIndex == 0,
            onClick = { selectedIndex = 0 }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Email,
                    contentDescription = null,
                    modifier = Modifier.scale(if (selectedIndex == 1) 1.2f else 1f)
                )
            },
            label = { Text("Email") },
            selected = selectedIndex == 1,
            onClick = { selectedIndex = 1 }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.scale(if (selectedIndex == 2) 1.2f else 1f)
                )
            },
            label = { Text("Settings") },
            selected = selectedIndex == 2,
            onClick = {
                selectedIndex = 2
                onSettings()
            }
        )
    }
}

