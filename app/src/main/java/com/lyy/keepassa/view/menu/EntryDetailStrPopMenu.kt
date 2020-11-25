/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentActivity
import com.arialyy.frame.util.ReflectionUtil
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.util.ClipboardUtil
import com.lyy.keepassa.util.HitUtil

/**
 * 项目详情自定义字段悬浮菜单
 * @param x 偏移量
 */
@SuppressLint("RestrictedApi")
class EntryDetailStrPopMenu(
  private val context: FragmentActivity,
  view: View,
  private val str: ProtectedString
) : IPopMenu {
  private val popup: PopupMenu = PopupMenu(context, view, Gravity.END)
  private val help: MenuPopupHelper
  private var showPassCallback: OnShowPassCallback? = null
  private var showPass = true

  interface OnShowPassCallback {
    fun showPass(showPass: Boolean)
  }

  init {
    val inflater: MenuInflater = popup.menuInflater
    inflater.inflate(R.menu.entry_detail_text_summary, popup.menu)

    // 是否显示密码
    popup.menu.findItem(R.id.show_pass).isVisible = str.isProtected
    // 是否显示打开url
    popup.menu.findItem(R.id.open_url).isVisible =
      str.toString()
          .startsWith("http", ignoreCase = true)

    // 以下代码为强制显示icon
    val mPopup = ReflectionUtil.getField(PopupMenu::class.java, "mPopup")
    mPopup.isAccessible = true
    help = mPopup.get(popup) as MenuPopupHelper
    help.setForceShowIcon(true)
    popup.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.copy_clip -> {
          ClipboardUtil.get()
              .copyDataToClip(str.toString())
          HitUtil.toaskShort(context.getString(R.string.hint_copy_to_clip))
        }
        R.id.open_url -> {
          val uri: Uri = Uri.parse(str.toString())
          val intent = Intent(Intent.ACTION_VIEW, uri)
          context.startActivity(intent)
        }
        R.id.show_pass -> {
          if (showPassCallback != null) {
            showPassCallback!!.showPass(showPass)
          }
        }
      }
      popup.dismiss()

      true
    }
  }

  /**
   * 设置隐藏密码
   */
  fun setHidePass() {
    showPass = false
    val item = popup.menu.findItem(R.id.show_pass)
    item.icon = context.getDrawable(R.drawable.ic_view_off_black)
    item.title = context.getString(R.string.hide_pass)
  }

  fun setOnShowPassCallback(callback: OnShowPassCallback) {
    this.showPassCallback = callback
  }

  fun show() {
    popup.show()
  }

  fun show(
    x: Int,
    y: Int
  ) {
    help.show(x, y)
  }

  fun getPopMenu(): PopupMenu {
    return popup
  }

}