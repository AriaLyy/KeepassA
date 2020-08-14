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