package com.lyy.keepassa.event

import com.keepassdroid.database.PwGroup

/**
 * 创建群组的事件
 */
data class CreateOrUpdateGroupEvent(
  val pwGroup: PwGroup,
  val isUpdate: Boolean = false
)