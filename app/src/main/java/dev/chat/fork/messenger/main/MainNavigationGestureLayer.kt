/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package dev.chat.fork.messenger.main

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Spring used for every settle / snap of the navigation bar.
 *
 * Slightly under-damped so the bar accelerates smoothly, decelerates naturally and settles
 * without an abrupt stop — the same character as Telegram's chrome transitions.
 */
private val NavigationSettleSpring: AnimationSpec<Float> = spring(
  dampingRatio = 0.78f,
  stiffness = Spring.StiffnessMediumLow,
  visibilityThreshold = 0.5f
)

/**
 * An immutable snapshot of everything the navigation gesture layer knows about the current
 * interaction. Handy for tests, logging and previews.
 */
data class MainNavigationGestureSnapshot(
  val owner: NavigationGestureOwner = NavigationGestureOwner.UNDECIDED,
  val position: NavigationPosition = NavigationPosition.CENTER,
  val offsetPx: Float = 0f,
  val progress: Float = 0f,
  val isDragging: Boolean = false,
  val isAnimating: Boolean = false
) {
  val isActive: Boolean
    get() = isDragging || isAnimating
}

/**
 * The gesture management layer that sits between the main application window and the individual
 * components.
 *
 * The recognition modifier feeds it pointer data, the floating navigation bar reads [offsetPx] to
 * render itself, and the rest of the application is untouched. Crucially it never captures the
 * window: it only ever reacts to pointers delivered to components that opted in.
 */
@Stable
class MainNavigationGestureState internal constructor(
  private val scope: CoroutineScope
) {
  /** Horizontal translation, in pixels, that should be applied to the navigation bar. */
  var offsetPx: Float by mutableFloatStateOf(0f)
    private set

  /** Current owner of the in-flight gesture. */
  var owner: NavigationGestureOwner by mutableStateOf(NavigationGestureOwner.UNDECIDED)
    private set

  /** Where the bar is currently locked. */
  var position: NavigationPosition by mutableStateOf(NavigationPosition.CENTER)
    private set

  /** True while a finger is actively dragging the navigation bar. */
  var isDragging: Boolean by mutableStateOf(false)
    private set

  /** True while the release animation is playing. */
  var isAnimating: Boolean by mutableStateOf(false)
    private set

  /** Width of a navigation slot, published by the navigation bar during layout. */
  var slotWidthPx: Float by mutableFloatStateOf(0f)
    internal set

  private var settleJob: Job? = null

  /** Normalised progress in `[-1, 1]`. Negative means the bar travelled left. */
  val progress: Float
    get() = if (slotWidthPx <= 0f) 0f else (offsetPx / slotWidthPx).coerceIn(-1f, 1f)

  fun snapshot(): MainNavigationGestureSnapshot = MainNavigationGestureSnapshot(
    owner = owner,
    position = position,
    offsetPx = offsetPx,
    progress = progress,
    isDragging = isDragging,
    isAnimating = isAnimating
  )

  internal fun onGestureStart() {
    owner = NavigationGestureOwner.UNDECIDED
  }

  internal fun onOwnerDecided(newOwner: NavigationGestureOwner) {
    owner = newOwner
    if (newOwner == NavigationGestureOwner.NAVIGATION) {
      settleJob?.cancel()
      isAnimating = false
      isDragging = true
    }
  }

  /** Called from the recognition loop while the navigation bar follows the finger. */
  internal fun dragTo(offset: Float) {
    offsetPx = offset
  }

  /**
   * Animate to the decided target with the settle spring, hand any destination change to the
   * router, then glide back to the neutral resting position. [onSettled] fires once all visual
   * movement has finished.
   */
  internal fun settle(
    decision: NavigationReleaseDecision,
    onCommit: (NavigationBarMoveDirection) -> Unit,
    onSettled: () -> Unit
  ) {
    isDragging = false
    position = decision.position

    settleJob?.cancel()
    settleJob = scope.launch {
      isAnimating = true
      try {
        if (decision.commits) {
          // Follow through in the direction of the throw ...
          animate(
            initialValue = offsetPx,
            targetValue = decision.targetOffsetPx,
            animationSpec = NavigationSettleSpring
          ) { value, _ -> offsetPx = value }

          // ... commit the destination change ...
          decision.move?.let(onCommit)

          // ... and let the bar come in from the opposite side so the swap reads as one
          // continuous movement rather than a jump.
          offsetPx = -decision.targetOffsetPx
        }

        animate(
          initialValue = offsetPx,
          targetValue = 0f,
          animationSpec = NavigationSettleSpring
        ) { value, _ -> offsetPx = value }

        offsetPx = 0f
        position = NavigationPosition.CENTER
        owner = NavigationGestureOwner.UNDECIDED
        onSettled()
      } finally {
        isAnimating = false
      }
    }
  }

  /** Abandon the current gesture and spring back to rest. */
  fun cancel() {
    isDragging = false
    owner = NavigationGestureOwner.UNDECIDED
    settleJob?.cancel()
    settleJob = scope.launch {
      isAnimating = true
      try {
        animate(
          initialValue = offsetPx,
          targetValue = 0f,
          animationSpec = NavigationSettleSpring
        ) { value, _ -> offsetPx = value }
        offsetPx = 0f
        position = NavigationPosition.CENTER
      } finally {
        isAnimating = false
      }
    }
  }
}

@Composable
fun rememberMainNavigationGestureState(): MainNavigationGestureState {
  val scope = rememberCoroutineScope()
  return remember(scope) { MainNavigationGestureState(scope) }
}

