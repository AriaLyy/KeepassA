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
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.DialogCreateTotpBinding
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KLog

class CreateTotpDialog : BaseDialog<DialogCreateTotpBinding>(), View.OnClickListener {
  private var arithmetic = "sha-1"
  private var time = 30
  private var len = 6
  override fun setLayoutId(): Int {
    return R.layout.dialog_create_totp
  }

  override fun initData() {
    super.initData()
    binding.enter.setOnClickListener(this)
    binding.cancel.setOnClickListener(this)
    binding.rgTotp.setOnCheckedChangeListener { group, checkedId ->
      val rb = findViewById<RadioButton>(checkedId)
      when (rb.tag) {
        "default" -> {
          binding.clConstom.visibility = View.GONE
        }
        "steam" -> {
          binding.clConstom.visibility = View.GONE
        }
        "custom" -> {
          binding.clConstom.visibility = View.VISIBLE
        }
      }
    }
    binding.rbDefault.isChecked = true
    binding.sp.onItemSelectedListener =  (object :AdapterView.OnItemSelectedListener{
      override fun onNothingSelected(parent: AdapterView<*>?) {
      }

      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        arithmetic = when (position) {
          0 -> "sha-1"
          1 -> "sha-256"
          2 -> "sha-512"
          else -> "sha-1"
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
        if (binding.strKey.text.toString().isEmpty()){
          binding.strKey.error = "key is null"
          return
        }
        KLog.d(
            TAG,
            "key = ${binding.strKey.text.toString()} arit = $arithmetic, time = $time, len = $len"
        )
        dismiss()
        return
      }
    }
    dismiss()
  }
}