/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.ToastUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogPlayDonateBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2022/2/7
 **/
@Route(path = "/dialog/playDonate")
class PlayDonateDialog : BaseDialog<DialogPlayDonateBinding>() {
  private lateinit var module: PlayDonateModule

  override fun setLayoutId(): Int {
    return R.layout.dialog_play_donate
  }

  override fun onStart() {
    super.onStart()
    dialog?.window?.setLayout(
      resources.getDimension(R.dimen.dialog_min_width).toInt(),
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  override fun initData() {
    super.initData()
    module = ViewModelProvider(this)[PlayDonateModule::class.java]
    binding.dialog = this
    binding.slider.addOnChangeListener { _, value, _ ->
      Timber.d("value = $value")
      module.curIndex = value
      binding.tvMoney.text = module.convertValue(value)
    }
    binding.slider.setLabelFormatter { it ->
      module.convertValue(it)
    }
    lifecycleScope.launch {
      module.playFlow.collectLatest {
        if (it == PlayDonateModule.STATE_DEFAULT){
          return@collectLatest
        }
        val resId = when (it) {
          PlayDonateModule.STATE_CONNECT_PLAY_SERVICE_ERROR -> {
            R.string.error_connect_play
          }
          PlayDonateModule.STATE_DONATE_SUCCESS -> {
            dismiss()
            R.string.thank_donate
          }
          PlayDonateModule.STATE_DONATE_FAIL -> {
            R.string.error_donate
          }
          else -> {
            R.string.error_donate
          }
        }
        ToastUtils.showLong(resId)
      }
    }
  }

  fun onClick(v: View) {
    when (v.id) {
      R.id.enter -> {
        module.startFlow(requireActivity(), module.curIndex)
      }
      R.id.cancel -> {
        dismiss()
      }
    }
  }
}