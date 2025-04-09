/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnSliderTouchListener
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivityGeneratePassNewBinding
import com.lyy.keepassa.util.ClipboardUtil
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.PasswordBuildUtil
import com.lyy.keepassa.util.doClick

/**
 * 密码生成器
 */
class GeneratePassActivity : BaseActivity<ActivityGeneratePassNewBinding>(),
  OnCheckedChangeListener {

  private lateinit var generater: PasswordBuildUtil
  private var passLen = 16
  private var isUserInputPass = false

  companion object {
    const val DATA_PASS_WORD = "DATA_PASS_WORD"
  }

  override fun setLayoutId(): Int {
    return R.layout.activity_generate_pass_new
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    toolbar.title = getString(R.string.pass_generater)
    toolbar.inflateMenu(R.menu.menu_password)

    toolbar.setOnMenuItemClickListener { item ->

      when (item.itemId) {
        R.id.enter -> {
          val intent = Intent()
          intent.putExtra(DATA_PASS_WORD, binding.edPass.text.toString().trim())
          setResult(Activity.RESULT_OK, intent)
          finishAfterTransition()
        }
      }
      true
    }
    binding.edPassLen.setText("$passLen")

    binding.slider.addOnSliderTouchListener(object : OnSliderTouchListener {
      override fun onStartTrackingTouch(slider: Slider) {
      }

      override fun onStopTrackingTouch(slider: Slider) {
        isUserInputPass = false
        passLen = slider.value.toInt()
        binding.edPassLen.setText("$passLen")
      }
    })

    generater = PasswordBuildUtil.getInstance()
    binding.scUAZ.setOnCheckedChangeListener(this)
    binding.scLAZ.setOnCheckedChangeListener(this)
    binding.scNum.setOnCheckedChangeListener(this)
    binding.scCh.setOnCheckedChangeListener(this)
    binding.scBracketChar.setOnCheckedChangeListener(this)
    binding.scSpace.setOnCheckedChangeListener(this)

    binding.scUAZ.isChecked = true
    binding.scLAZ.isChecked = true
    binding.scNum.isChecked = true
    binding.scCh.isChecked = true
    generatePass(passLen)

    binding.ivRefresh.doClick {
      if (checkParamsIsInvalid()) {
        HitUtil.toaskShort(getString(R.string.error_genera_params))
        return@doClick
      }
      generatePass(passLen)
    }

    binding.ivCopy.doClick {
      if (checkParamsIsInvalid()) {
        HitUtil.toaskShort(getString(R.string.error_genera_params))
        return@doClick
      }
      ClipboardUtil.get()
        .copyDataToClip(binding.edPass.text.toString())
    }


    binding.edPassLen.doAfterTextChanged { text ->
      if (!TextUtils.isEmpty(text)) {
        passLen = text.toString()
          .toInt()
        generatePass(passLen)
      }
    }

    binding.slider.setLabelFormatter {
      "${it.toInt()}"
    }
  }


  /**
   * 检查密码生成条件
   * @return true: 条件无效
   */
  private fun checkParamsIsInvalid(): Boolean {
    return !binding.scUAZ.isChecked
      && !binding.scLAZ.isChecked
      && !binding.scNum.isChecked
      && !binding.scCh.isChecked
      && !binding.scBracketChar.isChecked
      && !binding.scSpace.isChecked
  }

  /**
   * 生产密码
   * @param len 密码长度
   */
  private fun generatePass(len: Int): String {
    if (checkParamsIsInvalid()) {
      binding.edPass.setText("")
      return ""
    }
    generater.clear()
    if (binding.scUAZ.isChecked) {
      generater.addUpChar()
    }
    if (binding.scLAZ.isChecked) {
      generater.addLowerChar()
    }
    if (binding.scNum.isChecked) {
      generater.addNumChar()
    }
    if (binding.scCh.isChecked) {
      generater.addMinus()
      generater.addUnderline()
      generater.addSymbolChar()
    }
    if (binding.scSpace.isChecked) {
      generater.addSpaceChar()
    }
    if (binding.scBracketChar.isChecked) {
      generater.addBracketChar()
    }

    val pass = generater.builder(len)
    binding.edPass.setText(pass)
    return pass
  }

  override fun onCheckedChanged(
    buttonView: CompoundButton?,
    isChecked: Boolean
  ) {
    generatePass(passLen)
  }
}