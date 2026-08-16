/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package dev.chat.fork.messenger.main

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import org.signal.core.ui.compose.DayNightPreviews
import org.signal.core.ui.compose.Previews
import org.signal.core.ui.compose.theme.Dimensions
import org.signal.core.ui.compose.theme.SignalTheme
import dev.chat.fork.messenger.R
import dev.chat.fork.messenger.avatar.AvatarImage
import dev.chat.fork.messenger.recipients.Recipient
import kotlin.math.abs

private val LOTTIE_SIZE = 28.dp

// ============================================================================
// Legacy enum — preserved for backward compatibility.
// ============================================================================

enum class MainNavigationListLocation(
  @StringRes val label: Int,
  @RawRes val icon: Int,
  @StringRes val contentDescription: Int = label
) {
  CHATS(
    label = R.string.ConversationListTabs__chats,
    icon = R.raw.chats_28
  ),
  ARCHIVE(
    label = R.string.ConversationListTabs__chats,
    icon = R.raw.chats_28
  ),
  CALLS(
    label = R.string.ConversationListTabs__calls,
    icon = R.raw.calls_28
  ),
  STORIES(
    label = R.string.ConversationListTabs__stories,
    icon = R.raw.stories_28
  ),
  SETTINGS(
    label = R.string.ConversationListTabs__settings,
    icon = R.raw.settings_28
  ),
  PROFILE(
    label = R.string.ConversationListTabs__profile,
    icon = R.raw.profile_28
  );

  val isChatsTab: Boolean
    get() = this == CHATS || this == ARCHIVE
}

// ============================================================================
// State container (unchanged signature — full backward compat).
// ============================================================================

data class MainNavigationState(
  val chatsCount: Int = 0,
  val callsCount: Int = 0,
  val storiesCount: Int = 0,
  val storyFailure: Boolean = false,
  val isStoriesFeatureEnabled: Boolean = true,
  val currentListLocation: MainNavigationListLocation = MainNavigationListLocation.CHATS,
  val compact: Boolean = false
)

/** Controls main-window swipe navigation without affecting the navigation destinations themselves. */
data class NavigationSwipeConfig(
  val enabled: Boolean = true,
  val minimumSwipeDistance: Dp = 64.dp
)

enum class NavigationBarMoveDirection {
  PREVIOUS,
  NEXT
}

// ============================================================================
// Professional Floating Bottom Navigation Bar
// ============================================================================

/**
 * Professional floating bottom navigation bar with a modern card-like appearance.
 *
 * Design features:
 * - Floating container with rounded corners and subtle elevation
 * - Active tab highlighted with a pill-shaped indicator background
 * - Smooth color and shape transition animations between tabs
 * - Consistent icon + label layout across all destinations
 * - Full support for light mode, dark mode, and custom themes
 * - Badge indicators for unread counts
 * - Five-tab layout: Chats, Calls, Stories, Settings, Profile
 *
 * When a user taps a new destination (SETTINGS, PROFILE), the [onNewDestinationSelected]
 * callback is invoked instead of [onDestinationSelected].
 */
@Composable
fun MainNavigationBar(
  state: MainNavigationState,
  onDestinationSelected: (MainNavigationListLocation) -> Unit,
  onNewDestinationSelected: (MainNavigationDestination) -> Unit = {},
  menuConfig: NavigationMenuConfig = NavigationMenuConfig.default(),
  selfRecipient: Recipient = Recipient.UNKNOWN
) {
  val navItems = NavigationMenuProvider.getItems(
    currentDestination = state.currentListLocation,
    chatsBadge = state.chatsCount,
    callsBadge = state.callsCount,
    storiesBadge = state.storiesCount,
    isStoriesEnabled = state.isStoriesFeatureEnabled,
    config = menuConfig
  )

  val onItemSelected: (NavigationMenuItemData) -> Unit = { item ->
    val listLocation = item.destination.toListLocationOrNull()
    if (listLocation != null) {
      onDestinationSelected(listLocation)
    } else {
      onNewDestinationSelected(item.destination)
    }
  }

  val containerColor = SignalTheme.colors.colorSurface2

  Surface(
    shape = RoundedCornerShape(28.dp),
    color = containerColor,
    shadowElevation = 8.dp,
    tonalElevation = 3.dp,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(if (state.compact) 60.dp else 68.dp)
        .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      navItems.forEach { item ->
        FloatingNavigationBarItem(
          item = item,
          compact = state.compact,
          selfRecipient = selfRecipient,
          onSelected = { onItemSelected(item) }
        )
      }
    }
  }
}

