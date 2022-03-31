/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.search

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwDataInf
import com.lyy.keepassa.R
import com.lyy.keepassa.R.color
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogSearchBinding
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.doOnItemClickListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 搜索
 */
class SearchDialog : BaseDialog<DialogSearchBinding>() {

  private lateinit var module: SearchModule
  private lateinit var adapter: SearchAdapter

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
    initList()
    initSearchWidget()
    listenerGetSearchData()
    getRecordData()
  }

  private fun initSearchWidget() {
    binding.search.requestFocusFromTouch()
    binding.search.setIconifiedByDefault(true)
    binding.search.isIconified = false
    binding.search.setOnQueryTextListener(object : OnQueryTextListener {
      /**
       * 当点击搜索按钮时触发该方法
       */
      override fun onQueryTextSubmit(query: String?): Boolean {
        if (!query.isNullOrBlank()) {
          adapter.queryString = query
          searchData(query)
        }

        return true
      }

      /**
       * 当搜索内容改变时触发该方法
       */
      override fun onQueryTextChange(newText: String?): Boolean {
        if (!newText.isNullOrBlank()) {
          adapter.queryString = newText
          searchData(newText)
        }
        return true
      }
    })
  }

  @SuppressLint("NotifyDataSetChanged")
  private fun listenerGetSearchData() {
    lifecycleScope.launch {
      module.searchDataFlow.collectLatest { list ->
        if (!list.isNullOrEmpty()) {
          adapter.notifyDataSetChanged()
        }
      }
    }
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
  }

  private fun getRecordData() {
    module.getSearchRecord()
  }

  /**
   * 初始化列表
   */
  private fun initList() {
    adapter = SearchAdapter(requireContext(), module.listData) { v ->
      val position = v.tag as Int
      val item = module.listData[position]
      module.delHistoryRecord(item.title.toString()) {
        module.listData.remove(item)
        adapter.notifyItemRemoved(position)
      }
    }
    binding.list.layoutManager = LinearLayoutManager(context)
    binding.list.adapter = adapter
    binding.list.doOnItemClickListener { _, position, _ ->
      val item = module.listData[position]
      if (item.type == SearchAdapter.ITEM_TYPE_RECORD) {
        // 处理历史记录，直接使用该历史搜索数据，输入框设置该历史记录
        binding.search.setQuery(item.title, true)
        return@doOnItemClickListener
      }
      // 处理搜索结果
      module.saveSearchRecord(item.title.toString())
      KeepassAUtil.instance.turnEntryDetail(context as FragmentActivity, item.obj as PwDataInf)
      dismiss()
    }
  }
}