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
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.arialyy.frame.util.ReflectionUtil
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.event.DelEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.VibratorUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.view.ChooseGroupActivity
import com.lyy.keepassa.view.dialog.LoadingDialog
import com.lyy.keepassa.view.dialog.ModifyGroupDialog
import com.lyy.keepassa.view.dialog.MsgDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * 群组长按菜单
 * @param x 偏移量
 */
@SuppressLint("RestrictedApi")
class GroupPopMenu(
  private val context: FragmentActivity,
  view: View,
  private val pwGroup: PwGroup,
  private val x: Int,
  private val isInRecycleBin: Boolean = false
) : IPopMenu {
  private val popup: PopupMenu = PopupMenu(context, view, Gravity.START)
  private val help: MenuPopupHelper
  private lateinit var loadDialog: LoadingDialog
  private val saveDbReqCode = 0xA6
  private val scope = MainScope()

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
          editGroup()
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
   * 编辑群组
   */
  private fun editGroup() {
    val dialog = ModifyGroupDialog.generate {
      modifyPwGroup = pwGroup
      build()
    }
    dialog.show(context.supportFragmentManager, "modify_group")
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
      loadDialog = LoadingDialog(context)
      loadDialog.show()
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

    val dialog = MsgDialog.generate {
      msgTitle = this@GroupPopMenu.context.getString(R.string.del_group)
      msgContent = msg
      build()
    }
    dialog.setOnBtClickListener(object : MsgDialog.OnBtClickListener {
      override fun onBtClick(
        type: Int,
        view: View
      ) {
        if (type == MsgDialog.TYPE_ENTER) {
          loadDialog = LoadingDialog(context)
          loadDialog.show()
          handleDelGroup()
        }
        dialog.dismiss()
      }
    })
    dialog.show()
  }

  /**
   * 处理删除群组
   */
  private fun handleDelGroup() {
    scope.launch {
      val code = withContext(Dispatchers.IO) {
        try {
          if (BaseApp.isV4) {
            if (BaseApp.KDB!!.pm.canRecycle(pwGroup) && !isInRecycleBin) {
              (BaseApp.KDB!!.pm as PwDatabaseV4).recycle(pwGroup as PwGroupV4)
            } else {
              // 回收站中直接删除
              KdbUtil.deleteGroup(pwGroup, false, needUpload = false)
            }
          } else {
            KdbUtil.deleteGroup(pwGroup, false, needUpload = false)
          }
          return@withContext KdbUtil.saveDb()
        } catch (e: Exception) {
          e.printStackTrace()
          HitUtil.toaskOpenDbException(e)
        }
        return@withContext DbSynUtil.STATE_SAVE_DB_FAIL
      }
      EventBus.getDefault()
          .post(DelEvent(pwGroup))


      if (code == DbSynUtil.STATE_SUCCEED) {
        HitUtil.toaskShort(
            "${context.getString(R.string.del_group)}${context.getString(R.string.success)}"
        )
      } else {
        HitUtil.toaskShort(context.getString(R.string.save_db_fail))
      }

      VibratorUtil.vibrator(300)
      loadDialog.dismiss()
      scope.cancel()
    }
  }

  public fun show() {
    help.show(x, 0)
  }

  fun getPopMenu(): PopupMenu {
    return popup
  }

}