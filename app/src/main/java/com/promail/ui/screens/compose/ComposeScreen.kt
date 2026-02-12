package com.smartmail.ui.screens.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmail.domain.models.Account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    emailId: Long? = null,
    isReply: Boolean = false,
    isForward: Boolean = false,
    draftId: Long? = null,
    viewModel: ComposeViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onEmailSent: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAccountPicker by remember { mutableStateOf(false) }

    // Setup reply/forward/draft on first load
    LaunchedEffect(Unit) {
        when {
            draftId != null -> viewModel.loadDraft(draftId)
            emailId != null && isReply -> viewModel.setupReply(emailId)
            emailId != null && isForward -> viewModel.setupForward(emailId)
        }
    }

    // Gestisci risultato invio
    LaunchedEffect(uiState.sendResult) {
        when (uiState.sendResult) {
            is SendResult.Success -> {
                onEmailSent()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            ComposeTopBar(
                isSending = uiState.isSending,
                canSend = uiState.to.isNotBlank() && !uiState.isSending,
                onBack = onNavigateBack,
                onSend = { viewModel.send() },
                onAttach = { /* TODO: apri file picker */ }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Selettore account mittente
            AccountSelector(
                selectedAccount = uiState.selectedAccount,
                onClick = { showAccountPicker = true }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Campo A (destinatario)
            RecipientField(
                label = "A",
                value = uiState.to,
                onValueChange = { viewModel.updateTo(it) },
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleCcBcc() }) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = "Mostra CC/CCN",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            // CC e BCC (espandibili)
            AnimatedVisibility(visible = uiState.showCcBcc) {
                Column {
                    RecipientField(
                        label = "Cc",
                        value = uiState.cc,
                        onValueChange = { viewModel.updateCc(it) }
                    )
                    RecipientField(
                        label = "Ccn",
                        value = uiState.bcc,
                        onValueChange = { viewModel.updateBcc(it) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Oggetto
            TextField(
                value = uiState.subject,
                onValueChange = { viewModel.updateSubject(it) },
                placeholder = { Text("Oggetto", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Corpo email
            TextField(
                value = uiState.body,
                onValueChange = { viewModel.updateBody(it) },
                placeholder = { Text("Scrivi il messaggio...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 300.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            // Lista allegati
            if (uiState.attachments.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Allegati (${uiState.attachments.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    uiState.attachments.forEach { file ->
                        AttachmentChip(
                            fileName = file.name,
                            onRemove = { viewModel.removeAttachment(file) }
                        )
                    }
                }
            }

            // Messaggio di errore
            if (uiState.sendResult is SendResult.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            (uiState.sendResult as SendResult.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    // Bottom sheet per selezione account
    if (showAccountPicker) {
        AccountPickerDialog(
            accounts = uiState.accounts,
            selectedAccount = uiState.selectedAccount,
            onAccountSelected = {
                viewModel.selectAccount(it)
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeTopBar(
    isSending: Boolean,
    canSend: Boolean,
    onBack: () -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                if (isSending) "Invio in corso..." else "Nuova email",
                style = MaterialTheme.typography.titleMedium
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Chiudi")
            }
        },
        actions = {
            IconButton(onClick = onAttach) {
                Icon(Icons.Outlined.AttachFile, contentDescription = "Allega file")
            }
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onSend, enabled = canSend) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Invia",
                        tint = if (canSend) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun AccountSelector(
    selectedAccount: Account?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Da",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )

        if (selectedAccount != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        try {
                            Color(selectedAccount.color.toColorInt())
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    selectedAccount.emailAddress.first().uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    selectedAccount.displayName ?: selectedAccount.emailAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    selectedAccount.emailAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                "Seleziona un account",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecipientField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true
        )
        trailingIcon?.invoke()
    }
}

@Composable
private fun AttachmentChip(
    fileName: String,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.InsertDriveFile,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                fileName,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Rimuovi allegato",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerDialog(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                "Seleziona account mittente",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            accounts.forEach { account ->
                val isSelected = account.id == selectedAccount?.id
                ListItem(
                    headlineContent = {
                        Text(
                            account.displayName ?: account.emailAddress,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    supportingContent = {
                        Text(account.emailAddress)
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    try {
                                        Color(account.color.toColorInt())
                                    } catch (_: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                account.emailAddress.first().uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    trailingContent = {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.clickable { onAccountSelected(account) }
                )
            }
        }
    }
}
