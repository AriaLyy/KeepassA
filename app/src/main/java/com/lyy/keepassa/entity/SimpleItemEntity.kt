/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.entity

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.view.menu.EntryPopMenu
import com.lyy.keepassa.view.menu.GroupPopMenu

class SimpleItemEntity {
  var title: CharSequence = ""
  var subTitle: CharSequence = ""
  var content: CharSequence = ""
  var icon: Int = 0
  var id: Int = -1
  var time: Long = 0
  lateinit var obj: Any
  var isSelected: Boolean = false

  var type: Int = 0

  /**
   * 是否受保护
   */
  var isProtected = false

  /**
   * 是否选中
   */
  var isCheck = false
}

/**
 * show pop menu
 */
fun SimpleItemEntity.showPopMenu(
  ac: FragmentActivity,
  v: View,
  curx: Int,
  isInRecycleBin: Boolean = false
) {
  if (obj is PwGroup) {
    val pop = GroupPopMenu(
      ac,
      v,
      obj as PwGroupV4,
      curx,
      isInRecycleBin
    )
    pop.show()
    return
  }
  if (obj is PwEntry) {
    val pop = EntryPopMenu(
      ac,
      v,
      obj as PwEntry,
      curx,
      isInRecycleBin
    )
    pop.show()
  }
}