package com.example.sharedmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.R
import com.example.sharedmusic.core.SharyConfig
import com.example.sharedmusic.domain.RoomCode
import com.example.sharedmusic.ui.components.SharyIconButton
import com.example.sharedmusic.ui.components.SharyLogo
import com.example.sharedmusic.ui.theme.SharyPink
import com.example.sharedmusic.ui.theme.SharyTheme

/**
 * Экран «Комната»: создание новой комнаты или вход по коду.
 *
 * Показывается, если пользователь ещё не в комнате.
 * Минимум полей: код или параметры комнаты и пароль.
 */
@Composable
fun RoomEntryScreen(
    isLoading: Boolean,
    notice: String?,
    noticeDetails: String?,
    onCreateRoom: (capacity: Int, password: String) -> Unit,
    onJoinRoom: (code: String, password: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var capacity by remember { mutableStateOf(SharyConfig.DEFAULT_ROOM_CAPACITY) }
    var createPassword by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var joinPassword by remember { mutableStateOf("") }
    var showJoin by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
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

            Spacer(modifier = Modifier.weight(1f))

            SharyLogo(size = 30.dp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.room_entry_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = stringResource(R.string.room_entry_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // ---------- Создание комнаты ----------
        AnimatedVisibility(visible = !showJoin) {
            Column {
                CardBlock {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Groups)

                        Text(
                            text = stringResource(R.string.room_create_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.room_capacity_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (value in SharyConfig.MIN_ROOM_CAPACITY..SharyConfig.MAX_ROOM_CAPACITY) {
                            CapacityChip(
                                value = value,
                                selected = capacity == value,
                                onClick = { capacity = value },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = createPassword,
                        onValueChange = { createPassword = it.take(32) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.room_password_optional)) },
                        leadingIcon = {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PrimaryButton(
                        text = stringResource(R.string.room_create),
                        enabled = !isLoading,
                        loading = isLoading,
                        onClick = { onCreateRoom(capacity, createPassword) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                SecondaryButton(
                    text = stringResource(R.string.room_join),
                    icon = Icons.Rounded.Key,
                    enabled = !isLoading,
                    onClick = { showJoin = true }
                )
            }
        }

        // ---------- Вход по коду ----------
        AnimatedVisibility(visible = showJoin) {
            Column {
                CardBlock {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Key)

                        Text(
                            text = stringResource(R.string.room_join_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = RoomCode.normalize(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.room_code_label)) },
                        placeholder = { Text(stringResource(R.string.room_code_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(16.dp),
                        supportingText = {
                            if (codeInput.isNotEmpty() && !RoomCode.isValid(codeInput)) {
                                Text(
                                    text = stringResource(R.string.room_code_invalid),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = joinPassword,
                        onValueChange = { joinPassword = it.take(32) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.room_password_label)) },
                        leadingIcon = {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PrimaryButton(
                        text = stringResource(R.string.room_join),
                        enabled = !isLoading && RoomCode.isValid(codeInput),
                        loading = isLoading,
                        onClick = { onJoinRoom(codeInput, joinPassword) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                SecondaryButton(
                    text = stringResource(R.string.room_create),
                    icon = Icons.Rounded.Groups,
                    enabled = !isLoading,
                    onClick = { showJoin = false }
                )
            }
        }

        if (notice != null) {
            Text(
                text = notice,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            // Техническая причина: по ней видно, виновата сеть, адрес базы
            // или правила Realtime Database в консоли Firebase
            Text(
                text = stringResource(
                    R.string.room_error_details,
                    noticeDetails ?: stringResource(R.string.room_error_details_unknown)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 8.dp, end = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Карточка-блок формы */
@Composable
private fun CardBlock(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        content()
    }
}

/** Круглая розовая иконка-акцент */
@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(SharyPink, SharyPink.copy(alpha = 0.6f)))),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(17.dp)
        )
    }
}

/** Выбор вместимости комнаты */
@Composable
private fun CapacityChip(
    value: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.room_capacity_value, value),
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/** Основная розовая кнопка */
@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (enabled) {
                    Brush.linearGradient(listOf(SharyPink, SharyPink.copy(alpha = 0.8f)))
                } else {
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/** Второстепенная кнопка с иконкой */
@Composable
private fun SecondaryButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Preview(name = "Вход в комнату", showBackground = true)
@Composable
private fun RoomEntryPreview() {
    SharyTheme {
        RoomEntryScreen(
            isLoading = false,
            notice = null,
            noticeDetails = null,
            onCreateRoom = { _, _ -> },
            onJoinRoom = { _, _ -> },
            onBack = {}
        )
    }
}
