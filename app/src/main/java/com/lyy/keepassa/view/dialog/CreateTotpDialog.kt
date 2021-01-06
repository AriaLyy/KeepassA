/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog

import android.view.View
import android.widget.AdapterView
import android.widget.RadioButton
import com.arialyy.frame.base.BaseDialog
import com.google.android.material.slider.Slider
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.DialogCreateTotpBinding
import com.lyy.keepassa.entity.TotpType
import com.lyy.keepassa.entity.TotpType.CUSTOM
import com.lyy.keepassa.entity.TotpType.DEFAULT
import com.lyy.keepassa.entity.TotpType.STEAM
import com.lyy.keepassa.event.CreateAttrStrEvent
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.util.getArgument
import com.lyy.keepassa.widget.expand.AttrStrItemView
import org.greenrobot.eventbus.EventBus

class CreateTotpDialog : BaseDialog<DialogCreateTotpBinding>(), View.OnClickListener {
  private var arithmetic = "SHA1"
  private var time = 30
  private var len = 6
  private var totpType = DEFAULT

  private val isEdit by lazy {
    getArgument<Boolean>("isEdit") ?: false
  }

  private val entryTitle by lazy {
    getArgument<String>("entryTitle") ?: "title"
  }

  private val entryUserName by lazy {
    getArgument<String>("entryUserName") ?: "name"
  }

  var itemView: AttrStrItemView? = null

  override fun setLayoutId(): Int {
    return R.layout.dialog_create_totp
  }

  override fun initData() {
    super.initData()
    binding.enter.setOnClickListener(this)
    binding.cancel.setOnClickListener(this)
    binding.rgTotp.setOnCheckedChangeListener { _, checkedId ->
      val rb = findViewById<RadioButton>(checkedId)
      totpType = TotpType.from(rb.tag as String)
      when (totpType) {
        DEFAULT -> {
          binding.clConstom.visibility = View.GONE
        }
        STEAM -> {
          binding.clConstom.visibility = View.GONE
        }
        CUSTOM -> {
          binding.clConstom.visibility = View.VISIBLE
        }
      }
    }
    binding.rbDefault.isChecked = true
    binding.sp.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener {
      override fun onNothingSelected(parent: AdapterView<*>?) {
      }

      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        arithmetic = when (position) {
          0 -> "SHA1"
          1 -> "SHA256"
          2 -> "SHA512"
          else -> "SHA1"
        }
      }
    })
    binding.slTime.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
      override fun onStartTrackingTouch(slider: Slider) {
      }

      override fun onStopTrackingTouch(slider: Slider) {
        time = slider.value.toInt()
      }
    })
    binding.slLen.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
      override fun onStartTrackingTouch(slider: Slider) {
      }

      override fun onStopTrackingTouch(slider: Slider) {
        len = slider.value.toInt()
      }
    })
  }

  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.enter -> {
        val keyStr = binding.strKey.text.toString()
        if (keyStr.isEmpty()) {
          binding.strKey.error = "key is null"
          return
        }
        KLog.d(TAG, "key = $keyStr arit = $arithmetic, time = $time, len = $len")
        createTotpStr()
        dismiss()
        return
      }
    }
    dismiss()
  }

  private fun createTotpStr() {
    when (totpType) {
      DEFAULT, STEAM -> {
        val seed = ProtectedString(true, binding.strKey.text.toString())
        seed.isOtpPass = true
        EventBus.getDefault()
            .post(
                CreateAttrStrEvent(
                    "TOTP Seed",
                    seed,
                    isEdit,
                    itemView
                )
            )
        EventBus.getDefault()
            .post(
                CreateAttrStrEvent(
                    "TOTP Settings",
                    ProtectedString(false, "$time;${if (totpType == STEAM) "S" else "6"}"),
                    isEdit,
                    itemView
                )
            )
      }
      CUSTOM -> {
        val seedStr =
          "otpauth://totp/$entryTitle:$entryUserName?secret=${binding.strKey.text.toString()}&period=$time&digits=$len&issuer=$entryTitle&algorithm=$arithmetic"
        EventBus.getDefault()
            .post(
                CreateAttrStrEvent(
                    "otp",
                    ProtectedString(true, seedStr),
                    isEdit,
                    itemView
                )
            )
      }
    }
  }
}