/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.detail

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.arialyy.frame.util.PinyinUtil
import com.keepassdroid.database.PwDataInf
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.common.SortType
import com.lyy.keepassa.common.SortType.CHAR_ASC
import com.lyy.keepassa.common.SortType.CHAR_DESC
import com.lyy.keepassa.common.SortType.NONE
import com.lyy.keepassa.common.SortType.TIME_ASC
import com.lyy.keepassa.common.SortType.TIME_DESC
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.createNewEntry
import com.lyy.keepassa.util.deleteEntry
import com.lyy.keepassa.util.moveEntry
import com.lyy.keepassa.util.updateModifyEntry
import com.lyy.keepassa.view.SimpleEntryAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class GroupDetailModule : BaseModule() {

  val entryData = mutableListOf<SimpleItemEntity>()
  val getDataFlow = MutableStateFlow<MutableList<SimpleItemEntity>?>(null)
  private var curGroupV4: PwGroupV4? = null

  /**
   * update the status of deleted items
   */
  fun deleteEntry(adapter: SimpleEntryAdapter, pwEntryV4: PwEntryV4) {
    curGroupV4?.let {
      adapter.deleteEntry(entryData, pwEntryV4, it)
    }
  }

  /**
   * update the status of modified items
   */
  fun updateModifyEntry(adapter: SimpleEntryAdapter, pwEntryV4: PwEntryV4) {
    curGroupV4?.let {
      adapter.updateModifyEntry(entryData, pwEntryV4, it)
    }
  }

  /**
   * move entry from other group
   */
  fun moveEntry(adapter: SimpleEntryAdapter, pwEntryV4: PwEntryV4, oldParent: PwGroupV4) {
    curGroupV4?.let {
      adapter.moveEntry(entryData, pwEntryV4, oldParent, it)
    }
  }

  /**
   * update root list state
   */
  fun createNewEntry(adapter: SimpleEntryAdapter, pwEntryV4: PwEntryV4) {
    curGroupV4?.let {
      adapter.createNewEntry(entryData, pwEntryV4, it)
    }
  }

  /**
   * 获取v3版本的group数据
   */
  fun getGroupData(context: Context, groupId: PwGroupId) {
    entryData.clear()
    viewModelScope.launch {
      val group = BaseApp.KDB.pm.groups[groupId]
      curGroupV4 = group as PwGroupV4?
      if (group == null) {
        getDataFlow.emit(null)
        return@launch
      }
      entryData.addAll(convertGroup(context, group))
      getDataFlow.emit(entryData)
      return@launch
    }
  }

  /**
   * 排序
   * @param sortType
   */
  fun sortData(adapter: SimpleEntryAdapter, sortType: SortType) {
    val entryList = arrayListOf<SimpleItemEntity>()
    val groupList = arrayListOf<SimpleItemEntity>()
    val tempList = arrayListOf<SimpleItemEntity>()

    for (item in entryData) {
      if (item.obj is PwGroup) {
        groupList.add(item)
        continue
      }
      entryList.add(item)
    }
    tempList.addAll(sortEntry(sortType, groupList))
    tempList.addAll(sortEntry(sortType, entryList))
    entryData.clear()
    entryData.addAll(tempList)
    adapter.notifyDataSetChanged()
  }

  private fun sortEntry(
    sortType: SortType,
    data: List<SimpleItemEntity>
  ): Set<SimpleItemEntity> {
    val map = hashMapOf<SimpleItemEntity, Char?>()
    for (item in data) {
      map[item] = PinyinUtil.getFirstSpellChar(item.title)
    }
    return when (sortType) {
      CHAR_ASC -> {
        map.toList()
          .sortedBy { it.second }
          .toMap().keys
      }
      CHAR_DESC -> {
        map.toList()
          .sortedByDescending { it.second }
          .toMap().keys
      }
      TIME_ASC -> {
        map.toList()
          .sortedBy {
            (it.first.obj as PwDataInf).creationTime.time
          }
          .toMap().keys
      }
      TIME_DESC -> {
        map.toList()
          .sortedByDescending { (it.first.obj as PwDataInf).creationTime.time }
          .toMap().keys
      }
      NONE -> {
        emptySet()
      }
    }
  }

  private fun convertGroup(
    context: Context,
    group: PwGroup
  ): ArrayList<SimpleItemEntity> {
    val data = ArrayList<SimpleItemEntity>()
    for (cGroup in group.childGroups) {
      val item = SimpleItemEntity()
      item.title = cGroup.name
      item.subTitle =
        context.getString(
          R.string.hint_group_desc, KdbUtil.getGroupAllEntryNum(cGroup)
            .toString()
        )
      item.obj = cGroup
      data.add(item)
    }

    for (entry in group.childEntries) {
      val item = SimpleItemEntity()
      item.title = entry.title
      item.subTitle = KdbUtil.getUserName(entry)
      item.obj = entry
      data.add(item)
    }
    return data
  }
}