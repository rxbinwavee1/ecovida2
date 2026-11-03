package com.econexeinsatzliste.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.econexeinsatzliste.data.Einsatz

@Composable
fun RescheduleDialog(
    einsatz: Einsatz,
    onDismiss: () -> Unit,
    onConfirmReschedule: (newDateStr: String) -> Unit
) {
    var newDateStr by remember { mutableStateOf(einsatz.datumStr) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Einsatz verschieben",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Auf welches Datum soll der Einsatz verschoben werden?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newDateStr,
                    onValueChange = { newDateStr = it },
                    label = { Text("Neues Datum (z.B. Fr, 20.11.2026)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmReschedule(newDateStr)
                    onDismiss()
                },
                enabled = newDateStr.isNotBlank()
            ) {
                Text(
                    text = "Verschieben & Anpassen",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
