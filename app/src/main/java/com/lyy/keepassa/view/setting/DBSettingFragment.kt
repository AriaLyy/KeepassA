/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.setting

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.arialyy.frame.util.StringUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.event.ModifyDbNameEvent
import com.lyy.keepassa.event.ModifyPassEvent
import com.lyy.keepassa.util.AutoLockDbUtil
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.view.dialog.LoadingDialog
import com.lyy.keepassa.view.dialog.ModifyPassDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN

/**
 * 数据库设置
 */
class DBSettingFragment : PreferenceFragmentCompat() {
  private val TAG = StringUtil.getClassName(this)
  private lateinit var loadingDialog: LoadingDialog
  private lateinit var module: SettingModule

  override fun onCreatePreferences(
    savedInstanceState: Bundle?,
    rootKey: String?
  ) {
    EventBusHelper.reg(this)
    module = ViewModelProvider(this).get(SettingModule::class.java)
    setPreferencesFromResource(R.xml.db_setting, rootKey)
    handleEvent()
  }

  private fun handleEvent() {
    // 修改数据库密码
    val modifyDbPass =
      findPreference<Preference>(getString(R.string.set_key_modify_db_pass))
    modifyDbPass!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
      val dialog = ModifyPassDialog()
      dialog.show(childFragmentManager, "modify_pass")
      return@OnPreferenceClickListener true
    }

    setModifyDbName()
    setOutDb()
    setLockDbTime()
  }

  /**
   * 处理锁屏时间
   */
  private fun setLockDbTime() {
    val lockDb = findPreference<ListPreference>(getString(R.string.set_key_auto_lock_db_time))!!
    lockDb.setOnPreferenceChangeListener { _, _ ->
      AutoLockDbUtil.get()
          .resetTimer()
      return@setOnPreferenceChangeListener true
    }
  }

  /**
   * 处理导出数据库
   */
  private fun setOutDb() {
    val outDb = findPreference<ListPreference>(getString(R.string.set_key_out_db))
    outDb!!.setOnPreferenceChangeListener { preference, newValue ->
      // todo 导出文件
      when (newValue as Int) {
        0 -> {
          // 导出kdbx数据库
        }
        1 -> {
          // 导出xml格式的文件
        }
        2 -> {
          // 导出csv格式的文件
        }
      }
      Log.d(TAG, "outDb value = $newValue")
      false
    }
  }

  /**
   * 处理数据库名的修改
   */
  private fun setModifyDbName() {
    // 修改数据库名
    val modifyDbName =
      findPreference<EditTextPreference>(getString(R.string.set_key_modify_db_name))
    modifyDbName!!.text = BaseApp.dbName
    modifyDbName.setOnBindEditTextListener { et ->
      // 将光标移动到最后
      et.setSelection(BaseApp.dbName.length)
    }
    modifyDbName.setOnPreferenceChangeListener { _, newValue ->
      if ((newValue as String).isNotEmpty()) {
        if (newValue == BaseApp.KDB!!.pm.name) {
          HitUtil.toaskShort(getString(R.string.db_name_no_alter))
          return@setOnPreferenceChangeListener false
        }
        loadingDialog = LoadingDialog(context)
        loadingDialog.show()
        module.modifyDbName(newValue)
            .observe(this, Observer { success ->
              loadingDialog.dismiss()
              if (success) {
                EventBus.getDefault().post(ModifyDbNameEvent(newValue))
                HitUtil.toaskShort("${getString(R.string.db_name_modify)}${getString(R.string.success)}")
              } else {
                HitUtil.toaskShort("${getString(R.string.db_name_modify)} ${getString(R.string.fail)}")
              }
            })
      }
      false
    }
  }

  /**
   * 修改密码
   */
  private fun modifyPass(newValue: String) {
    if (newValue.isNotEmpty()) {
      if (newValue == QuickUnLockUtil.decryption(BaseApp.dbPass)) {
        HitUtil.toaskShort(getString(R.string.db_pass_no_alter))
        return
      }

      loadingDialog = LoadingDialog(context)
      loadingDialog.show()
      module.modifyPass(requireContext(), newValue)
          .observe(this, Observer { success ->

            loadingDialog.dismiss()
            if (success) {
              HitUtil.toaskShort(getString(R.string.hint_db_pass_modify_success))
            } else {
              HitUtil.toaskShort("${getString(R.string.hint_db_pass_modify)}${getString(R.string.fail)}")
            }
          })
    }
  }

  @Subscribe(threadMode = MAIN)
  fun onModifyPassEvent(event: ModifyPassEvent) {
    modifyPass(event.pass)
  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
  }

}