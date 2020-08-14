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