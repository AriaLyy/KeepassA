/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.icon

import androidx.lifecycle.liveData
import com.keepassdroid.database.PwDatabaseV4
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.SimpleItemEntity

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/3/24
 **/
class IconModule : BaseModule() {

  /**
   * get default icon entiry
   */
  fun getDefaultIconList() = liveData<List<SimpleItemEntity>>{
    val list = arrayListOf<SimpleItemEntity>()
    for (i in 0..68) {
      val item = SimpleItemEntity()
      item.id = i
      item.icon = i
      list.add(item)
    }
    emit(list)
  }

  /**
   * only pwV4 has custom icon
   */
  fun getCustomIconList() = liveData<List<SimpleItemEntity>> {
    val list = arrayListOf<SimpleItemEntity>()
    if (!BaseApp.isV4){
      emit(list)
      return@liveData
    }

    val v4Group = BaseApp.KDB.pm as PwDatabaseV4
    for ((i, icon) in v4Group.customIcons.withIndex()) {
      val item = SimpleItemEntity()
      item.id = i
      item.icon = -1
      item.obj = icon
      list.add(item)
      emit(list)
    }
  }
}