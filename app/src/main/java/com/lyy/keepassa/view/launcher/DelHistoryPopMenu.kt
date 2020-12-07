/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import com.arialyy.frame.util.ReflectionUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.view.menu.IPopMenu

/**
 * @Author laoyuyu
 * @Description
 * @Date 2020/12/7
 **/
@SuppressLint("RestrictedApi")
class DelHistoryPopMenu(
  val context: Context,
  val view: View,
  val curX: Int
) : IPopMenu {
  private val popup: PopupMenu = PopupMenu(context, view, Gravity.START)
  private val help: MenuPopupHelper

  init {
    val inflater: MenuInflater = popup.menuInflater
    inflater.inflate(R.menu.pop_dele_history_record, popup.menu)
    // 以下代码为强制显示icon
    val mPopup = ReflectionUtil.getField(PopupMenu::class.java, "mPopup")
    mPopup.isAccessible = true
    help = mPopup.get(popup) as MenuPopupHelper
    help.setForceShowIcon(true)
  }

  fun getPopMenu():PopupMenu{
    return popup
  }

  fun show(){
    help.show(curX, 0)
  }

}