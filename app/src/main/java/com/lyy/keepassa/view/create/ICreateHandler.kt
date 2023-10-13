/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create

/**
 * @Author laoyuyu
 * @Description
 * @Date 7:32 PM 2023/10/13
 **/
interface ICreateHandler {
  fun initData(ac: CreateEntryActivity)

  fun getTitle():String
}