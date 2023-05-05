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
import com.lyy.keepassa.base.BaseApp
import timber.log.Timber
import java.util.UUID

object KdbUtil {
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
    val topDomain = Regex(RegularRule.DOMAIN_TOP, RegexOption.IGNORE_CASE).find(domain)?.value.toString()
    Timber.d("topDomain = $topDomain")
    for (entry in BaseApp.KDB.pm.entries.values) {
      val pe4 = entry as PwEntryV4
      if (pe4.url.contains(topDomain, true)
        || pe4.strings["URL"]?.toString()?.contains(topDomain) ==true
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
          break
        }
      }
    }
  }

  fun getGroupEntryNum(pwGroup: PwGroup): Int {
    return pwGroup.childEntries.size + pwGroup.childGroups.size
  }

  /**
   * 获取组中的条目数
   */
  fun getGroupAllEntryNum(pwGroup: PwGroup): Int {
    var num = 0
    if (pwGroup.childEntries.isEmpty() && pwGroup.childGroups.isEmpty()) {
      return 0
    }
    if (pwGroup.childGroups.isNotEmpty()) {
      for (group in pwGroup.childGroups) {
        num += getGroupAllEntryNum(group)
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