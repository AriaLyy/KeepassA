/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.router

import com.arialyy.frame.router.RouterArgName
import com.arialyy.frame.router.RouterPath
import com.lyy.keepassa.view.main.EntryListFragment
import com.lyy.keepassa.view.main.HomeFragment

/**
 * @Author laoyuyu
 * @Description
 * @Date 3:08 下午 2021/10/27
 **/
interface FragmentRouter {

  @RouterPath(path = "/main/fragment/home")
  fun toMainHomeFragment(): HomeFragment

  @RouterPath(path = "/main/fragment/entry")
  fun toMainHistoryFragment(@RouterArgName(name = "type") type: String = EntryListFragment.TYPE_HISTORY): EntryListFragment

  @RouterPath(path = "/main/fragment/entry")
  fun toMainTOTPFragment(@RouterArgName(name = "type") type: String = EntryListFragment.TYPE_TOTP): EntryListFragment
}