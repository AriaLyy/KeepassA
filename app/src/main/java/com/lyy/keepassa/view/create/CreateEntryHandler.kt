/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create

import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R

/**
 * @Author laoyuyu
 * @Description
 * @Date 7:34 PM 2023/10/13
 **/
internal class CreateEntryHandler(val context:CreateEntryActivity) : ICreateHandler {



  override fun bindData() {

  }

  override fun getTitle(): String {
    return ResUtil.getString(R.string.create_entry)
  }
}