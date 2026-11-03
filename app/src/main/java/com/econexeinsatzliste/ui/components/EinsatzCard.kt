package com.econexeinsatzliste.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.econexeinsatzliste.data.Einsatz
import com.econexeinsatzliste.data.EinsatzStatus
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EinsatzCard(
    einsatz: Einsatz,
    onEditClick: (Einsatz) -> Unit,
    onDeleteClick: (Einsatz) -> Unit,
    onStatusChange: (Einsatz, EinsatzStatus) -> Unit,
    onRescheduleClick: (Einsatz) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }

    val isCompleted = einsatz.status == EinsatzStatus.ABGESCHLOSSEN

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Checkbox Toggle + Date Badge + Status Badge + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Quick Checkbox Toggle & Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val nextStatus = if (isCompleted) EinsatzStatus.OFFEN else EinsatzStatus.ABGESCHLOSSEN
                            onStatusChange(einsatz, nextStatus)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                            contentDescription = "Abhaken",
                            tint = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Date Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = einsatz.datumStr.ifBlank { "Kein Datum" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }

                    // Status Chip with Menu Selector
                    Box {
                        StatusChip(
                            status = einsatz.status,
                            onClick = { statusMenuExpanded = true }
                        )

                        DropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false }
                        ) {
                            EinsatzStatus.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.label) },
                                    onClick = {
                                        statusMenuExpanded = false
                                        if (status == EinsatzStatus.VERSCHOBEN) {
                                            onRescheduleClick(einsatz)
                                        } else {
                                            onStatusChange(einsatz, status)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Actions Menu
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Aktionen"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Datum verschieben") },
                            onClick = {
                                menuExpanded = false
                                onRescheduleClick(einsatz)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bearbeiten") },
                            onClick = {
                                menuExpanded = false
                                onEditClick(einsatz)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Löschen") },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick(einsatz)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Route: Abfahrt -> Ankunft
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Abfahrt
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Abfahrt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = einsatz.abZeit.ifBlank { "--:--" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                    Text(
                        text = einsatz.abBahnhof.ifBlank { "Unbekannter Ab-Bhf" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "nach",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Ankunft
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ankunft",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = einsatz.anZeit.ifBlank { "--:--" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                    Text(
                        text = einsatz.anBahnhof.ifBlank { "Unbekannter An-Bhf" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Chips: Tour-Nr | Linie | Honorar (€)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (einsatz.tourNr.isNotBlank()) {
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                text = "Tour-Nr: ${einsatz.tourNr}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                if (einsatz.linie.isNotBlank()) {
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                text = "Linie: ${einsatz.linie}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                if (einsatz.betragEuro > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = String.format(Locale.GERMANY, "Honorar: %.2f €", einsatz.betragEuro),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: EinsatzStatus,
    onClick: () -> Unit
) {
    val (bgColor, textColor, icon) = when (status) {
        EinsatzStatus.OFFEN -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), Icons.Default.Schedule)
        EinsatzStatus.ABGESCHLOSSEN -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.CheckCircle)
        EinsatzStatus.ABGEBROCHEN -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Icons.Default.Cancel)
        EinsatzStatus.VERSCHOBEN -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Icons.Default.Schedule)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
