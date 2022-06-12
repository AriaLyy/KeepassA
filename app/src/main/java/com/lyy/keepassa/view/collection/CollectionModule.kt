/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.collection

import androidx.lifecycle.viewModelScope
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.view.SimpleEntryAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 19:48 上午 2022/3/29
 **/
internal class CollectionModule : BaseModule() {
  val itemDataList = arrayListOf<SimpleItemEntity>()

  val itemDataFlow = MutableStateFlow<ArrayList<SimpleItemEntity>?>(null)

  fun removeItem(adapter: SimpleEntryAdapter, newEntry: PwEntryV4?) {
    if (newEntry == null) {
      Timber.d("entry is null")
      return
    }
    val newItem = KeepassAUtil.instance.convertPwEntry2Item(newEntry)
    var removePosition = -1
    itemDataList.forEachIndexed { index, simpleItemEntity ->
      if (simpleItemEntity.obj == newItem.obj) {
        removePosition = index
        return@forEachIndexed
      }
    }

    if (removePosition == -1) {
      Timber.d("the entry is not in the list, title = ${newEntry.title}")
      return
    }
    itemDataList.removeAt(removePosition)
    adapter.notifyItemRemoved(removePosition)
  }

  /**
   * add new collection
   */
  fun addNewItem(adapter: SimpleEntryAdapter, newEntry: PwEntryV4?) {
    if (newEntry == null) {
      Timber.d("entry is null")
      return
    }
    val newItem = KeepassAUtil.instance.convertPwEntry2Item(newEntry)
    val temp = itemDataList.find { it.obj == newItem.obj }
    if (temp != null) {
      Timber.d("already has the entry, title = ${newEntry.title}")
      return
    }
    val newPosition = itemDataList.size
    itemDataList.add(newItem)
    adapter.notifyItemInserted(newPosition)
  }

  fun getData() {
    itemDataList.clear()
    KpaUtil.kdbHandlerService.getCollectionEntries().forEach {
      itemDataList.add(KeepassAUtil.instance.convertPwEntry2Item(it))
    }
    viewModelScope.launch {
      itemDataFlow.emit(itemDataList)
    }
  }
}