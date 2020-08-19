/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.widget.discreteSeekBar.internal.drawable;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.annotation.NonNull;

/**
 * Simple {@link StateDrawable} implementation
 * to draw rectangles
 *
 * @hide
 */
public class TrackRectDrawable extends StateDrawable {
  public TrackRectDrawable(@NonNull ColorStateList tintStateList) {
    super(tintStateList);
  }

  @Override
  void doDraw(Canvas canvas, Paint paint) {
    canvas.drawRect(getBounds(), paint);
  }
}