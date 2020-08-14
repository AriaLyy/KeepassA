package com.lyy.keepassa.view.setting

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivitySettingBinding

class SettingActivity : BaseActivity<ActivitySettingBinding>() {

  companion object {
    // 设置类型
    const val KEY_TYPE = "KEY_TYPE"

    // 数据库设置
    const val TYPE_DB = 0

    // 应用设置
    const val TYPE_APP = 1
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