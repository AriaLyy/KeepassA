/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.text.InputType
import android.widget.TextView
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.arialyy.frame.util.StringUtil
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.EntryRecord
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.router.ServiceRouter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber
import java.util.Date
import java.util.Locale

/**
 * @Author laoyuyu
 * @Description
 * @Date 2022/3/22
 **/
object KpaUtil {

  var scope = MainScope()
  private var isEmptyPass = false
  val kdbHandlerService by lazy {
    Routerfit.create(ServiceRouter::class.java).getDbSaveService()
  }

  val kdbOpenService by lazy {
    Routerfit.create(ServiceRouter::class.java).getDbOpenService()
  }

  val openEntryRecordFlow = MutableSharedFlow<EntryRecord>()

  fun isEmptyPass(): Boolean {
    return isEmptyPass
  }

  fun setEmptyPass(isEmptyPass: Boolean) {
    this.isEmptyPass = isEmptyPass
  }

  fun isChina(): Boolean {
    return LanguageUtil.getSysCurrentLan().country == Locale.CHINA.country
  }

  fun updateEntryItemInfo(item: SimpleItemEntity) {
    val entry = (item.obj as PwEntryV4)
    item.title = entry.title
    item.subTitle = if (entry.isRef()) {
      val refStr = "${BaseApp.APP.resources.getString(R.string.ref_entry)}: "
      val tempStr = "${refStr}${KdbUtil.getUserName(entry)}"
      StringUtil.highLightStr(tempStr, refStr, ResUtil.getColor(R.color.colorPrimary), true)
    } else {
      entry.username
    }
  }

  fun updateGroupItemInfo(item: SimpleItemEntity) {
    val pwGroup = (item.obj as PwGroupV4)
    item.title = pwGroup.name
    item.subTitle = ResUtil.getString(
      R.string.hint_group_desc, KdbUtil.getGroupEntryNum(pwGroup)
        .toString()
    )
    item.obj = pwGroup
  }

  /**
   * open url with browser
   */
  fun openUrlWithBrowser(url: String) {
    try {
      ActivityUtils.getTopActivity().startActivity(Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
      })
    } catch (e: Exception) {
      ToastUtils.showLong("${ResUtil.getString(R.string.invalid)}${ResUtil.getString(R.string.url)}")
      Timber.e(e)
    }
  }

  /**
   * 处理密码的显示
   */
  fun handleShowPass(tv: TextView, show: Boolean) {
    tv.inputType = if (show) {
      InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    } else {
      InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }
  }

  /**
   * 处理过期的view，并加上中横线
   */
  fun handleExpire(tv: TextView, pwEntryV4: PwEntryV4) {
    if (pwEntryV4.expires()
      && pwEntryV4.expiryTime != null
      && pwEntryV4.expiryTime.before(Date(System.currentTimeMillis()))
    ) {
      val paint = tv.paint
      paint.flags = Paint.STRIKE_THRU_TEXT_FLAG
      paint.isAntiAlias = true
    }
  }
}