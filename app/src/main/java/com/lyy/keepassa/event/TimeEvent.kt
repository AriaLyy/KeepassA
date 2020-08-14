package com.lyy.keepassa.event

/**
 * 时间事件
 */
data class TimeEvent(
  val year: Int,
  val month: Int,
  val dayOfMonth: Int,
  val hour: Int,
  val minute: Int
)