/**
 * Observes unhandled horizontal swipes from the main window. Pointer changes are read during the
 * final event pass and are never consumed, allowing child controls, scrolling, message gestures,
 * and system navigation to keep priority.
 */
fun Modifier.mainWindowNavigationSwipe(
  layoutDirection: LayoutDirection,
  config: NavigationSwipeConfig = NavigationSwipeConfig(),
  onMove: (NavigationBarMoveDirection) -> Unit
): Modifier {
  if (!config.enabled) {
    return this
  }

  return pointerInput(config, layoutDirection) {
    awaitPointerEventScope {
      while (true) {
        val downEvent = awaitPointerEvent(PointerEventPass.Final)
        val down = downEvent.changes.firstOrNull { change -> change.pressed && !change.previousPressed } ?: continue
        val pointerId = down.id
        val startPosition = down.position
        var endPosition = startPosition
        var handledByChild = down.isConsumed
        var pointerIsDown = true

        while (pointerIsDown) {
          val event = awaitPointerEvent(PointerEventPass.Final)
          if (event.changes.count { change -> change.pressed } > 1) {
            handledByChild = true
          }

          val change = event.changes.firstOrNull { it.id == pointerId }
          if (change == null) {
            handledByChild = true
            pointerIsDown = false
          } else {
            handledByChild = handledByChild || change.isConsumed
            endPosition = change.position
            pointerIsDown = change.pressed
          }
        }

        val drag = endPosition - startPosition
        if (!handledByChild && abs(drag.x) >= config.minimumSwipeDistance.toPx() && abs(drag.x) > abs(drag.y)) {
          val moveTowardRight = drag.x < 0f
          onMove(
            when {
              moveTowardRight && layoutDirection == LayoutDirection.Ltr -> NavigationBarMoveDirection.NEXT
              moveTowardRight -> NavigationBarMoveDirection.PREVIOUS
              layoutDirection == LayoutDirection.Ltr -> NavigationBarMoveDirection.PREVIOUS
              else -> NavigationBarMoveDirection.NEXT
            }
          )
        }
      }
    }
  }
}

/**
 * A single item in the floating navigation bar with pill-shaped active indicator.
 */
