package com.econexeinsatzliste.ui.components

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.econexeinsatzliste.data.Einsatz
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun RescheduleDialog(
    einsatz: Einsatz,
    onDismiss: () -> Unit,
    onConfirmReschedule: (newDateStr: String) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(einsatz.id) {
        val calendar = Calendar.getInstance().apply {
            if (einsatz.datumMillis > 0L) {
                timeInMillis = einsatz.datumMillis
            }
        }

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val germanFormat = SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY)
                val formattedDate = germanFormat.format(selectedCal.time)
                onConfirmReschedule(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.setOnCancelListener {
            onDismiss()
        }

        datePickerDialog.setOnDismissListener {
            // Dismissed
        }

        datePickerDialog.show()
    }
}
