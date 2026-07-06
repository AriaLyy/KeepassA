/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import android.view.autofill.AutofillId

internal object AutofillSearchOrUrlFieldPolicy {

  /**
   * 当被分类的字段全部是搜索/URL 栏时,无视填充。原实现里只在 Mi Browser 上启用,但
   * 其它浏览器(Chromium 系/Edge 等)同样会出现 URL 栏被识别成 username 的场景,因此
   * 这里改成对所有浏览器统一生效。
   */
  fun shouldIgnoreClassifiedFields(
    classifiedIds: Set<AutofillId>,
    searchOrUrlIds: Set<AutofillId>
  ): Boolean {
    return classifiedIds.isNotEmpty() &&
      searchOrUrlIds.isNotEmpty() &&
      classifiedIds.all(searchOrUrlIds::contains)
  }
}
