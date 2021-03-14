/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.setting

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceFragmentCompat
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivitySettingBinding

class SettingActivity : BaseActivity<ActivitySettingBinding>() {

  companion object {
    // 设置类型
    private const val KEY_TYPE = "KEY_TYPE"

    // 数据库设置
    private const val TYPE_DB = 0

    // 应用设置
    private const val TYPE_APP = 1

    /**
     * 跳转应用设置
     */
    fun turnAppSetting(context: FragmentActivity) {
      context.startActivity(
          Intent(context, SettingActivity::class.java).apply {
            putExtra(KEY_TYPE, TYPE_APP)
          }
          ,
          ActivityOptions.makeSceneTransitionAnimation(context)
              .toBundle()
      )
//      context.overridePendingTransition(R.anim.translate_right_in, R.anim.translate_left_out)
    }

    /**
     * 跳转数据库设置
     */
    fun turnDbSetting(context: FragmentActivity) {
      context.startActivity(
          Intent(context, SettingActivity::class.java).apply {
            putExtra(KEY_TYPE, TYPE_DB)
          }
          , ActivityOptions.makeSceneTransitionAnimation(context)
              .toBundle()
      )
//      context.overridePendingTransition(R.anim.translate_right_in, R.anim.translate_left_out)
    }
  }

  private var type = TYPE_APP

  override fun setLayoutId(): Int {
    return R.layout.activity_setting
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)

    type = intent.getIntExtra(KEY_TYPE, TYPE_APP)
    val title: String
    val fragment: PreferenceFragmentCompat
    if (type == TYPE_DB) {
      title = getString(R.string.db_setting)
      fragment = DBSettingFragment()
    } else {
      title = getString(R.string.app_setting)
      fragment = AppSettingFragment()
    }
    toolbar.title = title
    supportFragmentManager.beginTransaction()
        .replace(R.id.content, fragment)
        .commitNow()

  }

}