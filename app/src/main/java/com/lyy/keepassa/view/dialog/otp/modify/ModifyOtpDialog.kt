package com.lyy.keepassa.view.dialog.otp.modify

import android.view.View
import android.widget.AdapterView
import android.widget.RadioButton
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.ToastUtils
import com.google.android.material.slider.Slider
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogOtpModifyBinding
import com.lyy.keepassa.entity.IOtpBean
import com.lyy.keepassa.entity.TotpType
import com.lyy.keepassa.entity.TotpType.CUSTOM
import com.lyy.keepassa.entity.TotpType.DEFAULT
import com.lyy.keepassa.entity.TotpType.STEAM
import com.lyy.keepassa.util.doClick
import com.lyy.keepassa.util.getKeeTrayBean
import com.lyy.keepassa.util.getKeepassBean
import com.lyy.keepassa.util.getKeepassXcBean
import com.lyy.keepassa.util.otpIsKeeTraySteam
import com.lyy.keepassa.util.otpIsKeeTrayTotp
import com.lyy.keepassa.util.otpKeepass
import com.lyy.keepassa.util.otpKeepassXC
import com.lyy.keepassa.util.totp.OtpEnum
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import java.util.UUID

/**
 * @Author laoyuyu
 * @Description
 * @Date 2:27 PM 2024/1/11
 **/
@Route(path = "/dialog/otpModify")
class ModifyOtpDialog : BaseDialog<DialogOtpModifyBinding>() {
  private var arithmetic = HashAlgorithm.SHA1
  private var time = 30
  private var len = 6
  private var secret = ""

  @Autowired(name = "uid")
  lateinit var uid: UUID
  private lateinit var pwEntryV4: PwEntryV4
  private lateinit var otpBean: IOtpBean
  private var entryType = OtpEnum.TRAY_TOTP
  private lateinit var handler: IOtpModifyHandler

  override fun initData() {
    super.initData()
    ARouter.getInstance().inject(this)
    pwEntryV4 = BaseApp.KDB.pm.entries[uid] as PwEntryV4
    handleLayoutSwitch()
    handleSp()
    handleSlTime()
    handleBtn()
  }

  override fun setLayoutId(): Int {
    return R.layout.dialog_otp_modify
  }

  private fun handleLayoutSwitch() {
    binding.contentLayout.rgTotp.setOnCheckedChangeListener { _, checkedId ->
      val rb = findViewById<RadioButton>(checkedId)
      val totpType = TotpType.from(rb.tag as String)
      when (totpType) {
        DEFAULT, STEAM -> {
          binding.contentLayout.group.visibility = View.GONE
        }

        CUSTOM -> {
          binding.contentLayout.group.visibility = View.VISIBLE
        }
      }
    }
    when {
      pwEntryV4.otpIsKeeTrayTotp() -> {
        handler = OtpKeeTrayHandler()

      }

      pwEntryV4.otpIsKeeTraySteam() -> {
        binding.contentLayout.group.isVisible = false
        binding.contentLayout.rbSteam.isChecked = true
        handleTrayOtp()
      }

      pwEntryV4.otpKeepassXC() -> {
        binding.contentLayout.group.isVisible = true
        binding.contentLayout.rbCustom.isChecked = true
        handleKeepassXc()
      }

      pwEntryV4.otpKeepass() -> {
        binding.contentLayout.group.isVisible = true
        binding.contentLayout.rbSteam.isVisible = false
        binding.contentLayout.rbCustom.isChecked = true
        handleKeepass()
      }
    }
  }


  private fun handleKeepassXc() {
    val bean = pwEntryV4.getKeepassXcBean()
    entryType = OtpEnum.KEEPASSXC
    otpBean = bean
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

  private fun handleKeepass() {
    val oBean = pwEntryV4.getKeepassBean()
    val bean = oBean.otpBean
    if (bean == null) {
      ToastUtils.showLong(ResUtil.getString(R.string.not_souper_otp))
      return
    }
    entryType = OtpEnum.KEEPASSXC
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

  private fun handleSp() {
    binding.contentLayout.sp.onItemSelectedListener =
      (object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        override fun onItemSelected(
          parent: AdapterView<*>?,
          view: View?,
          position: Int,
          id: Long
        ) {
          arithmetic = when (position) {
            1 -> HashAlgorithm.SHA256
            2 -> HashAlgorithm.SHA512
            else -> HashAlgorithm.SHA1
          }
        }
      })
  }

  private fun handleSlTime() {
    binding.contentLayout.slTime
      .addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {
        }

        override fun onStopTrackingTouch(slider: Slider) {
          time = slider.value.toInt()
        }
      })

    binding.contentLayout.slLen
      .addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {
        }

        override fun onStopTrackingTouch(slider: Slider) {
          len = slider.value.toInt()
        }
      })

    binding.contentLayout.strKey.doAfterTextChanged {
      secret = it?.trim().toString()
    }
  }

  private fun handleBtn() {
    binding.contentLayout.enter.doClick {

    }

    binding.contentLayout.cancel.doClick {
      dismiss()
    }
  }

}