/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.view.SimpleEntryAdapter
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2:38 下午 2022/4/2
 **/

/**
 * Check whether the entry is in the follow group
 */
fun PwGroupV4.checkGroupIsParent(group: PwGroupV4?): Boolean {
  if (group == null) {
    return false
  }
  return this.parent == group
}

fun SimpleEntryAdapter.createGroup(
  entryList: MutableList<SimpleItemEntity>,
  groupV4: PwGroupV4,
  dirGroup: PwGroupV4?
) {
  if (dirGroup == null) {
    Timber.w("parent group is null")
    return
  }
  if (groupV4.checkGroupIsParent(dirGroup)) {
    var lastGroupIndex = -1
    entryList.forEachIndexed { index, item ->
      if (item.obj is PwGroupV4) {
        lastGroupIndex = index
      }
    }

    if (lastGroupIndex == -1) {
      val lastIndex = entryList.size
      entryList.add(KeepassAUtil.instance.convertPwGroup2Item(groupV4))
      notifyItemInserted(lastIndex)
      return
    }
    entryList.add(lastGroupIndex + 1, KeepassAUtil.instance.convertPwGroup2Item(groupV4))
    notifyItemInserted(lastGroupIndex + 1)

    // notifyDataSetChanged()
    return
  }
  Timber.d("The entry is not from the home page, title = ${groupV4.name}")
}

fun SimpleEntryAdapter.updateModifyGroup(
  entryList: MutableList<SimpleItemEntity>,
  groupV4: PwGroupV4,
  curDirGroup: PwGroupV4?
) {
  if (curDirGroup == null) {
    Timber.w("parent group is null")
    return
  }

  if (!groupV4.checkGroupIsParent(curDirGroup)) {
    Timber.d("The entry is not from the home page, title = ${groupV4.name}")
    return
  }
  entryList.forEachIndexed { index, item ->
    if (item.obj !is PwGroupV4) {
      return@forEachIndexed
    }

    if ((item.obj as PwGroupV4).uuid == groupV4.uuid) {
      KpaUtil.updateGroupItemInfo(item)
      notifyItemChanged(index)
      return
    }
  }
}

fun SimpleEntryAdapter.deleteGroup(
  entryList: MutableList<SimpleItemEntity>,
  groupV4: PwGroupV4,
  oldParentGroup: PwGroupV4,
  curDirGroup: PwGroupV4?
) {
  if (curDirGroup == null) {
    Timber.w("parent group is null")
    return
  }

  if (oldParentGroup != curDirGroup) {
    Timber.d("The entry is not from the home page, title = ${groupV4.name}, curDirGroup = ${curDirGroup.name}")
    return
  }

  // remove group form entryList
  val delIndex = kotlin.run breaking@{
    entryList.forEachIndexed { index, item ->
      if (item.obj !is PwGroupV4) {
        return@forEachIndexed
      }

      if ((item.obj as PwGroupV4).uuid == groupV4.uuid) {
        return@breaking index
      }
    }
    return@breaking -1
  }

  entryList.removeAt(delIndex)
  notifyItemRemoved(delIndex)

  // update recycling bin
  if (curDirGroup == BaseApp.KDB.pm.rootGroup) {

    entryList.forEachIndexed { index, item ->
      if (item.obj == BaseApp.KDB.pm.recycleBin) {
        item.subTitle = ResUtil.getString(
          R.string.hint_group_desc, KdbUtil.getGroupEntryNum(BaseApp.KDB.pm.recycleBin)
        )
        notifyItemChanged(index)
        return
      }
    }
  }
}