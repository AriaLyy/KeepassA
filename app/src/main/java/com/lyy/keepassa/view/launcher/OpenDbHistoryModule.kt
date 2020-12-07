/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import androidx.lifecycle.liveData
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.DbRecord
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.isAFS
import com.lyy.keepassa.view.DbPathType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * @Author laoyuyu
 * @Description
 * @Date 2020/12/7
 **/
class OpenDbHistoryModule : BaseModule() {

  /**
   * delete history
   */
  fun deleteHistoryRecord(item: SimpleItemEntity) {
    val dao = BaseApp.appDatabase.dbRecordDao()
    GlobalScope.launch {
      dao.deleteRecord(item.obj as DbRecord)
    }
  }

  /**
   * 获取数据库打开列表记录
   */
  fun getDbOpenRecordList(context: Context) = liveData {
    val data = withContext(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.dbRecordDao()
      val records = dao.getAllRecord()
      val needRemoveRecord = ArrayList<DbRecord>()
      if (records.isNotEmpty()) {
        val list = ArrayList<SimpleItemEntity>()
        for (record in records) {
          val item = SimpleItemEntity()
          item.icon = DbPathType.valueOf(record.type).icon

          // 检查权限，如果本地uri失效，者删除记录
          val uri = Uri.parse(record.localDbUri)
          if (!UriUtil.checkPermissions(context, uri) && record.isAFS()) {
            needRemoveRecord.add(record)
            continue
          }
          val tx = UriUtil.getFileNameFromUri(context, uri)
          if (TextUtils.isEmpty(tx)) {
            continue
          }
          item.title = tx
          item.subTitle = KeepassAUtil.formatTime(Date(record.time))
          item.obj = record
          list.add(item)
        }

        for (record in needRemoveRecord) {
          dao.deleteRecord(record)
        }

        list
      } else {
        null
      }
    }
    emit(data)
  }
}