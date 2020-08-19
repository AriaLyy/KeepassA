/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

/**
 * 常用单位转换的辅助类
 */
public class DpUtils {
  private DpUtils() {
    /* cannot be instantiated */
    throw new UnsupportedOperationException("cannot be instantiated");
  }

  private static final float DENSITY = Resources.getSystem().getDisplayMetrics().density;

  /**
   * 另外一种dp转PX方法
   */
  public static int dp2px(int dp) {
    return Math.round(dp * DENSITY);
  }

  /**
   * dp转px
   */
  public static int dp2px(Context context, float dpVal) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        dpVal, context.getResources().getDisplayMetrics());
  }

  /**
   * sp转px
   */
  public static int sp2px(Context context, float spVal) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
        spVal, context.getResources().getDisplayMetrics());
  }

  /**
   * px转dp
   */
  public static float px2dp(Context context, float pxVal) {
    final float scale = context.getResources().getDisplayMetrics().density;
    return (pxVal / scale);
  }

  /**
   * px转sp
   */
  public static float px2sp(Context context, float pxVal) {
    return (pxVal / context.getResources().getDisplayMetrics().scaledDensity);
  }
}