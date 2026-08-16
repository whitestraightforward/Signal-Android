/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package dev.chat.fork.messenger.main

import kotlin.math.abs
import kotlin.math.sign

/**
 * Where the floating navigation bar is currently resting.
 *
 * [CENTER] is the neutral / at-rest position. [LEFT] and [RIGHT] describe the
 * physical direction the bar was thrown towards by the user before it locked into place.
 */
enum class NavigationPosition {
  LEFT,
  CENTER,
  RIGHT;

  val isSettled: Boolean
    get() = this == CENTER
}

/**
 * Which component owns the in-flight gesture.
 *
 * A gesture always starts as [UNDECIDED]. The recognition layer inspects the movement of the
 * pointer (and whether anything else already consumed it) before assigning ownership. Once
 * ownership has been assigned it is never re-assigned for that gesture.
 */
enum class NavigationGestureOwner {
  /** Not enough information yet, nobody may move anything. */
  UNDECIDED,

  /** The navigation layer owns the gesture and the bar may follow the finger. */
  NAVIGATION,

  /** Interface content (lists, conversations, media, item swipes, ...) owns the gesture. */
  CONTENT
}

/**
 * The kind of region that sits underneath the user's finger when a gesture starts.
 *
 * Content regions always win (Priority 1), navigation regions may claim horizontal drags
 * (Priority 2) and unknown regions are treated as content so we never steal gestures from
 * something we do not understand.
 */
enum class NavigationGestureRegion {
  /** Bottom navigation bar / floating navigation container / navigation controls. */
  NAVIGATION,

  /** Scrollable or interactive interface content: chats, messages, settings, media, ... */
  CONTENT,

  /** Anything we could not classify. Treated as content. */
  UNKNOWN;

  val canOwnNavigationGesture: Boolean
    get() = this == NAVIGATION
}

/**
 * Where the finger travelled, in physical screen terms.
 */
enum class NavigationSwipeDirection {
  LEFT,
  RIGHT
}

/**
 * Tuning values for the Telegram-style navigation drag.
 *
 * All distances are expressed in pixels so that this configuration stays free of Compose /
 * Android types and can be exercised by plain JVM unit tests.
 */
data class NavigationGestureMetrics(
  /** Width of a single navigation slot. Drag distance is measured relative to this. */
  val slotWidthPx: Float,
  /** Touch slop, below which we refuse to assign ownership. */
  val touchSlopPx: Float,
  /** Minimum absolute travel required for a slow drag to commit. */
  val minimumSwipeDistancePx: Float,
  /** Minimum |velocity| (px/s) that turns a short drag into a committing fling. */
  val minimumFlingVelocity: Float = 1_000f,
  /** Fraction of a slot that a slow drag has to cover before it commits. */
  val commitDistanceFraction: Float = 0.35f,
  /** Multiplier applied to raw finger travel: 200px of finger -> 160px of navigation. */
  val dragResistance: Float = 0.8f,
  /** How far past a single slot the bar may be dragged. */
  val maxTravelFraction: Float = 1f,
  /** |dx| must be this much larger than |dy| before navigation may claim the gesture. */
  val horizontalDominanceRatio: Float = 1.4f
)

/**
 * The outcome of releasing the navigation bar.
 */
data class NavigationReleaseDecision(
  /** Position the bar should lock into. */
  val position: NavigationPosition,
  /** Logical destination movement, or null when the bar simply returns to rest. */
  val move: NavigationBarMoveDirection?,
  /** Physical direction the bar travelled, or null for a cancelled drag. */
  val direction: NavigationSwipeDirection?,
  /** Offset (px) the bar should animate towards before its position is reset to rest. */
  val targetOffsetPx: Float
) {
  val commits: Boolean
    get() = move != null
}

/**
 * Pure, framework-free gesture arbitration used by the navigation gesture layer.
 *
 * This is deliberately free of Compose types: the recognition layer feeds it raw numbers and it
 * answers two questions:
 *
 *  1. Who owns this gesture? ([resolveOwner])
 *  2. What should happen when the finger lifts? ([resolveRelease])
 */
object MainNavigationGestureResolver {

  /**
   * Decide which component owns a gesture.
   *
   * Priority 1 - interface content. If the touch did not start on a navigation component, or if
   * some other component already consumed the pointer (a list fling, an item swipe, a media
   * gesture, ...), content keeps the gesture.
   *
   * Priority 2 - navigation. Only a clearly horizontal drag that started on a navigation
   * component and travelled past touch slop may be claimed by navigation. A vertical-ish drag on
   * the navigation bar is handed to content so pull-to-dismiss / scroll behaviours keep working.
   */
  @JvmStatic
  fun resolveOwner(
    region: NavigationGestureRegion,
    dx: Float,
    dy: Float,
    metrics: NavigationGestureMetrics,
    consumedByOther: Boolean = false,
    navigationEnabled: Boolean = true
  ): NavigationGestureOwner {
    if (consumedByOther) {
      return NavigationGestureOwner.CONTENT
    }

    if (!navigationEnabled || !region.canOwnNavigationGesture) {
      return NavigationGestureOwner.CONTENT
    }

    val horizontal = abs(dx)
    val vertical = abs(dy)
    val slop = metrics.touchSlopPx.coerceAtLeast(0f)

    // Vertical intent is answered immediately so content can take over without a delay.
    if (vertical > slop && vertical >= horizontal * metrics.horizontalDominanceRatio) {
      return NavigationGestureOwner.CONTENT
    }

    if (horizontal <= slop) {
      return NavigationGestureOwner.UNDECIDED
    }

    return if (horizontal >= vertical * metrics.horizontalDominanceRatio) {
      NavigationGestureOwner.NAVIGATION
    } else {
      NavigationGestureOwner.UNDECIDED
    }
  }

