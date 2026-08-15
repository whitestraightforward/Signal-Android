/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.core.ui.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

/**
 * Comprehensive typography token system for the Signal messaging application.
 *
 * Defines semantic text styles for all UI contexts:
 * - **Display**: Hero headlines, splash text
 * - **Headline**: Screen titles, section headers
 * - **Title**: Toolbar titles, card titles
 * - **Body**: Conversation text, settings descriptions
 * - **Label**: Button labels, tags, badges
 * - **Caption**: Timestamps, metadata, footnotes
 * - **Message-specific**: Bubble text, composer text, footer text
 *
 * All typography tokens are immutable and provided via CompositionLocal.
 */
@Immutable
data class SignalTypography(
  // Display
  val displayLarge: TextStyle,
  val displayMedium: TextStyle,
  val displaySmall: TextStyle,

  // Headline
  val headlineLarge: TextStyle,
  val headlineMedium: TextStyle,
  val headlineSmall: TextStyle,

  // Title
  val titleLarge: TextStyle,
  val titleMedium: TextStyle,
  val titleSmall: TextStyle,

  // Body
  val bodyLarge: TextStyle,
  val bodyMedium: TextStyle,
  val bodySmall: TextStyle,

  // Label
  val labelLarge: TextStyle,
  val labelMedium: TextStyle,
  val labelSmall: TextStyle,

  // Caption / Timestamp
  val caption: TextStyle,
  val captionSmall: TextStyle,

  // Message-specific
  val messageBody: TextStyle,
  val messageBodyLarge: TextStyle,
  val messageFooter: TextStyle,
  val messageTimestamp: TextStyle,
  val messageSubject: TextStyle,
  val messageSystem: TextStyle,

  // Composer
  val composerInput: TextStyle,
  val composerHint: TextStyle,
  val composerLabel: TextStyle,

  // Navigation
  val navigationLabel: TextStyle,
  val navigationActive: TextStyle,
  val navigationInactive: TextStyle,

  // Button
  val buttonLarge: TextStyle,
  val buttonMedium: TextStyle,
  val buttonSmall: TextStyle,

  // Toolbar
  val toolbarTitle: TextStyle,
  val toolbarSubtitle: TextStyle,
  val toolbarAction: TextStyle,

  // Settings
  val settingsTitle: TextStyle,
  val settingsSubtitle: TextStyle,
  val settingsDescription: TextStyle,
  val settingsSectionHeader: TextStyle,

  // Search
  val searchInput: TextStyle,
  val searchPlaceholder: TextStyle,
  val searchResultTitle: TextStyle,
  val searchResultSnippet: TextStyle,

  // Dialog
  val dialogTitle: TextStyle,
  val dialogBody: TextStyle,
  val dialogAction: TextStyle,

  // Notification
  val notificationTitle: TextStyle,
  val notificationBody: TextStyle,
  val notificationTimestamp: TextStyle,

  // Badge
  val badge: TextStyle,
  val badgeLarge: TextStyle,

  // Tab
  val tabLabel: TextStyle,
  val tabLabelActive: TextStyle,
  val tabLabelInactive: TextStyle,

  // BottomSheet
  val bottomSheetTitle: TextStyle,
  val bottomSheetSubtitle: TextStyle,
  val bottomSheetAction: TextStyle,

  // Media
  val mediaTitle: TextStyle,
  val mediaCaption: TextStyle,
  val mediaOverlayText: TextStyle,

  // Story
  val storyText: TextStyle,
  val storyCaption: TextStyle,

  // Release Notes
  val releaseNotesTitle: TextStyle,
  val releaseNotesBody: TextStyle,

  // Chat-specific
  val chatGroupName: TextStyle,
  val chatGroupDescription: TextStyle,
  val chatGroupMember: TextStyle,
  val chatInviteText: TextStyle,

  // Contacts
  val contactName: TextStyle,
  val contactSubtitle: TextStyle,
  val contactDetail: TextStyle,

  // Profile
  val profileName: TextStyle,
  val profileBio: TextStyle,
  val profileStatus: TextStyle,

  // Backup
  val backupTitle: TextStyle,
  val backupBody: TextStyle,
  val backupLabel: TextStyle,

  // Error/Warning
  val errorTitle: TextStyle,
  val errorBody: TextStyle,
  val warningTitle: TextStyle,
  val warningBody: TextStyle,

  // Status / Delivery
  val deliveryStatus: TextStyle,
  val readReceipt: TextStyle,

  // Emoji
  val emojiLarge: TextStyle,
  val emojiMedium: TextStyle,
  val emojiSmall: TextStyle,

  // Code
  val code: TextStyle,
  val codeBlock: TextStyle
)