@Composable
private fun RowScope.FloatingNavigationBarItem(
  item: NavigationMenuItemData,
  compact: Boolean,
  selfRecipient: Recipient,
  onSelected: () -> Unit
) {
  val isSelected = item.isSelected

  val indicatorAlpha by animateFloatAsState(
    targetValue = if (isSelected) 0.12f else 0f,
    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
    label = "navIndicatorAlpha"
  )

  val iconTint by animateColorAsState(
    targetValue = if (isSelected) {
      MaterialTheme.colorScheme.primary
    } else {
      MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    },
    animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
    label = "navIconTint"
  )

  val labelColor by animateColorAsState(
    targetValue = if (isSelected) {
      MaterialTheme.colorScheme.primary
    } else {
      MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    },
    animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
    label = "navLabelColor"
  )

  val indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = indicatorAlpha)

  Box(
    modifier = Modifier
      .weight(1f)
      .height(if (compact) 48.dp else 56.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(indicatorColor)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onSelected
      ),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(contentAlignment = Alignment.TopEnd) {
        if (item.destination == MainNavigationDestination.PROFILE) {
          AvatarImage(
            recipient = selfRecipient,
            modifier = Modifier
              .size(if (compact) 26.dp else 28.dp)
              .clip(CircleShape),
            contentDescription = stringResource(item.destination.labelRes)
          )
        } else {
          Icon(
            painter = painterResource(id = item.destination.iconRes),
            contentDescription = stringResource(item.destination.labelRes),
            tint = iconTint,
            modifier = Modifier.size(if (compact) 22.dp else 24.dp)
          )
        }

        if (item.badgeCount > 0) {
          if (item.badgeCount < 100) {
            NavigationBadgeDot(count = item.badgeCount)
          } else {
            NavigationBadgePill(count = item.badgeCount)
          }
        }
      }

      if (!compact) {
        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = stringResource(item.destination.labelRes),
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
          ),
          color = labelColor,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

/**
 * Small red dot with count — Telegram-style badge for < 100.
 */
@Composable
private fun BoxScope.NavigationBadgeDot(count: Int) {
  Box(
    modifier = Modifier
      .size(18.dp)
      .clip(CircleShape)
      .background(colorResource(R.color.ConversationListTabs__unread)),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = if (count > 9) "9+" else count.toString(),
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      ),
      textAlign = TextAlign.Center
    )
  }
}

/**
 * Pill-style badge for counts >= 100.
 */
@Composable
private fun BoxScope.NavigationBadgePill(count: Int) {
  Box(
    modifier = Modifier
      .height(16.dp)
      .width(22.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(colorResource(R.color.ConversationListTabs__unread)),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = stringResource(R.string.ConversationListTabs__99p),
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      ),
      textAlign = TextAlign.Center
    )
  }
}

// ============================================================================
// Legacy Rail (unchanged except using new item rendering)
// ============================================================================

@Composable
fun MainNavigationRail(
  state: MainNavigationState,
  mainFloatingActionButtonsCallback: MainFloatingActionButtonsCallback,
  onDestinationSelected: (MainNavigationListLocation) -> Unit
) {
  NavigationRail(
    containerColor = SignalTheme.colors.colorSurface1
  ) {
    Spacer(modifier = Modifier.height(40.dp).weight(1f, fill = false))

    MainFloatingActionButtons(
      destination = state.currentListLocation,
      callback = mainFloatingActionButtonsCallback,
      modifier = Modifier.padding(vertical = 8.dp)
    )

    Spacer(modifier = Modifier.height(40.dp).weight(1f, fill = false))

    val entries = remember(state.isStoriesFeatureEnabled) {
      val hiddenTabs = mutableSetOf(
        MainNavigationListLocation.ARCHIVE,
        MainNavigationListLocation.SETTINGS,
        MainNavigationListLocation.PROFILE
      )
      if (!state.isStoriesFeatureEnabled) {
        hiddenTabs.add(MainNavigationListLocation.STORIES)
      }
      MainNavigationListLocation.entries.filterNot { it in hiddenTabs }
    }

    val selectedDestination = if (state.currentListLocation == MainNavigationListLocation.ARCHIVE) {
      MainNavigationListLocation.CHATS
    } else {
      state.currentListLocation
    }

    entries.forEachIndexed { idx, destination ->
      val selected = selectedDestination == destination

      Box {
        NavigationRailItem(
          modifier = Modifier.padding(
            bottom = if (MainNavigationListLocation.entries.lastIndex == idx) {
              Dimensions.space0
            } else {
              Dimensions.navigationRailItemSpacing
            }
          ),
          icon = {
            NavigationDestinationIcon(
              destination = destination,
              selected = selected
            )
          },
          label = {
            NavigationDestinationLabel(destination)
          },
          selected = selected,
          onClick = {
            onDestinationSelected(destination)
          }
        )

        NavigationRailCountIndicator(
          state = state,
          destination = destination
        )
      }
    }
  }
}

