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
import android.view.animation.TranslateAnimation
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.lyy.keepassa.R
import com.lyy.keepassa.event.CheckEnvEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

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