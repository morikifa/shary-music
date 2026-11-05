package com.example.sharedmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.R
import com.example.sharedmusic.data.model.HistoryEntry
import com.example.sharedmusic.domain.TimeFormat
import com.example.sharedmusic.ui.components.SharyIconButton
import com.example.sharedmusic.ui.components.SharyLogo
import com.example.sharedmusic.ui.components.TrackArtwork
import com.example.sharedmusic.ui.theme.SharyTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * История прослушивания: локальные записи, сгруппированные по дням.
 */
@Composable
fun HistoryScreen(
    entries: List<HistoryEntry>,
    onBack: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SharyIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
                size = 40.dp,
                iconSize = 20.dp
            )

            SharyLogo(size = 26.dp)

            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            )

            if (entries.isNotEmpty()) {
                SharyIconButton(
                    icon = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.history_clear),
                    onClick = { confirmClear = true }
                )
            }
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Column
        }

        val todayLabel = stringResource(R.string.history_today)
        val yesterdayLabel = stringResource(R.string.history_yesterday)
        val grouped = entries.groupBy { dayKey(it.startedAtMs, todayLabel, yesterdayLabel) }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            grouped.forEach { (day, dayEntries) ->
                item(key = "header-$day") {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                items(items = dayEntries, key = { it.id }) { entry ->
                    HistoryRow(entry)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear)) },
            text = { Text(stringResource(R.string.history_clear_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    }
                ) { Text(stringResource(R.string.action_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/** Одна запись истории */
@Composable
private fun HistoryRow(entry: HistoryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArtwork(
            artworkPath = null,
            seed = entry.trackId,
            cornerRadius = 10.dp,
            modifier = Modifier
                .padding(end = 10.dp)
                .size(40.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.trackTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Text(
                text = stringResource(
                    R.string.history_row_subtitle,
                    modeLabel(entry.roomCode),
                    stringResource(
                        R.string.history_duration,
                        TimeFormat.shortDuration(entry.listenedMs)
                    )
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

    }
}

/** Подпись режима прослушивания: «Локально» или «Комната ABC123» */
@Composable
private fun modeLabel(roomCode: String?): String =
    if (roomCode == null) {
        stringResource(R.string.mode_local)
    } else {
        stringResource(R.string.mode_room, roomCode)
    }

/** Ключ группировки: «Сегодня», «Вчера» или дата */
private fun dayKey(timestampMs: Long, todayLabel: String, yesterdayLabel: String): String {
    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L

    return when {
        timestampMs >= now - dayMs -> todayLabel
        timestampMs >= now - 2 * dayMs -> yesterdayLabel
        else -> SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(timestampMs))
    }
}

@Preview(name = "История", showBackground = true)
@Composable
private fun HistoryPreview() {
    SharyTheme {
        HistoryScreen(
            entries = listOf(
                HistoryEntry(
                    id = 1,
                    trackId = "a",
                    trackTitle = "NinjaristicNinja CHU CHU LOVELY",
                    trackArtist = "Ninja",
                    startedAtMs = System.currentTimeMillis(),
                    listenedMs = 55_000,
                    roomCode = "7KX2MQ"
                ),
                HistoryEntry(
                    id = 2,
                    trackId = "b",
                    trackTitle = "Ночной город",
                    trackArtist = "Ambient",
                    startedAtMs = System.currentTimeMillis() - 26 * 60 * 60 * 1000,
                    listenedMs = 180_000
                )
            ),
            onBack = {},
            onClear = {}
        )
    }
}
