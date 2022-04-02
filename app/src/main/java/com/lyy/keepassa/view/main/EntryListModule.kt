/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main

import androidx.lifecycle.viewModelScope
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.utils.Types
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.hasTOTP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @Author laoyuyu
 * @Description
 * @Date 4:02 下午 2021/10/25
 **/
internal class EntryListModule : BaseModule() {

  val entryData = mutableListOf<SimpleItemEntity>()
  val getDataFlow = MutableSharedFlow<MutableList<SimpleItemEntity>?>()

  fun getData(type: String) {
    if (type == EntryListFragment.TYPE_HISTORY) {
      getEntryHistoryRecord()
      return
    }
    if (type == EntryListFragment.TYPE_TOTP) {
      getTOTPData()
      return
    }
  }

  /**
   * get totp data
   */
  private fun getTOTPData() {
    entryData.clear()
    viewModelScope.launch {
      if (BaseApp.KDB == null) {
        getDataFlow.emit(null)
        return@launch
      }
      val pm = BaseApp.KDB!!.pm

      if (pm == null) {
        getDataFlow.emit(null)
        return@launch
      }
      for (map in pm.entries) {
        val entry = map.value
        if (entry is PwEntryV4 && entry.hasTOTP()) {
          entryData.add(KeepassAUtil.instance.convertPwEntry2Item(entry))
        }
      }
      getDataFlow.emit(entryData)
    }
  }

  /**
   * 删除历史记录
   */
  fun delHistoryRecord(entry: PwEntry) {
    viewModelScope.launch(Dispatchers.IO) {
      BaseApp.dbRecord?.let {
        val dao = BaseApp.appDatabase.entryRecordDao()
        val record = dao.getRecord(Types.UUIDtoBytes(entry.uuid), it.localDbUri)
        if (record != null) {
          dao.delReocrd(record)
        }
      }
    }
  }

  /**
   * 获取历史记录
   */
  private fun getEntryHistoryRecord() {
    entryData.clear()
    viewModelScope.launch {
      if (BaseApp.dbRecord == null) {
        getDataFlow.emit(null)
        return@launch
      }
      withContext(Dispatchers.IO) {
        val dao = BaseApp.appDatabase.entryRecordDao()
        val records = dao.getRecord(BaseApp.dbRecord!!.localDbUri)
        if (records.isNullOrEmpty()) {
          return@withContext
        }

        for (record in records) {
          val entry = BaseApp.KDB!!.pm.entries[Types.bytestoUUID(record.uuid)] ?: continue
          val item = KeepassAUtil.instance.convertPwEntry2Item(entry)
          item.time = record.time
          entryData.add(item)
        }
      }
      getDataFlow.emit(entryData)
    }
  }
}