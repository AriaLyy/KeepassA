package com.lyy.keepassa.event

import com.keepassdroid.database.security.ProtectedString

/**
 * 删除自定义字段事件
 */
data class DelAttrStrEvent(
  val key: String,
  val str: ProtectedString
)