package com.lyy.keepassa.view.dialog.otp.modify

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:40 PM 2024/1/25
 **/
internal interface IOtpModifyHandler {
  fun initView(context: ModifyOtpDialog)

  fun save(context: ModifyOtpDialog)
}