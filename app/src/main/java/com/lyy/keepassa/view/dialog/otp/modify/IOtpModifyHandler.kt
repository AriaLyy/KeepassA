package com.lyy.keepassa.view.dialog.otp.modify

import com.lyy.keepassa.util.totp.TokenCalculator
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:40 PM 2024/1/25
 **/
internal interface IOtpModifyHandler {
  fun initView(context: ModifyOtpDialog)

  fun save(
    context: ModifyOtpDialog,
    secret: String,
    arithmetic: HashAlgorithm = HashAlgorithm.SHA1,
    digits: Int = TokenCalculator.TOTP_DEFAULT_DIGITS,
    period: Int = TokenCalculator.TOTP_DEFAULT_PERIOD,
    isSteam: Boolean = false
  ) {
    // context.lifecycleScope.launch {
    //   KpaUtil.kdbHandlerService.saveOnly {
    //     context.dismiss()
    //   }
    // }
  }
}