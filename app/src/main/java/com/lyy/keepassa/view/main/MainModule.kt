/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.main

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.arialyy.frame.util.AndroidUtils
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.Database
import com.keepassdroid.database.PwDatabase
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.helper.KDBHandlerHelper
import com.keepassdroid.utils.Types
import com.keepassdroid.utils.UriUtil
import com.lahm.library.EasyProtectorLib
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.entity.DbRecord
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.view.UpgradeLogDialog
import com.lyy.keepassa.view.dialog.DonateDialog
import com.lyy.keepassa.view.dialog.MsgDialog
import com.lyy.keepassa.widget.BubbleTextView
import com.lyy.keepassa.widget.BubbleTextView.OnIconClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime

class MainModule : BaseModule() {

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
    if (!needCheckEnv){
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
          val msgDialog = MsgDialog.generate {
            msgTitle = cxt.getString(R.string.hint)
            msgContent = msg
            showCancelBt = false
            msgTitleEndIcon = vector
            build()
          }
          msgDialog.show()
        }
      }
    })
  }

  /**
   * 同步数据库
   */
  fun syncDb() = liveData(Dispatchers.IO) {
    val code = KdbUtil.saveDb(true)
    Log.d(TAG, "同步数据库结束，code = $code")
    if (code != DbSynUtil.STATE_SUCCEED) {
      emit(false)
      return@liveData
    }
    emit(true)
  }

  private fun openDbFile(
    context: Context,
    record: DbRecord,
    dbPass: String
  ): Database? {
    val dbUri = record.getDbUri()
    val keyUri = record.getDbKeyUri()
    val db = KDBHandlerHelper.getInstance(context)
        .openDb(dbUri, dbPass, keyUri)
    if (db != null) {
      val dbName = UriUtil.getFileNameFromUri(context, dbUri)
      BaseApp.dbPass = QuickUnLockUtil.encryptStr(dbPass)
       KeepassAUtil.instance.subShortPass()
      if (keyUri != null) {
        BaseApp.dbKeyPath = QuickUnLockUtil.encryptStr(keyUri.toString())
      }
      //              BaseApp.KDB?.clear(context)
      // 保存打开记录
      BaseApp.KDB = db
      BaseApp.dbName = db.pm.name
      BaseApp.dbFileName = dbName

      BaseApp.dbVersion = "Keepass ${if (PwDatabase.isKDBExtension(dbName)) "3.x" else "4.x"}"
      BaseApp.isV4 = !PwDatabase.isKDBExtension(dbName)
      BaseApp.dbRecord = record
       KeepassAUtil.instance.saveLastOpenDbHistory(record)
    }
    return db
  }

  /**
   * 显示版本日志对话框，显示逻辑：
   * 配置文件的版本号不存在，或当前版本号大于配置文件的版本号
   */
  fun showVersionLog(activity: BaseActivity<*>) {
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
   * 删除历史记录
   */
  fun delHistoryRecord(entry: PwEntry) {
    viewModelScope.launch(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.entryRecordDao()
      val record = dao.getRecord(Types.UUIDtoBytes(entry.uuid), BaseApp.dbRecord.localDbUri)
      if (record != null) {
        dao.delReocrd(record)
      }
    }
  }

  /**
   * 检查是否有记录
   */
  fun checkHasHistoryRecord() = liveData {
    if (BaseApp.dbRecord == null || BaseApp.dbRecord.localDbUri.isNullOrEmpty()) {
      emit(false)
      return@liveData
    }
    val b = withContext(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.entryRecordDao()
      dao.hasRecord(BaseApp.dbRecord.localDbUri) > 0
    }
    emit(b)
  }

  /**
   * 获取历史记录
   */
  fun getEntryHistoryRecord() = liveData {
    if (BaseApp.dbRecord == null) {
      emit(null)
      return@liveData
    }
    val list = withContext(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.entryRecordDao()
      val records = dao.getRecord(BaseApp.dbRecord.localDbUri)
      if (records.isNullOrEmpty()) {
        null
      } else {
        val temp = ArrayList<SimpleItemEntity>()
        for (record in records) {
          val entry = BaseApp.KDB.pm.entries[Types.bytestoUUID(record.uuid)] ?: continue
          val item =  KeepassAUtil.instance.convertPwEntry2Item(entry)
          item.time = record.time
          temp.add(item)
        }
        temp
      }
    }
    emit(list)
  }

  /**
   * 获取keepassdb的首页数据
   */
  fun getRootEntry(context: Context) = liveData {
    val data = ArrayList<SimpleItemEntity>()
    if (BaseApp.KDB == null) {
      emit(data)
      return@liveData
    }
    val pm = BaseApp.KDB.pm

    if (pm == null) {
      emit(data)
      return@liveData
    }
    val rootGroup = pm.rootGroup

    for (group in rootGroup.childGroups) {
      data.add(KeepassAUtil.instance.convertPwGroup2Item(group))
    }
    Log.d(
        TAG,
        "getRootEntry， 保存前的数据库hash：${BaseApp.KDB.hashCode()}, num = ${BaseApp.KDB.pm.entries.size}"
    )
    for (entry in rootGroup.childEntries) {
      data.add(   KeepassAUtil.instance.convertPwEntry2Item(entry))
    }
    emit(data)
  }

  /**
   * 只获取群组数据
   */
  fun getRootGroup(
    context: Context,
    rootGroup: PwGroup
  ) = liveData {

    val data = ArrayList<SimpleItemEntity>()

    for (group in rootGroup.childGroups) {
      val item = SimpleItemEntity()
      item.title = group.name
      item.subTitle =
        context.getString(
            R.string.hint_group_desc, KdbUtil.getGroupEntryNum(group)
            .toString()
        )
      item.obj = group
      data.add(item)
    }

    emit(data)
  }

  fun checkDevBirthdayData(context: Context) {
//    val dt = DateTime(2020, 10, 2, 0, 0)
    val dt = DateTime(System.currentTimeMillis())
    if (dt.monthOfYear == 10 && dt.dayOfMonth == 2) {
      showDevBirthdayDialog(context)
    }
  }

  private fun showDevBirthdayDialog(context: Context) {
    val dialog = MsgDialog.generate {
      msgTitle = context.getString(R.string.donate)
      msgContent = context.getString(R.string.dev_birthday)
      setCancelBtText("NO")
      setEnterBtText("YES")
      build()
    }
    dialog.setOnBtClickListener(object : MsgDialog.OnBtClickListener {
      override fun onBtClick(
        type: Int,
        view: View
      ) {
        if (type == MsgDialog.TYPE_ENTER) {
          DonateDialog().show()
        }
      }
    })

    dialog.show()
  }

}