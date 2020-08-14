package com.lyy.keepassa.event

import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroupV4

/**
 * 恢复数据的事件
 */
data class UndoEvent(
  val type: Int = 1, // 1：群组，2：项目
  val entryV4: PwEntryV4? = null,
  val pwGroupV4: PwGroupV4? = null
)