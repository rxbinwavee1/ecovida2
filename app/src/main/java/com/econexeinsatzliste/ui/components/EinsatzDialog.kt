package com.econexeinsatzliste.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.econexeinsatzliste.data.Einsatz

@Composable
fun EinsatzDialog(
    initialEinsatz: Einsatz? = null,
    onDismiss: () -> Unit,
    onSave: (Einsatz) -> Unit
) {
    var datumStr by remember { mutableStateOf(initialEinsatz?.datumStr ?: "") }
    var abZeit by remember { mutableStateOf(initialEinsatz?.abZeit ?: "") }
    var abBahnhof by remember { mutableStateOf(initialEinsatz?.abBahnhof ?: "") }
    var anZeit by remember { mutableStateOf(initialEinsatz?.anZeit ?: "") }
    var anBahnhof by remember { mutableStateOf(initialEinsatz?.anBahnhof ?: "") }
    var tourNr by remember { mutableStateOf(initialEinsatz?.tourNr ?: "") }
    var linie by remember { mutableStateOf(initialEinsatz?.linie ?: "") }
    var betragStr by remember { mutableStateOf(initialEinsatz?.betragEuro?.let { if (it > 0) it.toString().replace(".", ",") else "" } ?: "") }

    val isEditing = initialEinsatz != null && initialEinsatz.id != 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Einsatz bearbeiten" else "Neuen Einsatz eintragen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = datumStr,
                    onValueChange = { datumStr = it },
                    label = { Text("Datum (z.B. Di, 10.11.2026)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = abZeit,
                        onValueChange = { abZeit = it },
                        label = { Text("Ab-Zeit (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = anZeit,
                        onValueChange = { anZeit = it },
                        label = { Text("An-Zeit (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = abBahnhof,
                    onValueChange = { abBahnhof = it },
                    label = { Text("Abfahrts-Bahnhof (z.B. Dinslaken, Bahnhof)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = anBahnhof,
                    onValueChange = { anBahnhof = it },
                    label = { Text("Ankunfts-Bahnhof (z.B. Wesel Bf)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tourNr,
                        onValueChange = { tourNr = it },
                        label = { Text("Tour-Nr") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = linie,
                        onValueChange = { linie = it },
                        label = { Text("Linie (z.B. SB3)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = betragStr,
                    onValueChange = { betragStr = it },
                    label = { Text("Honorar (€)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedMillis = Einsatz.parseDateStrToMillis(datumStr)
                    val parsedEuro = betragStr.replace(",", ".").toDoubleOrNull() ?: 0.0

                    val einsatzToSave = (initialEinsatz ?: Einsatz()).copy(
                        datumStr = datumStr,
                        datumMillis = parsedMillis,
                        abZeit = abZeit,
                        abBahnhof = abBahnhof,
                        anZeit = anZeit,
                        anBahnhof = anBahnhof,
                        tourNr = tourNr,
                        linie = linie,
                        betragEuro = parsedEuro
                    )
                    onSave(einsatzToSave)
                }
            ) {
                Text(
                    text = if (isEditing) "Speichern" else "Hinzufügen",
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
