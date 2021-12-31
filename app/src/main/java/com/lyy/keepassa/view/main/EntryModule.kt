/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main

import android.content.Context
import androidx.lifecycle.liveData
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 4:05 下午 2021/10/25
 **/
class EntryModule : BaseModule() {

  /**
   * 同步数据库
   */
  fun syncDb() = liveData(Dispatchers.IO) {
    val code = KdbUtil.saveDb(true)
    Timber.d("同步数据库结束，code = $code")
    if (code != DbSynUtil.STATE_SUCCEED) {
      emit(false)
      return@liveData
    }
    emit(true)
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
    val pm = BaseApp.KDB!!.pm

    if (pm == null) {
      emit(data)
      return@liveData
    }
    val rootGroup = pm.rootGroup

    for (group in rootGroup.childGroups) {
      data.add(KeepassAUtil.instance.convertPwGroup2Item(group))
    }
    Timber.d(
      "getRootEntry， 保存前的数据库hash：${BaseApp.KDB.hashCode()}, num = ${BaseApp.KDB!!.pm.entries.size}"
    )
    for (entry in rootGroup.childEntries) {
      data.add(KeepassAUtil.instance.convertPwEntry2Item(entry))
    }
    emit(data)
  }
}