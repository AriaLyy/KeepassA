/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.menu

import android.annotation.SuppressLint
import android.text.Html
import android.text.Spanned
import android.view.Gravity
import android.view.MenuInflater
import android.view.View
import android.widget.Button
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ReflectionUtil
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.event.DelEvent
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.VibratorUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import com.lyy.keepassa.view.dir.ChooseGroupActivity
import org.greenrobot.eventbus.EventBus

/**
 * 群组长按菜单
 * @param x 偏移量
 */
@SuppressLint("RestrictedApi")
class GroupPopMenu(
  private val context: FragmentActivity,
  view: View,
  private val pwGroup: PwGroupV4,
  private val x: Int,
  private val isInRecycleBin: Boolean = false
) : IPopMenu {
  private val popup: PopupMenu = PopupMenu(context, view, Gravity.START)
  private val help: MenuPopupHelper

  init {
    val inflater: MenuInflater = popup.menuInflater
    inflater.inflate(R.menu.pop_group_summary, popup.menu)

    popup.menu.findItem(R.id.undo)
      .isVisible = isInRecycleBin
    // 回收站不允许删除
    popup.menu.findItem(R.id.del)
      .isVisible = !(BaseApp.KDB!!.pm.recycleBin != null && BaseApp.KDB!!.pm.recycleBin == pwGroup)

    // 以下代码为强制显示icon
    val mPopup = ReflectionUtil.getField(PopupMenu::class.java, "mPopup")
    mPopup.isAccessible = true
    help = mPopup.get(popup) as MenuPopupHelper
    help.setForceShowIcon(true)
    popup.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.del -> {
          delGroup()
        }
        R.id.edit -> {
          Routerfit.create(DialogRouter::class.java).showModifyGroupDialog(pwGroup)
        }
        R.id.undo, R.id.move -> {
          ChooseGroupActivity.moveGroup(context, pwGroup.id)
        }
      }
      popup.dismiss()

      true
    }
  }

  /**
   * 删除群组
   */
  @SuppressLint("StringFormatMatches")
  private fun delGroup() {
    // 是否直接删除条目
    val deleteDirectly = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
      .getBoolean(context.getString(R.string.set_key_delete_no_recycle_bin), false)

    if (deleteDirectly) {
      handleDelGroup()
      return
    }
    val msg: Spanned = if (BaseApp.isV4) {
      if (isInRecycleBin) {
        Html.fromHtml(context.getString(R.string.hint_del_group_no_recycle, pwGroup.name))
      } else {
        Html.fromHtml(
          context.getString(R.string.hint_del_group, pwGroup.name, pwGroup.name)
        )
      }
    } else {
      Html.fromHtml(context.getString(R.string.hint_del_group_no_recycle, pwGroup.name))
    }

    Routerfit.create(DialogRouter::class.java).showMsgDialog(
      msgTitle = ResUtil.getString(R.string.del_group),
      msgContent = msg,
      btnClickListener = object : OnMsgBtClickListener {
        override fun onCover(v: Button) {
        }

        override fun onEnter(v: Button) {
          handleDelGroup()
        }

        override fun onCancel(v: Button) {
        }
      }
    )
  }

  /**
   * 处理删除群组
   */
  private fun handleDelGroup() {
    KpaUtil.kdbHandlerService.deleteGroup(pwGroup)
    KpaUtil.kdbHandlerService.saveDbByForeground { code ->
      EventBus.getDefault().post(DelEvent(pwGroup))
      if (code == DbSynUtil.STATE_SUCCEED) {
        HitUtil.toaskShort(
          "${context.getString(R.string.del_group)}${context.getString(R.string.success)}"
        )
      } else {
        HitUtil.toaskShort(context.getString(R.string.save_db_fail))
      }

      VibratorUtil.vibrator(300)
    }
  }

  public fun show() {
    help.show(x, 0)
  }

  fun getPopMenu(): PopupMenu {
    return popup
  }
}