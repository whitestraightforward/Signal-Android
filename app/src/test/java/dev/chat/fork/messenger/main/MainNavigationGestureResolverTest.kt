/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package dev.chat.fork.messenger.main

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.Test

class MainNavigationGestureResolverTest {

  private val metrics = NavigationGestureMetrics(
    slotWidthPx = 200f,
    touchSlopPx = 10f,
    minimumSwipeDistancePx = 30f
  )

  // --------------------------------------------------------------------------------------------
  // Priority 1: interface content always wins.
  // --------------------------------------------------------------------------------------------

  @Test
  fun `content region never yields the gesture to navigation`() {
    val owner = MainNavigationGestureResolver.resolveOwner(
      region = NavigationGestureRegion.CONTENT,
      dx = 400f,
      dy = 0f,
      metrics = metrics
    )

    assertThat(owner).isEqualTo(NavigationGestureOwner.CONTENT)
  }

  @Test
  fun `unknown region is treated as content`() {
    val owner = MainNavigationGestureResolver.resolveOwner(
      region = NavigationGestureRegion.UNKNOWN,
      dx = 400f,
      dy = 0f,
      metrics = metrics
    )

    assertThat(owner).isEqualTo(NavigationGestureOwner.CONTENT)
  }

  @Test
  fun `a pointer already consumed elsewhere belongs to content`() {
    val owner = MainNavigationGestureResolver.resolveOwner(
      region = NavigationGestureRegion.NAVIGATION,
      dx = 400f,
      dy = 0f,
      metrics = metrics,
      consumedByOther = true
    )

    assertThat(owner).isEqualTo(NavigationGestureOwner.CONTENT)
  }

  @Test
  fun `a vertical drag on the navigation bar is handed to content`() {
    val owner = MainNavigationGestureResolver.resolveOwner(
      region = NavigationGestureRegion.NAVIGATION,
      dx = 5f,
      dy = 120f,
      metrics = metrics
    )

    assertThat(owner).isEqualTo(NavigationGestureOwner.CONTENT)
  }

  @Test
  fun `disabling navigation gestures hands everything to content`() {
    val owner = MainNavigationGestureResolver.resolveOwner(
      region = NavigationGestureRegion.NAVIGATION,
      dx = 400f,
      dy = 0f,
      metrics = metrics,
      navigationEnabled = false
    )

    assertThat(owner).isEqualTo(NavigationGestureOwner.CONTENT)
  }

  // --------------------------------------------------------------------------------------------
  // Priority 2: navigation components.
  // --------------------------------------------------------------------------------------------

  @Test
  fun `a clearly horizontal drag on navigation is owned by navigation`() {
    val owner = MainNavigationGestureResolver.resolveOwner(
      region = NavigationGestureRegion.NAVIGATION,
      dx = -150f,
      dy = 12f,
      metrics = metrics
    )

    assertThat(owner).isEqualTo(NavigationGestureOwner.NAVIGATION)
  }

  @Test
  fun `movement below touch slop leaves ownership undecided`() {
    val owner = MainNavigationGestureResolver.resolveOwner(
      region = NavigationGestureRegion.NAVIGATION,
      dx = 6f,
      dy = 4f,
      metrics = metrics
    )

    assertThat(owner).isEqualTo(NavigationGestureOwner.UNDECIDED)
  }

  @Test
  fun `an ambiguous diagonal drag stays undecided`() {
    val owner = MainNavigationGestureResolver.resolveOwner(
      region = NavigationGestureRegion.NAVIGATION,
      dx = 40f,
      dy = 38f,
      metrics = metrics
    )

    assertThat(owner).isEqualTo(NavigationGestureOwner.UNDECIDED)
  }

  // --------------------------------------------------------------------------------------------
  // Movement resistance.
  // --------------------------------------------------------------------------------------------

  @Test
  fun `finger travel of 200 moves navigation by 160`() {
    assertThat(MainNavigationGestureResolver.applyResistance(200f, metrics)).isEqualTo(160f)
  }

  @Test
  fun `resistance is symmetric for leftward travel`() {
    assertThat(MainNavigationGestureResolver.applyResistance(-200f, metrics)).isEqualTo(-160f)
  }

  @Test
  fun `travel is clamped to a single slot`() {
    assertThat(MainNavigationGestureResolver.applyResistance(5_000f, metrics)).isEqualTo(200f)
    assertThat(MainNavigationGestureResolver.applyResistance(-5_000f, metrics)).isEqualTo(-200f)
  }

  @Test
  fun `zero slot width produces no movement`() {
    val zero = metrics.copy(slotWidthPx = 0f)
    assertThat(MainNavigationGestureResolver.applyResistance(300f, zero)).isEqualTo(0f)
  }

  // --------------------------------------------------------------------------------------------
  // Release / snap behaviour.
  // --------------------------------------------------------------------------------------------

