package com.example.sharedmusic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.R

/**
 * Поиск по библиотеке: лупа в шапке, раскрывающееся поле ввода.
 *
 * Пока поиск закрыт, компонент занимает ровно размер кнопки. Иначе строка
 * шапки получала нулевую ширину при `weight(0f)` и лупа выталкивала логотип
 * и название за край экрана.
 */
@Composable
fun SearchBar(
    query: String,
    isActive: Boolean,
    onQueryChange: (String) -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(isActive) {
        if (isActive) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !isActive,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SharyIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.action_search),
                onClick = onOpen,
                inactiveColor = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn() + slideInHorizontally { it / 2 },
            exit = fadeOut() + slideOutHorizontally { it / 2 }
        ) {
            Row(
                modifier = Modifier
                    .widthIn(min = 220.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.merge(
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(
                            MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { keyboard?.hide() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )

                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.library_search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SharyIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_close),
                    onClick = {
                        onQueryChange("")
                        keyboard?.hide()
                        onClose()
                    },
                    size = 32.dp,
                    iconSize = 18.dp
                )
            }
        }
    }
}