@Composable
private fun BoxScope.NavigationRailCountIndicator(
  state: MainNavigationState,
  destination: MainNavigationListLocation
) {
  val count = remember(state, destination) {
    when (destination) {
      MainNavigationListLocation.ARCHIVE -> error("Not supported")
      MainNavigationListLocation.CHATS -> state.chatsCount
      MainNavigationListLocation.CALLS -> state.callsCount
      MainNavigationListLocation.STORIES -> state.storiesCount
      MainNavigationListLocation.SETTINGS -> 0
      MainNavigationListLocation.PROFILE -> 0
    }
  }

  if (count > 0) {
    Box(
      modifier = Modifier
        .padding(start = 42.dp)
        .height(16.dp)
        .width(if (count < 100) 16.dp else 22.dp)
        .clip(CircleShape)
        .background(color = colorResource(R.color.ConversationListTabs__unread))
        .align(Alignment.TopStart),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = formatCount(count),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
        color = Color.White,
        maxLines = 1
      )
    }
  }
}

// ============================================================================
// Legacy icon / label composables (preserved for backward compatibility)
// ============================================================================

@Composable
private fun NavigationDestinationIcon(
  destination: MainNavigationListLocation,
  selected: Boolean
) {
  val dynamicProperties = rememberLottieDynamicProperties(
    rememberLottieDynamicProperty(
      property = LottieProperty.COLOR_FILTER,
      value = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
        MaterialTheme.colorScheme.onSurface.hashCode(),
        BlendModeCompat.SRC_ATOP
      ),
      keyPath = arrayOf("**")
    )
  )

  val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(destination.icon))
  val progress by animateFloatAsState(targetValue = if (selected) 1f else 0f, animationSpec = tween(durationMillis = composition?.duration?.toInt() ?: 0))

  LottieAnimation(
    composition = composition,
    progress = { if (selected) progress else 0f },
    dynamicProperties = dynamicProperties,
    modifier = Modifier.size(LOTTIE_SIZE)
  )
}

@Composable
private fun NavigationDestinationLabel(destination: MainNavigationListLocation) {
  Text(stringResource(destination.label))
}

@Composable
internal fun formatCount(count: Int): String {
  if (count > 99) {
    return stringResource(R.string.ConversationListTabs__99p)
  }
  return count.toString()
}

// ============================================================================
// Previews
// ============================================================================

@DayNightPreviews
@Composable
private fun MainNavigationBarPreview() {
  Previews.Preview {
    var selected by remember { mutableStateOf(MainNavigationListLocation.CHATS) }

    MainNavigationBar(
      state = MainNavigationState(
        chatsCount = 5,
        callsCount = 0,
        storiesCount = 3,
        currentListLocation = selected,
        compact = false
      ),
      onDestinationSelected = { selected = it }
    )
  }
}

@DayNightPreviews
@Composable
private fun MainNavigationBarWithAllDestinationsPreview() {
  Previews.Preview {
    MainNavigationBar(
      state = MainNavigationState(
        chatsCount = 99,
        callsCount = 2,
        storiesCount = 7,
        currentListLocation = MainNavigationListLocation.CALLS,
        compact = false
      ),
      onDestinationSelected = {},
      menuConfig = NavigationMenuConfig.fromIds("chats", "calls", "stories", "settings", "profile")
    )
  }
}

@DayNightPreviews
@Preview(device = "spec:parent=pixel_7,orientation=landscape")
@Composable
private fun MainNavigationRailPreview() {
  Previews.Preview {
    var selected by remember { mutableStateOf(MainNavigationListLocation.CHATS) }

    MainNavigationRail(
      state = MainNavigationState(
        chatsCount = 500,
        callsCount = 10,
        storiesCount = 5,
        currentListLocation = selected
      ),
      mainFloatingActionButtonsCallback = MainFloatingActionButtonsCallback.Empty,
      onDestinationSelected = { selected = it }
    )
  }
}
