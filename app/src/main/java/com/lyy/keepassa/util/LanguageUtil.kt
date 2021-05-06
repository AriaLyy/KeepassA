/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.text.TextUtils
import com.lyy.keepassa.base.Constance
import java.util.Locale

/**
 * 语言工具
 */
object LanguageUtil {
  private val LOCALE_KEY_LANG = "LOCALE_KEY_LANG"
  private val LOCALE_KEY_COUNTRY = "LOCALE_KEY_COUNTRY"

  @JvmField
  val SUPPORT_LAN =
    arrayListOf(Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE, Locale.CANADA_FRENCH)

  /**
   * 设置app语言
   * @return context, activity 需要使用attachBaseContext()来更新该context
   */
//  @CheckResult(suggest = "activity需要使用attachBaseContext()来更新该context")
  fun setLanguage(
    context: Context,
    locale: Locale
  ): Context {
    val res: Resources = context.resources
    val conf = res.configuration
    conf.setLocale(locale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      conf.setLocales(LocaleList(locale))
    }
    res.updateConfiguration(conf, res.displayMetrics)
    val cx = context.createConfigurationContext(conf)
    return cx
  }

  fun getLanguageConfig(context: Context, locale: Locale):Configuration{
    val res: Resources = context.resources
    val conf = res.configuration
    conf.setLocale(locale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      conf.setLocales(LocaleList(locale))
    }
    return conf
  }

  /**
   * 获取当前系统语言
   * https://mp.weixin.qq.com/s/A27dvFV3glX26Ur0WsdJ9g
   */
  fun getSysCurrentLan(): Locale {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      Locale.getDefault()
    } else {
      LocaleList.getDefault()
          .get(0)
    }
  }

  /**
   * 保存语言到配置文件
   */
  fun saveLanguage(
    context: Context,
    locale: Locale
  ) {
    val pre = context.getSharedPreferences(Constance.PRE_FILE_NAME, Context.MODE_PRIVATE)
    val editor = pre.edit()
    editor.putString(LOCALE_KEY_LANG, locale.language)
    editor.putString(LOCALE_KEY_COUNTRY, locale.country)
    editor.apply()
  }

  /**
   * 读取保存的语言
   * @return 如果没有，返回null
   */
  fun getDefLanguage(context: Context): Locale? {
    val pre = context.getSharedPreferences(Constance.PRE_FILE_NAME, Context.MODE_PRIVATE)
    val lang = pre.getString(LOCALE_KEY_LANG, "")
    val country = pre.getString(LOCALE_KEY_COUNTRY, "")
    return if (TextUtils.isEmpty(lang) && TextUtils.isEmpty(country)) {
      null
    } else {
      Locale(lang, country)
    }
  }

}