/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main

import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.FragmentOnlyListBinding

/**
 * @Author laoyuyu
 * @Description
 * @Date 3:58 下午 2021/10/25
 **/
class TOTPFragment : BaseFragment<FragmentOnlyListBinding>() {

  override fun initData() {

  }

  override fun setLayoutId(): Int {
    return R.layout.fragment_only_list
  }
}