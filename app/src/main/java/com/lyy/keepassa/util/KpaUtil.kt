/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.arialyy.frame.util.StringUtil
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.router.ServiceRouter
import kotlinx.coroutines.MainScope
import java.util.Locale

/**
 * @Author laoyuyu
 * @Description
 * @Date 2022/3/22
 **/
object KpaUtil {
  var scope = MainScope()
  val kdbHandlerService by lazy {
    Routerfit.create(ServiceRouter::class.java).getDbSaveService()
  }

  val kdbOpenService by lazy {
    Routerfit.create(ServiceRouter::class.java).getDbOpenService()
  }

  fun isChina():Boolean{
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
}