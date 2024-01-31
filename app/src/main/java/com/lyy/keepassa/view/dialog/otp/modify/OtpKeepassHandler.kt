package com.lyy.keepassa.view.dialog.otp.modify

import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.ToastUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.entity.KeepassBean
import com.lyy.keepassa.entity.toOtpStringMap
import com.lyy.keepassa.util.getKeepassBean
import com.lyy.keepassa.util.totp.OtpEnum
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import com.lyy.keepassa.view.dialog.otp.CreateOtpModule
import kotlinx.coroutines.launch

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:41 PM 2024/1/25
 **/
internal class OtpKeepassHandler : IOtpModifyHandler {
  private lateinit var otpBean: KeepassBean
  override fun initView(context: ModifyOtpDialog) {
    val binding = context.binding
    binding.contentLayout.group.isVisible = true
    binding.contentLayout.rbSteam.isVisible = false
    binding.contentLayout.rbCustom.isChecked = true
    binding.contentLayout.rbCustom.isVisible = true
    val oBean = context.pwEntryV4.getKeepassBean()
    val bean = oBean.otpBean
    if (bean == null) {
      ToastUtils.showLong(ResUtil.getString(R.string.not_souper_otp))
      return
    }
    otpBean = oBean
    binding.contentLayout.strKey.setText(bean.secret)
    binding.contentLayout.sp.setSelection(
      when (bean.algorithm) {
        HashAlgorithm.SHA1 -> 0
        HashAlgorithm.SHA256 -> 1
        HashAlgorithm.SHA512 -> 2
      }
    )
    binding.contentLayout.slTime.value = bean.period.toFloat()
    binding.contentLayout.slLen.value = bean.digits.toFloat()
  }

  override fun save(
    context: ModifyOtpDialog,
    secret: String,
    arithmetic: HashAlgorithm,
    digits: Int,
    period: Int,
    isSteam: Boolean
  ) {
    otpBean.otpBean?.let {
      it.secret = secret
      it.period = period
      it.algorithm = arithmetic
      it.digits = digits
    }
    otpBean.toOtpStringMap().forEach {
      context.pwEntryV4.strings[it.key] = it.value
    }

    context.lifecycleScope.launch {
      CreateOtpModule.otpFlow.emit(Pair(OtpEnum.KEEPASS, otpBean))
      context.dismiss()
    }
  }
}