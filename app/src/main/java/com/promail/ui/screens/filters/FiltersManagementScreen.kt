package com.smartmail.ui.screens.filters

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmail.domain.usecases.FilterSuggestion
import com.smartmail.domain.usecases.SuggestionPriority
import com.smartmail.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersManagementScreen(
    onBack: () -> Unit = {},
    onCreateFilter: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.FilterAlt,
                            contentDescription = null,
                            tint = DeepPurple
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Gestione Filtri",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateFilter,
                containerColor = DeepPurple,
                contentColor = Color.White,
                icon = {
                    Icon(Icons.Filled.Add, contentDescription = null)
                },
                text = {
                    Text("Nuovo Filtro", fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = DeepPurple
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Suggerimenti IA") },
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("I Miei Filtri") },
                    icon = { Icon(Icons.Filled.FilterList, contentDescription = null) }
                )
            }

            // Content
            when (selectedTab) {
                0 -> AISuggestionsTab()
                1 -> MyFiltersTab()
            }
        }
    }
}

@Composable
fun AISuggestionsTab() {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // Dati simulati - sostituire con dati reali da SmartLearningEngine
    val suggestions = remember {
        listOf(
            FilterSuggestion(
                title = "Automatizza Amazon",
                description = "Hai spostato 8 email da Amazon in 'Shopping'. Vuoi creare un filtro automatico?",
                filterRule = """{"type":"fromAddress","addresses":["noreply@amazon.it"]}""",
                targetFolderName = "Shopping",
                confidence = 0.92f,
                priority = SuggestionPriority.HIGH
            ),
            FilterSuggestion(
                title = "Filtro per @company.com",
                description = "Hai categorizzato 12 email da company.com in 'Lavoro'. Crea un filtro automatico?",
                filterRule = """{"type":"fromDomain","domains":["company.com"]}""",
                targetFolderName = "Lavoro",
                confidence = 0.85f,
                priority = SuggestionPriority.HIGH
            ),
            FilterSuggestion(
                title = "Newsletter automatiche",
                description = "Rilevate 15 newsletter. Vuoi filtrarle automaticamente?",
                filterRule = """{"type":"bodyContains","keywords":["unsubscribe","newsletter"]}""",
                targetFolderName = "Newsletter",
                confidence = 0.78f,
                priority = SuggestionPriority.MEDIUM
            ),
            FilterSuggestion(
                title = "Filtro tracking spedizioni",
                description = "Email di tracking rilevate. Vuoi categorizzarle automaticamente?",
                filterRule = """{"type":"subjectContains","keywords":["tracking","spedizione","consegna"]}""",
                targetFolderName = "Shopping",
                confidence = 0.71f,
                priority = SuggestionPriority.MEDIUM
            )
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AIIntroCard()
            }

            if (suggestions.isEmpty()) {
                item {
                    EmptySuggestionsCard()
                }
            } else {
                itemsIndexed(suggestions) { index, suggestion ->
                    FilterSuggestionCard(
                        suggestion = suggestion,
                        animationDelay = index * 100,
                        onAccept = { /* TODO: Apply filter */ },
                        onDismiss = { /* TODO: Dismiss */ }
                    )
                }
            }
        }
    }
}

@Composable
fun AIIntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DeepPurple.copy(alpha = 0.1f),
                            ElectricBlue.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepPurple, IndigoBlue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🤖", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "IA al tuo servizio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ProMail analizza il tuo comportamento e suggerisce filtri intelligenti per automatizzare la categorizzazione",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSuggestionCard(
    suggestion: FilterSuggestion,
    animationDelay: Int = 0,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var isDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val priorityColor = when (suggestion.priority) {
        SuggestionPriority.CRITICAL -> Color(0xFFE53935)
        SuggestionPriority.HIGH -> CoralOrange
        SuggestionPriority.MEDIUM -> ElectricBlue
        SuggestionPriority.LOW -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    AnimatedVisibility(
        visible = isVisible && !isDismissed,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = priorityColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = suggestion.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Confidence badge
                    ConfidenceBadge(confidence = suggestion.confidence)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = suggestion.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Target folder chip
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = priorityColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Folder,
                            contentDescription = null,
                            tint = priorityColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "→ ${suggestion.targetFolderName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = priorityColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isDismissed = true
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ignora")
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = priorityColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Applica", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfidenceBadge(confidence: Float) {
    val percentage = (confidence * 100).toInt()
    val color = when {
        confidence >= 0.8f -> FolderPersonal
        confidence >= 0.6f -> ElectricBlue
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$percentage%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptySuggestionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "✨", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nessun suggerimento al momento",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Continua ad usare ProMail e l'IA imparerà le tue abitudini per suggerirti filtri personalizzati",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun MyFiltersTab() {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // Filtri simulati - sostituire con dati reali da database
    val myFilters = remember {
        listOf(
            UserFilter(
                id = 1,
                name = "Lavoro Urgente",
                icon = "🔥",
                description = "Email da @company.com con 'urgente' nell'oggetto",
                targetFolder = "Importanti",
                color = CoralOrange,
                isActive = true,
                matchCount = 45
            ),
            UserFilter(
                id = 2,
                name = "Shopping Online",
                icon = "🛒",
                description = "Email da Amazon, eBay, Zalando",
                targetFolder = "Shopping",
                color = FolderShopping,
                isActive = true,
                matchCount = 128
            ),
            UserFilter(
                id = 3,
                name = "Newsletter Tech",
                icon = "📰",
                description = "Newsletter da Medium, Dev.to, CSS-Tricks",
                targetFolder = "Newsletter",
                color = FolderNewsletter,
                isActive = false,
                matchCount = 67
            )
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (myFilters.isEmpty()) {
                item {
                    EmptyFiltersCard()
                }
            } else {
                itemsIndexed(myFilters) { index, filter ->
                    UserFilterCard(
                        filter = filter,
                        animationDelay = index * 100,
                        onToggle = { /* TODO: Toggle filter */ },
                        onEdit = { /* TODO: Edit filter */ },
                        onDelete = { /* TODO: Delete filter */ }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFilterCard(
    filter: UserFilter,
    animationDelay: Int = 0,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { isExpanded = !isExpanded },
            colors = CardDefaults.cardColors(
                containerColor = if (filter.isActive)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(if (filter.isActive) 4.dp else 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(filter.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = filter.icon, fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = filter.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filter.matchCount} email categorizzate",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    // Toggle
                    Switch(
                        checked = filter.isActive,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = filter.color
                        )
                    )
                }

                // Expanded content
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        Spacer(modifier = Modifier.height(12.dp))

                        // Description
                        Text(
                            text = filter.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target folder
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                tint = filter.color,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Categoria: ${filter.targetFolder}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = filter.color
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onEdit,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Modifica")
                            }

                            OutlinedButton(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFE53935)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Elimina")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyFiltersCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🎯", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nessun filtro personalizzato",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Crea il tuo primo filtro toccando il pulsante '+'",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Data class
data class UserFilter(
    val id: Long,
    val name: String,
    val icon: String,
    val description: String,
    val targetFolder: String,
    val color: Color,
    val isActive: Boolean,
    val matchCount: Int
)
