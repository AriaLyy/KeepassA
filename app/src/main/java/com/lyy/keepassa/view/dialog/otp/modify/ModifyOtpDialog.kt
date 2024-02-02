package com.lyy.keepassa.view.dialog.otp.modify

import android.view.View
import android.widget.AdapterView
import android.widget.RadioButton
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
import com.lyy.keepassa.entity.TotpType
import com.lyy.keepassa.entity.TotpType.CUSTOM
import com.lyy.keepassa.entity.TotpType.DEFAULT
import com.lyy.keepassa.entity.TotpType.STEAM
import com.lyy.keepassa.util.doClick
import com.lyy.keepassa.util.otpIsKeeTraySteam
import com.lyy.keepassa.util.otpIsKeeTrayTotp
import com.lyy.keepassa.util.otpKeepass
import com.lyy.keepassa.util.otpKeepassXC
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
  private var otpType = DEFAULT

  @Autowired(name = "uid")
  lateinit var uid: UUID
  lateinit var pwEntryV4: PwEntryV4
  private var handler: IOtpModifyHandler? = null

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
      otpType = TotpType.from(rb.tag as String)
      when (otpType) {
        DEFAULT, STEAM -> {
          binding.contentLayout.group.visibility = View.GONE
        }

        CUSTOM -> {
          binding.contentLayout.group.visibility = View.VISIBLE
        }
      }
    }
    handler = when {

      pwEntryV4.otpIsKeeTraySteam() -> {
        OtpKeeTraySteamHandler()
      }

      pwEntryV4.otpIsKeeTrayTotp() -> {
        OtpKeeTrayHandler()
      }

      pwEntryV4.otpKeepassXC() -> {
        OtpKeepassXcHandler()
      }

      pwEntryV4.otpKeepass() -> {
        OtpKeepassHandler()
      }

      else -> {
        ToastUtils.showLong(ResUtil.getString(R.string.error_totp))
        null
      }
    }
    handler?.initView(this)
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
      handler?.save(this, secret, arithmetic, len, time, otpType == STEAM)
    }

    binding.contentLayout.cancel.doClick {
      dismiss()
    }
  }
}