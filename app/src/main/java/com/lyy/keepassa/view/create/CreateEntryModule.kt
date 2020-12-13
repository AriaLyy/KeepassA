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
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.UUID

/**
 * 创建条目、群组的module
 */
class CreateEntryModule : BaseModule() {

  var customIcon: PwIconCustom? = null
  val attrStrMap = LinkedHashMap<String, ProtectedString>()
  val attrFileMap = LinkedHashMap<String, ProtectedBinary>()
  var icon = PwIconStandard(0)
  var loseDate: Date? = null // 失效时间
  var userNameCache = arrayListOf<String>()
  var noteStr: CharSequence = ""

  /**
   * Traverse database and get all userName
   */
  fun getUserNameCache() = liveData<List<String>> {
    if (userNameCache.isNotEmpty()) {
      emit(userNameCache)
      return@liveData
    }
    val temp = hashSetOf<String>()
    for (map in BaseApp.KDB.pm.entries) {
      if (map.value.username.isNullOrEmpty()) {
        continue
      }
      val userName = KdbUtil.getUserName(map.value)
      temp.add(userName)
    }
    userNameCache.addAll(temp)
    emit(userNameCache)
  }

  /**
   * 更新实体
   */
  fun updateEntry(
    entry: PwEntryV4,
    title: String,
    userName: String?,
    pass: String?,
    url: String,
    tags: String
  ) {
    if (customIcon != null) {
      entry.customIcon = customIcon
    }
    entry.tags = tags
    if (attrStrMap.isNotEmpty()) {
      entry.strings.clear()
      entry.strings.putAll(attrStrMap)
    } else {
      entry.strings.clear()
    }
    if (attrFileMap.isNotEmpty()) {
      val binPool = (BaseApp.KDB.pm as PwDatabaseV4).binPool
      entry.binaries.clear()
      for (d in attrFileMap) {
        entry.binaries[d.key] = d.value
        if (binPool.poolFind(d.value) == -1) {
          binPool.poolAdd(d.value)
        }
      }
    } else {
      entry.binaries.clear()
    }

    entry.setTitle(title, BaseApp.KDB.pm)
    entry.setUsername(userName, BaseApp.KDB.pm)
    entry.setPassword(pass, BaseApp.KDB.pm)
    entry.setUrl(url, BaseApp.KDB.pm)

    if (noteStr.isNotEmpty()){
      KLog.d(TAG, "notes = $noteStr")
      entry.setNotes(noteStr.toString(), BaseApp.KDB.pm)
    }
    entry.setExpires(loseDate != null)
    if (loseDate != null) {
      entry.expiryTime = loseDate
    }
    entry.icon = icon
  }

  /**
   * 是否已经存在totp
   * @return false 不存在
   */
  fun hasTotp(pwEntryV4: PwEntryV4): Boolean {
    pwEntryV4.strings.forEach {
      if (it.value.isOtpPass) {
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
  ): PwEntryV4 {
    val listStorage = ArrayList<PwEntry>()
    KdbUtil.searchEntriesByPackageName(apkPkgName, listStorage)
    val entry: PwEntryV4
    if (listStorage.isEmpty()) {
      entry = PwEntryV4(BaseApp.KDB.pm.rootGroup as PwGroupV4)
      val icon = IconUtil.getAppIcon(context, apkPkgName)
      if (icon != null) {
        val baos = ByteArrayOutputStream()
        icon.compress(PNG, 100, baos)
        val datas: ByteArray = baos.toByteArray()
        val customIcon = PwIconCustom(UUID.randomUUID(), datas)
        entry.customIcon = customIcon
        (BaseApp.KDB.pm as PwDatabaseV4).putCustomIcons(customIcon)
        entry.strings["KP2A_URL_1"] = ProtectedString(false, "androidapp://$apkPkgName")
      }

      val appName = KDBAutoFillRepository.getAppName(context, apkPkgName)
      entry.setTitle(appName ?: "newEntry", BaseApp.KDB.pm)
      entry.icon = PwIconStandard(0)
    } else {
      entry = listStorage[0] as PwEntryV4
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
          "${BaseApp.APP.getString(R.string.create_group)}${
            BaseApp.APP.getString(
                R.string.success
            )
          }"
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
          "${BaseApp.APP.getString(R.string.create_entry)}${
            BaseApp.APP.getString(
                R.string.success
            )
          }"
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