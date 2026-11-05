package com.econexeinsatzliste.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.econexeinsatzliste.ai.EinsatzCsvParser
import com.econexeinsatzliste.ai.EinsatzScreenshotParser
import com.econexeinsatzliste.data.Einsatz
import com.econexeinsatzliste.data.EinsatzRepository
import com.econexeinsatzliste.data.EinsatzStatus
import com.econexeinsatzliste.notification.EinsatzNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EinsatzViewModel(private val repository: EinsatzRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(EinsatzUiState())
    val uiState: StateFlow<EinsatzUiState> = _uiState.asStateFlow()

    init {
        loadEinsatze()
    }

    fun loadEinsatze() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.refresh()
            repository.einsatzeFlow.collect { list ->
                val autoDeleted = repository.lastDeletedCount.value
                val totalSum = list.filter { it.status != EinsatzStatus.ABGEBROCHEN }
                    .sumOf { it.betragEuro }
                _uiState.update { state ->
                    val filtered = applyFilter(list, state.activeTab, state.searchQuery, state.selectedStatusFilter)
                    state.copy(
                        einsatze = list,
                        filteredEinsatze = filtered,
                        isLoading = false,
                        totalEarnings = totalSum,
                        autoDeletedCount = autoDeleted
                    )
                }
            }
        }
    }

    fun toggleDarkTheme() {
        _uiState.update { state ->
            val nextTheme = !state.isDarkTheme
            state.copy(
                isDarkTheme = nextTheme,
                userMessage = if (nextTheme) "Dark Mode aktiviert" else "Light Mode aktiviert"
            )
        }
    }

    fun setActiveTab(tab: EinsatzTab) {
        _uiState.update { state ->
            val filtered = applyFilter(state.einsatze, tab, state.searchQuery, null)
            state.copy(activeTab = tab, selectedStatusFilter = null, filteredEinsatze = filtered)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.einsatze, state.activeTab, query, state.selectedStatusFilter)
            state.copy(searchQuery = query, filteredEinsatze = filtered)
        }
    }

    fun setStatusFilter(status: EinsatzStatus?) {
        _uiState.update { state ->
            val filtered = applyFilter(state.einsatze, state.activeTab, state.searchQuery, status)
            state.copy(selectedStatusFilter = status, filteredEinsatze = filtered)
        }
    }

    private fun applyFilter(
        list: List<Einsatz>,
        tab: EinsatzTab,
        query: String,
        statusFilter: EinsatzStatus?
    ): List<Einsatz> {
        var result = when (tab) {
            EinsatzTab.AKTIV -> list.filter { it.status != EinsatzStatus.ABGESCHLOSSEN }
            EinsatzTab.ARCHIV -> list.filter { it.status == EinsatzStatus.ABGESCHLOSSEN }
        }

        if (statusFilter != null) {
            result = result.filter { it.status == statusFilter }
        }

        if (query.isNotBlank()) {
            val q = query.lowercase().trim()
            result = result.filter {
                it.datumStr.lowercase().contains(q) ||
                        it.abBahnhof.lowercase().contains(q) ||
                        it.anBahnhof.lowercase().contains(q) ||
                        it.tourNr.lowercase().contains(q) ||
                        it.linie.lowercase().contains(q)
            }
        }

        return result.sortedWith(compareBy({ it.getEffectiveMillis() }, { it.abZeit }))
    }

    fun analyzeScreenshot(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isParsingScreenshot = true) }
            val parsedList = EinsatzScreenshotParser.parseScreenshot(context, uri)
            _uiState.update { state ->
                if (parsedList.isEmpty()) {
                    state.copy(
                        isParsingScreenshot = false,
                        userMessage = "Keine Einsätze auf dem Screenshot gefunden."
                    )
                } else {
                    state.copy(
                        isParsingScreenshot = false,
                        parsedPreviewList = parsedList
                    )
                }
            }
        }
    }

    fun analyzeCsvUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isParsingCsv = true) }
            val parsedList = EinsatzCsvParser.parseCsvFromUri(context, uri)
            _uiState.update { state ->
                if (parsedList.isEmpty()) {
                    state.copy(
                        isParsingCsv = false,
                        userMessage = "Keine Einsätze in der CSV-Datei gefunden."
                    )
                } else {
                    state.copy(
                        isParsingCsv = false,
                        parsedPreviewList = parsedList
                    )
                }
            }
        }
    }

    fun analyzeCsvText(csvText: String) {
        viewModelScope.launch {
            val parsedList = EinsatzCsvParser.parseCsvText(csvText)
            _uiState.update { state ->
                if (parsedList.isEmpty()) {
                    state.copy(
                        userMessage = "Keine Einsätze aus dem CSV-Text gelesen."
                    )
                } else {
                    state.copy(
                        parsedPreviewList = parsedList
                    )
                }
            }
        }
    }

    fun dismissParsedPreview() {
        _uiState.update { it.copy(parsedPreviewList = null) }
    }

    fun confirmParsedEinsatze(context: Context, confirmedList: List<Einsatz>) {
        viewModelScope.launch {
            if (confirmedList.isNotEmpty()) {
                val addedCount = repository.insertAll(confirmedList)
                confirmedList.forEach { einsatz ->
                    if (einsatz.status != EinsatzStatus.ABGESCHLOSSEN) {
                        EinsatzNotificationHelper.scheduleEveningReminder(context, einsatz)
                    }
                }
                _uiState.update { state ->
                    state.copy(
                        parsedPreviewList = null,
                        userMessage = "$addedCount Einsatz/Einsätze hinzugefügt & Erinnerung (19 Uhr) aktiviert!"
                    )
                }
            } else {
                _uiState.update { it.copy(parsedPreviewList = null) }
            }
        }
    }

    fun toggleEinsatzStatus(context: Context, einsatz: Einsatz, newStatus: EinsatzStatus) {
        viewModelScope.launch {
            val updated = einsatz.copy(status = newStatus)
            repository.updateEinsatz(updated)

            if (newStatus == EinsatzStatus.ABGESCHLOSSEN) {
                EinsatzNotificationHelper.cancelEveningReminder(context, einsatz.id)
                _uiState.update {
                    it.copy(userMessage = "Einsatz abgeschlossen ➔ Ins Archiv verschoben!")
                }
            } else {
                EinsatzNotificationHelper.scheduleEveningReminder(context, updated)
                _uiState.update {
                    it.copy(userMessage = "Status geändert: ${newStatus.label}")
                }
            }
        }
    }

    fun rescheduleEinsatz(context: Context, einsatz: Einsatz, newDateStr: String) {
        viewModelScope.launch {
            val newMillis = Einsatz.parseDateStrToMillis(newDateStr)
            val updated = einsatz.copy(
                datumStr = newDateStr,
                datumMillis = newMillis,
                status = EinsatzStatus.VERSCHOBEN
            )
            repository.updateEinsatz(updated)
            EinsatzNotificationHelper.scheduleEveningReminder(context, updated)
            _uiState.update {
                it.copy(userMessage = "Einsatz auf $newDateStr verschoben & automatisch neu eingeordnet!")
            }
        }
    }

    fun saveEinsatz(context: Context, einsatz: Einsatz) {
        viewModelScope.launch {
            if (einsatz.id == 0L) {
                val id = repository.insertEinsatz(einsatz)
                val created = einsatz.copy(id = id)
                if (created.status != EinsatzStatus.ABGESCHLOSSEN) {
                    EinsatzNotificationHelper.scheduleEveningReminder(context, created)
                }
                _uiState.update { it.copy(userMessage = "Einsatz hinzugefügt!") }
            } else {
                repository.updateEinsatz(einsatz)
                if (einsatz.status != EinsatzStatus.ABGESCHLOSSEN) {
                    EinsatzNotificationHelper.scheduleEveningReminder(context, einsatz)
                } else {
                    EinsatzNotificationHelper.cancelEveningReminder(context, einsatz.id)
                }
                _uiState.update { it.copy(userMessage = "Einsatz aktualisiert!") }
            }
        }
    }

    fun deleteEinsatz(context: Context, id: Long) {
        viewModelScope.launch {
            EinsatzNotificationHelper.cancelEveningReminder(context, id)
            repository.deleteEinsatz(id)
            _uiState.update { it.copy(userMessage = "Einsatz gelöscht.") }
        }
    }

    fun purgeAll(context: Context) {
        viewModelScope.launch {
            _uiState.value.einsatze.forEach { einsatz ->
                EinsatzNotificationHelper.cancelEveningReminder(context, einsatz.id)
            }
            repository.purgeAll()
            _uiState.update { it.copy(userMessage = "Alle Einsätze wurden gelöscht.") }
        }
    }

    fun clear30DaysOld() {
        viewModelScope.launch {
            val count = repository.clearOldEntries()
            _uiState.update {
                it.copy(
                    userMessage = if (count > 0) "$count alte Einsätze (älter als 30 Tage) gelöscht." else "Keine Einsätze älter als 30 Tage vorhanden."
                )
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = EinsatzRepository(context)
            return EinsatzViewModel(repository) as T
        }
    }
}
