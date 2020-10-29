/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.search

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.arialyy.frame.base.BaseDialog
import com.arialyy.frame.util.ResUtil
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.keepassdroid.database.PwDataInf
import com.lyy.keepassa.R
import com.lyy.keepassa.R.color
import com.lyy.keepassa.databinding.DialogSearchBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.KeepassAUtil

/**
 * 搜索
 */
class SearchDialog : BaseDialog<DialogSearchBinding>() {

  private lateinit var module: SearchModule
  private lateinit var adapter: SearchAdapter
  private val date: ArrayList<SimpleItemEntity> = ArrayList()

  init {
    setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog)
  }

  override fun setLayoutId(): Int {
    return R.layout.dialog_search
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    dialog!!.window!!.setBackgroundDrawable(ColorDrawable(ResUtil.getColor(color.mask)))
    return super.onCreateView(inflater, container, savedInstanceState)
  }

  override fun initData() {
    super.initData()
    module = ViewModelProvider(requireActivity()).get(SearchModule::class.java)
    binding.bg.setOnClickListener {
      dismiss()
    }
    binding.search.requestFocusFromTouch()
    binding.search.setIconifiedByDefault(true)
    binding.search.isIconified = false
    binding.search.setOnQueryTextListener(object : OnQueryTextListener {
      /**
       * 当点击搜索按钮时触发该方法
       */
      override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null) {
          searchData(query)
        }

        return true
      }

      /**
       * 当搜索内容改变时触发该方法
       */
      override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null) {
          searchData(newText)
        }
        return true
      }

    })
    initList()
  }

  /**
   * 搜索数据
   */
  private fun searchData(query: String) {
    if (query.isEmpty()) {
      getRecordData()
      return
    }
    module.searchEntry(query)
        .observe(this, Observer { list ->
          if (list != null) {
            date.clear()
            date.addAll(list)
            adapter.queryString = query
            adapter.notifyDataSetChanged()
          }
        })
  }

  private fun getRecordData() {
    module.getSearchRecord()
        .observe(this, Observer { list ->
          date.clear()
          if (list != null) {
            date.addAll(list)
          }

          adapter.notifyDataSetChanged()
        })
  }

  /**
   * 初始化列表
   */
  private fun initList() {
    adapter = SearchAdapter(requireContext(), date, OnClickListener { v ->
      val position = v.tag as Int
      val item = date[position]
      module.delHistoryRecord(item.title)
          .observe(this, Observer {
            date.remove(item)
            adapter.notifyDataSetChanged()
          })

    })
    binding.list.layoutManager = LinearLayoutManager(context)
    binding.list.adapter = adapter
    RvItemClickSupport.addTo(binding.list)
        .setOnItemClickListener { _, position, _ ->
          val item = date[position]
          if (item.type == SearchAdapter.ITEM_TYPE_RECORD) {
            // 处理历史记录，直接使用该历史搜索数据，输入框设置该历史记录
            binding.search.setQuery(item.title, true)
            return@setOnItemClickListener
          }
          // 处理搜索结果
          module.saveSearchRecord(item.title)
          KeepassAUtil.turnEntryDetail(context as FragmentActivity, item.obj as PwDataInf)
          dismiss()
        }
    getRecordData()
  }

}