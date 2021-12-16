/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.setting

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivitySettingBinding
import com.lyy.keepassa.router.FragmentRouter

@Route(path = "/setting/app")
class SettingActivity : BaseActivity<ActivitySettingBinding>() {

  companion object {
    // 设置类型
    const val KEY_TYPE = "KEY_TYPE"

    // 数据库设置
    const val TYPE_DB = 0

    // 应用设置
    const val TYPE_APP = 1
  }

  @Autowired(name = KEY_TYPE)
  @JvmField
  var type = TYPE_APP

  @Autowired(name = "scrollKey")
  @JvmField
  var scrollKey: String? = null

  override fun setLayoutId(): Int {
    return R.layout.activity_setting
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    ARouter.getInstance().inject(this)
    val title: String
    val fragment: PreferenceFragmentCompat
    if (type == TYPE_DB) {
      title = getString(R.string.db_setting)
      fragment = Routerfit.create(FragmentRouter::class.java).getDbSettingFragment()
    } else {
      title = getString(R.string.app_setting)
      fragment = Routerfit.create(FragmentRouter::class.java).getAppSettingFragment(scrollKey = scrollKey)
    }
    toolbar.title = title
    supportFragmentManager.beginTransaction()
      .replace(R.id.content, fragment)
      .commitNow()
  }
}