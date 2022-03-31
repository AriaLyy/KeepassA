/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.main

import android.content.Context
import android.widget.Button
import androidx.core.content.edit
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.AndroidUtils
import com.arialyy.frame.util.ResUtil
import com.lahm.library.EasyProtectorLib
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.view.UpgradeLogDialog
import com.lyy.keepassa.view.dialog.DonateDialog
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import com.lyy.keepassa.widget.BubbleTextView
import com.lyy.keepassa.widget.BubbleTextView.OnIconClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime

class MainModule : BaseModule() {

  private var upgradeLogDialogIsShow = false
  private var devBirthDayDialogIsShow = false

  override fun onCleared() {
    super.onCleared()
  }

  /**
   * 安全级别对话框
   * root 红色，提示危险
   */
  fun setEcoIcon(
    cxt: Context,
    btText: BubbleTextView
  ) {
    val needCheckEnv = PreferenceManager.getDefaultSharedPreferences(cxt)
      .getBoolean(cxt.resources.getString(R.string.set_key_need_root_check), true)
    if (!needCheckEnv) {
      btText.clearIcon(BubbleTextView.LOCATION_RIGHT)
      return
    }

    var vector = ResUtil.getSvgIcon(R.drawable.ic_eco, R.color.green)
    var msg = cxt.getString(R.string.hint_security_green)
    if (EasyProtectorLib.checkIsRoot()) {
      vector = ResUtil.getSvgIcon(R.drawable.ic_eco, R.color.red)
      msg = cxt.getString(R.string.hint_security_red)
    } else if (EasyProtectorLib.checkIsRunningInEmulator(cxt) {
//          BuglyLog.d(TAG, it)
      }) {
      vector = ResUtil.getSvgIcon(R.drawable.ic_eco, R.color.yellow)
      msg = cxt.getString(R.string.hint_security_yellow)
    }
    btText.setEndIcon(vector!!)
    btText.setOnIconClickListener(object : OnIconClickListener {
      override fun onClick(
        view: BubbleTextView,
        index: Int
      ) {
        if (index == 2) {
          Routerfit.create(DialogRouter::class.java).showMsgDialog(
            msgContent = msg,
            showCancelBt = false,
            msgTitleEndIcon = vector
          )
        }
      }
    })
  }

  fun showInfoDialog(activity: BaseActivity<*>) {
    showVersionLog(activity)
    if (!upgradeLogDialogIsShow) {
      checkDevBirthdayData(activity)
      return
    }
    if (!devBirthDayDialogIsShow) {
      showDonateDialog(activity)
      return
    }
  }

  private fun showDonateDialog(context: Context) {
    val pre = context.getSharedPreferences(Constance.PRE_FILE_NAME, Context.MODE_PRIVATE)
    val startNum = pre.getInt(Constance.PRE_KEY_START_APP_NUM, 0)
    if (startNum >= Constance.START_DONATE_JUDGMENT_VALUE) {
      val donateDialog = DonateDialog()
      donateDialog.setOnDismissListener {
        pre.edit {
          putInt(Constance.PRE_KEY_START_APP_NUM, 0)
        }
      }
      donateDialog.show()
    }
  }

  /**
   * 显示版本日志对话框，显示逻辑：
   * 配置文件的版本号不存在，或当前版本号大于配置文件的版本号
   */
  private fun showVersionLog(activity: BaseActivity<*>) {
    upgradeLogDialogIsShow = true
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        delay(600)
      }
      if (activity.isDestroyed || activity.isFinishing) {
        return@launch
      }
      val sharedPreferences =
        activity.getSharedPreferences(Constance.PRE_FILE_NAME, Context.MODE_PRIVATE)
      val versionCode = sharedPreferences.getInt(Constance.VERSION_CODE, -1)
      if (versionCode < 0 || versionCode < AndroidUtils.getVersionCode(activity)) {
        UpgradeLogDialog().show()
      }
    }
  }

  /**
   * 检查是否有记录
   */
  fun checkHasHistoryRecord() = liveData {
    BaseApp.dbRecord?.let {
      if (BaseApp.dbRecord == null || it.localDbUri.isNullOrEmpty()) {
        emit(false)
        return@liveData
      }
      val b = withContext(Dispatchers.IO) {
        val dao = BaseApp.appDatabase.entryRecordDao()
        dao.hasRecord(it.localDbUri) > 0
      }
      emit(b)
    }

  }


  private fun checkDevBirthdayData(context: Context) {
//    val dt = DateTime(2020, 10, 2, 0, 0)
    val dt = DateTime(System.currentTimeMillis())
    if (dt.monthOfYear == 10 && dt.dayOfMonth == 2) {
      showDevBirthdayDialog(context)
    }
  }

  private fun showDevBirthdayDialog(context: Context) {
    devBirthDayDialogIsShow = true
    Routerfit.create(DialogRouter::class.java).showMsgDialog(
      msgTitle = ResUtil.getString(R.string.donate),
      msgContent = ResUtil.getString(R.string.dev_birthday),
      cancelText = "NO",
      enterText = "YES",
      btnClickListener = object : OnMsgBtClickListener {
        override fun onCover(v: Button) {
        }

        override fun onEnter(v: Button) {
          DonateDialog().show()
        }

        override fun onCancel(v: Button) {
        }
      }
    )
  }
}