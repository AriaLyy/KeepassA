/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import com.arialyy.frame.util.RegularRule
import com.keepassdroid.Database
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupIdV3
import com.keepassdroid.database.PwGroupIdV4
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.helper.KDBHandlerHelper
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.cloud.DbSynUtil
import timber.log.Timber
import java.util.UUID

object KdbUtil {
  private val TAG = javaClass.simpleName

  fun Database?.isNull(): Boolean {
    return this == null || this.pm == null
  }

  /**
   * 获取用户名，如果是引用其它条目的，解析其引用
   */
  fun getUserName(entry: PwEntry): String {
    return if (entry.isRef())
      entry.getUsername(true, BaseApp.KDB!!.pm)
    else
      entry.username
  }

  /**
   * 获取密码，如果是引用其它条目的，解析其引用
   */
  fun getPassword(entry: PwEntry): String {
    val pass = entry.password
    return if (entry.isRef())
      entry.getPassword(true, BaseApp.KDB!!.pm)
    else
      pass
  }

  /**
   * 删除分组
   *
   * @param save 是否需要保存数据库；true 删除后保存数据库，false 删除后不保存数据库
   */
  suspend fun deleteGroup(
    group: PwGroup,
    save: Boolean,
    needUpload: Boolean = false
  ) {
    KDBHandlerHelper.getInstance(BaseApp.APP)
        .deleteGroup(BaseApp.KDB, group, save)
    if (needUpload) {
      uploadDb()
    }
  }

  /**
   * 保存分组
   *
   * @param name 组名
   * @param icon 分组图标
   * @param parent 父组，如果是想添加到跟目录，设置null
   */
  suspend fun createGroup(
    groupName: String,
    icon: PwIconCustom?,
    parent: PwGroupV4
  ): PwGroup? {
    val group = KDBHandlerHelper.getInstance(BaseApp.APP)
        .createGroup(BaseApp.KDB, groupName, icon, parent)
    val code = uploadDb()
    if (code != DbSynUtil.STATE_SUCCEED) {
      return null
    }
    return group
  }

  /**
   * 创建分组
   *
   * @param name 组名
   * @param icon 分组图标
   * @param parent 父组，如果是想添加到跟目录，设置null
   */
  suspend fun createGroup(
    groupName: String,
    icon: PwIconStandard,
    parent: PwGroup
  ): PwGroup {
    val group = KDBHandlerHelper.getInstance(BaseApp.APP)
        .createGroup(BaseApp.KDB, groupName, icon, parent)
    uploadDb()
    return group
  }

  /**
   * 删除条目
   * @param save 是否需要保存数据库；true 删除后保存数据库，false 删除后不保存数据库
   */
  suspend fun deleteEntry(
    entry: PwEntry,
    save: Boolean,
    needUpload: Boolean = false
  ): Int {
    KDBHandlerHelper.getInstance(BaseApp.APP)
        .deleteEntry(BaseApp.KDB, entry, save)
    if (needUpload) {
      return uploadDb()
    }
    return DbSynUtil.STATE_SUCCEED
  }

  /**
   * 只添加群组，不进行保存，不进行上传
   */
  fun addGroup(group: PwGroup) {
    BaseApp.KDB!!.pm.addGroupTo(group, group.parent)
  }

  /**
   * 添加条目
   */
  suspend fun addEntry(
    entry: PwEntry,
    save: Boolean = true,
    uploadDb: Boolean = false
  ): Int {
    if (save) {
      KDBHandlerHelper.getInstance(BaseApp.APP)
          .saveEntry(BaseApp.KDB, entry)
    } else {
      BaseApp.KDB!!.pm.addEntryTo(entry, entry.parent)
    }
    if (uploadDb) {
      return uploadDb()
    }
    return DbSynUtil.STATE_SUCCEED
  }

  /**
   * 保存数据库，并上传数据库到云端
   * @return [DbSynUtil.STATE_SUCCEED]
   */
  suspend fun saveDb(
    uploadDb: Boolean = true,
    isSync: Boolean = false
  ): Int {
    Timber.d( "保存前的数据库hash：${BaseApp.KDB.hashCode()}，num = ${BaseApp.KDB!!.pm.entries.size}")
    val b = KDBHandlerHelper.getInstance(BaseApp.APP)
        .save(BaseApp.KDB)
    if (uploadDb) {
      return uploadDb()
    }
    Timber.d( "保存后的数据库hash：${BaseApp.KDB.hashCode()}，num = ${BaseApp.KDB!!.pm.entries.size}")
//    // 更新rootGroup条目
//    if (b && isSync) {
//      updateRootGroup()
//    }
    return if (b) DbSynUtil.STATE_SUCCEED else DbSynUtil.STATE_SAVE_DB_FAIL
  }

