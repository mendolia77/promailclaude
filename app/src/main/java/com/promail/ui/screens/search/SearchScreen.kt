package com.smartmail.ui.screens.search

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmail.data.local.database.SmartMailDatabase
import com.smartmail.data.repository.EmailRepository
import com.smartmail.domain.models.Email
import com.smartmail.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SearchUiState(
    val query: String = "",
    val results: List<Email> = emptyList(),
    val isSearching: Boolean = false
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val db = SmartMailDatabase.getInstance(application)
    private val repository = EmailRepository(db.accountDao(), db.emailDao(), db.smartFolderDao())

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.length >= 2) {
            search(query)
        } else {
            _uiState.value = _uiState.value.copy(results = emptyList())
        }
    }

    private fun search(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true)
        viewModelScope.launch {
            repository.searchEmails(query).collect { emails ->
                _uiState.value = _uiState.value.copy(
                    results = emails,
                    isSearching = false
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit = {},
    onEmailClick: (Long) -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.updateQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("Cerca email...") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Cancella")
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
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
        ) {
            when {
                uiState.query.isEmpty() -> {
                    // Stato iniziale
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Cerca nelle tue email",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Cerca per mittente, oggetto o contenuto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }

                uiState.query.length < 2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Inserisci almeno 2 caratteri",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }

                uiState.isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.results.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Nessun risultato",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Prova con termini diversi",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "${uiState.results.size} risultati",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(uiState.results, key = { it.id }) { email ->
                            SearchResultItem(
                                email = email,
                                query = uiState.query,
                                onClick = { onEmailClick(email.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    email: Email,
    query: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepPurple, ElectricBlue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (email.fromName ?: email.fromAddress).first().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Mittente
                Text(
                    text = email.fromName ?: email.fromAddress,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Oggetto
                Text(
                    text = email.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!email.isRead) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Preview corpo
                Text(
                    text = email.bodyText?.take(100) ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Data e icone
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatDate(email.receivedDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (email.hasAttachments) {
                        Icon(
                            Icons.Filled.AttachFile,
                            contentDescription = "Allegati",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (email.isStarred) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Importante",
                            modifier = Modifier.size(16.dp),
                            tint = BrightPink
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(date: Date): String {
    val now = Calendar.getInstance()
    val emailCal = Calendar.getInstance().apply { time = date }

    return when {
        // Oggi: mostra solo ora
        now.get(Calendar.DAY_OF_YEAR) == emailCal.get(Calendar.DAY_OF_YEAR)
                && now.get(Calendar.YEAR) == emailCal.get(Calendar.YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.ITALIAN).format(date)
        }
        // Ieri
        now.get(Calendar.DAY_OF_YEAR) - emailCal.get(Calendar.DAY_OF_YEAR) == 1
                && now.get(Calendar.YEAR) == emailCal.get(Calendar.YEAR) -> {
            "Ieri"
        }
        // Stessa settimana: giorno della settimana
        now.get(Calendar.WEEK_OF_YEAR) == emailCal.get(Calendar.WEEK_OF_YEAR)
                && now.get(Calendar.YEAR) == emailCal.get(Calendar.YEAR) -> {
            SimpleDateFormat("EEEE", Locale.ITALIAN).format(date)
        }
        // Stesso anno: giorno e mese
        now.get(Calendar.YEAR) == emailCal.get(Calendar.YEAR) -> {
            SimpleDateFormat("dd MMM", Locale.ITALIAN).format(date)
        }
        // Anno diverso: data completa
        else -> {
            SimpleDateFormat("dd/MM/yy", Locale.ITALIAN).format(date)
        }
    }
}
