/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog.otp

import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.AdapterView
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.constraintlayout.widget.Group
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.ToastUtils
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogCreateTotpBinding
import com.lyy.keepassa.entity.GoogleOtpBean
import com.lyy.keepassa.entity.KeepassXcBean
import com.lyy.keepassa.entity.TotpType
import com.lyy.keepassa.entity.TotpType.CUSTOM
import com.lyy.keepassa.entity.TotpType.DEFAULT
import com.lyy.keepassa.entity.TotpType.STEAM
import com.lyy.keepassa.entity.TrayTotpBean
import com.lyy.keepassa.util.totp.OtpEnum
import com.lyy.keepassa.util.totp.OtpEnum.GOOGLE_OTP
import com.lyy.keepassa.util.totp.OtpEnum.KEEPASSXC
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import com.lyy.keepassa.view.QrCodeScannerActivity
import com.lyy.keepassa.widget.toPx
import kotlinx.coroutines.launch
import timber.log.Timber

@Route(path = "/dialog/createOtp")
internal class CreateOtpDialog : BaseDialog<DialogCreateTotpBinding>(), View.OnClickListener {
  private var arithmetic = HashAlgorithm.SHA1
  private var time = 30
  private var len = 6
  private var totpType = DEFAULT
  private var secret = ""
  private lateinit var module: CreateOtpModule

  @Autowired(name = "entryTitle")
  @JvmField
  var entryTitle: String = "title"

  @Autowired(name = "entryUserName")
  @JvmField
  var entryUserName: String = "name"

  private val barcodeLauncher =
    registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
      val str = result.contents
      if (str == null) {
        ToastUtils.showLong("Cancelled")
        return@registerForActivityResult
      }
      Timber.d("contents = ${str}")
      if (!str.startsWith("otpauth://")) {
        ToastUtils.showLong(ResUtil.getString(R.string.error_qr_code_str))
        return@registerForActivityResult
      }

      lifecycleScope.launch {
        CreateOtpModule.otpFlow.emit(
          Pair(GOOGLE_OTP, GoogleOtpBean(str))
        )
      }

      dismiss()
    }

  override fun setLayoutId(): Int {
    return R.layout.dialog_create_totp
  }

  override fun initData() {
    super.initData()
    ARouter.getInstance().inject(this)
    module = ViewModelProvider(requireActivity())[CreateOtpModule::class.java]
    handleOptionSwitch()
  }

  private fun handleOptionSwitch() {
    binding.menuLayout.ivNormal.setOnClickListener(this)
    binding.menuLayout.ivQrCode.setOnClickListener(this)
  }

  /**
   * handle default create otp ui
   */
  private fun handleDefaultFlow() {
    val vs = binding.root.findViewById<ViewStub>(R.id.vsCustom)
    vs.setOnInflateListener { _, parent ->
      val clConstom = findViewById<Group>(R.id.group)
      findViewById<Button>(R.id.enter).setOnClickListener(this)
      findViewById<Button>(R.id.cancel).setOnClickListener(this)

      findViewById<RadioGroup>(R.id.rgTotp).setOnCheckedChangeListener { _, checkedId ->
        val rb = findViewById<RadioButton>(checkedId)
        totpType = TotpType.from(rb.tag as String)
        when (totpType) {
          DEFAULT, STEAM -> {
            parent.layoutParams.height = 228.toPx()
            clConstom.visibility = View.GONE
          }

          CUSTOM -> {
            parent.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            clConstom.visibility = View.VISIBLE
          }
        }
      }

      findViewById<RadioButton>(R.id.rbDefault).isChecked = true

      findViewById<Spinner>(R.id.sp).onItemSelectedListener =
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

      findViewById<Slider>(R.id.slTime)
        .addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
          override fun onStartTrackingTouch(slider: Slider) {
          }

          override fun onStopTrackingTouch(slider: Slider) {
            time = slider.value.toInt()
          }
        })

      findViewById<Slider>(R.id.slLen)
        .addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
          override fun onStartTrackingTouch(slider: Slider) {
          }

          override fun onStopTrackingTouch(slider: Slider) {
            len = slider.value.toInt()
          }
        })

      findViewById<TextInputEditText>(R.id.str_key).doAfterTextChanged {
        secret = it?.trim().toString()
      }

    }


    if (vs != null && vs.parent != null && vs.visibility == View.GONE) {
      vs.inflate()
    }
  }

  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.enter -> {
        if (secret.isEmpty()) {
          ToastUtils.showLong("key is null")
          return
        }
        Timber.d("key = $secret arit = $arithmetic, time = $time, len = $len")
        createTotpStr()
        dismiss()
        return
      }

      R.id.ivNormal -> {
        handleDefaultFlow()
        binding.menuLayout.root.visibility = View.GONE
        return
      }

      R.id.ivQrCode -> {
        val options = ScanOptions()
        options.captureActivity = QrCodeScannerActivity::class.java
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan a barcode")
        options.setCameraId(0) // Use a specific camera of the device
        options.setBeepEnabled(false)
        options.setBarcodeImageEnabled(true)
        barcodeLauncher.launch(options)
        return
      }
    }
    dismiss()
  }

  private fun createTotpStr() {
    var otpEnum = OtpEnum.TRAY_TOTP
    val otpBean = when (totpType) {
      CUSTOM -> {
        otpEnum = KEEPASSXC
        KeepassXcBean(
          title = entryTitle,
          userName = entryUserName,
          isSteam = false,
          secret = secret,
          issuer = entryTitle,
          period = time,
          digits = len,
          algorithm = arithmetic
        )
      }

      DEFAULT -> {
        otpEnum = OtpEnum.TRAY_TOTP
        TrayTotpBean(secret = secret, period = time, isSteam = false)
      }

      STEAM -> {
        otpEnum = OtpEnum.TRAY_TOTP
        TrayTotpBean(secret = secret, period = time, isSteam = true)
      }
    }

    lifecycleScope.launch {
      CreateOtpModule.otpFlow.emit(Pair(otpEnum, otpBean))
    }
  }
}