  /**
   * Convert raw finger travel into navigation travel.
   *
   * Movement resistance keeps the bar slightly behind the finger, which is what gives the
   * interaction its weighted, natural feel:
   *
   * ```
   * navigationOffset = gestureDistance x 0.8
   * ```
   *
   * The result is clamped so the bar can never be dragged further than [NavigationGestureMetrics.maxTravelFraction]
   * of a slot.
   */
  @JvmStatic
  fun applyResistance(rawDistancePx: Float, metrics: NavigationGestureMetrics): Float {
    val resistance = metrics.dragResistance.coerceIn(0f, 1f)
    val limit = (metrics.slotWidthPx * metrics.maxTravelFraction).coerceAtLeast(0f)
    if (limit == 0f) {
      return 0f
    }
    return (rawDistancePx * resistance).coerceIn(-limit, limit)
  }

  /**
   * Map a physical swipe direction to a logical destination change, honouring layout direction.
   *
   * The mapping is inverted relative to a "drag the strip of tabs" model: a swipe towards the
   * right advances to the NEXT destination and a swipe towards the left goes back to the
   * PREVIOUS one.
   *
   * This is what makes both directions usable in practice. The default tab (Chats) sits at index
   * 0, so under the previous mapping a right swipe resolved to PREVIOUS, hit the start-of-list
   * edge guard and silently did nothing — the gesture appeared dead on the screen users start on.
   * With this mapping a right swipe from Chats advances normally, and the edge guard only blocks
   * the genuine ends of the list.
   *
   * RTL is still mirrored, so the gesture keeps matching the on-screen order of the tabs.
   */
  @JvmStatic
  fun moveDirectionFor(direction: NavigationSwipeDirection, isRtl: Boolean): NavigationBarMoveDirection {
    return when (direction) {
      NavigationSwipeDirection.RIGHT -> if (isRtl) NavigationBarMoveDirection.PREVIOUS else NavigationBarMoveDirection.NEXT
      NavigationSwipeDirection.LEFT -> if (isRtl) NavigationBarMoveDirection.NEXT else NavigationBarMoveDirection.PREVIOUS
    }
  }

  /**
   * Decide where the bar should end up once the finger lifts.
   *
   * * A small drag (below both the distance and the velocity threshold) springs back to
   *   [NavigationPosition.CENTER].
   * * A long drag, or a fast fling, locks to [NavigationPosition.LEFT] / [NavigationPosition.RIGHT]
   *   and reports the destination change that should be applied.
   * * A commit that has nowhere to go (already on the first / last destination) always returns to
   *   rest instead of leaving the bar stranded off-centre.
   */
  @JvmStatic
  fun resolveRelease(
    offsetPx: Float,
    velocityPxPerSecond: Float,
    metrics: NavigationGestureMetrics,
    isRtl: Boolean = false,
    canMovePrevious: Boolean = true,
    canMoveNext: Boolean = true
  ): NavigationReleaseDecision {
    val rest = NavigationReleaseDecision(
      position = NavigationPosition.CENTER,
      move = null,
      direction = null,
      targetOffsetPx = 0f
    )

    val fling = abs(velocityPxPerSecond) >= metrics.minimumFlingVelocity
    val travel = abs(offsetPx)
    val distanceThreshold = maxOf(
      metrics.minimumSwipeDistancePx,
      metrics.slotWidthPx * metrics.commitDistanceFraction
    )
    val longEnough = travel >= distanceThreshold

    if (!fling && !longEnough) {
      return rest
    }

    // Velocity wins over displacement for flings: a fast flick back the other way should follow
    // the flick, not the (stale) offset it left behind.
    val signal = if (fling) velocityPxPerSecond else offsetPx
    if (signal == 0f) {
      return rest
    }

    val direction = if (signal.sign < 0f) NavigationSwipeDirection.LEFT else NavigationSwipeDirection.RIGHT
    val move = moveDirectionFor(direction, isRtl)

    val allowed = when (move) {
      NavigationBarMoveDirection.PREVIOUS -> canMovePrevious
      NavigationBarMoveDirection.NEXT -> canMoveNext
    }

    if (!allowed) {
      return rest
    }

    val position = when (direction) {
      NavigationSwipeDirection.LEFT -> NavigationPosition.LEFT
      NavigationSwipeDirection.RIGHT -> NavigationPosition.RIGHT
    }

    val target = when (direction) {
      NavigationSwipeDirection.LEFT -> -metrics.slotWidthPx
      NavigationSwipeDirection.RIGHT -> metrics.slotWidthPx
    }

    return NavigationReleaseDecision(
      position = position,
      move = move,
      direction = direction,
      targetOffsetPx = target
    )
  }

  /**
   * Normalised drag progress in `[-1, 1]`, used for cross-fading / synchronising other chrome.
   */
  @JvmStatic
  fun progressFor(offsetPx: Float, metrics: NavigationGestureMetrics): Float {
    if (metrics.slotWidthPx <= 0f) {
      return 0f
    }
    return (offsetPx / metrics.slotWidthPx).coerceIn(-1f, 1f)
  }
}