val LocalSignalTypography = staticCompositionLocalOf {
  signalTypography()
}

/** Create the default Signal typography system. */
internal fun signalTypography(): SignalTypography {
  val base = Typography()

  fun TextStyle.copy(
    fontSize: Int = this.fontSize.value.toInt(),
    lineHeight: Int = this.lineHeight.value.toInt(),
    letterSpacing: Float = this.letterSpacing.value,
    fontWeight: FontWeight = this.fontWeight,
    fontStyle: FontStyle = this.fontStyle,
    fontFamily: FontFamily = this.fontFamily
  ) = TextStyle(
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
    fontFamily = fontFamily
  )

  return SignalTypography(
    // Display
    displayLarge = base.displayLarge.copy(fontSize = 36, lineHeight = 44, letterSpacing = 0),
    displayMedium = base.displayMedium.copy(fontSize = 32, lineHeight = 40, letterSpacing = 0),
    displaySmall = base.displaySmall.copy(fontSize = 28, lineHeight = 36, letterSpacing = 0),

    // Headline
    headlineLarge = base.headlineLarge.copy(fontSize = 32, lineHeight = 40, letterSpacing = 0),
    headlineMedium = base.headlineMedium.copy(fontSize = 28, lineHeight = 36, letterSpacing = 0),
    headlineSmall = base.headlineSmall.copy(fontSize = 24, lineHeight = 32, letterSpacing = 0),

    // Title
    titleLarge = base.titleLarge.copy(fontSize = 22, lineHeight = 28, letterSpacing = 0),
    titleMedium = base.titleMedium.copy(fontSize = 18, lineHeight = 24, letterSpacing = 0),
    titleSmall = base.titleSmall.copy(fontSize = 16, lineHeight = 22, letterSpacing = 0),

    // Body
    bodyLarge = base.bodyLarge.copy(fontSize = 16, lineHeight = 22, letterSpacing = 0),
    bodyMedium = base.bodyMedium.copy(fontSize = 14, lineHeight = 20, letterSpacing = 0),
    bodySmall = base.bodySmall.copy(fontSize = 13, lineHeight = 16, letterSpacing = 0),

    // Label
    labelLarge = base.labelLarge.copy(fontSize = 14, lineHeight = 20, letterSpacing = 0),
    labelMedium = base.labelMedium.copy(fontSize = 13, lineHeight = 16, letterSpacing = 0),
    labelSmall = base.labelSmall.copy(fontSize = 12, lineHeight = 16, letterSpacing = 0),

    // Caption
    caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.sp, fontFamily = FontFamily.SansSerif),
    captionSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.02.sp, fontFamily = FontFamily.SansSerif),

    // Message-specific
    messageBody = base.bodyMedium,
    messageBodyLarge = base.bodyLarge,
    messageFooter = base.labelSmall,
    messageTimestamp = base.labelSmall,
    messageSubject = base.titleMedium,
    messageSystem = base.labelMedium,

    // Composer
    composerInput = base.bodyLarge,
    composerHint = base.bodyMedium,
    composerLabel = base.labelMedium,

    // Navigation
    navigationLabel = base.labelSmall,
    navigationActive = base.labelMedium.copy(fontWeight = FontWeight.Medium),
    navigationInactive = base.labelMedium,

    // Button
    buttonLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
    buttonMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium),
    buttonSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium),

    // Toolbar
    toolbarTitle = base.titleLarge.copy(fontWeight = FontWeight.Medium),
    toolbarSubtitle = base.bodySmall,
    toolbarAction = base.bodySmall.copy(fontWeight = FontWeight.Medium),

    // Settings
    settingsTitle = base.titleMedium,
    settingsSubtitle = base.bodyMedium,
    settingsDescription = base.bodySmall,
    settingsSectionHeader = base.labelLarge.copy(fontWeight = FontWeight.Medium),

    // Search
    searchInput = base.bodyLarge,
    searchPlaceholder = base.bodyMedium,
    searchResultTitle = base.titleSmall,
    searchResultSnippet = base.bodySmall,

    // Dialog
    dialogTitle = base.titleLarge,
    dialogBody = base.bodyMedium,
    dialogAction = base.labelLarge.copy(fontWeight = FontWeight.Medium),

    // Notification
    notificationTitle = base.titleSmall.copy(fontWeight = FontWeight.Medium),
    notificationBody = base.bodySmall,
    notificationTimestamp = base.captionSmall,

    // Badge
    badge = base.labelSmall.copy(fontWeight = FontWeight.Bold),
    badgeLarge = base.labelMedium.copy(fontWeight = FontWeight.Bold),

    // Tab
    tabLabel = base.labelMedium,
    tabLabelActive = base.labelMedium.copy(fontWeight = FontWeight.Bold),
    tabLabelInactive = base.labelMedium,

    // BottomSheet
    bottomSheetTitle = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    bottomSheetSubtitle = base.bodySmall,
    bottomSheetAction = base.labelLarge.copy(fontWeight = FontWeight.Medium),

    // Media
    mediaTitle = base.titleSmall,
    mediaCaption = base.caption,
    mediaOverlayText = base.labelMedium,

    // Story
    storyText = base.bodyLarge,
    storyCaption = base.bodySmall,

    // Release Notes
    releaseNotesTitle = base.titleLarge,
    releaseNotesBody = base.bodyMedium,

    // Chat-specific
    chatGroupName = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    chatGroupDescription = base.bodySmall,
    chatGroupMember = base.bodyMedium,
    chatInviteText = base.bodyMedium,

    // Contacts
    contactName = base.titleSmall.copy(fontWeight = FontWeight.Medium),
    contactSubtitle = base.bodySmall,
    contactDetail = base.bodySmall,

    // Profile
    profileName = base.headlineMedium,
    profileBio = base.bodyMedium,
    profileStatus = base.bodySmall,

    // Backup
    backupTitle = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    backupBody = base.bodyMedium,
    backupLabel = base.labelMedium,

    // Error/Warning
    errorTitle = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    errorBody = base.bodyMedium,
    warningTitle = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    warningBody = base.bodyMedium,

    // Status / Delivery
    deliveryStatus = base.labelSmall,
    readReceipt = base.labelSmall,

    // Emoji
    emojiLarge = TextStyle(fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    emojiMedium = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    emojiSmall = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp),

    // Code
    code = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.01.sp, fontFamily = FontFamily.Monospace),
    codeBlock = TextStyle(fontSize = 13.sp, lineHeight = 20.sp, letterSpacing = 0.01.sp, fontFamily = FontFamily.Monospace)
  )
}

/**
 * Convert SignalTypography to a Material3 Typography object for backward compatibility.
 */
internal fun SignalTypography.toMaterial3Typography(): Typography = Typography(
  displayLarge = displayLarge,
  displayMedium = displayMedium,
  displaySmall = displaySmall,
  headlineLarge = headlineLarge,
  headlineMedium = headlineMedium,
  headlineSmall = headlineSmall,
  titleLarge = titleLarge,
  titleMedium = titleMedium,
  titleSmall = titleSmall,
  bodyLarge = bodyLarge,
  bodyMedium = bodyMedium,
  bodySmall = bodySmall,
  labelLarge = labelLarge,
  labelMedium = labelMedium,
  labelSmall = labelSmall
)