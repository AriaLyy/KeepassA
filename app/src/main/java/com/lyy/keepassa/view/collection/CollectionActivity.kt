/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.collection

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntry
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivityCollectionBinding
import com.lyy.keepassa.event.CollectionEventType.COLLECTION_STATE_ADD
import com.lyy.keepassa.event.CollectionEventType.COLLECTION_STATE_REMOVE
import com.lyy.keepassa.event.CollectionEventType.COLLECTION_STATE_TOTAL
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.view.SimpleEntryAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @Author laoyuyu
 * @Description
 * @Date 19:43 上午 2022/3/29
 **/
@Route(path = "/collection/ac")
internal class CollectionActivity : BaseActivity<ActivityCollectionBinding>() {
  private lateinit var module: CollectionModule
  private lateinit var adapter: SimpleEntryAdapter

  override fun setLayoutId(): Int {
    return R.layout.activity_collection
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    toolbar.title = ResUtil.getString(R.string.my_collection)
    module = ViewModelProvider(this)[CollectionModule::class.java]

    adapter = SimpleEntryAdapter(this, module.itemDataList)
    binding.rvList.let {
      it.layoutManager = LinearLayoutManager(this)
      it.setHasFixedSize(true)
      it.adapter = adapter
    }

    binding.rvList.doOnItemClickListener { _, position, v ->
      val item = module.itemDataList[position]
      val icon = v.findViewById<AppCompatImageView>(R.id.icon)
      KeepassAUtil.instance.turnEntryDetail(this, item.obj as PwEntry, icon)
    }

    listenerCollection()
    listenerGetData()
    module.getData()
  }

  @SuppressLint("NotifyDataSetChanged")
  private fun listenerGetData() {
    lifecycleScope.launch {
      module.itemDataFlow.collectLatest {
        if (it == null) {
          binding.emptyView.visibility = View.VISIBLE
          return@collectLatest
        }
        binding.emptyView.visibility = View.GONE
        adapter.notifyDataSetChanged()
      }
    }
  }

  @SuppressLint("NotifyDataSetChanged")
  private fun listenerCollection() {
    lifecycleScope.launch {
      KpaUtil.kdbHandlerService.collectionStateFlow.collectLatest {
        if (it.collectionNum == 0) {
          binding.emptyView.visibility = View.VISIBLE
          return@collectLatest
        }
        binding.emptyView.visibility = View.GONE
        when (it.state) {
          COLLECTION_STATE_TOTAL -> {
            adapter.notifyDataSetChanged()
          }
          COLLECTION_STATE_ADD -> {
            module.addNewItem(adapter, it.pwEntryV4)
          }
          COLLECTION_STATE_REMOVE -> {
            module.removeItem(adapter, it.pwEntryV4)
          }
        }
      }
    }
  }
}