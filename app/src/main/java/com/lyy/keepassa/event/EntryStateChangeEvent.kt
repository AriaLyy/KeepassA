/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.event

import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.event.EntryState.UNKNOWN

/**
 * @Author laoyuyu
 * @Description
 * @Date 2022/3/30
 **/
data class EntryStateChangeEvent(
  val state: EntryState = UNKNOWN,
  val pwEntryV4: PwEntryV4? = null
)

enum class EntryState {
  /**
   * new entry
   */
  CREATE,
  DELETE,
  MODIFY,
  UNKNOWN
}