/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.github.chrisbanes.photoview;

import android.widget.ImageView;

/**
 * Callback when the user tapped outside of the photo
 */
public interface OnOutsidePhotoTapListener {

    /**
     * The outside of the photo has been tapped
     */
    void onOutsidePhotoTap(ImageView imageView);
}
