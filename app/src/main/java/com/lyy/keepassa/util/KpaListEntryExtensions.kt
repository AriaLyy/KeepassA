/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.util

import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.view.SimpleEntryAdapter
import timber.log.Timber

/**
 * Check whether the entry is in the follow group
 */
fun PwEntryV4.checkGroupIsParent(group: PwGroupV4): Boolean {
  return this.parent == group
}

/**
 * Check whether the entry is in the same group leve
 */
fun PwEntryV4.checkInGroupLeve(group: PwGroupV4): Boolean {
  group.childGroups.forEach {
    if (this.parent == it) {
      return true
    }
  }
  return false
}

/**
 * update the status of deleted items
 */
fun SimpleEntryAdapter.deleteEntry(
  entryList: MutableList<SimpleItemEntity>,
  entry: PwEntryV4,
  oldParentGroup: PwGroupV4,
  dirGroup: PwGroupV4
) {

  if (oldParentGroup == dirGroup) {
    val entryItem =
      entryList.find { (it.obj is PwEntryV4) && (it.obj as PwEntryV4).uuid == entry.uuid }
    if (entryItem != null) {
      val index = entryList.indexOf(entryItem)
      if (index != -1) {
        entryList.removeAt(index)
        notifyItemRemoved(index)
      }
    }
  }

  if (oldParentGroup == BaseApp.KDB.pm.rootGroup) {
    val recycleBinItem = entryList.find { it.obj == BaseApp.KDB.pm.recycleBin }
    if (recycleBinItem != null) {
      val index = entryList.indexOf(recycleBinItem)
      if (index != -1) {
        recycleBinItem.subTitle =
          ResUtil.getString(
            R.string.hint_group_desc,
            KdbUtil.getGroupEntryNum(BaseApp.KDB.pm.recycleBin)
          )
        notifyItemChanged(index)
      }
    }
  }

  if (dirGroup.childGroups.contains(oldParentGroup)) {
    entryList.forEachIndexed { index, it ->
      if (it.obj == oldParentGroup) {
        it.subTitle =
          ResUtil.getString(R.string.hint_group_desc, KdbUtil.getGroupEntryNum(oldParentGroup))
        notifyItemChanged(index)
        return
      }
    }
  }
  Timber.d("The entry is not from the home page, title = ${entry.title}")
}

/**
 * update the status of modified items
 */
fun SimpleEntryAdapter.updateModifyEntry(
  entryList: MutableList<SimpleItemEntity>,
  pwEntryV4: PwEntryV4,
  dirGroup: PwGroupV4
) {
  if (pwEntryV4.checkGroupIsParent(dirGroup)) {
    entryList.forEachIndexed { index, item ->
      if (item.obj == pwEntryV4) {
        notifyItemInserted(index)
        return
      }
    }
    return
  }
  Timber.d("The entry is not from the root page, title = ${pwEntryV4.title}")
}

/**
 * update root list state
 */
fun SimpleEntryAdapter.createNewEntry(
  entryList: MutableList<SimpleItemEntity>,
  pwEntryV4: PwEntryV4,
  dirGroup: PwGroupV4
) {
  if (pwEntryV4.checkGroupIsParent(dirGroup)) {
    val index = entryList.size
    entryList.add(KeepassAUtil.instance.convertPwEntry2Item(pwEntryV4))
    notifyItemInserted(index)
    // notifyDataSetChanged()
    return
  }
  if (pwEntryV4.checkInGroupLeve(dirGroup)) {
    entryList.forEachIndexed { index, simpleItemEntity ->
      if (simpleItemEntity.obj == pwEntryV4.parent) {
        simpleItemEntity.subTitle =
          ResUtil.getString(R.string.hint_group_desc, KdbUtil.getGroupEntryNum(pwEntryV4.parent))
        notifyItemChanged(index)
        return
      }
    }
  }

  Timber.d("The entry is not from the home page, title = ${pwEntryV4.title}")
}

/**
 * move entry from other group
 * @param oldParentGroup Group before the item is moved
 * @param dirGroup The group directory currently displayed
 */
fun SimpleEntryAdapter.moveEntry(
  entryList: MutableList<SimpleItemEntity>,
  entry: PwEntryV4,
  oldParentGroup: PwGroupV4,
  dirGroup: PwGroupV4
) {

  // 检查是否是当前目录下
  if (entry.parent == dirGroup) {
    val isInDir =
      entryList.find { (it.obj is PwEntryV4) && (it.obj as PwEntryV4).uuid == entry.uuid } != null

    if (!isInDir) {
      val index = entryList.size
      entryList.add(KeepassAUtil.instance.convertPwEntry2Item(entry))
      notifyItemInserted(index)
    }
  }

  // 检查是否在当前目录的子目录下，不能return 因为有可能是移动到同级目录下
  if (entry.checkInGroupLeve(dirGroup)) {
    kotlin.run breaking@{
      entryList.forEachIndexed { index, simpleItemEntity ->
        if (simpleItemEntity.obj == entry.parent) {
          simpleItemEntity.subTitle =
            ResUtil.getString(R.string.hint_group_desc, KdbUtil.getGroupEntryNum(entry.parent))
          notifyItemChanged(index)
          return@breaking
        }
      }
    }
  }

  // 检查是否是从当前目录下被移走
  if (oldParentGroup == dirGroup) {
    val tempIndex = kotlin.run braking@{
      entryList.forEachIndexed { index, simpleItemEntity ->
        if (simpleItemEntity.obj !is PwEntryV4) {
          return@forEachIndexed
        }
        if ((simpleItemEntity.obj as PwEntryV4).uuid == entry.uuid) {
          return@braking index
        }
      }
      return@braking -1
    }

    if (tempIndex != -1) {
      entryList.removeAt(tempIndex)
      notifyItemRemoved(tempIndex)
    }
    return
  }

  // 检查是否是从当前目录的子目录下被移走
  if (dirGroup.childGroups.contains(oldParentGroup)) {
    kotlin.run braking@{
      entryList.forEachIndexed { index, simpleItemEntity ->
        if (simpleItemEntity.obj !is PwGroupV4) {
          return@forEachIndexed
        }
        if (simpleItemEntity.obj == oldParentGroup) {
          simpleItemEntity.subTitle =
            ResUtil.getString(R.string.hint_group_desc, KdbUtil.getGroupEntryNum(oldParentGroup))
          notifyItemChanged(index)
          return@braking
        }
      }
    }
    return
  }

  Timber.d("The entry is not from the home page, title = ${entry.title}")
}

