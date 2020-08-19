/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.fingerprint

import android.annotation.TargetApi
import android.content.res.Resources
import android.os.Build
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.QuickUnLockRecord
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.view.fingerprint.FingerprintActivity.Companion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@TargetApi(Build.VERSION_CODES.O)
class FingerprintModule : BaseModule() {

  // 旧的类型
  var oldFlag = FingerprintActivity.FLAG_CLOSE
  var curFlag = FingerprintActivity.FLAG_CLOSE

  /**
   * 获取快速解锁记录
   */
  fun getQuickUnlockRecord(dbUri: String) = liveData {
    var record: QuickUnLockRecord? = null
    withContext(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.quickUnlockDao()
      record = dao.findRecord(dbUri)
    }
    emit(record)
  }

  /**
   * 移除快速解锁
   */
  fun deleteQuickInfo() {
    viewModelScope.launch(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.quickUnlockDao()
      val record = dao.findRecord(BaseApp.dbRecord.localDbUri)
      if (record != null) {
        dao.deleteRecord(record)
      }
    }
  }

  /**
   * 保存快速解锁数据
   */
  fun saveQuickInfo(info: QuickUnLockRecord) {
    viewModelScope.launch(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.quickUnlockDao()
      val record = dao.findRecord(info.dbUri)
      if (record != null) {
        record.dbPass = info.dbPass
        record.isFullUnlock = info.isFullUnlock
        record.isUseKey = info.isUseKey
        record.keyPath = info.keyPath
        record.passIv = info.passIv
        dao.updateRecord(record)
      } else {
        dao.saveRecord(info)
      }
    }
  }
}