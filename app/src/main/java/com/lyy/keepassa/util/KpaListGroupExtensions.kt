/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import com.keepassdroid.database.PwGroupV4
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
fun PwGroupV4.checkGroupIsParent(group: PwGroupV4): Boolean {
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
    val index = entryList.size
    entryList.add(KeepassAUtil.instance.convertPwGroup2Item(groupV4))
    notifyItemInserted(index)
    // notifyDataSetChanged()
    return
  }
  Timber.d("The entry is not from the home page, title = ${groupV4.name}")
}

fun SimpleEntryAdapter.updateModifyEntry(
  entryList: MutableList<SimpleItemEntity>,
  groupV4: PwGroupV4,
  dirGroup: PwGroupV4?
) {
  if (dirGroup == null) {
    Timber.w("parent group is null")
    return
  }

  if (!groupV4.checkGroupIsParent(dirGroup)) {
    Timber.d("The entry is not from the home page, title = ${groupV4.name}")
    return
  }
  entryList.forEachIndexed { index, item ->
    if (item.obj !is PwGroupV4) {
      return@forEachIndexed
    }

    if ((item.obj as PwGroupV4).uuid == groupV4.uuid) {
      notifyItemChanged(index)
      return
    }
  }
}