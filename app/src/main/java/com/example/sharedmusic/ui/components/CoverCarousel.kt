package com.example.sharedmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.R
import com.example.sharedmusic.data.model.Track
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

/**
 * Горизонтальная лента обложек.
 *
 * Центральная карточка крупнее остальных — её трек готов к запуску.
 * Прокрутка «прилипает» к карточкам, поэтому крайние обложки тоже можно
 * вывести в центр, а не оставить урезанными у края экрана.
 *
 * Трек запускается по нажатию или когда прокрутка остановилась на карточке.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoverCarousel(
    tracks: List<Track>,
    centerTrackId: String?,
    isPlaying: Boolean,
    onTrackSelect: (Track) -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 132.dp
) {
    if (tracks.isEmpty()) return

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val configuration = LocalConfiguration.current
    val sidePadding = ((configuration.screenWidthDp.dp - cardWidth) / 2)
        .coerceAtLeast(0.dp)

    // Держим выбранный трек по центру, когда он изменился извне
    LaunchedEffect(centerTrackId) {
        val index = tracks.indexOfFirst { it.id == centerTrackId }
        if (index >= 0 && listState.firstVisibleItemIndex != index) {
            listState.animateScrollToItem(index)
        }
    }

    // Прокрутка остановилась — центральный трек становится текущим
    LaunchedEffect(listState, tracks) {
        snapshotFlow {
            listState.isScrollInProgress to centerIndex(listState.layoutInfo)
        }
            .distinctUntilChanged()
            .filter { (scrolling, _) -> !scrolling }
            .collect { (_, index) ->
                val track = tracks.getOrNull(index) ?: return@collect
                if (track.id != centerTrackId) {
                    onTrackSelect(track)
                }
            }
    }

    val carouselDescription = stringResource(R.string.cd_cover_slider)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardWidth + 52.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(horizontal = sidePadding),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = carouselDescription }
            ) {
                itemsIndexed(
                    items = tracks,
                    key = { _, track -> track.id }
                ) { _, track ->
                    val isCenter = track.id == centerTrackId

                    val scale by animateFloatAsState(
                        targetValue = if (isCenter) 1f else 0.84f,
                        label = "cover-scale"
                    )

                    val alpha by animateFloatAsState(
                        targetValue = if (isCenter) 1f else 0.6f,
                        label = "cover-alpha"
                    )

                    val borderColor by animateColorAsState(
                        targetValue = if (isCenter) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        },
                        label = "cover-border"
                    )

                    val verticalOffset by animateDpAsState(
                        targetValue = if (isCenter) 0.dp else 8.dp,
                        label = "cover-offset"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(top = verticalOffset)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(cardWidth)
                                .clip(RoundedCornerShape(18.dp))
                                .background(borderColor)
                                .padding(2.dp)
                                .clickable { onTrackSelect(track) }
                        ) {
                            TrackArtwork(
                                artworkPath = track.artworkPath,
                                seed = track.id,
                                cornerRadius = 16.dp,
                                contentDescription = track.title,
                                modifier = Modifier.size(cardWidth - 4.dp)
                            )

                            if (isCenter && isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PlaybackBars(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        animate = true
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Название под обложкой: длинное уплывает, исчезая по краям
                        MarqueeText(
                            text = track.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCenter) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fadeColor = MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .width(cardWidth)
                                .height(16.dp),
                            animate = isCenter
                        )
                    }
                }
            }
        }

        val currentIndex = tracks.indexOfFirst { it.id == centerTrackId }

        Text(
            text = if (currentIndex >= 0) {
                "${currentIndex + 1} / ${tracks.size}"
            } else {
                stringResource(R.string.library_track_count, tracks.size)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}

/** Индекс карточки, ближайшей к центру ленты */
private fun centerIndex(layoutInfo: androidx.compose.foundation.lazy.LazyListLayoutInfo): Int {
    if (layoutInfo.visibleItemsInfo.isEmpty()) return 0

    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2

    return layoutInfo.visibleItemsInfo
        .minByOrNull { item -> abs(item.offset + item.size / 2 - viewportCenter) }
        ?.index ?: 0
}
