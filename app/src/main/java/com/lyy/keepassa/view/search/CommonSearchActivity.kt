/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.search

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.arialyy.frame.router.Routerfit
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivityAutoFillEntrySearchBinding
import com.lyy.keepassa.event.EntryState.CREATE
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.service.autofill.W3cHints
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.view.create.CreateEntryActivity
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 自动填充条目找不到时的选择页面
 */
@SuppressLint("NotifyDataSetChanged")
@Route(path = "/search/common")
internal class CommonSearchActivity : BaseActivity<ActivityAutoFillEntrySearchBinding>() {

  companion object {
    val searchFlow = MutableSharedFlow<PwEntry>()
  }

  private lateinit var module: SearchModule
  private lateinit var adapter: SearchAdapter
  private var curEntry: PwEntry? = null

  /**
   * 第三方应用包名
   */
  @Autowired(name = "apkPkgName")
  @JvmField
  var apkPkgName: String? = null

  /**
   * only search, not  Association
   */
  @Autowired(name = "onlySearch")
  @JvmField
  var onlySearch: Boolean = false

  override fun setLayoutId(): Int {
    return R.layout.activity_auto_fill_entry_search
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    module = ViewModelProvider(this).get(SearchModule::class.java)
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

    binding.exFab.setOnClickListener {
      startActivity(
        Intent(this, CreateEntryActivity::class.java).apply {
          putExtra(CreateEntryActivity.KEY_TYPE, CreateEntryActivity.TYPE_NEW_ENTRY)
        },
        ActivityOptions.makeSceneTransitionAnimation(this)
          .toBundle()
      )
    }

    initList()
    listenerGetSearchData()
    listenerEntryStateChange()
  }

  /**
   * listener the entry status change, there are three states: create, delete, and modify.
   */
  private fun listenerEntryStateChange() {
    lifecycleScope.launch {
      KpaUtil.kdbHandlerService.entryStateChangeFlow.collectLatest {
        if (it.pwEntryV4 == null) {
          return@collectLatest
        }
        when (it.state) {
          CREATE -> {
            val lastIndex = module.listData.size
            module.listData.add(KeepassAUtil.instance.convertPwEntry2Item(it.pwEntryV4))
            adapter.notifyItemInserted(lastIndex)
            binding.noEntryLayout.visibility = View.GONE
          }
          else -> {
            Timber.d("ignore other status")
          }
        }
      }
    }
  }

  private fun listenerGetSearchData() {
    lifecycleScope.launch {
      module.searchDataFlow.collectLatest { list ->
        if (list != null) {
          binding.noEntryLayout.visibility = View.GONE
          adapter.notifyDataSetChanged()
          return@collectLatest
        }
        adapter.notifyDataSetChanged()
        binding.noEntryLayout.visibility = View.VISIBLE
      }
    }
  }

  /**
   * 搜索数据
   */
  private fun searchData(query: String) {
    if (query.isEmpty()) {
      module.listData.clear()
      adapter.notifyDataSetChanged()
      binding.noEntryLayout.visibility = View.VISIBLE
      return
    }
    module.searchEntry1(query, false)
  }

  /**
   * 初始化列表
   */
  private fun initList() {
    adapter = SearchAdapter(this, module.listData, object : DelListener {
      override fun onDel(v: View, position: Int) {
        val item = module.listData[position]
        module.delHistoryRecord(item.title.toString()) {
          module.listData.remove(item)
          adapter.notifyDataSetChanged()
        }
      }
    })
    binding.list.layoutManager = LinearLayoutManager(this)
    binding.list.adapter = adapter

    binding.list.doOnItemClickListener { _, position, _ ->
      if (apkPkgName.isNullOrEmpty()) {
        return@doOnItemClickListener
      }
      val item = module.listData[position]
      val entry = item.obj as PwEntry
      // if is from browser, that entry will be ignore
      if (W3cHints.isBrowser(apkPkgName!!)) {
        callbackAutoFillService(entry)
        return@doOnItemClickListener
      }

      val msg = Html.fromHtml(getString(R.string.hint_save_auto_fill, apkPkgName, entry.title))
      Routerfit.create(DialogRouter::class.java).showMsgDialog(
        msgContent = msg,
        btnClickListener = object : OnMsgBtClickListener {
          override fun onCover(v: Button) {
          }

          override fun onEnter(v: Button) {
            if (entry is PwEntryV4 && !onlySearch) {
              // 保存记录
              relevanceEntry(entry)
            } else {
              callbackAutoFillService(entry)
            }
          }

          override fun onCancel(v: Button) {
            callbackAutoFillService(entry)
          }
        }
      )
    }
  }

  /**
   * 关联记录
   */
  private fun relevanceEntry(pwEntry: PwEntryV4) {
    curEntry = pwEntry

    module.relevanceEntry(pwEntry, apkPkgName!!) {
      if (it != DbSynUtil.STATE_SUCCEED) {
        HitUtil.toaskShort("${getString(R.string.relevance_db)}${getString(R.string.fail)}")
      }
      HitUtil.toaskShort("${getString(R.string.relevance_db)}${getString(R.string.success)}")
      callbackAutoFillService(pwEntry)
    }
  }

  /**
   * 填充数据到自动填充服务
   */
  @TargetApi(Build.VERSION_CODES.O)
  private fun callbackAutoFillService(pwEntry: PwEntry) {
    lifecycleScope.launch {
      searchFlow.emit(pwEntry)
    }
    finish()
  }

  override fun onBackPressed() {
    super.onBackPressed()
    setResult(Activity.RESULT_CANCELED)
  }
}