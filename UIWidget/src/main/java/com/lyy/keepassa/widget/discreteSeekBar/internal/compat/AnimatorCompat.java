/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.widget.discreteSeekBar.internal.compat;

import android.os.Build;

/**
 * Currently, there's no {@link android.animation.ValueAnimator} compatibility version
 * and as we didn't want to throw in external dependencies, we made this small class.
 * <p/>
 * <p>
 * This will work like {@link androidx.core.view.ViewPropertyAnimatorCompat}, that is,
 * not doing anything on API<11 and using the default {@link android.animation.ValueAnimator}
 * on API>=11
 * </p>
 * <p>
 * This class is used to provide animation to the {@link com.lyy.keepassa.widget.discreteSeekBar.DiscreteSeekBar}
 * when navigating with the Keypad
 * </p>
 *
 * @hide
 */
public abstract class AnimatorCompat {
  public interface AnimationFrameUpdateListener {
    public void onAnimationFrame(float currentValue);
  }

  AnimatorCompat() {

  }

  public abstract void cancel();

  public abstract boolean isRunning();

  public abstract void setDuration(int progressAnimationDuration);

  public abstract void start();

  public static final AnimatorCompat create(float start, float end,
      AnimationFrameUpdateListener listener) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      return new AnimatorCompatV11(start, end, listener);
    } else {
      return new AnimatorCompatBase(start, end, listener);
    }
  }

  private static class AnimatorCompatBase extends AnimatorCompat {

    private final AnimationFrameUpdateListener mListener;
    private final float mEndValue;

    public AnimatorCompatBase(float start, float end, AnimationFrameUpdateListener listener) {
      mListener = listener;
      mEndValue = end;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isRunning() {
      return false;
    }

    @Override
    public void setDuration(int progressAnimationDuration) {

    }

    @Override
    public void start() {
      mListener.onAnimationFrame(mEndValue);
    }
  }
}