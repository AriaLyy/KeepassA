/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseBottomSheetDialogFragment
import com.lyy.keepassa.databinding.DialogPathTypeBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.view.DbPathType.AFS
import com.lyy.keepassa.view.DbPathType.DROPBOX
import com.lyy.keepassa.view.DbPathType.ONE_DRIVE
import com.lyy.keepassa.view.DbPathType.WEBDAV
import com.lyy.keepassa.view.SimpleAdapter

/**
 * 数据库路径选择
 * @param dbName 如果是webdav，该字段为文件的http url
 */
class PathTypeDialog(
  private val dbName: String
) : BaseBottomSheetDialogFragment<DialogPathTypeBinding>(), View.OnClickListener {

  private lateinit var module: CreateDbModule

  override fun setLayoutId(): Int {
    return R.layout.dialog_path_type
  }

  override fun init(savedInstanceState: Bundle?) {
    super.init(savedInstanceState)
    module = ViewModelProvider(requireActivity())
        .get(CreateDbModule::class.java)

    val data: ArrayList<SimpleItemEntity> = ArrayList()
    val adapter = SimpleAdapter(requireContext(), data)
    binding.list.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    binding.list.adapter = adapter
    binding.content.post {
      module.getDbOpenTypeData(requireContext())
          .observe(this, Observer { items ->
            data.addAll(items)
            adapter.notifyDataSetChanged()
          })
    }
    mRootView.setOnClickListener(this)
    binding.close.setOnClickListener(this)
    RvItemClickSupport.addTo(binding.list)
        .setOnItemClickListener { _, position, _ ->
          val item = data[position]
          when (item.icon) {
            R.drawable.ic_android -> {//使用系统文件管理器
              module.dbPathType = AFS
            }
            R.drawable.ic_dropbox -> { // dropbox
              module.dbPathType = DROPBOX
            }
            R.drawable.ic_http -> { // webDav
              module.dbPathType = WEBDAV
            }
            R.drawable.ic_onedrive -> { // onedrive
              module.dbPathType = ONE_DRIVE
            }
          }
          dismiss()
        }

  }

  override fun onClick(v: View?) {
    when (v!!.id) {
      mRootView.id, R.id.close -> dismiss()
    }
  }

}