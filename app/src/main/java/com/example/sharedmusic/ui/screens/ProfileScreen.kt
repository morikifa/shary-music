package com.example.sharedmusic.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sharedmusic.R
import com.example.sharedmusic.data.model.UserProfile
import com.example.sharedmusic.ui.components.SharyIconButton
import com.example.sharedmusic.ui.components.SharyLogo
import com.example.sharedmusic.ui.theme.SharyPink
import com.example.sharedmusic.ui.theme.SharyTheme

/**
 * Профиль: фото, имя, ID, статистика и выход.
 *
 * Фото выбирается системным пикером изображений — широкие разрешения
 * на хранилище не запрашиваются.
 */
@Composable
fun ProfileScreen(
    profile: UserProfile,
    trackCount: Int,
    likedCount: Int,
    roomHours: String,
    onBack: () -> Unit,
    onSaveName: (String) -> Unit,
    onSaveAvatar: (String?) -> Unit,
    onOpenHistory: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var nameInput by remember(profile.displayName) { mutableStateOf(profile.displayName) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSaveAvatar(uri.toString())
        }
    }

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

            SharyLogo(size = 26.dp)

            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Аватар и имя
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(SharyPink, SharyPink.copy(alpha = 0.5f)))
                        )
                        .clickable {
                            photoPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.avatarPath != null) {
                        AsyncImage(
                            model = profile.avatarPath,
                            contentDescription = stringResource(R.string.cd_avatar),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = stringResource(R.string.cd_avatar),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { showNameDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.profile_edit_name),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Text(
                text = profile.displayName.ifBlank {
                    stringResource(R.string.onboarding_title)
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )

            Text(
                text = "${stringResource(R.string.profile_id)}: ${profile.shortId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Статистика
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                value = trackCount.toString(),
                label = stringResource(R.string.profile_tracks_count),
                modifier = Modifier.weight(1f)
            )

            StatCard(
                value = likedCount.toString(),
                label = stringResource(R.string.profile_liked_count),
                modifier = Modifier.weight(1f)
            )

            StatCard(
                value = stringResource(R.string.profile_stat_hours, roomHours),
                label = stringResource(R.string.profile_room_hours),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProfileRow(
            icon = Icons.Rounded.Edit,
            title = stringResource(R.string.profile_edit_name),
            onClick = { showNameDialog = true }
        )

        ProfileRow(
            icon = Icons.Rounded.PhotoCamera,
            title = stringResource(R.string.profile_edit_photo),
            onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )

        // Удалить фото можно только когда оно есть
        if (profile.avatarPath != null) {
            ProfileRow(
                icon = Icons.Rounded.Delete,
                title = stringResource(R.string.profile_remove_photo),
                onClick = { onSaveAvatar(null) }
            )
        }

        ProfileRow(
            icon = Icons.Rounded.History,
            title = stringResource(R.string.history_title),
            onClick = onOpenHistory
        )

        ProfileRow(
            icon = Icons.Rounded.Logout,
            title = stringResource(R.string.profile_sign_out),
            onClick = { showSignOutDialog = true },
            accent = true
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.profile_edit_name)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.take(32) },
                        label = { Text(stringResource(R.string.profile_name_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (nameInput.isBlank()) {
                        Text(
                            text = stringResource(R.string.profile_name_empty),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = nameInput.isNotBlank(),
                    onClick = {
                        onSaveName(nameInput.trim())
                        showNameDialog = false
                    }
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.profile_sign_out)) },
            text = { Text(stringResource(R.string.profile_sign_out_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    }
                ) { Text(stringResource(R.string.profile_sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/** Плитка статистики */
@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Строка действия в профиле */
@Composable
private fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    accent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (accent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (accent) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Preview(name = "Профиль", showBackground = true)
@Composable
private fun ProfilePreview() {
    SharyTheme {
        ProfileScreen(
            profile = UserProfile(uid = "abc123def456", displayName = "Морикита"),
            trackCount = 24,
            likedCount = 7,
            roomHours = "3.5",
            onBack = {},
            onSaveName = {},
            onSaveAvatar = {},
            onOpenHistory = {},
            onSignOut = {}
        )
    }
}
