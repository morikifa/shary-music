package com.example.sharedmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.BuildConfig
import com.example.sharedmusic.R
import com.example.sharedmusic.data.model.ThemeMode
import com.example.sharedmusic.data.model.UserSettings
import com.example.sharedmusic.ui.components.SharyIconButton
import com.example.sharedmusic.ui.components.SharyTopBar
import com.example.sharedmusic.ui.theme.SharyPink
import com.example.sharedmusic.ui.theme.SharyTheme

/**
 * Настройки приложения.
 *
 * Первый блок — эквалайзер: включение и четыре полосы. Второй — экономия
 * трафика при загрузке треков комнаты. Третий — тема оформления.
 */
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onBack: () -> Unit,
    onToggleEqualizer: (Boolean) -> Unit,
    onChangeEqualizerBand: (index: Int, decibels: Float) -> Unit,
    onToggleWifiOnly: (Boolean) -> Unit,
    onChangeTheme: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SharyIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
                size = 40.dp,
                iconSize = 20.dp
            )

            SharyTopBar(
                modifier = Modifier.weight(1f),
                logoSize = 26.dp,
                title = stringResource(R.string.settings_title)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- Эквалайзер ----------
        SectionTitle(stringResource(R.string.settings_section_playback))

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.Rounded.Equalizer)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_equalizer),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (settings.equalizerEnabled) {
                            stringResource(R.string.settings_equalizer_enabled)
                        } else {
                            stringResource(R.string.settings_equalizer_disabled)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = settings.equalizerEnabled,
                    onCheckedChange = onToggleEqualizer,
                    colors = SwitchDefaults.colors(checkedTrackColor = SharyPink)
                )
            }

            if (settings.equalizerEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                val labels = listOf(
                    stringResource(R.string.settings_band_bass),
                    stringResource(R.string.settings_band_mid),
                    stringResource(R.string.settings_band_treble),
                    stringResource(R.string.settings_band_vocal)
                )

                labels.forEachIndexed { index, label ->
                    EqualizerBand(
                        label = label,
                        value = settings.equalizerBands.getOrElse(index) { 0f },
                        onChange = { decibels -> onChangeEqualizerBand(index, decibels) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Сеть ----------
        SectionTitle(stringResource(R.string.settings_section_network))

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.Rounded.Wifi)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_wifi_only),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_wifi_only_description),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = settings.downloadOverWifiOnly,
                    onCheckedChange = onToggleWifiOnly,
                    colors = SwitchDefaults.colors(checkedTrackColor = SharyPink)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Тема ----------
        SectionTitle(stringResource(R.string.settings_section_appearance))

        SettingsCard {
            ThemeOption(
                icon = Icons.Rounded.DarkMode,
                label = stringResource(R.string.settings_theme_dark),
                selected = settings.themeMode == ThemeMode.DARK,
                onClick = { onChangeTheme(ThemeMode.DARK) }
            )

            ThemeOption(
                icon = Icons.Rounded.LightMode,
                label = stringResource(R.string.settings_theme_light),
                selected = settings.themeMode == ThemeMode.LIGHT,
                onClick = { onChangeTheme(ThemeMode.LIGHT) }
            )

            ThemeOption(
                icon = Icons.Rounded.DarkMode,
                label = stringResource(R.string.settings_theme_system),
                selected = settings.themeMode == ThemeMode.SYSTEM,
                onClick = { onChangeTheme(ThemeMode.SYSTEM) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionTitle(stringResource(R.string.settings_about))

        Text(
            text = "SHARY · ${stringResource(R.string.settings_version, BuildConfig.VERSION_NAME)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
    }
}

/** Подпись группы настроек */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

/** Карточка с настройками */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        content()
    }
}

/** Круглая розовая иконка настройки */
@Composable
private fun RoundIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(SharyPink, SharyPink.copy(alpha = 0.55f)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Одна полоса эквалайзера: -12..+12 дБ */
@Composable
private fun EqualizerBand(
    label: String,
    value: Float,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${value.toInt()} дБ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = -12f..12f,
            steps = 23,
            colors = SliderDefaults.colors(
                thumbColor = SharyPink,
                activeTrackColor = SharyPink,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

/** Вариант темы: точка выбора и подпись */
@Composable
private fun ThemeOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) SharyPink else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )

        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    if (selected) SharyPink else MaterialTheme.colorScheme.surfaceVariant
                )
        )
    }
}

@Preview(name = "Настройки", showBackground = true)
@Composable
private fun SettingsPreview() {
    SharyTheme {
        SettingsScreen(
            settings = UserSettings(
                equalizerEnabled = true,
                equalizerBands = listOf(4f, -2f, 6f, 0f)
            ),
            onBack = {},
            onToggleEqualizer = {},
            onChangeEqualizerBand = { _, _ -> },
            onToggleWifiOnly = {},
            onChangeTheme = {}
        )
    }
}
