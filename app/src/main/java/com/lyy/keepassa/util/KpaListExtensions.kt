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
  pwEntryV4: PwEntryV4,
  pwGroupV4: PwGroupV4
) {
  if (pwEntryV4.checkGroupIsParent(pwGroupV4)) {
    var removeIndex = -1
    entryList.forEachIndexed { index, item ->
      if (item.obj == pwEntryV4) {
        removeIndex = index
        return@forEachIndexed
      }
    }
    if (removeIndex != -1) {
      entryList.removeAt(removeIndex)
      notifyItemRemoved(removeIndex)
    }
    return
  }
  if (pwEntryV4.checkInGroupLeve(pwGroupV4)) {
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
 * update the status of modified items
 */
fun SimpleEntryAdapter.updateModifyEntry(
  entryList: MutableList<SimpleItemEntity>,
  pwEntryV4: PwEntryV4,
  pwGroupV4: PwGroupV4
) {
  if (pwEntryV4.checkGroupIsParent(pwGroupV4)) {
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
  pwGroupV4: PwGroupV4
) {
  if (pwEntryV4.checkGroupIsParent(pwGroupV4)) {
    val index = entryList.size
    entryList.add(KeepassAUtil.instance.convertPwEntry2Item(pwEntryV4))
    notifyItemInserted(index)
    return
  }
  if (pwEntryV4.checkInGroupLeve(pwGroupV4)) {
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

