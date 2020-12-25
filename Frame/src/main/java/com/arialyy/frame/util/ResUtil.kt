/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.arialyy.frame.util

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.arialyy.frame.base.FrameApp

/**
 * @Author laoyuyu
 * @Description 资源操作类
 * @Date 2020/10/29
 **/
object ResUtil {

  /**
   * 颜色兼容工具
   */
  fun getColor(@ColorRes color: Int): Int {

    return if (VERSION.SDK_INT >= VERSION_CODES.M) {
      FrameApp.app.resources.getColor(color, FrameApp.app.theme)
    } else {
      FrameApp.app.resources.getColor(color)
    }
  }

  /**
   * 获取svg图标
   * @param drawableId svg 图标
   * @param colorId 颜色Id
   */
  fun getSvgIcon(
    @DrawableRes drawableId: Int,
    @ColorRes colorId: Int
  ): VectorDrawableCompat? {
    val vector = VectorDrawableCompat.create(FrameApp.app.resources, drawableId, FrameApp.app.theme)
    vector?.setTint(getColor(colorId))
    return vector
  }
}