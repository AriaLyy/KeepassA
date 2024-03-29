/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view

import androidx.fragment.app.FragmentActivity
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.event.MoveEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KpaUtil
import org.greenrobot.eventbus.EventBus
import java.util.UUID

class ChoseDirModule : BaseModule() {

  /**
   * 恢复群组
   * @param groupId 需要恢复的群组
   * @param curGroup 当前群组
   */
  fun moveGroup(
    ac: FragmentActivity,
    groupId: PwGroupId,
    curGroup: PwGroup
  ) {
    val group = BaseApp.KDB.pm.groups[groupId] as PwGroupV4
    if (group.parent == BaseApp.KDB.pm.recycleBin) {
      (BaseApp.KDB.pm as PwDatabaseV4).undoRecycle(group, curGroup)
    } else {
      (BaseApp.KDB.pm as PwDatabaseV4).moveGroup(group, curGroup)
    }
    KpaUtil.kdbHandlerService.saveDbByBackground()
    EventBus.getDefault().post(MoveEvent(MoveEvent.MOVE_TYPE_GROUP, null, group))
    HitUtil.toaskShort(ac.getString(R.string.undo_grouped))
    ac.finishAfterTransition()
  }

  /**
   * 恢复条目
   */
  fun moveEntry(
    ac: FragmentActivity,
    entryId: UUID,
    curGroup: PwGroupV4
  ) {
    val entry = BaseApp.KDB.pm.entries[entryId] ?: return
    val entryV4 = entry as PwEntryV4
    KpaUtil.kdbHandlerService.moveEntry(entryV4, curGroup)
    HitUtil.toaskShort(ac.getString(R.string.undo_entryed))
    ac.finishAfterTransition()
  }
}