/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.widget.discreteSeekBar.internal.compat;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;

/**
 * Class to wrap a {@link ValueAnimator}
 * for use with AnimatorCompat
 *
 * @hide
 * @see {@link AnimatorCompat}
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AnimatorCompatV11 extends AnimatorCompat {

  ValueAnimator animator;

  public AnimatorCompatV11(float start, float end, final AnimationFrameUpdateListener listener) {
    super();
    animator = ValueAnimator.ofFloat(start, end);
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        listener.onAnimationFrame((Float) animation.getAnimatedValue());
      }
    });
  }

  @Override
  public void cancel() {
    animator.cancel();
  }

  @Override
  public boolean isRunning() {
    return animator.isRunning();
  }

  @Override
  public void setDuration(int duration) {
    animator.setDuration(duration);
  }

  @Override
  public void start() {
    animator.start();
  }
}