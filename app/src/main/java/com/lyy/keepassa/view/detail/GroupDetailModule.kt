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
import com.arialyy.frame.util.PinyinUtil
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.common.SortType
import com.lyy.keepassa.common.SortType.CHAR_ASC
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.KdbUtil

class GroupDetailModule : BaseModule() {

  /**
   * 获取v3版本的group数据
   */
  fun getGroupData(
    context: Context,
    groupId: PwGroupId
  ) = liveData {

    val group = BaseApp.KDB.pm.groups[groupId]
    if (group != null) {
      emit(convertGroup(context, group))
    } else {
      emit(null)
    }
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
    return if (sortType == CHAR_ASC) {
      map.toList()
          .sortedBy { it.second }
          .toMap().keys
    } else {
      map.toList()
          .sortedByDescending { it.second }
          .toMap().keys
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
            R.string.hint_group_desc, KdbUtil.getGroupEntryNum(cGroup)
            .toString()
        )
      item.obj = cGroup
      data.add(item)
    }

    for (entry in group.childEntries) {
      val item = SimpleItemEntity()
      item.title = entry.title
      item.subTitle = entry.username
      item.obj = entry
      data.add(item)
    }
    return data
  }
}