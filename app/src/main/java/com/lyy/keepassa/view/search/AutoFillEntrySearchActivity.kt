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
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.autofill.AutofillManager
import android.widget.Button
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arialyy.frame.router.Routerfit
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 自动填充条目找不到时的选择页面
 */
@SuppressLint("NotifyDataSetChanged")
class AutoFillEntrySearchActivity : BaseActivity<ActivityAutoFillEntrySearchBinding>() {

  private lateinit var module: SearchModule
  private lateinit var adapter: SearchAdapter
  private var curEntry: PwEntry? = null

  companion object {
    /**
     * 是否保存关联
     */
    const val EXTRA_IS_SAVE_RELEVANCE = "EXTRA_IS_SAVE_RELEVANCE"

    /**
     * 条目id
     */
    const val EXTRA_ENTRY_ID = "EXTRA_ENTRY_ID"
    private const val KEY_PKG_NAME = "KEY_PKG_NAME"
    private const val KEY_IS_AUTH_FORM_FILL = "KEY_IS_AUTH_FORM_FILL"
    private const val KEY_AUTO_FILL_FIELD = "KEY_IS_AUTH_FORM_FILL"

    /**
     * 从通知进入搜索页
     */
    internal fun createSearchPending(
      context: Context,
      apkPkgName: String,
      structure: AssistStructure
    ): PendingIntent {
      val intent = Intent(context, AutoFillEntrySearchActivity::class.java).also { it ->
        it.putExtra(KEY_IS_AUTH_FORM_FILL, true)
        it.putExtra(KEY_PKG_NAME, apkPkgName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          it.putExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE, structure)
        }
      }
      return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * 启动搜索界面
     * @param domain
     */
    fun turnSearchActivity(
      context: Activity,
      requestCode: Int,
      apkPkgName: String,
      domain: String? = null
    ) {
      val intent = Intent(context, AutoFillEntrySearchActivity::class.java).apply {
        putExtra(KEY_PKG_NAME, apkPkgName)
      }
      context.startActivityForResult(
        intent,
        requestCode,
        ActivityOptions.makeSceneTransitionAnimation(context)
          .toBundle()
      )
    }

    /**
     * 没有匹配数据时，启动搜索界面
     */
    internal fun getSearchIntentSender(
      context: Context,
      apkPackageName: String
    ): IntentSender {
      val intent = Intent(context, AutoFillEntrySearchActivity::class.java).also {
        it.putExtra(KEY_IS_AUTH_FORM_FILL, true)
        it.putExtra(KEY_PKG_NAME, apkPackageName)
        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      return PendingIntent.getActivity(
        context,
        1,
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
      )
        .intentSender
    }
  }

  /**
   * 是否由自动填充服务启动
   */
  private var isFromFill: Boolean = false

  /**
   * 第三方应用包名
   */
  private var apkPkgName: String? = null

  override fun setLayoutId(): Int {
    return R.layout.activity_auto_fill_entry_search
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    module = ViewModelProvider(this).get(SearchModule::class.java)
    apkPkgName = intent.getStringExtra(KEY_PKG_NAME)
    isFromFill = intent.getBooleanExtra(KEY_IS_AUTH_FORM_FILL, false)
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
    module.searchEntry(query, isFromFill)
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
      /*
        if is From autofill and that is group, No operation
       */
      if (isFromFill && item.obj is PwGroup) {
        return@doOnItemClickListener
      }
      val entry = item.obj as PwEntry
      // if is from browser, that entry will be ignore
      if (W3cHints.isBrowser(apkPkgName!!)) {
        callbackAutoFillService(false, entry)
        return@doOnItemClickListener
      }

      val msg = Html.fromHtml(getString(R.string.hint_save_auto_fill, apkPkgName, entry.title))
      Routerfit.create(DialogRouter::class.java).showMsgDialog(
        msgContent = msg,
        btnClickListener = object : OnMsgBtClickListener {
          override fun onCover(v: Button) {
          }

          override fun onEnter(v: Button) {
            if (entry is PwEntryV4) {
              // 保存记录
              relevanceEntry(entry)
            } else {
              callbackAutoFillService(false, entry)
            }
          }

          override fun onCancel(v: Button) {
            callbackAutoFillService(false, entry)
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
      callbackAutoFillService(false, pwEntry)
    }
  }

  /**
   * 填充数据到自动填充服务
   * @param isNotSaveRelevance 是否保存关联
   */
  @TargetApi(Build.VERSION_CODES.O)
  private fun callbackAutoFillService(
    isNotSaveRelevance: Boolean,
    pwEntry: PwEntry
  ) {
    if (!isFromFill) {
      val data = Intent().apply {
        putExtra(
          EXTRA_IS_SAVE_RELEVANCE, isNotSaveRelevance
        )
        putExtra(
          EXTRA_ENTRY_ID, pwEntry.uuid
        )
      }
      setResult(Activity.RESULT_OK, data)
      finishAfterTransition()
      return
    }
    if (isNotSaveRelevance) {
      setResult(
        Activity.RESULT_OK, KeepassAUtil.instance.getFillResponse(
          this,
          intent,
          apkPkgName!!
        )
      )
    } else {
      setResult(
        Activity.RESULT_OK,
        KeepassAUtil.instance.getFillResponse(this, intent, pwEntry, apkPkgName!!)
      )
    }
    finish()
  }

  override fun onBackPressed() {
    super.onBackPressed()
    setResult(Activity.RESULT_CANCELED)
  }
}