  @Test
  fun `a small drag returns to the original position`() {
    // 20% of a slot, released slowly.
    val decision = MainNavigationGestureResolver.resolveRelease(
      offsetPx = 40f,
      velocityPxPerSecond = 50f,
      metrics = metrics
    )

    assertThat(decision.position).isEqualTo(NavigationPosition.CENTER)
    assertThat(decision.move).isNull()
    assertThat(decision.commits).isFalse()
    assertThat(decision.targetOffsetPx).isEqualTo(0f)
  }

  @Test
  fun `a long leftward drag locks to the left position`() {
    val decision = MainNavigationGestureResolver.resolveRelease(
      offsetPx = -120f,
      velocityPxPerSecond = 0f,
      metrics = metrics
    )

    assertThat(decision.position).isEqualTo(NavigationPosition.LEFT)
    assertThat(decision.direction).isEqualTo(NavigationSwipeDirection.LEFT)
    assertThat(decision.move).isEqualTo(NavigationBarMoveDirection.PREVIOUS)
    assertThat(decision.targetOffsetPx).isEqualTo(-200f)
    assertThat(decision.commits).isTrue()
  }

  @Test
  fun `a long rightward drag locks to the right position`() {
    val decision = MainNavigationGestureResolver.resolveRelease(
      offsetPx = 120f,
      velocityPxPerSecond = 0f,
      metrics = metrics
    )

    assertThat(decision.position).isEqualTo(NavigationPosition.RIGHT)
    assertThat(decision.direction).isEqualTo(NavigationSwipeDirection.RIGHT)
    assertThat(decision.move).isEqualTo(NavigationBarMoveDirection.NEXT)
    assertThat(decision.targetOffsetPx).isEqualTo(200f)
  }

  @Test
  fun `a fast short fling still commits`() {
    val decision = MainNavigationGestureResolver.resolveRelease(
      offsetPx = -12f,
      velocityPxPerSecond = -3_000f,
      metrics = metrics
    )

    assertThat(decision.position).isEqualTo(NavigationPosition.LEFT)
    assertThat(decision.move).isEqualTo(NavigationBarMoveDirection.PREVIOUS)
  }

  @Test
  fun `velocity beats a stale offset when flinging back`() {
    // Dragged left, then flicked hard to the right before lifting.
    val decision = MainNavigationGestureResolver.resolveRelease(
      offsetPx = -80f,
      velocityPxPerSecond = 4_000f,
      metrics = metrics
    )

    assertThat(decision.direction).isEqualTo(NavigationSwipeDirection.RIGHT)
    assertThat(decision.position).isEqualTo(NavigationPosition.RIGHT)
  }

  @Test
  fun `a commit with nowhere to go springs back to center`() {
    val decision = MainNavigationGestureResolver.resolveRelease(
      offsetPx = -180f,
      velocityPxPerSecond = -3_000f,
      metrics = metrics,
      canMovePrevious = false
    )

    assertThat(decision.position).isEqualTo(NavigationPosition.CENTER)
    assertThat(decision.move).isNull()
    assertThat(decision.targetOffsetPx).isEqualTo(0f)
  }

  // --------------------------------------------------------------------------------------------
  // Layout direction.
  // --------------------------------------------------------------------------------------------

  @Test
  fun `swipe direction maps to logical movement in ltr`() {
    assertThat(MainNavigationGestureResolver.moveDirectionFor(NavigationSwipeDirection.RIGHT, isRtl = false))
      .isEqualTo(NavigationBarMoveDirection.NEXT)
    assertThat(MainNavigationGestureResolver.moveDirectionFor(NavigationSwipeDirection.LEFT, isRtl = false))
      .isEqualTo(NavigationBarMoveDirection.PREVIOUS)
  }

  @Test
  fun `swipe direction is mirrored in rtl`() {
    assertThat(MainNavigationGestureResolver.moveDirectionFor(NavigationSwipeDirection.RIGHT, isRtl = true))
      .isEqualTo(NavigationBarMoveDirection.PREVIOUS)
    assertThat(MainNavigationGestureResolver.moveDirectionFor(NavigationSwipeDirection.LEFT, isRtl = true))
      .isEqualTo(NavigationBarMoveDirection.NEXT)
  }

  @Test
  fun `rtl release maps a left throw to the next destination`() {
    val decision = MainNavigationGestureResolver.resolveRelease(
      offsetPx = -150f,
      velocityPxPerSecond = 0f,
      metrics = metrics,
      isRtl = true
    )

    assertThat(decision.position).isEqualTo(NavigationPosition.LEFT)
    assertThat(decision.move).isEqualTo(NavigationBarMoveDirection.NEXT)
  }

  // --------------------------------------------------------------------------------------------
  // Regression: both directions must be usable from the default tab.
  // --------------------------------------------------------------------------------------------

