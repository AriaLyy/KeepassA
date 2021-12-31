/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.setting

import android.os.Bundle
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.arialyy.frame.core.AbsFrame
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.R.string
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.event.CheckEnvEvent
import com.lyy.keepassa.event.ShowTOTPEvent
import com.lyy.keepassa.util.BarUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description ui setting
 * @Date 2020/11/25
 **/
class UISettingFragment : PreferenceFragmentCompat() {
  override fun onCreatePreferences(
    savedInstanceState: Bundle?,
    rootKey: String?
  ) {
    setPreferencesFromResource(R.xml.ui_setting, rootKey)
    handleEnvCheck()
    handleShowStatusBar()
    handleShowMainTotpTab()
    handleThemStyle()
  }

  override fun onCreateAnimation(
    transit: Int,
    enter: Boolean,
    nextAnim: Int
  ): Animation? {
    // clear anim
    return null
  }

  /**
   * handle theme style
   */
  private fun handleThemStyle() {
    val lp = findPreference<ListPreference>(ResUtil.getString(R.string.set_key_theme_style))
    val summaryArray =
      requireContext().resources.getStringArray(R.array.sek_ley_theme_style_entries)
    val mode = PreferenceManager.getDefaultSharedPreferences(requireContext())
      .getString(getString(string.set_key_theme_style), "0")!!.toInt()

    lp?.summary = summaryArray[mode]

    lp?.setOnPreferenceChangeListener { _, newValue ->
      val index = newValue.toString().toInt()
      lp.summary = summaryArray[index]
      when (index) {
        0 -> {
          AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        1 -> {
          AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        2 -> {
          AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
      }

      return@setOnPreferenceChangeListener true
    }
  }

  private fun handleShowMainTotpTab() {
    val sw = findPreference<SwitchPreference>(getString(R.string.set_key_main_show_totp_tab))
    sw?.setOnPreferenceChangeListener { _, newValue ->
      EventBus.getDefault().post(ShowTOTPEvent(newValue as Boolean))
      return@setOnPreferenceChangeListener true
    }
  }

  /**
   * handle status bar show or hide
   */
  private fun handleShowStatusBar() {
    val sw = findPreference<SwitchPreference>(getString(R.string.set_key_show_state_bar))
    sw?.isChecked = BarUtil.statusBarIsVisible(requireActivity().window)

    sw?.setOnPreferenceChangeListener { _, newValue ->
      BaseActivity.showStatusBar = newValue as Boolean
      for (ac in AbsFrame.getInstance().activityStack) {
        BarUtil.showStatusBar(ac, BaseActivity.showStatusBar)
      }
      return@setOnPreferenceChangeListener true
    }
  }

  /**
   * handle operating env check
   */
  private fun handleEnvCheck() {
    findPreference<SwitchPreference>(getString(R.string.set_key_need_root_check))?.setOnPreferenceChangeListener { _, _ ->
      GlobalScope.launch(Dispatchers.IO) {
        delay(200)
        EventBus.getDefault()
          .post(CheckEnvEvent())
      }

      return@setOnPreferenceChangeListener true
    }
  }
}