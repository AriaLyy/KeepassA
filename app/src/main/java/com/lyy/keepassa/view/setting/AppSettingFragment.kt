/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.setting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.autofill.AutofillManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup.PreferencePositionCallback
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.core.AbsFrame
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.LanguageUtils
import com.blankj.utilcode.util.ReflectUtils
import com.blankj.utilcode.util.RomUtils
import com.blankj.utilcode.util.ToastUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.common.PassType
import com.lyy.keepassa.service.autofill.ChromeAutofillSupport
import com.lyy.keepassa.service.autofill.ChromeThirdPartyAutofillState
import com.lyy.keepassa.util.FingerprintUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.LanguageUtil
import com.lyy.keepassa.util.PermissionsUtil
import com.lyy.keepassa.view.UpgradeLogDialog
import com.lyy.keepassa.view.fingerprint.FingerprintActivity
import de.psdev.licensesdialog.LicensesDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

/**
 * 应用设置
 */
@Route(path = "/setting/appFm")
class AppSettingFragment : PreferenceFragmentCompat() {
  private lateinit var passTypeList: ListPreference
  private var passLen = 3
  private lateinit var autoFill: SwitchPreference

  companion object {
    private val LANGUAGE_MAP = linkedMapOf(
      1 to Locale.ENGLISH,
      2 to Locale.SIMPLIFIED_CHINESE,
      3 to Locale.TRADITIONAL_CHINESE,
      4 to Locale.CANADA_FRENCH,
      5 to Locale("nb", "NO"),
      6 to Locale("ru", "RU"),
      7 to Locale.FRENCH,
      8 to Locale.GERMANY,
      9 to Locale("pl"),
      10 to Locale("tr"),
      11 to Locale("uk", "UA"),
      12 to Locale("es"),
      13 to Locale("ar"),
      14 to Locale("cs"),
      15 to Locale("fon"),
      16 to Locale.JAPANESE,
      17 to Locale("nl"),
      18 to Locale("pt"),
      19 to Locale("pt", "BR")
    )
  }

  @Autowired(name = "scrollKey")
  @JvmField
  var scrollKey: String? = null

  private var isHighlighted = false

  @RequiresApi(VERSION_CODES.O)
  private val autoFillLauncher =
    registerForActivityResult(object : ActivityResultContract<String, Int>() {
      override fun createIntent(context: Context, input: String): Intent {
        return Intent(
          Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE,
          Uri.parse(input)
        )
      }

      override fun parseResult(resultCode: Int, intent: Intent?): Int {
        return resultCode
      }
    }) {
      if (it == Activity.RESULT_OK) {
        autoFill.isChecked = true
      } else {
        autoFill.isChecked = requireContext().getSystemService(AutofillManager::class.java)
          .hasEnabledAutofillServices()
      }
    }

  override fun onCreatePreferences(
    savedInstanceState: Bundle?,
    rootKey: String?
  ) {
    ARouter.getInstance().inject(this)
    setPreferencesFromResource(R.xml.app_setting, rootKey)
    setSubPassType()
    setAtoFill()
    setBrowserAutofillSettings()
    setLanguage()
    setQuickUnLock()
    setFingerPrint()
    setVersionLog()
    setIme()
    license()
    screenLock()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    scrollToKey()
  }

  override fun onResume() {
    super.onResume()
    updateBrowserAutofillSettings()
  }

  /**
   * turn to scrollKey
   */
  private fun scrollToKey() {
    if (!scrollKey.isNullOrEmpty() && !isHighlighted) {
      try {
        val mList = ReflectUtils.reflect(this).field("mList").get<RecyclerView>()
        val adapter = mList.adapter
        val position =
          (adapter as PreferencePositionCallback).getPreferenceAdapterPosition(scrollKey!!)
        Timber.d("postiion = $position, key = $scrollKey")
        isHighlighted = true
        lifecycleScope.launch(Dispatchers.IO) {
          delay(200)
          withContext(Dispatchers.Main) {
            mList.scrollToPosition(position)
            val v = mList.layoutManager?.findViewByPosition(position)
            v?.let {
              itemViewAnim(v)
            }
          }
        }
      } catch (e: Exception) {
        Timber.e(e)
      }
    }
  }

  private suspend fun itemViewAnim(view: View) {
    withContext(Dispatchers.Main) {
      view.setBackgroundColor(ResUtil.getColor(R.color.color_524E85DB))
    }
    withContext(Dispatchers.IO) {
      delay(2000)
    }
    view.setBackgroundColor(ResUtil.getColor(R.color.color_FFFFFF))
  }

