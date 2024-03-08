package com.lyy.keepassa.view.dialog.otp.modify

import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.lyy.keepassa.entity.KeepassXcBean
import com.lyy.keepassa.entity.toOtpStringMap
import com.lyy.keepassa.util.getKeepassXcBean
import com.lyy.keepassa.util.otpIsKeepassXcSteam
import com.lyy.keepassa.util.totp.OtpEnum
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import com.lyy.keepassa.view.dialog.otp.CreateOtpModule
import kotlinx.coroutines.launch

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:41 PM 2024/1/25
 **/
internal class OtpKeepassXcHandler : IOtpModifyHandler {
  private lateinit var otpBean: KeepassXcBean
  override fun initView(context: ModifyOtpDialog) {
    val binding = context.binding
    binding.contentLayout.group.isVisible = true
    binding.contentLayout.rbCustom.isVisible = true
    val isSteam = context.pwEntryV4.otpIsKeepassXcSteam()
    if (!isSteam) {
      binding.contentLayout.rbCustom.isChecked = true
    } else {
      binding.contentLayout.rbSteam.isChecked = true
    }
    otpBean = context.pwEntryV4.getKeepassXcBean()
    binding.contentLayout.strKey.setText(otpBean.secret)
    binding.contentLayout.sp.setSelection(
      when (otpBean.algorithm) {
        HashAlgorithm.SHA1 -> 0
        HashAlgorithm.SHA256 -> 1
        HashAlgorithm.SHA512 -> 2
      }
    )
    binding.contentLayout.slTime.value = otpBean.period.toFloat()
    binding.contentLayout.slLen.value = otpBean.digits.toFloat()
  }

  override fun save(
    context: ModifyOtpDialog,
    secret: String,
    arithmetic: HashAlgorithm,
    digits: Int,
    period: Int,
    isSteam: Boolean
  ) {
    otpBean.digits = digits
    otpBean.secret = secret
    otpBean.period = period
    otpBean.algorithm = arithmetic
    otpBean.encoder = if (isSteam) "steam" else ""

    otpBean.toOtpStringMap().forEach {
      context.pwEntryV4.strings[it.key] = it.value
    }

    context.lifecycleScope.launch {
      CreateOtpModule.otpFlow.emit(Pair(OtpEnum.KEEPASSXC, otpBean))
      context.dismiss()
    }
  }
}