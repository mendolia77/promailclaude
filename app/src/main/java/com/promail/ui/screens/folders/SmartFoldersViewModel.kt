package com.smartmail.ui.screens.folders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartmail.data.local.database.SmartMailDatabase
import com.smartmail.domain.models.SmartFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SmartFoldersUiState(
    val folders: List<SmartFolder> = emptyList(),
    val showFolderDialog: Boolean = false,
    val editingFolder: SmartFolder? = null,
    val folderName: String = "",
    val folderIcon: String = "📁",
    val folderColor: String = "#2196F3"
)

class SmartFoldersViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SmartMailDatabase.getInstance(application)
    private val folderDao = database.smartFolderDao()

    private val _uiState = MutableStateFlow(SmartFoldersUiState())
    val uiState: StateFlow<SmartFoldersUiState> = _uiState.asStateFlow()

    init {
        loadFolders()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            folderDao.getAllFolders().collect { folders ->
                _uiState.value = _uiState.value.copy(folders = folders)
            }
        }
    }

    fun showFolderDialog(folder: SmartFolder?) {
        _uiState.value = _uiState.value.copy(
            showFolderDialog = true,
            editingFolder = folder,
            folderName = folder?.name ?: "",
            folderIcon = folder?.icon ?: "📁",
            folderColor = folder?.colorHex ?: "#2196F3"
        )
    }

    fun hideFolderDialog() {
        _uiState.value = _uiState.value.copy(
            showFolderDialog = false,
            editingFolder = null,
            folderName = "",
            folderIcon = "📁",
            folderColor = "#2196F3"
        )
    }

    fun updateFolderName(name: String) {
        _uiState.value = _uiState.value.copy(folderName = name)
    }

    fun updateFolderIcon(icon: String) {
        _uiState.value = _uiState.value.copy(folderIcon = icon)
    }

    fun updateFolderColor(color: String) {
        _uiState.value = _uiState.value.copy(folderColor = color)
    }

    fun saveFolder() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val folder = currentState.editingFolder

            if (folder != null) {
                // Modifica cartella esistente
                val updatedFolder = folder.copy(
                    name = currentState.folderName,
                    icon = currentState.folderIcon,
                    colorHex = currentState.folderColor
                )
                folderDao.update(updatedFolder)
            } else {
                // Crea nuova cartella
                val newFolder = SmartFolder(
                    name = currentState.folderName,
                    icon = currentState.folderIcon,
                    colorHex = currentState.folderColor,
                    rulesJson = """{"type":"manual","rules":[]}"""
                )
                folderDao.insert(newFolder)
            }

            hideFolderDialog()
        }
    }

    fun deleteFolder(folder: SmartFolder) {
        viewModelScope.launch {
            folderDao.delete(folder)
        }
    }
}
