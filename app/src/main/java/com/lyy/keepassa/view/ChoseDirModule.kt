/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view

import androidx.lifecycle.liveData
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ChoseDirModule : BaseModule() {

  /**
   * 恢复群组
   * @param groupId 需要恢复的群组
   * @param 当前群组
   */
  fun undoGroup(
    groupId: PwGroupId,
    curGroup: PwGroup
  ) = liveData {
    val p = withContext(Dispatchers.IO) {
      val group = BaseApp.KDB.pm.groups[groupId] as PwGroupV4
      (BaseApp.KDB.pm as PwDatabaseV4).undoRecycle(group, curGroup)
      val b = KdbUtil.saveDb()
      Pair(b == DbSynUtil.STATE_SUCCEED, group)
    }
    emit(p)
  }

  /**
   * 恢复条目
   */
  fun undoEntry(
    entryId: UUID,
    curGroup: PwGroup
  ) = liveData {
    val p = withContext(Dispatchers.IO) {
      val entryV4 = BaseApp.KDB.pm.entries[entryId] as PwEntryV4
      (BaseApp.KDB.pm as PwDatabaseV4).undoRecycle(entryV4, curGroup)
      val b = KdbUtil.saveDb()
      Pair(b == DbSynUtil.STATE_SUCCEED, entryV4)
    }
    emit(p)
  }

}