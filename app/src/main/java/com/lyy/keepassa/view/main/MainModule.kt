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
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.arialyy.frame.util.AndroidUtils
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
import com.lyy.keepassa.view.dialog.MsgDialog
import com.lyy.keepassa.widget.BubbleTextView
import com.lyy.keepassa.widget.BubbleTextView.OnIconClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainModule : BaseModule() {

  override fun onCleared() {
    super.onCleared()
  }

  /**
   * 安全级别对话框
   * root 红色，提示危险
   */
  fun setEcoIcon(
    context: Context,
    btText: BubbleTextView
  ) {
    val vector = VectorDrawableCompat.create(context.resources, R.drawable.ic_eco, context.theme)
    var msg = context.getString(R.string.hint_security_green)
    if (EasyProtectorLib.checkIsRoot()) {
      vector?.setTint(context.resources.getColor(R.color.red))
      msg = context.getString(R.string.hint_security_red)
    } else if (EasyProtectorLib.checkIsRunningInEmulator(context) {
//          BuglyLog.d(TAG, it)
        }) {
      vector?.setTint(context.resources.getColor(R.color.yellow))
      msg = context.getString(R.string.hint_security_yellow)
    }
    btText.setEndIcon(vector!!)
    btText.setOnIconClickListener(object : OnIconClickListener {
      override fun onClick(
        view: BubbleTextView,
        index: Int
      ) {
        if (index == 2) {
          val msgDialog = MsgDialog.generate {
            msgTitle = context.getString(R.string.hint)
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
      KeepassAUtil.subShortPass()
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
      KeepassAUtil.saveLastOpenDbHistory(record)
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
        delay(2000)
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
    val list = withContext(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.entryRecordDao()
      val records = dao.getRecord(BaseApp.dbRecord.localDbUri)
      if (records.isNullOrEmpty()) {
        null
      } else {
        val temp = ArrayList<SimpleItemEntity>()
        for (record in records) {
          val entry = BaseApp.KDB.pm.entries[Types.bytestoUUID(record.uuid)] ?: continue
          val item = SimpleItemEntity()
          item.title = record.title
          item.obj = entry
          item.subTitle = record.userName
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
      data.add(KeepassAUtil.convertPwGroup2Item(context, group))
    }
    Log.d(
        TAG,
        "getRootEntry， 保存前的数据库hash：${BaseApp.KDB.hashCode()}, num = ${BaseApp.KDB.pm.entries.size}"
    )
    for (entry in rootGroup.childEntries) {
      data.add(KeepassAUtil.convertPwEntry2Item(entry))
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

}