package com.smartmail.ui.screens.filters

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartmail.data.local.database.SmartMailDatabase
import com.smartmail.domain.models.EmailFilter
import com.smartmail.domain.models.SmartFolder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FiltersUiState(
    val filters: List<EmailFilter> = emptyList(),
    val smartFolders: List<SmartFolder> = emptyList(),
    val showFilterDialog: Boolean = false,
    val editingFilter: EmailFilter? = null,
    // Form campi
    val filterName: String = "",
    val filterFromContains: String? = null,
    val filterSubjectContains: String? = null,
    val filterHasAttachments: Boolean? = null,
    val filterMoveToSmartFolderId: Long? = null,
    val filterMarkAsStarred: Boolean = false,
    val filterMarkAsImportant: Boolean = false,
    val filterMarkAsRead: Boolean = false
)

class FiltersViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SmartMailDatabase.getInstance(application)
    private val filterDao = db.emailFilterDao()
    private val smartFolderDao = db.smartFolderDao()

    private val _uiState = MutableStateFlow(FiltersUiState())
    val uiState: StateFlow<FiltersUiState> = _uiState.asStateFlow()

    init {
        loadFilters()
        loadSmartFolders()
    }

    private fun loadSmartFolders() {
        viewModelScope.launch {
            smartFolderDao.getAllFolders().collect { folders ->
                _uiState.update { it.copy(smartFolders = folders) }
            }
        }
    }

    private fun loadFilters() {
        viewModelScope.launch {
            filterDao.getAllFilters().collect { filters ->
                _uiState.update { it.copy(filters = filters) }
            }
        }
    }

    fun showAddFilter() {
        _uiState.update {
            it.copy(
                showFilterDialog = true,
                editingFilter = null,
                filterName = "",
                filterFromContains = null,
                filterSubjectContains = null,
                filterHasAttachments = null,
                filterMoveToSmartFolderId = null,
                filterMarkAsStarred = false,
                filterMarkAsImportant = false,
                filterMarkAsRead = false
            )
        }
    }

    fun editFilter(filter: EmailFilter) {
        _uiState.update {
            it.copy(
                showFilterDialog = true,
                editingFilter = filter,
                filterName = filter.name,
                filterFromContains = filter.fromContains,
                filterSubjectContains = filter.subjectContains,
                filterHasAttachments = filter.hasAttachments,
                filterMoveToSmartFolderId = filter.moveToSmartFolderId,
                filterMarkAsStarred = filter.markAsStarred,
                filterMarkAsImportant = filter.markAsImportant,
                filterMarkAsRead = filter.markAsRead
            )
        }
    }

    fun hideFilterDialog() {
        _uiState.update { it.copy(showFilterDialog = false, editingFilter = null) }
    }

    fun updateFilterName(name: String) {
        _uiState.update { it.copy(filterName = name) }
    }

    fun updateFromContains(value: String) {
        _uiState.update { it.copy(filterFromContains = value.ifBlank { null }) }
    }

    fun updateSubjectContains(value: String) {
        _uiState.update { it.copy(filterSubjectContains = value.ifBlank { null }) }
    }

    fun updateHasAttachments(value: Boolean) {
        _uiState.update { it.copy(filterHasAttachments = if (value) true else null) }
    }

    fun updateMoveToSmartFolder(folderId: Long?) {
        _uiState.update { it.copy(filterMoveToSmartFolderId = folderId) }
    }

    fun updateMarkAsStarred(value: Boolean) {
        _uiState.update { it.copy(filterMarkAsStarred = value) }
    }

    fun updateMarkAsImportant(value: Boolean) {
        _uiState.update { it.copy(filterMarkAsImportant = value) }
    }

    fun updateMarkAsRead(value: Boolean) {
        _uiState.update { it.copy(filterMarkAsRead = value) }
    }

    fun saveFilter() {
        viewModelScope.launch {
            val state = _uiState.value
            val filter = if (state.editingFilter != null) {
                // Modifica filtro esistente
                state.editingFilter.copy(
                    name = state.filterName,
                    fromContains = state.filterFromContains,
                    subjectContains = state.filterSubjectContains,
                    hasAttachments = state.filterHasAttachments,
                    moveToSmartFolderId = state.filterMoveToSmartFolderId,
                    markAsStarred = state.filterMarkAsStarred,
                    markAsImportant = state.filterMarkAsImportant,
                    markAsRead = state.filterMarkAsRead
                )
            } else {
                // Nuovo filtro
                EmailFilter(
                    name = state.filterName,
                    fromContains = state.filterFromContains,
                    subjectContains = state.filterSubjectContains,
                    hasAttachments = state.filterHasAttachments,
                    moveToSmartFolderId = state.filterMoveToSmartFolderId,
                    markAsStarred = state.filterMarkAsStarred,
                    markAsImportant = state.filterMarkAsImportant,
                    markAsRead = state.filterMarkAsRead
                )
            }

            filterDao.insert(filter)
            hideFilterDialog()
        }
    }

    fun toggleFilter(filterId: Long) {
        viewModelScope.launch {
            val filter = filterDao.getById(filterId)
            if (filter != null) {
                filterDao.update(filter.copy(isEnabled = !filter.isEnabled))
            }
        }
    }

    fun deleteFilter(filterId: Long) {
        viewModelScope.launch {
            filterDao.deleteById(filterId)
        }
    }
}