/**
 * Attaches the navigation gesture recognition layer to a component.
 *
 * This modifier deliberately does **not** capture the whole window. It is applied only to
 * navigation chrome, and even there it arbitrates before moving anything:
 *
 * * the first pointer down is observed without being consumed, so taps, ripples and children
 *   still work exactly as before;
 * * every move is handed to [MainNavigationGestureResolver] together with the region under the
 *   finger and whether anybody else already consumed the pointer;
 * * only once the resolver answers [NavigationGestureOwner.NAVIGATION] do we start consuming and
 *   moving the bar. If it answers [NavigationGestureOwner.CONTENT] we bail out of the gesture
 *   entirely and never touch the pointer again, so message scrolling, chat list scrolling,
 *   archive swipes, item swipes, text input and media gestures behave as they always have.
 *
 * @param region the kind of component this modifier is attached to.
 * @param enabled when false the gesture is always handed to content.
 * @param isRtl layout direction, so physical swipes map to the right logical destination.
 * @param canMovePrevious / [canMoveNext] edge guards, evaluated at release time.
 * @param swipeConfig tuning for resistance, commit distance and fling velocity.
 * @param onCommit invoked with the logical destination change once a swipe commits.
 * @param onGestureStateChanged progress / activity reporting for state holders.
 */
fun Modifier.mainNavigationGesture(
  state: MainNavigationGestureState,
  region: NavigationGestureRegion = NavigationGestureRegion.NAVIGATION,
  enabled: Boolean = true,
  isRtl: Boolean = false,
  canMovePrevious: () -> Boolean = { true },
  canMoveNext: () -> Boolean = { true },
  swipeConfig: NavigationSwipeConfig = NavigationSwipeConfig(),
  onCommit: (NavigationBarMoveDirection) -> Unit = {},
  onGestureStateChanged: (progress: Float, isActive: Boolean) -> Unit = { _, _ -> }
): Modifier = this.pointerInput(state, region, enabled, isRtl, swipeConfig) {
  val touchSlop = viewConfiguration.touchSlop
  val minimumSwipeDistancePx = swipeConfig.minimumSwipeDistance.toPx()

  awaitEachGesture {
    // Observe the down without consuming it: taps, ripples and children keep working.
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
    state.onGestureStart()

    if (!enabled || !region.canOwnNavigationGesture) {
      return@awaitEachGesture
    }

    val metrics = NavigationGestureMetrics(
      slotWidthPx = state.slotWidthPx,
      touchSlopPx = touchSlop,
      minimumSwipeDistancePx = minimumSwipeDistancePx,
      minimumFlingVelocity = swipeConfig.minimumFlingVelocity,
      dragResistance = swipeConfig.dragResistance
    )

    if (metrics.slotWidthPx <= 0f) {
      return@awaitEachGesture
    }

    val velocityTracker = VelocityTracker()
    velocityTracker.addPosition(down.uptimeMillis, down.position)

    var total = Offset.Zero
    var owned = false
    var pointerId = down.id

    while (true) {
      val event = awaitPointerEvent(PointerEventPass.Main)
      val change: PointerInputChange = event.changes.firstOrNull { it.id == pointerId }
        ?: event.changes.firstOrNull { it.pressed }
        ?: break

      pointerId = change.id

      if (!change.pressed) {
        break
      }

      if (change.isConsumed && !owned) {
        // Somebody else (a list, an item swipe, a media viewer) claimed this pointer.
        state.onOwnerDecided(NavigationGestureOwner.CONTENT)
        return@awaitEachGesture
      }

      total += change.positionChange()
      velocityTracker.addPosition(change.uptimeMillis, change.position)

      if (!owned) {
        when (
          MainNavigationGestureResolver.resolveOwner(
            region = region,
            dx = total.x,
            dy = total.y,
            metrics = metrics,
            consumedByOther = change.isConsumed,
            navigationEnabled = enabled
          )
        ) {
          NavigationGestureOwner.CONTENT -> {
            state.onOwnerDecided(NavigationGestureOwner.CONTENT)
            return@awaitEachGesture
          }

          NavigationGestureOwner.NAVIGATION -> {
            owned = true
            state.onOwnerDecided(NavigationGestureOwner.NAVIGATION)
          }

          NavigationGestureOwner.UNDECIDED -> Unit
        }
      }

      if (owned) {
        change.consume()
        val offset = MainNavigationGestureResolver.applyResistance(total.x, metrics)
        state.dragTo(offset)
        onGestureStateChanged(MainNavigationGestureResolver.progressFor(offset, metrics), true)
      }
    }

    if (!owned) {
      return@awaitEachGesture
    }

    val velocity = velocityTracker.calculateVelocity().x
    val decision = MainNavigationGestureResolver.resolveRelease(
      offsetPx = MainNavigationGestureResolver.applyResistance(total.x, metrics),
      velocityPxPerSecond = velocity,
      metrics = metrics,
      isRtl = isRtl,
      canMovePrevious = canMovePrevious(),
      canMoveNext = canMoveNext()
    )

    state.settle(
      decision = decision,
      onCommit = onCommit,
      onSettled = { onGestureStateChanged(0f, false) }
    )
  }
}

/**
 * Marks a subtree as interface content, guaranteeing that no navigation gesture can originate
 * from it.
 *
 * This is the explicit expression of Priority 1: conversations, chat lists, archived chats,
 * settings lists, profile lists and media viewers own their own gestures. It observes the initial
 * pass without consuming anything, so it is completely transparent to the content it wraps.
 *
 * @param onContentClaimed notified whenever content takes ownership, so state holders can record it.
 */
fun Modifier.navigationContentGestureRegion(
  state: MainNavigationGestureState,
  onContentClaimed: () -> Unit = {}
): Modifier = this.pointerInput(state) {
  awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    state.onOwnerDecided(NavigationGestureOwner.CONTENT)
    onContentClaimed()
  }
}
