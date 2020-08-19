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
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivityGeneratePassBinding
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.PasswordBuilUtil
import com.lyy.keepassa.widget.discreteSeekBar.DiscreteSeekBar
import com.lyy.keepassa.widget.discreteSeekBar.DiscreteSeekBar.OnProgressChangeListener

/**
 * 密码生成器
 */
class GeneratePassActivity : BaseActivity<ActivityGeneratePassBinding>(), OnCheckedChangeListener {

  private lateinit var generater: PasswordBuilUtil
  private var passLen = 6
  private var isUserInputPass = false

  companion object {
    const val DATA_PASS_WORD = "DATA_PASS_WORD"
  }

  override fun setLayoutId(): Int {
    return R.layout.activity_generate_pass
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    toolbar.title = getString(R.string.pass_generater)
    binding.cancel.setOnClickListener {
      finishAfterTransition()
    }
    binding.passLen.setText("$passLen")
    binding.passLenProgress.setOnProgressChangeListener(object : OnProgressChangeListener {
      override fun onProgressChanged(
        seekBar: DiscreteSeekBar?,
        value: Int,
        fromUser: Boolean
      ) {
        isUserInputPass = false
        passLen = value
        binding.passLen.setText("$value")
      }

      override fun onStartTrackingTouch(seekBar: DiscreteSeekBar?) {
      }

      override fun onStopTrackingTouch(seekBar: DiscreteSeekBar?) {
      }

    })
    generater = PasswordBuilUtil.getInstance()
    binding.upper.setOnCheckedChangeListener(this)
    binding.lower.setOnCheckedChangeListener(this)
    binding.numer.setOnCheckedChangeListener(this)
    binding.minus.setOnCheckedChangeListener(this)
    binding.underline.setOnCheckedChangeListener(this)
    binding.space.setOnCheckedChangeListener(this)
    binding.special.setOnCheckedChangeListener(this)
    binding.bracket.setOnCheckedChangeListener(this)

    binding.upper.isChecked = true
    binding.lower.isChecked = true
    binding.numer.isChecked = true
    generatePass(passLen)

    binding.enter.setOnClickListener {
      if (checkParamsIsInvalid()) {
        HitUtil.toaskShort(getString(R.string.error_genera_params))
        return@setOnClickListener
      }
      val intent = Intent()
      intent.putExtra(DATA_PASS_WORD, binding.passEdit.text.toString().trim())
      setResult(Activity.RESULT_OK, intent)
      finishAfterTransition()
    }
    binding.passLen.doAfterTextChanged { text ->
      if (!TextUtils.isEmpty(text)) {
        passLen = text.toString()
            .toInt()
        generatePass(passLen)
      }
    }
  }

  /**
   * 检查密码生成条件
   * @return true: 条件无效
   */
  private fun checkParamsIsInvalid(): Boolean {
    return !binding.upper.isChecked
        && !binding.lower.isChecked
        && !binding.numer.isChecked
        && !binding.minus.isChecked
        && !binding.underline.isChecked
        && !binding.space.isChecked
        && !binding.special.isChecked
        && !binding.bracket.isChecked
  }

  /**
   * 生产密码
   * @param len 密码长度
   */
  private fun generatePass(len: Int): String {
    if (checkParamsIsInvalid()) {
      binding.passEdit.setText("")
      return ""
    }
    generater.clear()
    if (binding.upper.isChecked) {
      generater.addUpChar()
    }
    if (binding.lower.isChecked) {
      generater.addLowerChar()
    }
    if (binding.numer.isChecked) {
      generater.addNumChar()
    }
    if (binding.minus.isChecked) {
      generater.addMinus()
    }
    if (binding.underline.isChecked) {
      generater.addUnderline()
    }
    if (binding.space.isChecked) {
      generater.addSpaceChar()
    }
    if (binding.special.isChecked) {
      generater.addSymbolChar()
    }
    if (binding.bracket.isChecked) {
      generater.addbracketChar()
    }

    val pass = generater.builder(len)
    binding.passEdit.setText(pass)
    return pass
  }

  override fun onCheckedChanged(
    buttonView: CompoundButton?,
    isChecked: Boolean
  ) {
    generatePass(passLen)
  }
}