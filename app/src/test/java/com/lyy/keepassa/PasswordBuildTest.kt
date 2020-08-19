/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa

import com.lyy.keepassa.util.PasswordBuilUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import org.junit.Test

class PasswordBuildTest {
  private val TAG = "PasswordBuildTest"

  @Test
  fun sss(){
    println("https://store.steampowered.com/".contains("steampowered"))
  }

  @Test
  fun buildPassWord() {
    val sTime = System.currentTimeMillis()
    val pass = PasswordBuilUtil.getInstance()
    val len = 16
    pass
        .addNumChar()
        .addLowerChar()
//        .addUnderline()
//        .addSpaceChar()
//        .addSymbolChar()
//        .addMinus()
        .addUpChar()
//        .addbracketChar()
    println(" 密码：${pass.builder(len)}\n 密码长度：$len\n 耗时：${System.currentTimeMillis() - sTime}")
  }

}