  /**
   * when the screen lock ,the db will auto lock
   */
  private fun screenLock() {
    findPreference<SwitchPreference>(getString(R.string.set_key_lock_screen_auto_lock_db))?.setOnPreferenceChangeListener { _, _ ->
      BaseApp.APP.initReceiver()
      return@setOnPreferenceChangeListener true
    }
  }

  /**
   * 开放源码许可证
   */
  private fun license() {
    findPreference<Preference>(getString(R.string.set_key_license))?.setOnPreferenceClickListener {

      LicensesDialog.Builder(requireContext())
        .setNotices(R.raw.notices)
        .setIncludeOwnLicense(true)
        .build()
        .show()
      true
    }
  }

  /**
   * 处理安全键盘
   */
  private fun setIme() {
    findPreference<Preference>(getString(R.string.set_key_open_kpa_ime))?.setOnPreferenceClickListener {
      startActivity(
        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS),
        ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity()).toBundle()
      )
      true
    }
  }

  /**
   * 处理升级日志
   */
  private fun setVersionLog() {
    findPreference<Preference>(getString(R.string.set_key_version_log))?.setOnPreferenceClickListener {
      UpgradeLogDialog().show()
      true
    }
  }

  /**
   * 设置截取的短密码类型，和短密码的截取长度
   */
  private fun setSubPassType() {
    // 密码长度
    val passLenLayout =
      findPreference<ListPreference>(getString(R.string.set_quick_pass_len))
    passTypeList = findPreference(getString(R.string.set_quick_pass_type))!!

    if (BaseApp.passType == PassType.ONLY_KEY) {
      passLenLayout?.isVisible = false
      passTypeList.isVisible = false
      return
    }
    passLenLayout!!.setOnPreferenceChangeListener { _, newValue ->
      Timber.i("短密码长度：$newValue")
      passLen = newValue.toString()
        .toInt()
      setPassTypeEntries()
      true
    }
    passLen = passLenLayout.value.toInt()

    // 密码截取类型
    passTypeList.setOnPreferenceChangeListener { _, newValue ->
      val subTitle = passTypeList.entries[newValue.toString()
        .toInt() - 1]
      Timber.i("短密码类型：$subTitle")
      passTypeList.summary = subTitle.toString()
      subShortPass()
      true
    }
    passTypeList.summary = passTypeList.entries[0]

    // 默认截取一次
    setPassTypeEntries()
  }

  /**
   * 截取短密码，需要延时截取，因为Preference的保存是异步的，有可能会比较慢
   */
  private fun subShortPass() {
    KpaUtil.scope.launch {
      delay(1000)
      KeepassAUtil.instance.subShortPass()
    }
  }

  /**
   * 处理指纹解锁
   */
  private fun setFingerPrint() {
    val fingerprint =
      findPreference<Preference>(getString(R.string.set_key_fingerprint_unlock))
    if (!FingerprintUtil.hasBiometricPrompt(requireContext())) {
      fingerprint?.isVisible = false
      return
    }
    fingerprint!!.setOnPreferenceClickListener {
      FingerprintActivity.toFingerprintActivity(requireActivity())
      return@setOnPreferenceClickListener true
    }
  }

  /**
   * 处理自动填充服务
   */
  private fun setAtoFill() {
    autoFill = findPreference(getString(R.string.set_open_auto_fill))!!
    // 大于8.0 才能使用自带的填充框架，否则只能使用辅助功能来实现
    if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
      val am = requireContext().getSystemService(AutofillManager::class.java)
      if (am == null || !am.isAutofillSupported) {
        autoFill.isVisible = false
        return
      }

      // miui 检查后台弹出权限
      if (am.isAutofillSupported
        && RomUtils.isXiaomi()
        && !PermissionsUtil.miuiCanBackgroundStart()
      ) {
        PermissionsUtil.showAutoFillMsgDialog(
          requireContext(),
          getString(R.string.hint_open_backgroun_start)
        )
      }

      // vivo 检查后台弹出权限
      if (am.isAutofillSupported
        && RomUtils.isVivo()
        && !PermissionsUtil.vivoBackgroundStartAllowed()
      ) {
        PermissionsUtil.showAutoFillMsgDialog(
          requireContext(),
          getString(R.string.hint_open_backgroun_start)
        )
      }


      autoFill.isChecked = am.hasEnabledAutofillServices()
      if (!am.isAutofillSupported) {
        autoFill.isVisible = false
      }

      autoFill.setOnPreferenceChangeListener { _, newValue ->
        if (!(newValue as Boolean)) {
          // 如果已启用，需要包名不同才能重新打开自动填充设置
          autoFillLauncher.launch(
            "package:${requireContext().packageName}1",
            ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity())
          )
          return@setOnPreferenceChangeListener true
        }
        // 开启前必须先有后台弹出界面权限(MIUI/Vivo/华为 等特殊系统),
        // 否则 AutoFillService 收到 onFillRequest 后无法启动解锁界面,等于残废
        if (!PermissionsUtil.isCanBackgroundStart()) {
          PermissionsUtil.showAutoFillMsgDialog(
            requireContext(),
            getString(R.string.hint_open_backgroun_start),
            force = true
          )
          return@setOnPreferenceChangeListener false
        }
        autoFillLauncher.launch(
          "package:${requireContext().packageName}",
          ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity())
        )
        true
      }
    } else {
      autoFill.isVisible = false
    }
  }

  private fun setBrowserAutofillSettings() {
    val preference = findPreference<Preference>(
      getString(R.string.set_key_browser_autofill_settings)
    ) ?: return

    if (Build.VERSION.SDK_INT < VERSION_CODES.O) {
      preference.isVisible = false
      return
    }

    updateBrowserAutofillSettings()
    preference.setOnPreferenceClickListener {
      val opened = ChromeAutofillSupport.openSettings(requireContext())
      if (!opened) {
        ToastUtils.showLong(R.string.browser_autofill_settings_open_failed)
      }
      true
    }
  }

  private fun updateBrowserAutofillSettings() {
    val preference = findPreference<Preference>(
      getString(R.string.set_key_browser_autofill_settings)
    ) ?: return

    if (Build.VERSION.SDK_INT < VERSION_CODES.O) {
      preference.isVisible = false
      return
    }

    when (ChromeAutofillSupport.thirdPartyModeState(requireContext())) {
      ChromeThirdPartyAutofillState.NOT_INSTALLED -> {
        preference.isVisible = false
      }

      ChromeThirdPartyAutofillState.DISABLED -> {
        preference.isVisible = true
        preference.summary = getString(R.string.browser_autofill_settings_summary_disabled)
      }

      ChromeThirdPartyAutofillState.ENABLED -> {
        preference.isVisible = true
        preference.summary = getString(R.string.browser_autofill_settings_summary_enabled)
      }

      ChromeThirdPartyAutofillState.UNKNOWN -> {
        preference.isVisible = true
        preference.summary = getString(R.string.browser_autofill_settings_summary_unknown)
      }
    }
  }

  /**
   * 处理快速解锁，只有密钥的情况不允许使用快速解锁
   */
  private fun setQuickUnLock() {
    val unLock = findPreference<SwitchPreference>(getString(R.string.set_quick_unlock))!!
    if (BaseApp.passType == PassType.ONLY_KEY) {
      unLock.isVisible = false
      return
    }
    unLock.setOnPreferenceChangeListener { _, newValue ->
      Timber.d("quick unlock newValue = $newValue")
      if (newValue as Boolean) {
        subShortPass()
      }
      return@setOnPreferenceChangeListener true
    }
  }

  /**
   * 设置语言
   */
  private fun setLanguage() {
    val langPre = findPreference<ListPreference>(getString(R.string.set_key_language))
    val spm = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
    if (spm.getString(getString(R.string.set_key_language), null) == null) {
      val sysLan = LanguageUtils.getSystemLanguage()
      findLanguageValue(sysLan)?.let { langPre?.value = it }
    }
    langPre?.setOnPreferenceChangeListener { _, newValue ->
      val lang = LANGUAGE_MAP[newValue.toString().toIntOrNull()] ?: Locale.ENGLISH
      BaseApp.currentLang = lang
      LanguageUtil.saveLanguage(requireContext(), lang)
      LanguageUtil.setLanguage(requireContext(), lang)
      for (ac in AbsFrame.getInstance().activityStack.toList()) {
        AbsFrame.getInstance()
          .removeActivity(ac)
        ac.recreate()
      }
      true
    }
  }

  private fun findLanguageValue(locale: Locale): String? {
    val supportedLocale = LanguageUtil.getSupportedLanguage(locale) ?: return null
    return LANGUAGE_MAP.entries.firstOrNull { it.value == supportedLocale }
      ?.key
      ?.toString()
  }

  /**
   * 设置选择项类型条目
   */
  private fun setPassTypeEntries() {
    val entries = requireContext().resources.getStringArray(R.array.quick_pass_type_entries)
    val newEntries = arrayOfNulls<CharSequence>(entries.size)

    entries.forEachIndexed { index, value ->
      newEntries[index] = value.toString()
        .format(passLen.toString())
    }

    passTypeList.entries = newEntries
    passTypeList.summary = newEntries[passTypeList.value.toInt() - 1]
    // 截取短密码
    subShortPass()
  }
}
