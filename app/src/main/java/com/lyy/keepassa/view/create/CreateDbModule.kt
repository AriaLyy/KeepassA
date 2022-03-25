/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.content.Context
import android.net.Uri
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.liveData
import com.arialyy.frame.router.Routerfit
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.NotificationUtil
import com.lyy.keepassa.view.StorageType
import com.lyy.keepassa.view.StorageType.UNKNOWN
import timber.log.Timber

class CreateDbModule : BaseModule() {

  /**
   * 设置的数据库密码
   */
  var dbPass: String = ""

  /**
   * 数据库名，包含.kdbx
   */
  var dbName: String = ""

  /**
   * 数据库uri
   */
  var localDbUri: Uri? = null

  /**
   * key uri
   */
  var keyUri: Uri? = null

  /**
   * key 的名字
   */
  var keyName: String = ""

  /**
   * 数据库类型
   */
  var storageType: StorageType = UNKNOWN

  /**
   * 云盘路径
   */
  var cloudPath: String = ""

  /**
   * 创建并打开数据库
   */
  fun createAndOpenDb(ac: FragmentActivity) {
    KpaUtil.kdbService.createDb(dbName, localDbUri, dbPass, keyUri, cloudPath, storageType) {
      Timber.d("创建数据库成功")
      HitUtil.toaskShort(ac.getString(R.string.hint_db_create_success, dbName))
      NotificationUtil.startDbOpenNotify(ac)
      Routerfit.create(ActivityRouter::class.java, ac).toMainActivity(
        opt = ActivityOptionsCompat.makeSceneTransitionAnimation(ac)
      )
      KeepassAUtil.instance.saveLastOpenDbHistory(BaseApp.dbRecord)
    }
  }

  /**
   * 数据库打开方式
   */
  fun getDbOpenTypeData(context: Context) = liveData {

    val titles = context.resources.getStringArray(R.array.cloud_names)
    val icons = context.resources.obtainTypedArray(R.array.path_type_img)
    val items = ArrayList<SimpleItemEntity>()
    for ((index, title) in titles.withIndex()) {
      val item = SimpleItemEntity()
      item.title = title
      item.subTitle = titles[index]
      item.id = index
      item.icon = icons.getResourceId(index, 0)
      items.add(item)
    }
    icons.recycle()
    emit(items)
  }
}