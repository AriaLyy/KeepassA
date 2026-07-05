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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
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
import com.lyy.keepassa.service.autofill.ImeBrowserDomainContext
import com.lyy.keepassa.service.autofill.W3cHints
import com.lyy.keepassa.service.input.keyboard.ImeKeyAction
import com.lyy.keepassa.service.input.keyboard.ImeKeyboardPage
import com.lyy.keepassa.service.input.keyboard.ImeKeyboardPreferences
import com.lyy.keepassa.service.input.keyboard.ImeKeyboardState
import com.lyy.keepassa.service.input.keyboard.ImeKeyboardViewBinder
import com.lyy.keepassa.service.input.search.ImeEntrySearchEngine
import com.lyy.keepassa.service.input.search.ImeSearchSession
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.LanguageUtil
import com.lyy.keepassa.util.getRealUserName
import com.lyy.keepassa.util.isCanOpenQuickLock
import com.lyy.keepassa.util.totp.OtpUtil
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity
import com.lyy.keepassa.view.search.CommonSearchActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
  private val selectionTracker = CandidateSelectionTracker<PwEntry>()
  private val curEntry: PwEntry? get() = selectionTracker.current
  private lateinit var candidatesList: RecyclerView
  private val candidatesData = arrayListOf<SimpleItemEntity>()
  private lateinit var candidatesAdapter: CandidatesAdapter
  private var imeOption = EditorInfo.IME_ACTION_GO
  private var curImeView: View? = null
  private var scope = MainScope()
  private val keyboardState = ImeKeyboardState()
  private val searchSession = ImeSearchSession<PwEntry>()
  private val manualSelectionPolicy = ImeManualSelectionPolicy<PwEntry>()
  private lateinit var keyboardPreferences: ImeKeyboardPreferences
  private var keyboardBinder: ImeKeyboardViewBinder? = null
  private var imeSearchJob: Job? = null

  /**
   * 当 IME 首次显示时，系统会调用 onCreateInputView() 回调。在此方法的实现中，您可以创建要在 IME 窗口中显示的布局，并将布局返回系统。
   */
  override fun onCreateInputView(): View {

    val layout = LayoutInflater.from(this)
      .inflate(R.layout.layout_kpa_ime, null) as ViewGroup
    candidatesList = layout.findViewById(R.id.rvContent)
    for (i in 0 until layout.childCount) {
      val child = layout.getChildAt(i)
      if (child != null
        && (child is ImageView || child is TextView)
        && child.isClickable
      ) {
        child.setOnClickListener(this)
      }
    }
    curImeView = layout
    keyboardPreferences = ImeKeyboardPreferences(this)
    initImeSearchBar(layout)
    initKeyboard(layout)
    initCandidatesLayout()
    updateImeActionButtons()

    layout.findViewById<AppCompatImageView>(R.id.ivSearch).setOnClickListener {
      Routerfit.create(ActivityRouter::class.java).toCommonSearch()
    }
    scope = MainScope()
    scope.launch {
      CommonSearchActivity.searchFlow.collectLatest {
        showEntryList(arrayListOf<PwEntry>().apply { add(it) })
      }
    }

    return layout
  }

  private fun initImeSearchBar(layout: View) {
    val searchBar = layout.findViewById<View>(R.id.imeSearchBar)
    val searchInput = layout.findViewById<AppCompatEditText>(R.id.tvImeSearchQuery)
    val clear = layout.findViewById<View>(R.id.btImeSearchClear)
    searchInput.showSoftInputOnFocus = false
    searchInput.isCursorVisible = false
    searchInput.setOnClickListener {
      keyboardPreferences.performKeyboardHaptic(searchInput)
      enterImeSearchMode()
    }
    searchBar.setOnClickListener {
      keyboardPreferences.performKeyboardHaptic(searchBar)
      enterImeSearchMode()
    }
    clear.setOnClickListener {
      keyboardPreferences.performKeyboardHaptic(clear)
      if (keyboardState.searchQuery.isEmpty()) {
        exitImeSearchMode(clearResults = true)
      } else {
        keyboardState.clearSearchQuery()
        searchSession.clear()
        updateImeSearchUi()
        showImeSearchEmptyOrResults()
      }
    }
  }

  private fun initKeyboard(layout: View) {
    val container = layout.findViewById<LinearLayout>(R.id.llImeKeyboard)
    keyboardBinder = ImeKeyboardViewBinder(
      context = this,
      container = container,
      onKey = { view, action -> handleImeKeyAction(view, action) },
      onShiftLongPress = { view ->
        keyboardPreferences.performKeyboardHaptic(view)
        keyboardState.longPressShift()
        renderImeKeyboard()
      }
    )
    renderImeKeyboard()
  }

  private fun renderImeKeyboard() {
    keyboardBinder?.render(keyboardState.page, keyboardState.shiftState)
  }

  private fun initCandidatesLayout() {
    candidatesAdapter = CandidatesAdapter(this, candidatesData)
    candidatesList.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
    candidatesList.setHasFixedSize(true)
    candidatesList.adapter = candidatesAdapter
    RvItemClickSupport.addTo(candidatesList)
      .setOnItemClickListener(object : RvItemClickSupport.OnItemClickListener {
        override fun onItemClicked(
          recyclerView: RecyclerView?,
          position: Int,
          v: View?
        ) {
          Timber.d("select item, position = $position")
          if (!selectionTracker.click(position)) return
          candidatesData.forEachIndexed { i, item ->
            item.isSelected = selectionTracker.isSelected(i)
          }
          candidatesAdapter.notifyDataSetChanged()
          updateImeActionButtons()
          if (keyboardState.isSearchMode) {
            searchSession.results.getOrNull(position)?.let { entry ->
              searchSession.select(entry)
              manualSelectionPolicy.rememberManualSelection(appPkgName, entry)
              exitImeSearchMode(clearResults = false)
            }
          }
        }
      })
  }

  private fun handleImeKeyAction(view: View, action: ImeKeyAction) {
    keyboardPreferences.performKeyboardHaptic(view)
    when (action) {
      is ImeKeyAction.CommitText -> {
        val shiftBefore = keyboardState.shiftState
        val text = if (keyboardState.page == ImeKeyboardPage.ALPHABET) {
          keyboardState.applyShiftTo(action.text)
        } else {
          action.text
        }
        if (keyboardState.isSearchMode) {
          handleSearchTextInput(text)
        } else {
          fillData(text)
        }
        if (shiftBefore != keyboardState.shiftState) {
          renderImeKeyboard()
        }
      }
      ImeKeyAction.Space -> {
        if (keyboardState.isSearchMode) {
          handleSearchTextInput(" ")
        } else {
          fillData(" ")
        }
      }
      ImeKeyAction.Backspace -> handleBackspaceInput()
      ImeKeyAction.Enter -> {
        if (keyboardState.isSearchMode) {
          runImeSearchNow()
        } else {
          ic?.performEditorAction(imeOption)
        }
      }
      ImeKeyAction.Shift -> {
        keyboardState.tapShift(System.currentTimeMillis())
        renderImeKeyboard()
      }
      ImeKeyAction.SwitchToSymbols -> {
        keyboardState.switchToSymbols()
        renderImeKeyboard()
      }
      ImeKeyAction.SwitchToAlphabet -> {
        keyboardState.switchToAlphabet()
        renderImeKeyboard()
      }
      ImeKeyAction.EnterSearchMode -> enterImeSearchMode()
      ImeKeyAction.ClearSearch -> {
        keyboardState.clearSearchQuery()
        searchSession.clear()
        updateImeSearchUi()
        showImeSearchEmptyOrResults()
      }
      ImeKeyAction.ExitSearchMode -> exitImeSearchMode(clearResults = true)
    }
  }

  private fun handleSearchTextInput(text: String) {
    if (!keyboardState.appendSearchText(text)) return
    searchSession.setQuery(keyboardState.searchQuery)
    updateImeSearchUi()
    scheduleImeSearch()
  }

  private fun handleBackspaceInput() {
    if (keyboardState.isSearchMode) {
      keyboardState.backspaceSearchQuery()
      searchSession.setQuery(keyboardState.searchQuery)
      updateImeSearchUi()
      scheduleImeSearch()
    } else {
      ic?.deleteSurroundingText(1, 0)
    }
  }

  private fun enterImeSearchMode() {
    if (!isDatabaseUnlocked()) {
      showImeDatabaseLockedHint()
      return
    }
    manualSelectionPolicy.onNewSearch()
    keyboardState.enterSearchMode()
    searchSession.clear()
    updateImeSearchUi()
    focusImeSearchInput()
    showImeSearchEmptyOrResults()
  }

  private fun exitImeSearchMode(clearResults: Boolean) {
    keyboardState.exitSearchMode()
    imeSearchJob?.cancel()
    if (clearResults) {
      searchSession.clear()
      candidatesData.clear()
      candidatesAdapter.notifyDataSetChanged()
      candidatesList.visibility = View.GONE
    }
    updateImeSearchUi()
    updateImeActionButtons()
  }

  private fun updateImeSearchUi() {
    val root = curImeView ?: return
    val query = keyboardState.searchQuery
    val input = root.findViewById<AppCompatEditText>(R.id.tvImeSearchQuery)
    if (input.text.toString() != query) {
      input.setText(query)
    }
    input.isCursorVisible = keyboardState.isSearchMode
    if (keyboardState.isSearchMode) {
      input.setSelection(input.text?.length ?: 0)
    } else {
      input.clearFocus()
    }
    root.findViewById<View>(R.id.btImeSearchClear).visibility =
      if (keyboardState.isSearchMode) View.VISIBLE else View.GONE
  }

  private fun focusImeSearchInput() {
    val input = curImeView?.findViewById<AppCompatEditText>(R.id.tvImeSearchQuery) ?: return
    input.showSoftInputOnFocus = false
    input.isCursorVisible = true
    input.requestFocus()
    input.setSelection(input.text?.length ?: 0)
  }

  private fun scheduleImeSearch() {
    imeSearchJob?.cancel()
    imeSearchJob = scope.launch {
      delay(ImeSearchSession.DEBOUNCE_MS)
      runImeSearchNow()
    }
  }

  private fun runImeSearchNow() {
    if (!keyboardState.isSearchMode) return
    val results = ImeEntrySearchEngine.search(searchSession.query)
    searchSession.updateResults(results)
    showImeSearchEmptyOrResults()
  }

  private fun showImeSearchEmptyOrResults() {
    candidatesData.clear()
    if (searchSession.isEmptyStateVisible) {
      candidatesList.visibility = View.VISIBLE
      candidatesData.add(SimpleItemEntity().apply {
        type = CandidatesAdapter.ITEM_TYPE_EMPTY
        title = getString(R.string.ime_search_no_entry)
      })
      candidatesAdapter.notifyDataSetChanged()
      updateImeActionButtons()
      return
    }

    val results = searchSession.results
    selectionTracker.show(results)
    if (results.isEmpty()) {
      candidatesList.visibility = View.GONE
      candidatesAdapter.notifyDataSetChanged()
      updateImeActionButtons()
      return
    }

    candidatesList.visibility = View.VISIBLE
    val flags = selectionTracker.selectedFlags()
    results.forEachIndexed { index, entry ->
      candidatesData.add(entry.toImeCandidateItem(flags[index]))
    }
    candidatesAdapter.notifyDataSetChanged()
    updateImeActionButtons()
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
    candidatesList.visibility = View.GONE
    ic = currentInputConnection
    Timber.d("pkgName = ${info?.packageName}, inputType = ${info?.inputType}, fieldName = ${info?.fieldName}, fieldId = ${info?.fieldId}")
    appPkgName = info?.packageName
    manualSelectionPolicy.onStartInput(appPkgName)
    if (keyboardState.isSearchMode) {
      exitImeSearchMode(clearResults = manualSelectionPolicy.currentSelection == null)
    }

    if (!isDatabaseUnlocked()) {
      showImeDatabaseLockedHint()
      return
    }

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

    if (manualSelectionPolicy.shouldUseAutomaticCandidates(appPkgName)) {
      showEntryList(searchEntry(appPkgName))
    } else {
      manualSelectionPolicy.currentSelection?.let {
        showEntryList(listOf(it), forceVisible = true)
      }
    }
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
      // 用户名
      R.id.btAccount -> {
        if (!dbIsOpen()) {
          return
        }
        curEntry?.let {
          val userName = KdbUtil.getUserName(it)
          Timber.d("fill user name: $userName")
          fillData(userName)
          finishSearchModeAfterFill()
          return
        }
        showSelectEntryFirstIfSearching()
      }

      // 密码
      R.id.btPass -> {
        if (!dbIsOpen()) {
          return
        }
        curEntry?.let {
          val pass = KdbUtil.getPassword(it)
          Timber.d("fill password: $pass")
          fillData(pass)
          finishSearchModeAfterFill()
          return
        }
        showSelectEntryFirstIfSearching()
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
        if (curEntry == null) {
          return
        }
        val totp = OtpUtil.getOtpPass(curEntry as PwEntryV4)
        if (totp.second.isNullOrEmpty()) {
          HitUtil.toaskShort(getString(R.string.no_totp_token))
          return
        } else {
          fillData(totp.second!!)
          finishSearchModeAfterFill()
        }
      }

      // 其它信息
      R.id.btOtherInfo -> {
        if (!dbIsOpen()) {
          return
        }

        showMoreInfoDialog()
        finishSearchModeAfterFill()
      }
    }
  }

  private fun finishSearchModeAfterFill() {
    if (keyboardState.isSearchMode) {
      exitImeSearchMode(clearResults = false)
    }
  }

  private fun showSelectEntryFirstIfSearching() {
    if (keyboardState.isSearchMode) {
      HitUtil.toaskShort(getString(R.string.ime_search_select_entry_first))
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
      finishSearchModeAfterFill()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
    scope.cancel()
    imeSearchJob?.cancel()
    manualSelectionPolicy.clear()
    searchSession.clear()
    keyboardBinder = null
  }

  /**
   * 显示候选列表。
   *
   * 仅在 [onStartInputView] 收到新输入字段或 [CommonSearchActivity] 返回结果时调用,
   * 不要在账号/密码/TOTP 等按钮点击中重复调用——那会重置 [selectionTracker] 覆盖用户已选条目。
   *
   * 用 [CandidateSelectionTracker.resync] 而非 [show]:IME 隐藏→重显(如同 app 内切字段)
   * 会再次进入 [onStartInputView],若新候选与上次同引用,resync 保留用户已选条目;
   * 列表形状变化时 resync 自动 fallback 到 show。详见 issue #86。
   */
  private fun showEntryList(
    entries: List<PwEntry>,
    forceVisible: Boolean = false
  ) {
    selectionTracker.resync(entries)
    candidatesData.clear()
    if (selectionTracker.isEmpty) {
      candidatesList.visibility = View.GONE
      updateImeActionButtons()
      return
    }
    if (selectionTracker.size == 1 && !forceVisible) {
      candidatesList.visibility = View.GONE
      updateImeActionButtons()
      return
    }
    candidatesList.visibility = View.VISIBLE
    val flags = selectionTracker.selectedFlags()
    entries.forEachIndexed { i, pwEntry ->
      candidatesData.add(pwEntry.toImeCandidateItem(flags[i]))
    }
    candidatesAdapter.notifyDataSetChanged()
    updateImeActionButtons()
  }

  private fun showImeDatabaseLockedHint() {
    imeSearchJob?.cancel()
    keyboardState.exitSearchMode()
    manualSelectionPolicy.clear()
    selectionTracker.show(emptyList())
    candidatesData.clear()
    candidatesList.visibility = View.VISIBLE
    candidatesData.add(SimpleItemEntity().apply {
      type = CandidatesAdapter.ITEM_TYPE_EMPTY
      title = getString(R.string.ime_database_locked_hint)
    })
    candidatesAdapter.notifyDataSetChanged()
    updateImeSearchUi()
    updateImeActionButtons()
  }

  private fun updateImeActionButtons() {
    val root = curImeView ?: return
    val enabled = isDatabaseUnlocked() && curEntry != null
    listOf(
      R.id.btAccount,
      R.id.btPass,
      R.id.btTotp,
      R.id.btOtherInfo
    ).forEach { buttonId ->
      val button = root.findViewById<View>(buttonId) ?: return@forEach
      button.isEnabled = enabled
      button.isClickable = enabled
      button.alpha = if (enabled) 1f else 0.35f
    }
  }

  private fun PwEntry.toImeCandidateItem(selected: Boolean): SimpleItemEntity {
    return SimpleItemEntity().also { item ->
      item.title = title.orEmpty()
      item.subTitle = getRealUserName().ifBlank { url.orEmpty() }
      item.obj = this
      item.isSelected = selected
    }
  }

  /**
   * 填充数据
   */
  private fun fillData(text: String) {
    ic?.commitText(text, 1)
  }

  private fun isDatabaseUnlocked(): Boolean = BaseApp.KDB != null && !BaseApp.isLocked

  /**
   * 判断数据库是否打开，没有打开，启动登陆界面，如果是快速锁定，打开快速解锁界面
   */
  private fun dbIsOpen(): Boolean {
    if (!isDatabaseUnlocked()) {
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
      val domain = ImeBrowserDomainContext.resolve(pkgName)
      Timber.d("ime browser domain context available = ${domain != null}")
      KdbUtil.searchEntriesByDomain(domain, listStorage)
      return listStorage
    }

    KdbUtil.searchEntriesByPackageName(pkgName, listStorage)
    return listStorage
  }

  override fun attachBaseContext(newBase: Context?) {
    super.attachBaseContext(LanguageUtil.setLanguage(newBase!!, BaseApp.currentLang))
  }
}
