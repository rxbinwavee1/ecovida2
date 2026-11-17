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

    suspend fun refresh(isAutoDeleteEnabled: Boolean = true) = withContext(Dispatchers.IO) {
        if (isAutoDeleteEnabled) {
            val deleted = dbHelper.deleteEntriesOlderThan30Days()
            _lastDeletedCount.value = deleted
        } else {
            _lastDeletedCount.value = 0
        }
        val list = dbHelper.getAllEinsatze(isAutoDeleteEnabled)
        _einsatzeFlow.value = list
    }

    suspend fun insertEinsatz(einsatz: Einsatz, isAutoDeleteEnabled: Boolean = true): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insertEinsatz(einsatz)
        refresh(isAutoDeleteEnabled)
        id
    }

    suspend fun insertAll(list: List<Einsatz>, isAutoDeleteEnabled: Boolean = true): Int = withContext(Dispatchers.IO) {
        val count = dbHelper.insertAll(list)
        refresh(isAutoDeleteEnabled)
        count
    }

    suspend fun updateEinsatz(einsatz: Einsatz, isAutoDeleteEnabled: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.updateEinsatz(einsatz)
        refresh(isAutoDeleteEnabled)
        success
    }

    suspend fun deleteEinsatz(id: Long, isAutoDeleteEnabled: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.deleteEinsatz(id)
        refresh(isAutoDeleteEnabled)
        success
    }

    suspend fun clearOldEntries(): Int = withContext(Dispatchers.IO) {
        val deleted = dbHelper.deleteEntriesOlderThan30Days()
        _lastDeletedCount.value = deleted
        refresh(true)
        deleted
    }

    suspend fun purgeAll() = withContext(Dispatchers.IO) {
        dbHelper.deleteAll()
        refresh(false)
    }
}
