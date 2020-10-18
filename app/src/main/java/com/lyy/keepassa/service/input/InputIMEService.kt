/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.input

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.NotificationUtil
import com.lyy.keepassa.util.OtpUtil
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity

/**
 * 输入法
 * https://developer.android.com/guide/topics/text/creating-input-method?hl=zh-cn
 */
class InputIMEService : InputMethodService(), View.OnClickListener {
  private val TAG = "InputIMEService"

  private var appPkgName: String? = ""
  private var ic: InputConnection? = null
  private var curEntry: PwEntry? = null
  private lateinit var candidatesList: RecyclerView
  private val candidatesData = arrayListOf<SimpleItemEntity>()
  private lateinit var candidatesAdapter: CandidatesAdapter
  private var imeOption = EditorInfo.IME_ACTION_GO

  /**
   * 当 IME 首次显示时，系统会调用 onCreateInputView() 回调。在此方法的实现中，您可以创建要在 IME 窗口中显示的布局，并将布局返回系统。
   */
  override fun onCreateInputView(): View {

    val layout = LayoutInflater.from(this)
        .inflate(R.layout.layout_kpa_ime, null) as ViewGroup
    candidatesList = layout.findViewById(R.id.rvContent)
    for (i in 0..layout.childCount) {
      val child = layout.getChildAt(i)
      if (child != null
          && (child is ImageView || child is TextView)
          && child.isClickable
      ) {
        child.setOnClickListener(this)
      }
    }
    initCandidatesLayout()
    return layout
  }

  private fun initCandidatesLayout() {
    candidatesAdapter = CandidatesAdapter(this, candidatesData)
    candidatesList.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
    candidatesList.setHasFixedSize(true)
    candidatesList.adapter = candidatesAdapter
    RvItemClickSupport.addTo(candidatesList)
        .setOnItemClickListener(object : RvItemClickSupport.OnItemClickListener {
          var lastPosition = 0
          override fun onItemClicked(
            recyclerView: RecyclerView?,
            position: Int,
            v: View?
          ) {
            val lastItemEntity = candidatesData[lastPosition]
            val curItemEntity = candidatesData[position]
            lastItemEntity.isSelected = false
            curItemEntity.isSelected = true
            candidatesAdapter.notifyItemChanged(lastPosition)
            candidatesAdapter.notifyItemChanged(position)
            lastPosition = position
            curEntry = curItemEntity.obj as PwEntry
          }
        })
  }

  /**
   * 输入法被唤起，开始输入
   */
  override fun onStartInputView(
    info: EditorInfo?,
    restarting: Boolean
  ) {
    super.onStartInputView(info, restarting)
    imeOption = info?.imeOptions ?: EditorInfo.IME_ACTION_GO
    candidatesData.clear()
    candidatesAdapter.notifyDataSetChanged()
    candidatesList.visibility = View.GONE
    curEntry = null
    ic = currentInputConnection
    KLog.d(TAG, "pkgName = ${info?.packageName}")
    appPkgName = info?.packageName
    showEntryList(searchEntry(appPkgName))
  }

  /**
   * 填充数据，如果有多个条目，启动对话框，让用户选择特定的条目
   */
  override fun onClick(v: View) {
    when (v.id) {
      // 锁定
      R.id.btLock -> {
        if (BaseApp.KDB == null || BaseApp.isLocked) {
          return
        }
        BaseApp.isLocked = true
        NotificationUtil.startDbLocked(this)
        val isOpenQuickLock = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
            .getBoolean(applicationContext.getString(R.string.set_quick_unlock), false)
        if (isOpenQuickLock) {
          return
        }
        BaseApp.KDB.clear(this)
        BaseApp.KDB = null
        return
      }

      // 用户名
      R.id.btAccount -> {
        if (!dbIsOpen()) {
          return
        }
        showEntryList(searchEntry(appPkgName))
        if (curEntry == null) {
          return
        }
        fillData(KdbUtil.getUserName(curEntry!!))
      }

      // 密码
      R.id.btPass -> {
        if (!dbIsOpen()) {
          return
        }
        showEntryList(searchEntry(appPkgName))
        if (curEntry == null) {
          return
        }
        fillData(KdbUtil.getPassword(curEntry!!))
      }

      // 关键软键盘
      R.id.btClose -> {
        requestHideSelf(InputMethodManager.HIDE_NOT_ALWAYS)
      }

      // 选择输入法
      R.id.btChangeIme -> {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
      }

      // totp
      R.id.btTotp -> {
        if (!dbIsOpen()) {
          return
        }
        showEntryList(searchEntry(appPkgName))
        if (curEntry == null) {
          return
        }
        OtpUtil.getOtpPass(curEntry as PwEntryV4).second?.let { fillData(it) }
      }

      // 其它信息
      R.id.btOtherInfo -> {
        if (!dbIsOpen()) {
          return
        }
        showEntryList(searchEntry(appPkgName))
      }

      // 回退键
      R.id.btBackspace -> {
        ic?.deleteSurroundingText(1, 0)
      }

      // 回车键
      R.id.btEnter -> {
        ic?.performEditorAction(imeOption)
      }
    }
  }

  /**
   * 如果有多个条目，显示条目列表
   */
  private fun showEntryList(entries: List<PwEntry>) {
    if (entries.isNullOrEmpty()) {
      candidatesList.visibility = View.GONE
      return
    }
    if (entries.size == 1) {
      candidatesList.visibility = View.GONE
      curEntry = entries[0]
      return
    }
    candidatesList.visibility = View.VISIBLE
    entries.forEachIndexed { index, pwEntry ->
      val item = SimpleItemEntity()
      item.title = pwEntry.title
      item.obj = pwEntry
      if (index == 0) {
        item.isSelected = true
        curEntry = pwEntry
      }
      candidatesData.add(item)
    }
    candidatesAdapter.notifyDataSetChanged()
  }

  /**
   * 填充数据
   */
  private fun fillData(text: String) {
    ic?.commitText(text, 0)
  }

  /**
   * 判断数据库是否打开，没有打开，启动登陆界面，如果是快速锁定，打开快速解锁界面
   */
  private fun dbIsOpen(): Boolean {
    if (BaseApp.KDB == null || BaseApp.isLocked) {
      if (BaseApp.KDB == null) {
        LauncherActivity.startLauncherActivity(this, Intent.FLAG_ACTIVITY_NEW_TASK)
        return false
      }

      val isOpenQuickLock = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
          .getBoolean(applicationContext.getString(R.string.set_quick_unlock), false)

      if (isOpenQuickLock) {
        QuickUnlockActivity.startQuickUnlockActivity(this, Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      return false
    }

    return true
  }

  /**
   * 搜索条目
   */
  private fun searchEntry(pkgName: String?): List<PwEntry> {
    if (pkgName.isNullOrEmpty() || BaseApp.KDB == null) {
      return emptyList()
    }
    val listStorage = ArrayList<PwEntry>()
    KdbUtil.searchAutoFillEntries(pkgName, listStorage)
    return listStorage
  }

}