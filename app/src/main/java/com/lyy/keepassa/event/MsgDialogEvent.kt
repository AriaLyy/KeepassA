package com.lyy.keepassa.event

/**
 * msg dialog 事件
 */
data class MsgDialogEvent(
  /**
   * @param type 1、确认，2、覆盖、3、取消
   *
   */
  val type: Int = 1,
  val requestCode: Int = 0   // 请求码
)