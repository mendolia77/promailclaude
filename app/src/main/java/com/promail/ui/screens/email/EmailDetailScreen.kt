package com.smartmail.ui.screens.email

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.text.Html
import android.webkit.WebView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartmail.data.local.database.SmartMailDatabase
import com.smartmail.data.remote.ImapClient
import com.smartmail.data.repository.EmailRepository
import com.smartmail.domain.models.Email
import com.smartmail.domain.models.EmailAttachment
import com.smartmail.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.rememberCoroutineScope

data class EmailDetailUiState(
    val email: Email? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class EmailDetailViewModel(
    application: android.app.Application,
    private val emailId: Long
) : AndroidViewModel(application) {

    private val db = SmartMailDatabase.getInstance(application)
    private val repository = EmailRepository(db.accountDao(), db.emailDao(), db.smartFolderDao())

    private val _uiState = MutableStateFlow(EmailDetailUiState())
    val uiState: StateFlow<EmailDetailUiState> = _uiState.asStateFlow()

    init {
        loadEmail()
    }

    private fun loadEmail() {
        viewModelScope.launch {
            try {
                val email = repository.getEmailById(emailId)
                _uiState.value = EmailDetailUiState(
                    email = email,
                    isLoading = false
                )

                // Marca come letta
                if (email != null && !email.isRead) {
                    repository.markAsRead(emailId)
                }
            } catch (e: Exception) {
                _uiState.value = EmailDetailUiState(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun toggleStar() {
        viewModelScope.launch {
            _uiState.value.email?.let { email ->
                repository.toggleStar(emailId, !email.isStarred)
                _uiState.value = _uiState.value.copy(
                    email = email.copy(isStarred = !email.isStarred)
                )
            }
        }
    }

    fun deleteEmail(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.markAsDeleted(emailId)
            onDeleted()
        }
    }

    fun assignToFolder(folderId: Long?) {
        viewModelScope.launch {
            _uiState.value.email?.let { email ->
                val updatedEmail = email.copy(smartFolderId = folderId)
                repository.updateEmail(updatedEmail)
                _uiState.value = _uiState.value.copy(
                    email = updatedEmail
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailDetailScreen(
    emailId: Long,
    onBack: () -> Unit = {},
    onReply: (Long) -> Unit = {},
    onForward: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: EmailDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return EmailDetailViewModel(context.applicationContext as android.app.Application, emailId) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showQuickFilterDialog by remember { mutableStateOf(false) }
    var showAssignFolderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    // Stella
                    IconButton(onClick = { viewModel.toggleStar() }) {
                        Icon(
                            if (uiState.email?.isStarred == true) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Importante",
                            tint = if (uiState.email?.isStarred == true) BrightPink else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Assegna cartella
                    IconButton(onClick = { showAssignFolderDialog = true }) {
                        Icon(Icons.Filled.Folder, contentDescription = "Assegna cartella")
                    }

                    // Crea filtro rapido
                    IconButton(onClick = { showQuickFilterDialog = true }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Crea filtro")
                    }

                    // Elimina
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Elimina")
                    }

                    // Menu
                    IconButton(onClick = { /* TODO: More options */ }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Altro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Barra azioni
            if (uiState.email != null) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Rispondi
                        FilledTonalButton(
                            onClick = { onReply(emailId) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Reply, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rispondi")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Inoltra
                        OutlinedButton(
                            onClick = { onForward(emailId) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Forward, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Inoltra")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    // Loading
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null -> {
                    // Errore
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Errore caricamento email",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = uiState.error ?: "Errore sconosciuto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                uiState.email != null -> {
                    // Contenuto email
                    EmailContent(email = uiState.email!!)
                }
            }
        }
    }

    // Dialog conferma eliminazione
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminare email?") },
            text = { Text("L'email verrà spostata nel cestino.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteEmail(onBack)
                    }
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Dialog quick filter
    if (showQuickFilterDialog && uiState.email != null) {
        QuickFilterDialog(
            email = uiState.email!!,
            onDismiss = { showQuickFilterDialog = false },
            onFilterCreated = {
                showQuickFilterDialog = false
                // Mostra toast di conferma
                android.widget.Toast.makeText(
                    context,
                    "Filtro creato con successo!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    if (showAssignFolderDialog && uiState.email != null) {
        AssignFolderDialog(
            email = uiState.email!!,
            onDismiss = { showAssignFolderDialog = false },
            onFolderAssigned = { folderId ->
                viewModel.assignToFolder(folderId)
                showAssignFolderDialog = false
                // Mostra toast di conferma
                android.widget.Toast.makeText(
                    context,
                    "Cartella assegnata con successo!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
fun EmailContent(email: Email) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Header con mittente
        EmailHeader(email)

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Corpo email
        EmailBody(email)

        // Allegati
        if (email.hasAttachments) {
            Spacer(modifier = Modifier.height(16.dp))
            AttachmentsSection(email)
        }

        // Spazio per la bottom bar
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun EmailHeader(email: Email) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Oggetto
        Text(
            text = email.subject,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mittente
        Row(
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = email.fromName ?: email.fromAddress,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = email.fromAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Data
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDateTime(email.receivedDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Destinatari (se più di uno mostra espandibile)
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "A: ${email.toAddresses}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                if (!email.ccAddresses.isNullOrBlank()) {
                    Text(
                        text = "Cc: ${email.ccAddresses}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmailBody(email: Email) {
    val htmlContent = email.bodyHtml
    val textContent = email.bodyText

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            when {
                !htmlContent.isNullOrBlank() -> {
                    // Rendering HTML con WebView
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 2000.dp),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.setSupportZoom(false)
                            }
                        },
                        update = { webView ->
                            // HTML con stile per renderizzare correttamente
                            val styledHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <style>
                                        body {
                                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                                            font-size: 16px;
                                            line-height: 1.6;
                                            color: #333;
                                            margin: 0;
                                            padding: 0;
                                            word-wrap: break-word;
                                        }
                                        img {
                                            max-width: 100%;
                                            height: auto;
                                        }
                                        a {
                                            color: #2196F3;
                                        }
                                        table {
                                            max-width: 100%;
                                            border-collapse: collapse;
                                        }
                                    </style>
                                </head>
                                <body>
                                    $htmlContent
                                </body>
                                </html>
                            """.trimIndent()

                            webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
                        }
                    )
                }
                !textContent.isNullOrBlank() -> {
                    // Testo semplice
                    Text(
                        text = textContent,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                else -> {
                    // Email vuota
                    Text(
                        text = "(Email senza contenuto)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun AttachmentsSection(email: Email) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { SmartMailDatabase.getInstance(context) }

    // Parse attachments JSON
    val attachments = remember(email.attachmentsJson) {
        if (email.attachmentsJson != null) {
            try {
                val gson = Gson()
                val type = object : TypeToken<List<EmailAttachment>>() {}.type
                gson.fromJson<List<EmailAttachment>>(email.attachmentsJson, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    var downloadingIndex by remember { mutableStateOf<Int?>(null) }
    var expandedActions by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Allegati (${attachments.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        attachments.forEachIndexed { index, attachment ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = downloadingIndex == null) {
                                downloadingIndex = index
                                scope.launch {
                                    downloadAttachment(
                                        context = context,
                                        email = email,
                                        attachment = attachment,
                                        attachmentIndex = index,
                                        db = db,
                                        onComplete = { downloadingIndex = null }
                                    )
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                attachment.mimeType.startsWith("image/") -> Icons.Filled.Image
                                attachment.mimeType.startsWith("video/") -> Icons.Filled.VideoLibrary
                                attachment.mimeType.startsWith("audio/") -> Icons.Filled.AudioFile
                                attachment.mimeType.contains("pdf") -> Icons.Filled.PictureAsPdf
                                else -> Icons.Filled.AttachFile
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = attachment.filename,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatFileSize(attachment.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        if (downloadingIndex == index) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        expandedActions = if (expandedActions == index) null else index
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.MoreVert,
                                        contentDescription = "Altre opzioni",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Menu azioni espanso
                    if (expandedActions == index) {
                        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Pulsante Scarica
                            TextButton(
                                onClick = {
                                    expandedActions = null
                                    downloadingIndex = index
                                    scope.launch {
                                        downloadAttachment(
                                            context = context,
                                            email = email,
                                            attachment = attachment,
                                            attachmentIndex = index,
                                            db = db,
                                            onComplete = { downloadingIndex = null }
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scarica")
                            }

                            // Pulsante Visualizza
                            TextButton(
                                onClick = {
                                    expandedActions = null
                                    downloadingIndex = index
                                    scope.launch {
                                        downloadAndOpenAttachment(
                                            context = context,
                                            email = email,
                                            attachment = attachment,
                                            attachmentIndex = index,
                                            db = db,
                                            onComplete = { downloadingIndex = null }
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apri")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Int): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

private suspend fun downloadAttachment(
    context: android.content.Context,
    email: Email,
    attachment: EmailAttachment,
    attachmentIndex: Int,
    db: SmartMailDatabase,
    onComplete: () -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            // Get account for email
            val account = db.accountDao().getAccountById(email.accountId)
            if (account == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Account non trovato", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
                return@withContext
            }

            // Connect to IMAP and download
            val imapClient = ImapClient(account)
            val connectResult = imapClient.connect()

            if (connectResult.isFailure) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore connessione: ${connectResult.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
                return@withContext
            }

            val downloadResult = imapClient.downloadAttachment(
                messageId = email.messageId,
                folderName = email.folderName,
                attachmentIndex = attachmentIndex
            )

            imapClient.disconnect()

            if (downloadResult.isSuccess) {
                val data = downloadResult.getOrNull()!!

                // Save to Downloads folder
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, attachment.filename)

                // Handle duplicate filenames
                var counter = 1
                var finalFile = file
                while (finalFile.exists()) {
                    val nameWithoutExt = attachment.filename.substringBeforeLast(".")
                    val ext = attachment.filename.substringAfterLast(".", "")
                    finalFile = File(downloadsDir, "${nameWithoutExt}_${counter}.${ext}")
                    counter++
                }

                finalFile.writeBytes(data)

                withContext(Dispatchers.Main) {
                    // Mostra notifica di successo
                    showDownloadNotification(context, finalFile, attachment)

                    Toast.makeText(context, "✓ Allegato scaricato: ${finalFile.name}", Toast.LENGTH_LONG).show()

                    // Chiedi se aprire o condividere
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            finalFile
                        )

                        // Crea Intent per aprire il file
                        val openIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, attachment.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        context.startActivity(Intent.createChooser(openIntent, "Apri con"))
                    } catch (e: Exception) {
                        // File salvato ma non può essere aperto - mostra solo la posizione
                        Toast.makeText(context, "File salvato in Download/${finalFile.name}", Toast.LENGTH_SHORT).show()
                    }

                    onComplete()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore download: ${downloadResult.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete()
            }
        }
    }
}

private suspend fun downloadAndOpenAttachment(
    context: android.content.Context,
    email: Email,
    attachment: EmailAttachment,
    attachmentIndex: Int,
    db: SmartMailDatabase,
    onComplete: () -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            // Get account for email
            val account = db.accountDao().getAccountById(email.accountId)
            if (account == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Account non trovato", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
                return@withContext
            }

            // Connect to IMAP and download
            val imapClient = ImapClient(account)
            val connectResult = imapClient.connect()

            if (connectResult.isFailure) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore connessione", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
                return@withContext
            }

            val downloadResult = imapClient.downloadAttachment(
                messageId = email.messageId,
                folderName = email.folderName,
                attachmentIndex = attachmentIndex
            )

            imapClient.disconnect()

            if (downloadResult.isSuccess) {
                val data = downloadResult.getOrNull()!!

                // Save to temporary cache directory
                val cacheDir = context.cacheDir
                val tempFile = File(cacheDir, attachment.filename)
                tempFile.writeBytes(data)

                withContext(Dispatchers.Main) {
                    // Apri direttamente il file
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, attachment.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(intent, "Apri con"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Impossibile aprire il file", Toast.LENGTH_SHORT).show()
                    }

                    onComplete()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore download", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete()
            }
        }
    }
}

private fun showDownloadNotification(
    context: Context,
    file: File,
    attachment: EmailAttachment
) {
    val channelId = "download_channel"
    val notificationId = System.currentTimeMillis().toInt()

    // Crea il canale di notifica (necessario per Android 8.0+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Download Allegati",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifiche per download allegati email"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Crea Intent per aprire il file
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val openIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, attachment.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        Intent.createChooser(openIntent, "Apri con"),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // Crea Intent per condividere il file
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = attachment.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val sharePendingIntent = PendingIntent.getActivity(
        context,
        notificationId + 1,
        Intent.createChooser(shareIntent, "Condividi"),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // Costruisci la notifica
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("Download completato")
        .setContentText("${attachment.filename} (${formatFileSize(attachment.size)})")
        .setStyle(NotificationCompat.BigTextStyle()
            .bigText("${attachment.filename}\nSalvato in Download"))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .addAction(
            android.R.drawable.ic_menu_share,
            "Condividi",
            sharePendingIntent
        )
        .setAutoCancel(true)
        .build()

    // Mostra la notifica
    try {
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (e: SecurityException) {
        // Permesso notifiche non concesso - ignora
    }
}

private fun formatDateTime(date: Date): String {
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
            "Ieri ${SimpleDateFormat("HH:mm", Locale.ITALIAN).format(date)}"
        }
        // Stessa settimana: giorno della settimana
        now.get(Calendar.WEEK_OF_YEAR) == emailCal.get(Calendar.WEEK_OF_YEAR)
                && now.get(Calendar.YEAR) == emailCal.get(Calendar.YEAR) -> {
            SimpleDateFormat("EEEE HH:mm", Locale.ITALIAN).format(date)
        }
        // Stesso anno: giorno e mese
        now.get(Calendar.YEAR) == emailCal.get(Calendar.YEAR) -> {
            SimpleDateFormat("dd MMM, HH:mm", Locale.ITALIAN).format(date)
        }
        // Anno diverso: data completa
        else -> {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN).format(date)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignFolderDialog(
    email: Email,
    onDismiss: () -> Unit,
    onFolderAssigned: (Long?) -> Unit
) {
    val context = LocalContext.current
    val db = remember { SmartMailDatabase.getInstance(context) }

    var selectedFolder by remember { mutableStateOf<com.smartmail.domain.models.SmartFolder?>(null) }
    var smartFolders by remember { mutableStateOf<List<com.smartmail.domain.models.SmartFolder>>(emptyList()) }
    var expandedFolders by remember { mutableStateOf(false) }

    // Carica le cartelle
    LaunchedEffect(Unit) {
        db.smartFolderDao().getAllFolders().collect { folders ->
            smartFolders = folders
            // Seleziona la cartella corrente se esiste
            selectedFolder = folders.find { it.id == email.smartFolderId }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!expandedFolders) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !expandedFolders,
            dismissOnClickOutside = !expandedFolders
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Assegna Cartella")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Sposta questa email in una cartella smart per organizzarla meglio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Dropdown cartelle
                ExposedDropdownMenuBox(
                    expanded = expandedFolders,
                    onExpandedChange = { expandedFolders = it }
                ) {
                    OutlinedTextField(
                        value = selectedFolder?.name ?: "Nessuna cartella",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cartella") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .exposedDropdownSize(),
                        leadingIcon = {
                            if (selectedFolder != null) {
                                Text(
                                    text = selectedFolder!!.icon,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFolders)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedFolders,
                        onDismissRequest = { expandedFolders = false }
                    ) {
                        // Opzione "Nessuna cartella"
                        DropdownMenuItem(
                            text = { Text("Nessuna cartella") },
                            onClick = {
                                selectedFolder = null
                                expandedFolders = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )

                        // Cartelle disponibili
                        smartFolders.forEach { folder ->
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
                                    selectedFolder = folder
                                    expandedFolders = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                if (smartFolders.isEmpty()) {
                    Text(
                        "Nessuna cartella disponibile. Crea cartelle dalle Impostazioni.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Passa il folderId al callback che aggiornerà tramite il ViewModel
                    onFolderAssigned(selectedFolder?.id)
                },
                enabled = true  // Sempre abilitato, anche per rimuovere la cartella
            ) {
                Text("Assegna")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickFilterDialog(
    email: Email,
    onDismiss: () -> Unit,
    onFilterCreated: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { SmartMailDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var filterName by remember { mutableStateOf("Filtro da ${email.fromName ?: email.fromAddress}") }
    var selectedCriteria by remember { mutableStateOf("sender") }
    var selectedFolder by remember { mutableStateOf<com.smartmail.domain.models.SmartFolder?>(null) }
    var smartFolders by remember { mutableStateOf<List<com.smartmail.domain.models.SmartFolder>>(emptyList()) }
    var expandedFolders by remember { mutableStateOf(false) }

    // Carica le cartelle
    LaunchedEffect(Unit) {
        db.smartFolderDao().getAllFolders().collect { folders ->
            smartFolders = folders
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!expandedFolders) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !expandedFolders,
            dismissOnClickOutside = !expandedFolders
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crea Filtro Rapido")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = filterName,
                    onValueChange = { filterName = it },
                    label = { Text("Nome filtro") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Filtra per:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                // Opzioni filtro
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedCriteria == "sender",
                        onClick = { selectedCriteria = "sender" }
                    )
                    Text("Mittente: ${email.fromAddress}")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedCriteria == "domain",
                        onClick = { selectedCriteria = "domain" }
                    )
                    val domain = email.fromAddress.substringAfter("@")
                    Text("Dominio: @$domain")
                }

                Divider()

                Text("Azioni:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                // Dropdown cartelle
                ExposedDropdownMenuBox(
                    expanded = expandedFolders,
                    onExpandedChange = { expandedFolders = it }
                ) {
                    OutlinedTextField(
                        value = selectedFolder?.name ?: "Seleziona cartella",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sposta in") },
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
                        smartFolders.forEach { folder ->
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
                                    selectedFolder = folder
                                    expandedFolders = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        val filterValue = when (selectedCriteria) {
                            "sender" -> email.fromAddress
                            "domain" -> "@${email.fromAddress.substringAfter("@")}"
                            else -> email.fromAddress
                        }

                        val filter = com.smartmail.domain.models.EmailFilter(
                            name = filterName,
                            fromContains = filterValue,
                            moveToSmartFolderId = selectedFolder?.id,
                            isEnabled = true
                        )

                        db.emailFilterDao().insert(filter)
                        onFilterCreated()
                    }
                },
                enabled = filterName.isNotBlank() && selectedFolder != null
            ) {
                Text("Crea Filtro")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
