/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main

import androidx.lifecycle.viewModelScope
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.EntryType
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.view.SimpleEntryAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 4:05 下午 2021/10/25
 **/
internal class HomeModule : BaseModule() {

  val itemDataList = arrayListOf<SimpleItemEntity>()

  val itemDataFlow = MutableStateFlow<ArrayList<SimpleItemEntity>?>(null)

  val collectionEntry by lazy {
    val entity = SimpleItemEntity()
    entity.title = ResUtil.getString(R.string.my_collection)
    entity.subTitle = ResUtil.getString(R.string.current_collection_num, "0")
    entity.icon = R.drawable.ic_star
    entity.obj = EntryType.TYPE_COLLECTION
    entity
  }

  /**
   * Check whether the entry is in the follow group
   */
  private fun checkEntryIsRoot(pwEntryV4: PwEntryV4) = pwEntryV4.parent == BaseApp.KDB.pm.rootGroup

  /**
   * Check whether the entry is in the home group
   */
  private fun checkEntryInMainGroup(pwEntryV4: PwEntryV4): Boolean {
    BaseApp.KDB.pm.rootGroup.childGroups.forEach {
      if (pwEntryV4.parent == it) {
        return true
      }
    }
    return false
  }

  /**
   * update root list state
   */
  fun createNewEntry(adapter: SimpleEntryAdapter, pwEntryV4: PwEntryV4) {
    if (checkEntryIsRoot(pwEntryV4)) {
      val index = itemDataList.size
      itemDataList.add(KeepassAUtil.instance.convertPwEntry2Item(pwEntryV4))
      adapter.notifyItemInserted(index)
      return
    }
    if (checkEntryInMainGroup(pwEntryV4)) {
      itemDataList.forEachIndexed { index, simpleItemEntity ->
        if (simpleItemEntity.obj == pwEntryV4.parent) {
          simpleItemEntity.subTitle =
            ResUtil.getString(R.string.hint_group_desc, KdbUtil.getGroupEntryNum(pwEntryV4.parent))
          adapter.notifyItemChanged(index)
          return
        }
      }
    }

    if (!checkEntryIsRoot(pwEntryV4)) {
      Timber.d("The entry is not from the home page, title = ${pwEntryV4.title}")
      return
    }
  }

  /**
   * synchronize database
   */
  fun syncDb(callback: (Int) -> Unit) {
    KpaUtil.kdbHandlerService.saveDbByForeground(callback = callback)
  }

  /**
   * get root data
   */
  fun getRootEntry() {
    itemDataList.clear()
    viewModelScope.launch {
      if (BaseApp.KDB == null) {
        itemDataFlow.emit(null)
        return@launch
      }
      val pm = BaseApp.KDB!!.pm

      if (pm == null) {
        itemDataFlow.emit(null)
        return@launch
      }

//      collectionEntry.subTitle = ResUtil.getString(
//        R.string.current_collection_num,
//        KpaUtil.kdbHandlerService.getCollectionNum().toString()
//      )
//      itemDataList.add(collectionEntry)

      val rootGroup = pm.rootGroup
      for (group in rootGroup.childGroups) {
        itemDataList.add(KeepassAUtil.instance.convertPwGroup2Item(group))
      }
      Timber.d(
        "getRootEntry， 保存前的数据库hash：${BaseApp.KDB.hashCode()}, num = ${BaseApp.KDB!!.pm.entries.size}"
      )
      for (entry in rootGroup.childEntries) {
        itemDataList.add(KeepassAUtil.instance.convertPwEntry2Item(entry))
      }

      itemDataFlow.emit(itemDataList)
    }
  }
}