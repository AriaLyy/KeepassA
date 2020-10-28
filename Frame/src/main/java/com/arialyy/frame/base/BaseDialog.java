/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.base;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.arialyy.frame.core.AbsDialogFragment;
import com.arialyy.frame.core.AbsFrame;
import com.arialyy.frame.util.AndroidUtils;

/**
 * Created by Aria.Lao on 2017/12/4.
 */

public abstract class BaseDialog<VB extends ViewDataBinding> extends AbsDialogFragment<VB> {
  private WindowManager.LayoutParams mWpm;
  private Window mWindow;
  // 不要使用动画，莫名奇妙出现not associated with a fragment manager. 问题
  protected boolean useDefaultAnim = false;

  /**
   * 需要设置参数的构造函数，否则容易出现 could not find Fragment constructor
   * https://www.jianshu.com/p/e8c831e9ae73
   */
  public BaseDialog() {

  }

  @Override protected void initData() {
    mWindow = getDialog().getWindow();
    if (mWindow != null) {
      mWpm = mWindow.getAttributes();
    }
    if (mWpm != null && mWindow != null) {
      //mView = mWindow.getDecorView();
      //mRootView.setBackgroundColor(Color.WHITE);
      mWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      //in();
      if (useDefaultAnim) {
        in1();
      }
    }
  }

  public void show() {
    FragmentManager fm = AbsFrame.getInstance().getCurrentActivity().getSupportFragmentManager();
    if (fm.isDestroyed()) {
      Log.e(TAG, "FragmentManager 已被销毁");
      return;
    }
    try {
      show(fm, getClass().getSimpleName());
    }catch (Exception e){
      e.printStackTrace();
      fm.beginTransaction().add(this, getClass().getSimpleName()).commitAllowingStateLoss();
    }
  }

  @Override public void dismiss() {
    if (mWpm != null && mWindow != null) {
      if (useDefaultAnim) {
        out();
      } else {
        super.dismiss();
      }
    } else {
      super.dismiss();
    }
  }

  @Override protected void dataCallback(int result, Object data) {

  }

  /**
   * 进场动画
   */
  private void in() {
    int height = AndroidUtils.getScreenParams(getContext())[1];
    ValueAnimator animator = ValueAnimator.ofObject(new IntEvaluator(), -height / 2, 0);
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override public void onAnimationUpdate(ValueAnimator animation) {
        mWpm.y = (int) animation.getAnimatedValue();
        mWindow.setAttributes(mWpm);
      }
    });
    animator.setInterpolator(new BounceInterpolator()); //弹跳
    Animator alpha = ObjectAnimator.ofFloat(mRootView, "alpha", 0f, 1f);
    AnimatorSet set = new AnimatorSet();
    set.play(animator).with(alpha);
    set.setDuration(2000).start();
  }

  private void in1() {
    Animator alpha = ObjectAnimator.ofFloat(mRootView, "alpha", 0f, 1f);
    alpha.setDuration(400);
    alpha.start();
  }

  /**
   * 重力动画
   */
  private void out() {
    int height = AndroidUtils.getScreenParams(getContext())[1];
    ValueAnimator animator = ValueAnimator.ofObject(new IntEvaluator(), 0, height / 3);
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override public void onAnimationUpdate(ValueAnimator animation) {
        mWpm.y = (int) animation.getAnimatedValue();
        mWindow.setAttributes(mWpm);
      }
    });
    Animator alpha = ObjectAnimator.ofFloat(mRootView, "alpha", 1f, 0f);
    AnimatorSet set = new AnimatorSet();
    set.play(animator).with(alpha);
    set.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationEnd(Animator animation) {
        if (BaseDialog.this.isAdded() && BaseDialog.this.getFragmentManager() != null) {
          BaseDialog.super.dismissAllowingStateLoss();
        }
      }
    });
    set.setDuration(400).start();
  }
}