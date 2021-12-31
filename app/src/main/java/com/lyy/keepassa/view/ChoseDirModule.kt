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
   * @param curGroup 当前群组
   */
  fun moveGroup(
    groupId: PwGroupId,
    curGroup: PwGroup
  ) = liveData {
    val p = withContext(Dispatchers.IO) {
      val group = BaseApp.KDB.pm.groups[groupId] as PwGroupV4

      if (group.parent == BaseApp.KDB.pm.recycleBin){
        (BaseApp.KDB.pm as PwDatabaseV4).undoRecycle(group, curGroup)
      }else{
        (BaseApp.KDB.pm as PwDatabaseV4).moveGroup(group, curGroup)
      }

      val b = KdbUtil.saveDb()
      Pair(b == DbSynUtil.STATE_SUCCEED, group)
    }
    emit(p)
  }

  /**
   * 恢复条目
   */
  fun moveEntry(
    entryId: UUID,
    curGroup: PwGroup
  ) = liveData {
    val p = withContext(Dispatchers.IO) {
      val entry = BaseApp.KDB.pm.entries[entryId] ?: return@withContext null
      val entryV4 = entry as PwEntryV4

      if (entryV4.parent == BaseApp.KDB.pm.recycleBin){
        (BaseApp.KDB.pm as PwDatabaseV4).undoRecycle(entryV4, curGroup)
      }else{
        (BaseApp.KDB.pm as PwDatabaseV4).moveEntry(entryV4, curGroup)
      }

      val b = KdbUtil.saveDb()
      Pair(b == DbSynUtil.STATE_SUCCEED, entryV4)
    }
    emit(p)
  }
}