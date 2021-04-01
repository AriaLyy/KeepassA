/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.setting

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.view.autofill.AutofillManager
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.arialyy.frame.core.AbsFrame
import com.arialyy.frame.util.SharePreUtil
import com.arialyy.frame.util.StringUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.common.PassType
import com.lyy.keepassa.util.FingerprintUtil
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.LanguageUtil
import com.lyy.keepassa.util.PermissionsUtil
import com.lyy.keepassa.util.RoomUtil
import com.lyy.keepassa.view.UpgradeLogDialog
import com.lyy.keepassa.view.dialog.MsgDialog
import com.lyy.keepassa.view.fingerprint.FingerprintActivity
import de.psdev.licensesdialog.LicensesDialog
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 应用设置
 */
class AppSettingFragment : PreferenceFragmentCompat() {
  private val TAG = StringUtil.getClassName(this)
  private lateinit var passTypeList: ListPreference
  private var passLen = 3
  private val requestCodeAutoFill = 0xa1
  private lateinit var autoFill: SwitchPreference

  override fun onCreatePreferences(
    savedInstanceState: Bundle?,
    rootKey: String?
  ) {
    setPreferencesFromResource(R.xml.app_setting, rootKey)
    setSubPassType()
    setAtoFill()
    setLanguage()
    setQuickUnLock()
    setFingerPrint()
    setVersionLog()
    setIme()
    license()
    screenLock()
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
          .show();
      true
    }
  }

  /**
   * 处理安全键盘
   */
  private fun setIme() {
    findPreference<Preference>(getString(R.string.set_key_open_kpa_ime))?.setOnPreferenceClickListener {
      startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
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

    if (BaseApp.passType == PassType.ONLY_KEY){
      passLenLayout?.isVisible = false
      passTypeList.isVisible = false
      return
    }
    passLenLayout!!.setOnPreferenceChangeListener { _, newValue ->
      KLog.i(TAG, "短密码长度：$newValue")
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
      KLog.i(TAG, "短密码类型：$subTitle")
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
    GlobalScope.launch {
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
      startActivity(
          Intent(context, FingerprintActivity::class.java),
          ActivityOptions.makeSceneTransitionAnimation(activity)
              .toBundle()
      )
      return@setOnPreferenceClickListener true
    }
  }

  /**
   * 处理自动填充服务
   */
  private fun setAtoFill() {
    autoFill = findPreference(getString(R.string.set_open_auto_fill))!!
    // 大于8.0 才能使用自带的填充框架，否则只能使用辅助功能来实现
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val am = requireContext().getSystemService(AutofillManager::class.java)
      if (am == null) {
        autoFill.isVisible = false
        return
      }
      // miui 检查后台弹出权限
      if (am.isAutofillSupported
          && RoomUtil.isMiui()
          && !PermissionsUtil.miuiBackgroundStartAllowed(context)
      ) {
        showAutoFillMsgDialog(getString(R.string.setting_miui_background_start))
      }

      // vivo 检查后台弹出权限
      if (am.isAutofillSupported
          && RoomUtil.isVivo()
          && !PermissionsUtil.vivoBackgroundStartAllowed(context)
      ) {
        showAutoFillMsgDialog(getString(R.string.setting_vivo_background_start))
      }


      autoFill.isChecked = am.hasEnabledAutofillServices()
      if (!am.isAutofillSupported) {
        autoFill.isVisible = false
      }

      autoFill.setOnPreferenceChangeListener { _, newValue ->
        if (!(newValue as Boolean)) {
          // 如果已启用，需要包名不同才能重新打开自动填充设置
          startActivityForResult(
              Intent(
                  Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE,
                  Uri.parse("package:${requireContext().packageName}1")
              ),
              requestCodeAutoFill
          )
        } else {
          startActivityForResult(
              Intent(
                  Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE,
                  Uri.parse("package:${requireContext().packageName}")
              ),
              requestCodeAutoFill
          )
        }
        true
      }

    } else {
      autoFill.isVisible = false
    }
  }

  /**
   * 处理快速解锁，只有密钥的情况不允许使用快速解锁
   */
  private fun setQuickUnLock() {
    val unLock = findPreference<SwitchPreference>(getString(R.string.set_quick_unlock))!!
    if (BaseApp.passType == PassType.ONLY_KEY){
      unLock.isVisible = false
      return
    }
    unLock.setOnPreferenceChangeListener { _, newValue ->
      KLog.d(TAG, "quick unlock newValue = $newValue")
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
    langPre!!.setOnPreferenceChangeListener { _, newValue ->
      var lang = Locale.ENGLISH
      when (newValue.toString()
          .toInt()) {
        1 -> {
          lang = Locale.ENGLISH
        }
        2 -> {
          lang = Locale.SIMPLIFIED_CHINESE
        }
        3 -> {
          lang = Locale.TRADITIONAL_CHINESE
        }
        4 -> {
          lang = Locale.CANADA_FRENCH
        }
      }
      BaseApp.currentLang = lang
      LanguageUtil.saveLanguage(requireContext(), lang)
      for (ac in AbsFrame.getInstance().activityStack) {
        AbsFrame.getInstance()
            .removeActivity(ac)
        ac.recreate()
      }
      true
    }
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

  /**
   * 显示弹出框提示用户打开后台启动界面的权限
   */
  private fun showAutoFillMsgDialog(msg: String) {
    val IS_HOWED_AUTO_FILL_HINT_DIALOG = "IS_HOWED_AUTO_FILL_HINT_DIALOG"
    val isShowed =
      SharePreUtil.getBoolean(
          Constance.PRE_FILE_NAME,
          requireContext(),
          IS_HOWED_AUTO_FILL_HINT_DIALOG
      )

    if (!isShowed) {
      val msgDialog = MsgDialog.generate {
        msgTitle = BaseApp.APP.getString(R.string.hint)
        msgContent =
          Html.fromHtml(BaseApp.APP.getString(R.string.hint_background_start, msg))
        showCancelBt = false
        showCountDownTimer = Pair(true, 5)
        build()
      }
      msgDialog.show()
      SharePreUtil.putBoolean(
          Constance.PRE_FILE_NAME,
          requireContext(),
          IS_HOWED_AUTO_FILL_HINT_DIALOG,
          true
      )
    } else {
      KLog.i(TAG, "已显示过自动填充对话框，不再重复显示")
    }

  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    if (requestCode == requestCodeAutoFill && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (resultCode == Activity.RESULT_OK) {
        autoFill.isChecked = true
      } else {
        autoFill.isChecked = requireContext().getSystemService(AutofillManager::class.java)
            .hasEnabledAutofillServices()
      }
    }
  }

}