  /**
   * 更新根rootGroup条目，pm.rootGroup是新new的，并不是索引，从云端同步后，该内容不会更新，需要手动更新
   */
  private fun updateRootGroup() {
    val rootGroup = BaseApp.KDB.pm.rootGroup
    rootGroup.childEntries.clear()
    rootGroup.childGroups.clear()
    val rootGroupId = rootGroup.id
    BaseApp.KDB.pm.entries.forEach {
      if (it.value.parent.id == rootGroupId) {
        rootGroup.childEntries.add(it.value)
      }
    }

    BaseApp.KDB.pm.groups.forEach {
      if (it.value == null || it.value.parent == null) {
        return@forEach
      }
      if (it.value.parent.id == rootGroupId) {
        rootGroup.childGroups.add(it.value)
      }
    }
  }

  /**
   * 上传数据库到云端
   */
  private suspend fun uploadDb(): Int {
    if (!BaseApp.isAFS()) {
      val code = DbSynUtil.uploadSyn(BaseApp.APP, BaseApp.dbRecord!!)
      if (code != DbSynUtil.STATE_SUCCEED) {
        Timber.e( "同步数据库失败，code：$code")
      }
      return code
    }
    return DbSynUtil.STATE_SUCCEED
  }

  /**
   * 通过搜索条目
   * @param domain 域名
   * @param listStorage 搜索结果
   */
  fun searchEntriesByDomain(
    domain: String?,
    listStorage: MutableList<PwEntry>
  ) {
    if (domain.isNullOrEmpty()) {
      return
    }
    val topDomain = Regex(RegularRule.DOMAIN_TOP, RegexOption.IGNORE_CASE).find(domain)
    Timber.d( "topDomain = ${topDomain?.value}")
    for (entry in BaseApp.KDB.pm.entries.values) {
      val pe4 = entry as PwEntryV4
      if (pe4.getUrl()
              .contains(topDomain?.value.toString(), true)
      ) {
        listStorage.add(pe4)
      }
    }
  }

  /**
   * 通过包名搜索条目
   *
   * @param pkgName 包名
   * @param listStorage 搜索结果
   */
  fun searchEntriesByPackageName(
    pkgName: String,
    listStorage: MutableList<PwEntry>
  ) {
    for (entry in BaseApp.KDB.pm.entries.values) {
      val pe4 = entry as PwEntryV4
      if (pe4.strings == null || pe4.strings.isEmpty()) {
        continue
      }
      for (ps in pe4.strings.values) {
        if (ps.toString()
                .equals("androidapp://$pkgName", ignoreCase = true)
        ) {
          listStorage.add(pe4)
        }
      }
    }
  }

  /**
   * 获取组中的条目数
   */
  fun getGroupEntryNum(pwGroup: PwGroup): Int {
    var num = 0
    if (pwGroup.childEntries.isEmpty() && pwGroup.childGroups.isEmpty()) {
      return 0
    }
    if (pwGroup.childGroups.isNotEmpty()) {
      for (group in pwGroup.childGroups) {
        num += getGroupEntryNum(group)
      }
      num += pwGroup.childEntries.size
    } else {
      num += pwGroup.childEntries.size
    }
    return num
  }

  fun findEntry(uuid: UUID): PwEntry? {
    val enties = BaseApp.KDB.pm.entries
    return enties[uuid]
  }

  /**
   * 通过id 获取v3的group信息
   */
  fun findV3GroupById(groupId: Int): PwGroup? {
    val groups = BaseApp.KDB.pm.groups
    return groups[PwGroupIdV3(groupId)]
  }

  /**
   * 通过id 获取v4的group信息
   */
  fun findV4GroupById(groupId: UUID): PwGroup? {
    val groups = BaseApp.KDB.pm.groups

    return groups[PwGroupIdV4(groupId)]
  }

}