package com.smartmail.ui.screens.filters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmail.domain.models.EmailFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(
    viewModel: FiltersViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filtri Email") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddFilter() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi filtro")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.filters.isEmpty()) {
                // Stato vuoto
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Nessun filtro configurato",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Crea filtri per organizzare automaticamente le email",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Lista filtri
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filters) { filter ->
                        FilterCard(
                            filter = filter,
                            onToggle = { viewModel.toggleFilter(filter.id) },
                            onEdit = { viewModel.editFilter(filter) },
                            onDelete = { viewModel.deleteFilter(filter.id) }
                        )
                    }
                }
            }
        }
    }

    // Dialog aggiungi/modifica filtro
    if (uiState.showFilterDialog) {
        AddFilterDialog(
            viewModel = viewModel,
            filter = uiState.editingFilter
        )
    }
}

@Composable
private fun FilterCard(
    filter: EmailFilter,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    viewModel: FiltersViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folderName = remember(filter.moveToSmartFolderId, uiState.smartFolders) {
        uiState.smartFolders.find { it.id == filter.moveToSmartFolderId }?.name
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = filter.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mostra condizioni
                    if (!filter.fromContains.isNullOrBlank()) {
                        FilterConditionChip("Da: ${filter.fromContains}")
                    }
                    if (!filter.subjectContains.isNullOrBlank()) {
                        FilterConditionChip("Oggetto: ${filter.subjectContains}")
                    }
                    if (filter.hasAttachments == true) {
                        FilterConditionChip("Ha allegati")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mostra azioni
                    if (folderName != null) {
                        FilterActionChip("📁 Sposta in: $folderName")
                    }
                    if (filter.markAsStarred) {
                        FilterActionChip("⭐ Stellata")
                    }
                    if (filter.markAsImportant) {
                        FilterActionChip("❗ Importante")
                    }
                    if (filter.markAsRead) {
                        FilterActionChip("✓ Letta")
                    }
                }

                Switch(
                    checked = filter.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modifica")
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Elimina")
                }
            }
        }
    }
}

@Composable
private fun FilterConditionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun FilterActionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFilterDialog(
    viewModel: FiltersViewModel,
    filter: EmailFilter?
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expandedFolders by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!expandedFolders) {
                viewModel.hideFilterDialog()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !expandedFolders,
            dismissOnClickOutside = !expandedFolders
        ),
        title = {
            Text(if (filter == null) "Nuovo Filtro" else "Modifica Filtro")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.filterName,
                    onValueChange = { viewModel.updateFilterName(it) },
                    label = { Text("Nome filtro") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    "Condizioni",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = uiState.filterFromContains ?: "",
                    onValueChange = { viewModel.updateFromContains(it) },
                    label = { Text("Mittente contiene") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("es: info@esempio.it") }
                )

                OutlinedTextField(
                    value = uiState.filterSubjectContains ?: "",
                    onValueChange = { viewModel.updateSubjectContains(it) },
                    label = { Text("Oggetto contiene") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("es: Fattura") }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.filterHasAttachments ?: false,
                        onCheckedChange = { viewModel.updateHasAttachments(it) }
                    )
                    Text("Ha allegati")
                }

                Divider()

                Text(
                    "Azioni",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Dropdown cartelle
                val selectedFolder = remember(uiState.filterMoveToSmartFolderId, uiState.smartFolders) {
                    uiState.smartFolders.find { it.id == uiState.filterMoveToSmartFolderId }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedFolders,
                    onExpandedChange = { expandedFolders = it }
                ) {
                    OutlinedTextField(
                        value = selectedFolder?.name ?: "Nessuna cartella",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sposta in cartella") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .exposedDropdownSize(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFolders)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedFolders,
                        onDismissRequest = { expandedFolders = false }
                    ) {
                        // Opzione "Nessuna"
                        DropdownMenuItem(
                            text = { Text("Nessuna cartella") },
                            onClick = {
                                viewModel.updateMoveToSmartFolder(null)
                                expandedFolders = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )

                        // Cartelle disponibili
                        uiState.smartFolders.forEach { folder ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(folder.icon)
                                        Text(folder.name)
                                    }
                                },
                                onClick = {
                                    viewModel.updateMoveToSmartFolder(folder.id)
                                    expandedFolders = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.filterMarkAsStarred,
                        onCheckedChange = { viewModel.updateMarkAsStarred(it) }
                    )
                    Text("Segna come stellata ⭐")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.filterMarkAsImportant,
                        onCheckedChange = { viewModel.updateMarkAsImportant(it) }
                    )
                    Text("Segna come importante ❗")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.filterMarkAsRead,
                        onCheckedChange = { viewModel.updateMarkAsRead(it) }
                    )
                    Text("Segna come letta ✓")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.saveFilter() },
                enabled = uiState.filterName.isNotBlank()
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.hideFilterDialog() }) {
                Text("Annulla")
            }
        }
    )
}
