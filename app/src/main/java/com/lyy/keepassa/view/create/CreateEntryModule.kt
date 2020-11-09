/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import KDBAutoFillRepository
import android.content.Context
import android.graphics.Bitmap.CompressFormat.PNG
import android.util.Log
import androidx.lifecycle.liveData
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV3
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * 创建条目、群组的module
 */
class CreateEntryModule : BaseModule() {

  /**
   * 是否已经存在totp
   * @return false 不存在
   */
  fun hasTotp(pwEntryV4: PwEntryV4):Boolean{
    pwEntryV4.strings.forEach {
      if (it.value.isOtpPass){
        return true
      }
    }
    return false
  }

  /**
   * 自动填充进行保存数据时，搜索条目信息，如果条目不存在，新建条目
   */
  fun getEntryFromAutoFillSave(
    context: Context,
    apkPkgName: String,
    userName: String?,
    pass: String?
  ): PwEntry {
    val listStorage = ArrayList<PwEntry>()
    KdbUtil.searchEntriesByPackageName(apkPkgName, listStorage)
    val entry: PwEntry
    if (listStorage.isEmpty()) {
      if (BaseApp.isV4) {
        entry = PwEntryV4(BaseApp.KDB.pm.rootGroup as PwGroupV4)
        val icon = KDBAutoFillRepository.getAppIcon(context, apkPkgName)
        if (icon != null) {
          val baos = ByteArrayOutputStream()
          icon.compress(PNG, 100, baos)
          val datas: ByteArray = baos.toByteArray()
          val customIcon = PwIconCustom(UUID.randomUUID(), datas)
          entry.customIcon = customIcon
          (BaseApp.KDB.pm as PwDatabaseV4).putCustomIcons(customIcon)
          entry.strings["KP2A_URL_1"] = ProtectedString(false, "androidapp://$apkPkgName")
        }
      } else {
        entry = PwEntryV3()
        entry.setUrl("androidapp://$apkPkgName", BaseApp.KDB.pm)
      }
      val appName = KDBAutoFillRepository.getAppName(context, apkPkgName)
      entry.setTitle(appName ?: "newEntry", BaseApp.KDB.pm)
      entry.icon = PwIconStandard(0)
    } else {
      entry = listStorage[0]
      Log.w(TAG, "已存在含有【$apkPkgName】的条目，将更新条目")
    }
    if (!userName.isNullOrEmpty()) {
      entry.setUsername(userName, BaseApp.KDB.pm)
    }
    if (!pass.isNullOrEmpty()) {
      entry.setPassword(pass, BaseApp.KDB.pm)
    }
    return entry
  }

  /**
   * 创建群组
   * @param groupName 群组名
   * @param parentGroup 父群组
   * @param icon 标准图标
   * @param customIcon 自定义图标
   */
  fun createGroup(
    groupName: String,
    parentGroup: PwGroup,
    icon: PwIconStandard,
    customIcon: PwIconCustom?
  ) = liveData {
    val b = withContext(Dispatchers.IO) {
      try {
        if (customIcon != null && customIcon != PwIconCustom.ZERO && BaseApp.isV4) {
          return@withContext KdbUtil.createGroup(
              groupName, customIcon, parentGroup as PwGroupV4
          )
        }

        return@withContext KdbUtil.createGroup(groupName, icon, parentGroup)
      } catch (e: Exception) {
        e.printStackTrace()
        HitUtil.toaskOpenDbException(e)
      }
      return@withContext null
    }
    if (b != null) {
      HitUtil.toaskShort(
          "${BaseApp.APP.getString(R.string.create_group)}${BaseApp.APP.getString(
              R.string.success
          )}"
      )
    }
    emit(b)
  }

  /**
   * 添加条目
   * @param pwEntry 需要添加的条目
   */
  fun addEntry(pwEntry: PwEntry) = liveData {
    val code = withContext(Dispatchers.IO) {
      try {
        return@withContext KdbUtil.addEntry(pwEntry, save = true, uploadDb = true)
      } catch (e: Exception) {
        e.printStackTrace()
        HitUtil.toaskOpenDbException(e)
      }
      return@withContext DbSynUtil.STATE_FAIL
    }

    if (code == DbSynUtil.STATE_SUCCEED) {
      HitUtil.toaskShort(
          "${BaseApp.APP.getString(R.string.create_entry)}${BaseApp.APP.getString(
              R.string.success
          )}"
      )
    }
    emit(code == DbSynUtil.STATE_SUCCEED)
  }

  /**
   * 保存条目
   */
  fun saveDb() = liveData {
    val code = withContext(Dispatchers.IO) {
      try {
        return@withContext KdbUtil.saveDb()
      } catch (e: Exception) {
        e.printStackTrace()
        HitUtil.toaskOpenDbException(e)
      }
      return@withContext DbSynUtil.STATE_FAIL
    }
    emit(code == DbSynUtil.STATE_SUCCEED)

  }

  /**
   * 构建的更多选择项目
   */
  fun getMoreItem(context: Context): ArrayList<SimpleItemEntity> {
    val list = ArrayList<SimpleItemEntity>()
    if (!BaseApp.isV4) {
      val titles = context.resources.getStringArray(R.array.v3_add_mor_item)
      val icons = context.resources.obtainTypedArray(R.array.v3_add_more_icon)
      val len = titles.size - 1
      for (i in 0..len) {
        val item = SimpleItemEntity()
        item.title = titles[i]
        item.icon = icons.getResourceId(i, 0)
        list.add(item)
      }
      icons.recycle()
    } else {
      val titles = context.resources.getStringArray(R.array.v4_add_mor_item)
      val icons = context.resources.obtainTypedArray(R.array.v4_add_more_icon)
      val len = titles.size - 1
      for (i in 0..len) {
        val item = SimpleItemEntity()
        item.title = titles[i]
        item.icon = icons.getResourceId(i, 0)
        list.add(item)
      }
      icons.recycle()
    }
    return list
  }

}