/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.router

import androidx.core.app.ActivityOptionsCompat
import com.arialyy.frame.router.RouterArgName
import com.arialyy.frame.router.RouterPath
import com.keepassdroid.database.PwGroupId
import com.lyy.keepassa.entity.AutoFillParam
import com.lyy.keepassa.view.create.CreateEntryActivity
import com.lyy.keepassa.view.create.CreateEnum
import com.lyy.keepassa.view.detail.EntryDetailActivity
import com.lyy.keepassa.view.detail.GroupDetailActivity
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.setting.SettingActivity
import java.util.UUID

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/10/17
 **/
interface ActivityRouter {

  @RouterPath(path = "/search/common")
  fun toCommonSearch(
    @RouterArgName(name = "apkPkgName") apkPkgName: String? = null,
    @RouterArgName(name = "onlySearch") onlySearch: Boolean = true
  )

  @RouterPath(path = "/collection/ac")
  fun toMyCollection(@RouterArgName(name = "opt") opt: ActivityOptionsCompat? = null)

  @RouterPath(path = "/setting/app")
  fun toAppSetting(
    @RouterArgName(name = SettingActivity.KEY_TYPE) type: Int = SettingActivity.TYPE_APP,
    @RouterArgName(name = "opt") opt: ActivityOptionsCompat? = null,
    @RouterArgName(name = "scrollKey") scrollKey: String? = null
  )

  @RouterPath(path = "/setting/app")
  fun toDbSetting(
    @RouterArgName(name = SettingActivity.KEY_TYPE) type: Int = SettingActivity.TYPE_DB,
    @RouterArgName(name = "opt") opt: ActivityOptionsCompat? = null
  )

  @RouterPath(path = "/launcher/quickLock")
  fun toQuickUnlockActivity(
    @RouterArgName(name = "flag", isFlag = true) flags: Int
  )

  @RouterPath(path = "/entry/detail")
  fun toEntryDetailActivity(
    @RouterArgName(name = EntryDetailActivity.KEY_ENTRY_ID) entryId: UUID,
    @RouterArgName(name = EntryDetailActivity.KEY_GROUP_TITLE) groupName: String,
    @RouterArgName(name = "opt") opt: ActivityOptionsCompat? = null
  )

  /**
   * to group detail
   */
  @RouterPath(path = "/group/detail")
  fun toGroupDetailActivity(
    @RouterArgName(name = GroupDetailActivity.KEY_TITLE) groupName: String,
    @RouterArgName(name = GroupDetailActivity.KEY_GROUP_ID) groupId: PwGroupId,
    @RouterArgName(name = GroupDetailActivity.KEY_IS_IN_RECYCLE_BIN) isRecycleBin: Boolean = false,
    @RouterArgName(name = "opt") opt: ActivityOptionsCompat? = null
  )

  /**
   * create entry
   */
  @RouterPath(path = "/entry/create")
  fun toCreateEntryActivity(
    @RouterArgName(name = CreateEntryActivity.PARENT_GROUP_ID) groupId: PwGroupId?,
    @RouterArgName(name = "opt") opt: ActivityOptionsCompat? = null,
    @RouterArgName(name = CreateEntryActivity.IS_SHORTCUTS) isFromShortcuts: Boolean = false,
    @RouterArgName(name = CreateEntryActivity.KEY_TYPE) type: CreateEnum = CreateEnum.CREATE
  )

  /**
   * edit entry
   */
  @RouterPath(path = "/entry/create")
  fun toEditEntryActivity(
    @RouterArgName(name = CreateEntryActivity.KEY_ENTRY) uuid: UUID,
    @RouterArgName(name = "opt") opt: ActivityOptionsCompat? = null,
    @RouterArgName(name = CreateEntryActivity.KEY_TYPE) type: CreateEnum = CreateEnum.MODIFY
  )

  @RouterPath(path = "/entry/create")
  fun toEditEntryActivity(
    @RouterArgName(name = LauncherActivity.KEY_AUTO_FILL_PARAM) params: AutoFillParam
  )

  @RouterPath(path = "/main/ac")
  fun toMainActivity(
    @RouterArgName(name = "isShortcuts") isShortcuts: Boolean = false,
    @RouterArgName(name = "shortcutType") shortcutType: Int = 1,
    @RouterArgName(name = "opt") opt: ActivityOptionsCompat? = null
  )

  @RouterPath(path = "/launcher/createDb")
  fun toCreateDbActivity(
    @RouterArgName(name = "opt") opt: ActivityOptionsCompat? = null
  )
}