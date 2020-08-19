/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.menu

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.MenuInflater
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentActivity
import com.arialyy.frame.util.ReflectionUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.event.DelAttrFileEvent
import com.lyy.keepassa.widget.expand.AttrFileItemView
import org.greenrobot.eventbus.EventBus

/**
 * 创建或编辑条附件悬浮框
 */
@SuppressLint("RestrictedApi")
class EntryCreateFilePopMenu(
  private val context: FragmentActivity,
  view: AttrFileItemView,
  private val key: String
) : IPopMenu {
  private val popup: PopupMenu = PopupMenu(context, view, Gravity.END)
  private val help: MenuPopupHelper

  init {
    val inflater: MenuInflater = popup.menuInflater
    inflater.inflate(R.menu.pop_create_entry_summary, popup.menu)
    popup.menu.findItem(R.id.edit)
        .isVisible = false
    popup.menu.findItem(R.id.del)
        .title = context.getString(R.string.del_attr_file)

    // 以下代码为强制显示icon
    val mPopup = ReflectionUtil.getField(PopupMenu::class.java, "mPopup")
    mPopup.isAccessible = true
    help = mPopup.get(popup) as MenuPopupHelper
    help.setForceShowIcon(true)
    popup.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.del -> {
          EventBus.getDefault().post(DelAttrFileEvent(key))
        }
      }
      popup.dismiss()
      true
    }

  }

  public fun show() {
    help.show()
  }
}