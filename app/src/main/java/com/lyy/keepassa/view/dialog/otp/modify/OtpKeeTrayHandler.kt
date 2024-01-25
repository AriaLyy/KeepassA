package com.lyy.keepassa.view.dialog.otp.modify

import androidx.core.view.isVisible
import com.lyy.keepassa.entity.TotpType.DEFAULT
import com.lyy.keepassa.util.getKeeTrayBean
import com.lyy.keepassa.util.totp.OtpEnum

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:41 PM 2024/1/25
 **/
internal class OtpKeeTrayHandler : IOtpModifyHandler {
  override fun initView(context: ModifyOtpDialog) {
    val binding = context.getB
    binding.contentLayout.group.isVisible = false
    binding.contentLayout.rbDefault.isChecked = true
    handleTrayOtp()
  }

  override fun save(context: ModifyOtpDialog) {
  }

  private fun handleTrayOtp() {
    val bean = pwEntryV4.getKeeTrayBean()
    entryType = OtpEnum.TRAY_TOTP
    otpBean = bean
    binding.contentLayout.strKey.setText(bean.seed)
  }
}