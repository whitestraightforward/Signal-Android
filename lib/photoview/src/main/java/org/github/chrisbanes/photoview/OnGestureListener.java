/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.github.chrisbanes.photoview;

interface OnGestureListener {

    void onDrag(float dx, float dy);

    void onFling(float startX, float startY, float velocityX,
                 float velocityY);

    void onScale(float scaleFactor, float focusX, float focusY);

    void onScale(float scaleFactor, float focusX, float focusY, float dx, float dy);
}