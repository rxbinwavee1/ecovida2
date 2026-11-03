package com.econexeinsatzliste.ui

import com.econexeinsatzliste.data.Einsatz
import com.econexeinsatzliste.data.EinsatzStatus

enum class EinsatzTab(val label: String) {
    AKTIV("Aktive Einsätze"),
    ARCHIV("Archiv")
}

data class EinsatzUiState(
    val einsatze: List<Einsatz> = emptyList(),
    val filteredEinsatze: List<Einsatz> = emptyList(),
    val activeTab: EinsatzTab = EinsatzTab.AKTIV,
    val isDarkTheme: Boolean = true,
    val isLoading: Boolean = false,
    val isParsingScreenshot: Boolean = false,
    val isParsingCsv: Boolean = false,
    val parsedPreviewList: List<Einsatz>? = null,
    val searchQuery: String = "",
    val selectedStatusFilter: EinsatzStatus? = null,
    val totalEarnings: Double = 0.0,
    val autoDeletedCount: Int = 0,
    val userMessage: String? = null
)
