/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseBottomSheetDialogFragment
import com.lyy.keepassa.databinding.FragmentOnlyListBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.view.SimpleAdapter

/**
 * 云盘文件选择对话框
 */
class CloudDiskFileDialog(val data: ArrayList<SimpleItemEntity>) : BaseBottomSheetDialogFragment<FragmentOnlyListBinding>() {
  private lateinit var adapter: SimpleAdapter
//  private val data = ArrayList<SimpleItemEntity>()

  override fun setLayoutId(): Int {
    return R.layout.fragment_only_list
  }

  override fun init(savedInstanceState: Bundle?) {
    super.init(savedInstanceState)
    adapter = SimpleAdapter(context!!, data)
    binding.list.adapter = adapter
    binding.list.layoutManager = LinearLayoutManager(context)
    binding.list.setHasFixedSize(true)
  }

}