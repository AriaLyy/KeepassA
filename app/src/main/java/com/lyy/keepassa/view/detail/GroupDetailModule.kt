/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.detail

import android.content.Context
import androidx.lifecycle.liveData
import com.arialyy.frame.module.SingleLiveEvent
import com.arialyy.frame.util.PinyinUtil
import com.keepassdroid.database.PwDataInf
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
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

class GroupDetailModule : BaseModule() {

  private val groupLiveData = SingleLiveEvent<ArrayList<SimpleItemEntity>?>()

  /**
   * 获取v3版本的group数据
   */
  fun getGroupData(
    context: Context,
    groupId: PwGroupId
  ): SingleLiveEvent<ArrayList<SimpleItemEntity>?> {

    val group = BaseApp.KDB.pm.groups[groupId]
    if (group != null) {
      groupLiveData.postValue(convertGroup(context, group))
    } else {
      groupLiveData.postValue(null)
    }
    return groupLiveData
  }

  /**
   * 排序
   * @param sortType
   */
  fun sortData(
    sortType: SortType,
    data: List<SimpleItemEntity>
  ) = liveData {
    val entryList = arrayListOf<SimpleItemEntity>()
    val groupList = arrayListOf<SimpleItemEntity>()
    val tempList = arrayListOf<SimpleItemEntity>()

    for (item in data) {
      if (item.obj is PwGroup) {
        groupList.add(item)
        continue
      }
      entryList.add(item)
    }
    tempList.addAll(sortEntry(sortType, groupList))
    tempList.addAll(sortEntry(sortType, entryList))
    emit(tempList)
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