  @Test
  fun `a right swipe from the first destination advances instead of being blocked`() {
    // Chats is index 0, so canMovePrevious is false there. A right swipe must still commit,
    // otherwise the gesture is dead on the screen the app starts on.
    val decision = MainNavigationGestureResolver.resolveRelease(
      offsetPx = 150f,
      velocityPxPerSecond = 0f,
      metrics = metrics,
      canMovePrevious = false,
      canMoveNext = true
    )

    assertThat(decision.commits).isTrue()
    assertThat(decision.move).isEqualTo(NavigationBarMoveDirection.NEXT)
  }

  @Test
  fun `a right fling from the first destination advances instead of being blocked`() {
    val decision = MainNavigationGestureResolver.resolveRelease(
      offsetPx = 15f,
      velocityPxPerSecond = 3_000f,
      metrics = metrics,
      canMovePrevious = false,
      canMoveNext = true
    )

    assertThat(decision.commits).isTrue()
    assertThat(decision.move).isEqualTo(NavigationBarMoveDirection.NEXT)
  }

  @Test
  fun `left and right swipes resolve to opposite destinations`() {
    val left = MainNavigationGestureResolver.resolveRelease(
      offsetPx = -150f,
      velocityPxPerSecond = 0f,
      metrics = metrics
    )
    val right = MainNavigationGestureResolver.resolveRelease(
      offsetPx = 150f,
      velocityPxPerSecond = 0f,
      metrics = metrics
    )

    assertThat(left.commits).isTrue()
    assertThat(right.commits).isTrue()
    assertThat(left.move).isNotEqualTo(right.move)
  }

  @Test
  fun `both directions commit at identical distance and velocity thresholds`() {
    val distance = maxOf(metrics.minimumSwipeDistancePx, metrics.slotWidthPx * metrics.commitDistanceFraction)

    // Just under the threshold: neither direction may commit.
    assertThat(
      MainNavigationGestureResolver.resolveRelease(distance - 1f, 0f, metrics).commits
    ).isFalse()
    assertThat(
      MainNavigationGestureResolver.resolveRelease(-(distance - 1f), 0f, metrics).commits
    ).isFalse()

    // At the threshold: both directions must commit.
    assertThat(MainNavigationGestureResolver.resolveRelease(distance, 0f, metrics).commits).isTrue()
    assertThat(MainNavigationGestureResolver.resolveRelease(-distance, 0f, metrics).commits).isTrue()

    // Fling velocity behaves symmetrically too.
    val v = metrics.minimumFlingVelocity
    assertThat(MainNavigationGestureResolver.resolveRelease(0f, v, metrics).commits).isTrue()
    assertThat(MainNavigationGestureResolver.resolveRelease(0f, -v, metrics).commits).isTrue()
  }

  @Test
  fun `resistance and ownership behave symmetrically in both directions`() {
    assertThat(MainNavigationGestureResolver.applyResistance(120f, metrics))
      .isEqualTo(-MainNavigationGestureResolver.applyResistance(-120f, metrics))

    assertThat(
      MainNavigationGestureResolver.resolveOwner(NavigationGestureRegion.NAVIGATION, 150f, 10f, metrics)
    ).isEqualTo(NavigationGestureOwner.NAVIGATION)
    assertThat(
      MainNavigationGestureResolver.resolveOwner(NavigationGestureRegion.NAVIGATION, -150f, 10f, metrics)
    ).isEqualTo(NavigationGestureOwner.NAVIGATION)
  }

  // --------------------------------------------------------------------------------------------
  // Progress reporting.
  // --------------------------------------------------------------------------------------------

  @Test
  fun `progress is normalised against the slot width`() {
    assertThat(MainNavigationGestureResolver.progressFor(100f, metrics)).isEqualTo(0.5f)
    assertThat(MainNavigationGestureResolver.progressFor(-100f, metrics)).isEqualTo(-0.5f)
    assertThat(MainNavigationGestureResolver.progressFor(9_999f, metrics)).isEqualTo(1f)
  }

  @Test
  fun `progress is zero when the slot width is unknown`() {
    assertThat(MainNavigationGestureResolver.progressFor(100f, metrics.copy(slotWidthPx = 0f))).isEqualTo(0f)
  }

  @Test
  fun `center is the only settled position`() {
    assertThat(NavigationPosition.CENTER.isSettled).isTrue()
    assertThat(NavigationPosition.LEFT.isSettled).isFalse()
    assertThat(NavigationPosition.RIGHT.isSettled).isFalse()
  }

  @Test
  fun `only the navigation region may own a navigation gesture`() {
    assertThat(NavigationGestureRegion.NAVIGATION.canOwnNavigationGesture).isTrue()
    assertThat(NavigationGestureRegion.CONTENT.canOwnNavigationGesture).isFalse()
    assertThat(NavigationGestureRegion.UNKNOWN.canOwnNavigationGesture).isFalse()
  }
}
