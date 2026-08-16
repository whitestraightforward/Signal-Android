# Navigation Gesture System

A Telegram-style navigation gesture layer for the floating bottom navigation bar.

The system is built **inside** the existing navigation architecture. There is no global swipe
listener and nothing captures the application window. Instead a small gesture-management layer
decides, per gesture, which component is allowed to move.

## 1. Architecture

```
Main Application Window (MainActivity)
        |
Gesture Recognition Layer (MainNavigationGestureLayer.kt)
        |
        +----------------------------+
        |                            |
Navigation Gestures            Content Gestures
        |                            |
MainNavigationBar /            Chats / Messages /
MainBottomChrome               Settings / Lists / Media
```

| File | Responsibility |
| --- | --- |
| `MainNavigationGestureResolver.kt` | Pure, framework-free arbitration and physics. |
| `MainNavigationGestureLayer.kt` | Compose recognition layer, `MainNavigationGestureState`, spring animation. |
| `MainNavigation.kt` | Gesture container, navigation layout, detects navigation interactions. |
| `MainNavigationViewModel.kt` | Navigation position, current location, animation and interaction state. |
| `MainBottomChrome.kt` | Renders the movable navigation chrome and applies position changes. |
| `MainActivity.kt` | Owns the single shared gesture state; marks the content region. |
| `MainNavigationRouter.kt` | **Unchanged.** Destinations and routes are untouched. |
| `MainToolbar.kt` | **Unchanged.** Stays synchronised via `navigationGestureProgress`. |
| `MainFloatingActionButtons.kt` | **Unchanged.** Rides along inside the bottom chrome. |
| `MainNavigationDetailLocation.kt`, `MainDetailBackStack.kt`, `EmptyDetailScreen.kt` | **Unchanged.** Flow and back stack behaviour preserved. |

## 2. Touch detection

On touch down the layer records the starting position, the current active interface, the region
under the finger (`NavigationGestureRegion`) and whether any other component has already consumed
the pointer. Ownership is `UNDECIDED` until the movement is unambiguous — nothing moves before
then.

## 3. Gesture priority

**Priority 1 — Interface content.** Conversations, the chat list, archived chats, settings lists,
profile lists and media viewers always win. A gesture is handed to content when:

* it did not start on a navigation component, or
* another component already consumed the pointer (list fling, item swipe, archive swipe), or
* the movement is vertical-ish.

Once content owns a gesture the navigation layer bails out and never touches the pointer again.

**Priority 2 — Navigation components.** A clearly horizontal drag (|dx| ≥ 1.4·|dy|, past touch
slop) that started on the navigation bar is claimed by navigation, which then follows the finger.

## 4. Movement and resistance

```
navigationOffset = gestureDistance × 0.8

Finger moves ← 200 px  ⇒  Navigation moves ← 160 px
```

Travel is clamped to one navigation slot.

## 5. Release and snap

On release the layer measures the current position, the swipe distance and the swipe velocity:

* **Small movement** (below both the distance and the velocity threshold) → springs back to
  `CENTER`, no destination change.
* **Long drag or fast fling right** → locks to `RIGHT`, moves to the **next** destination.
* **Long drag or fast fling left** → locks to `LEFT`, moves to the **previous** destination.
* **At an edge** with nowhere to go → always returns to rest.

Velocity beats a stale offset, so flicking back the other way follows the flick. Layout direction
is honoured: in RTL the physical directions map to the mirrored logical destinations.

### Direction mapping

With the tab order `chats · calls · stories · settings · profile`:

| Current tab | Swipe right | Swipe left |
| --- | --- | --- |
| chats | calls | *(start of list)* |
| calls | stories | chats |
| stories | settings | calls |
| settings | profile | stories |
| profile | *(end of list)* | settings |

Both directions are symmetric and share identical distance/velocity thresholds. The mapping is
deliberately **not** the "drag the strip of tabs" model: because the default tab (Chats) is at
index 0, mapping a right swipe to PREVIOUS made it hit the start-of-list edge guard and silently
do nothing on the very screen the app opens on. Mapping right → NEXT keeps both directions alive
everywhere except the genuine ends of the list.

## 6. Animation

Every settle uses one under-damped spring (`dampingRatio = 0.78`, `StiffnessMediumLow`), giving
smooth acceleration, natural deceleration and spring-based settling with no sudden jumps. A
committing swipe follows through in the direction of the throw, applies the destination change,
then glides in from the opposite side so the swap reads as a single continuous movement.

## 7. System gesture edges

The navigation bar spans the full width, so its ends sit underneath the Android system
back-gesture strips (~20dp on each vertical screen edge under edge-to-edge). The platform claims
horizontal drags that start there before Compose sees them.

Because a right swipe naturally begins on the left-hand tab, that made right swipes appear
completely dead while left swipes worked. The bar therefore opts out via
`Modifier.systemGestureExclusion()`, the same approach already used by `VideoTrimBar`. The
exclusion is scoped to the navigation bar only, so the system back gesture keeps working
everywhere else.

## 8. Conflict prevention

Navigation gestures can never override message scrolling, chat scrolling, archive swipe actions,
list scrolling, text input or media gestures. `Modifier.navigationContentGestureRegion` marks the
whole content pane as content-owned, and the recognition loop yields immediately on any consumed
pointer or vertical intent.

## 9. Scope

Only the navigation interaction layer changed. Messaging, network, data handling, existing
features and application workflows are untouched.
