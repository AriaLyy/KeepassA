/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util.cloud

interface SynStateCode {
  val STATE_SUCCEED: Int
    get() = 0
  val STATE_FAIL: Int
    get() = 1
  val STATE_DEL_FILE_FAIL: Int
    get() = 2

  val STATE_DOWNLOAD_FILE_FAIL: Int
    get() = 3
  val STATE_SAVE_DB_FAIL: Int
    get() = 4
  val STATE_CANCEL: Int
    get() = 100
}