/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.event

import com.keepassdroid.database.PwEntryV4

/**
 * @Author laoyuyu
 * @Description
 * @Date 19:58 下午 2022/3/29
 **/
data class CollectionEvent(
  val state: CollectionEventType = CollectionEventType.COLLECTION_STATE_TOTAL,
  val collectionNum: Int = 0,
  val pwEntryV4: PwEntryV4? = null
)

enum class CollectionEventType {
  COLLECTION_STATE_ADD,
  COLLECTION_STATE_REMOVE,
  COLLECTION_STATE_TOTAL
}