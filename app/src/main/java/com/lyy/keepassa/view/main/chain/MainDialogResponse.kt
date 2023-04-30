/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main.chain

/**
 * @Author laoyuyu
 * @Description
 * @Date 2023/4/22
 **/
data class MainDialogResponse(val code: Int) {
  companion object {
    const val RESPONSE_OK = 1
    const val RESPONSE_BREAK = 2
    const val RESPONSE_FINISH = 3
  }
}