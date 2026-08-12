/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.thoughtcrime.securesms.registration.ui.permissions

import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.registration.fragments.WelcomePermissions
import org.thoughtcrime.securesms.registration.ui.welcome.WelcomeUserSelection
import org.thoughtcrime.securesms.util.BackupUtil

class GrantPermissionsFragment : ComposeFragment() {
  companion object {
    private val TAG = Log.tag(GrantPermissionsFragment::class.java)
    const val REQUEST_KEY = "GrantPermissionsFragment"
  }

  private val args by navArgs<GrantPermissionsFragmentArgs>()

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
    ::onPermissionsGranted
  )

  private val welcomeUserSelection: WelcomeUserSelection by lazy { args.welcomeUserSelection }

  @Composable
  override fun FragmentContent() {
    GrantPermissionsScreen(
      deviceBuildVersion = Build.VERSION.SDK_INT,
      isBackupSelectionRequired = BackupUtil.isUserSelectionRequired(LocalContext.current),
      onNextClicked = this::launchPermissionRequests,
      onNotNowClicked = this::proceedToNextScreen
    )
  }

  private fun launchPermissionRequests() {
    val isUserSelectionRequired = BackupUtil.isUserSelectionRequired(requireContext())
    val basePermissions = WelcomePermissions.getWelcomePermissions(isUserSelectionRequired)
    requestPermissionLauncher.launch(basePermissions)
  }

  private fun onPermissionsGranted(permissions: Map<String, Boolean>) {
    proceedToNextScreen()
    permissions.forEach {
      Log.d(TAG, "${it.key} = ${it.value}")
    }
  }

  private fun proceedToNextScreen() {
    setFragmentResult(REQUEST_KEY, bundleOf(REQUEST_KEY to welcomeUserSelection))
    findNavController().popBackStack()
  }
}