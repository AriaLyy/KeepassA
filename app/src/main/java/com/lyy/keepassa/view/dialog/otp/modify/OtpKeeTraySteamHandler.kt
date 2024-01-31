package com.lyy.keepassa.view.dialog.otp.modify

import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.lyy.keepassa.entity.TrayTotpBean
import com.lyy.keepassa.entity.toOtpStringMap
import com.lyy.keepassa.util.getKeeTrayBean
import com.lyy.keepassa.util.totp.OtpEnum
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import com.lyy.keepassa.view.dialog.otp.CreateOtpModule
import kotlinx.coroutines.launch

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:41 PM 2024/1/25
 **/
internal class OtpKeeTraySteamHandler : IOtpModifyHandler {
  private lateinit var bean: TrayTotpBean
  override fun initView(context: ModifyOtpDialog) {
    val binding = context.binding
    binding.contentLayout.group.isVisible = false
    binding.contentLayout.rbCustom.isVisible = false
    bean = context.pwEntryV4.getKeeTrayBean()
    binding.contentLayout.strKey.setText(bean.secret)
    binding.contentLayout.rbSteam.isChecked = true
    binding.contentLayout.rbDefault.isVisible = false
  }

  override fun save(
    context: ModifyOtpDialog,
    secret: String,
    arithmetic: HashAlgorithm,
    digits: Int,
    period: Int,
    isSteam: Boolean
  ) {
    bean.period = period
    bean.secret = secret
    val beanMap = bean.toOtpStringMap()
    beanMap.forEach {
      context.pwEntryV4.strings[it.key] = it.value
    }

    context.lifecycleScope.launch {
      CreateOtpModule.otpFlow.emit(Pair(OtpEnum.TRAY_TOTP, bean))
      context.dismiss()
    }
  }
}