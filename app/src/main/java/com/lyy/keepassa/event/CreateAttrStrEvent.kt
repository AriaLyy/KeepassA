package com.lyy.keepassa.event

import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.widget.expand.AttrStrItemView

/**
 * 创建自定义字段事件
 */
data class CreateAttrStrEvent(
  val key: String,
  val str: ProtectedString,
  val isEdit: Boolean = false,
  val updateView: AttrStrItemView? = null
)