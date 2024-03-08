/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.input

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.autofill.AutofillManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.event.FillInfoEvent
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.router.ServiceRouter
import com.lyy.keepassa.service.autofill.W3cHints
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.LanguageUtil
import com.lyy.keepassa.util.NotificationUtil
import com.lyy.keepassa.util.totp.OtpUtil
import com.lyy.keepassa.util.isCanOpenQuickLock
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity
import com.lyy.keepassa.view.search.CommonSearchActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import timber.log.Timber

/**
 * 输入法
 * https://developer.android.com/guide/topics/text/creating-input-method?hl=zh-cn
 */
class InputIMEService : InputMethodService(), View.OnClickListener {

  private var appPkgName: String? = ""
  private var ic: InputConnection? = null
  private var curEntry: PwEntry? = null
  private lateinit var candidatesList: RecyclerView
  private val candidatesData = arrayListOf<SimpleItemEntity>()
  private lateinit var candidatesAdapter: CandidatesAdapter
  private var imeOption = EditorInfo.IME_ACTION_GO
  private var curImeView: View? = null
  private var scope = MainScope()

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
    curImeView = layout
    initCandidatesLayout()

    layout.findViewById<AppCompatImageView>(R.id.ivSearch).setOnClickListener {
      Routerfit.create(ActivityRouter::class.java).toCommonSearch()
    }
    scope = MainScope()
    scope.launch {
      CommonSearchActivity.searchFlow.collectLatest {
        curEntry = it
        showEntryList(arrayListOf<PwEntry>().apply { add(it) })
      }
    }

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
          Timber.d("select item, position = $position")
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
    EventBusHelper.reg(this)
    imeOption = info?.imeOptions ?: EditorInfo.IME_ACTION_GO
    candidatesData.clear()
    candidatesAdapter.notifyDataSetChanged()
    candidatesList.visibility = View.GONE
    curEntry = null
    ic = currentInputConnection
    Timber.d("pkgName = ${info?.packageName}, inputType = ${info?.inputType}, fieldName = ${info?.fieldName}, fieldId = ${info?.fieldId}")
    appPkgName = info?.packageName

    if (W3cHints.isBrowser(appPkgName) && !checkCanOpenAutoFill()) {
      if (curImeView == null) {
        HitUtil.toaskLong(ResUtil.getString(R.string.ime_hint_open_auto_fill))
        return
      }

      HitUtil.snackLong(
        curImeView!!,
        ResUtil.getString(R.string.ime_hint_open_auto_fill),
        ResUtil.getString(R.string.setting)
      ) {
        Routerfit.create(ActivityRouter::class.java, this).toAppSetting(
          scrollKey = getString(R.string.set_open_auto_fill)
        )
      }
      return
    }

    showEntryList(searchEntry(appPkgName))
  }

  private fun checkCanOpenAutoFill(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      Timber.w("the sdk version ${Build.VERSION.SDK_INT} less than O")
      return false
    }
    val am = getSystemService(AutofillManager::class.java)
    if (!am.isAutofillSupported) {
      Timber.w("it not support autofill")
      return false
    }

    if (!am.hasEnabledAutofillServices()) {
      Timber.w("The auto-fill service is not turned on")
      return false
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
      && (am.autofillServiceComponentName?.packageName?.equals(packageName) == false)
    ) {
      Timber.w("The auto-fill service is not turned on")
      return false
    }
    return true
  }

  override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
    Timber.d("onCreateInlineSuggestionsRequest")
    return super.onCreateInlineSuggestionsRequest(uiExtras)
  }

  override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
    Timber.d("onInlineSuggestionsResponse")
    return super.onInlineSuggestionsResponse(response)
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
        if (appPkgName == packageName) {
          LauncherActivity.startLauncherActivity(this, Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        BaseApp.isLocked = true
        NotificationUtil.startDbLocked(this)
        if (BaseApp.APP.isCanOpenQuickLock()) {
          return
        }
        curEntry = null
        candidatesData.clear()
        Routerfit.create(ServiceRouter::class.java).getDbSaveService().clearDb()
        Timber.d("数据库已锁定")
        HitUtil.toaskShort(getString(R.string.notify_db_locked))
        return
      }

      // 用户名
      R.id.btAccount -> {
        if (!dbIsOpen()) {
          return
        }
        showEntryList(searchEntry(appPkgName))
        curEntry?.let {
          val userName = KdbUtil.getUserName(it)
          Timber.d("fill user name: $userName")
          fillData(userName)
          return
        }
      }

      // 密码
      R.id.btPass -> {
        if (!dbIsOpen()) {
          return
        }
        showEntryList(searchEntry(appPkgName))
        curEntry?.let {
          val pass = KdbUtil.getPassword(it)
          Timber.d("fill password: $pass")
          fillData(pass)
          return
        }
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
        val totp = OtpUtil.getOtpPass(curEntry as PwEntryV4)
        if (totp.second.isNullOrEmpty()) {
          HitUtil.toaskShort(getString(R.string.no_totp_token))
          return
        } else {
          fillData(totp.second!!)
        }
      }

      // 其它信息
      R.id.btOtherInfo -> {
        if (!dbIsOpen()) {
          return
        }
        showEntryList(searchEntry(appPkgName))

        showMoreInfoDialog()
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
   * 显示更多信息的对话框，点击item自动填充
   */
  private fun showMoreInfoDialog() {
    startActivity(Intent(this, EntryOtherInfoDialog::class.java).apply {
      putExtra(EntryOtherInfoDialog.KEY_DATA, curEntry?.uuid)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
  }

  @Subscribe(threadMode = MAIN)
  fun onFillOtherInfo(event: FillInfoEvent) {
    Timber.d("getOtherInfo, info = ${event.infoStr}")
    MainScope().launch {
      withContext(Dispatchers.IO) {
        delay(600)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        requestShowSelf(InputMethodManager.SHOW_IMPLICIT)
      } else {
        try {
          val inm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
          val field = InputMethodService::class.java.getDeclaredField("mToken")
          field.isAccessible = true
          inm.showSoftInputFromInputMethod(
            field.get(this@InputIMEService) as IBinder,
            InputMethodManager.SHOW_IMPLICIT
          )
        } catch (e: Exception) {
          Timber.e(e)
        }
      }

      withContext(Dispatchers.IO) {
        delay(600)
      }

      fillData(event.infoStr.toString())
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
    scope.cancel()
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
    candidatesData.clear()
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


      if (BaseApp.APP.isCanOpenQuickLock()) {
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
    if (W3cHints.isBrowser(pkgName)) {
      Timber.d("curDomain = ${W3cHints.curDomainUrl}")
      KdbUtil.searchEntriesByDomain(W3cHints.curDomainUrl, listStorage)
      return listStorage
    }

    KdbUtil.searchEntriesByPackageName(pkgName, listStorage)
    return listStorage
  }

  override fun attachBaseContext(newBase: Context?) {
    super.attachBaseContext(LanguageUtil.setLanguage(newBase!!, BaseApp.currentLang))
  }
}