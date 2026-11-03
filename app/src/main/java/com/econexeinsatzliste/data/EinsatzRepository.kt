package com.econexeinsatzliste.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class EinsatzRepository(context: Context) {
    private val dbHelper = EconexDatabaseHelper(context.applicationContext)

    private val _einsatzeFlow = MutableStateFlow<List<Einsatz>>(emptyList())
    val einsatzeFlow: StateFlow<List<Einsatz>> = _einsatzeFlow.asStateFlow()

    private val _lastDeletedCount = MutableStateFlow(0)
    val lastDeletedCount: StateFlow<Int> = _lastDeletedCount.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val deleted = dbHelper.deleteEntriesOlderThan30Days()
        _lastDeletedCount.value = deleted
        val list = dbHelper.getAllEinsatze()
        _einsatzeFlow.value = list
    }

    suspend fun insertEinsatz(einsatz: Einsatz): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insertEinsatz(einsatz)
        refresh()
        id
    }

    suspend fun insertAll(list: List<Einsatz>): Int = withContext(Dispatchers.IO) {
        val count = dbHelper.insertAll(list)
        refresh()
        count
    }

    suspend fun updateEinsatz(einsatz: Einsatz): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.updateEinsatz(einsatz)
        refresh()
        success
    }

    suspend fun deleteEinsatz(id: Long): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.deleteEinsatz(id)
        refresh()
        success
    }

    suspend fun clearOldEntries(): Int = withContext(Dispatchers.IO) {
        val deleted = dbHelper.deleteEntriesOlderThan30Days()
        _lastDeletedCount.value = deleted
        refresh()
        deleted
    }

    suspend fun purgeAll() = withContext(Dispatchers.IO) {
        dbHelper.deleteAll()
        refresh()
    }
}
