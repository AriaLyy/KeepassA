/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.fingerprint

import android.annotation.SuppressLint
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.FragmentFingerprintCloseBinding

/**
 * 指纹关闭展示页面
 */
class FingerprintCloseFragment : BaseFragment<FragmentFingerprintCloseBinding>() {
  @SuppressLint("SetTextI18n")
  override fun initData() {
    binding.tvHint.text = "${getString(R.string.fingerprint_unlock)}${getString(R.string.closed)}"
  }

  override fun setLayoutId(): Int {
    return R.layout.fragment_fingerprint_close
  }
}