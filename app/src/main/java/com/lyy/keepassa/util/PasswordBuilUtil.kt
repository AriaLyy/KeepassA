/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import kotlin.random.Random

/**
 * 创建密码工具
 */
class PasswordBuilUtil private constructor() {

  companion object {
    @Volatile
    private var INSTANCE: PasswordBuilUtil? = null

    @Synchronized
    fun getInstance(): PasswordBuilUtil {
      if (INSTANCE == null) {
        synchronized(PasswordBuilUtil::class.java) {
          INSTANCE = PasswordBuilUtil()
        }
      }
      return INSTANCE!!
    }
  }

  private val lowerChar = "abcdefghijklmnopqrstuvwxyz"
  private val upChar = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  private val numChar = "1234567890"
  // 特殊符号
  private val symbolChar = "`~!@#$%^&*+=;:'\",.?/|\\" // ~
  // 括号
  private val bracketChar = "()<>{}[]"
  // 空格
  private val spaceChar = " "
  // 下划线
  private val underline = "_"
  // 减号
  private val minus = "-"

  // 默认字符串
  private var defStr: StringBuffer = StringBuffer()

  /**
   * 清空字符
   */
  public fun clear() {
    defStr.setLength(0)
  }

  /**
   * 使用小写字符
   */
  public fun addLowerChar(): PasswordBuilUtil {
    defStr.append(lowerChar)
    return this
  }

  /**
   * 使用大写字符
   */
  public fun addUpChar(): PasswordBuilUtil {
    defStr.append(upChar)
    return this
  }

  /**
   * 使用数字
   */
  public fun addNumChar(): PasswordBuilUtil {
    defStr.append(numChar)
    return this
  }

  /**
   * 使用特殊字符
   */
  public fun addSymbolChar(): PasswordBuilUtil {
    defStr.append(symbolChar)
    return this
  }

  /**
   * 使用括号
   */
  public fun addbracketChar(): PasswordBuilUtil {
    defStr.append(bracketChar)
    return this
  }

  /**
   * 使用空格
   */
  public fun addSpaceChar(): PasswordBuilUtil {
    defStr.append(spaceChar)
    return this
  }

  /**
   * 使用下划线
   */
  public fun addUnderline(): PasswordBuilUtil {
    defStr.append(underline)
    return this
  }

  /**
   * 使用减号
   */
  public fun addMinus(): PasswordBuilUtil {
    defStr.append(minus)
    return this
  }

  /**
   * 构建密码
   * @param len 密码长度
   * @return 如果[种子][defStr]没有设置，或长度小于1 返回""
   */
  public fun builder(len: Int): String {
    if (defStr.isEmpty()) {
      return ""
    }

    if (len < 1) {
      return ""
    }
    val random = Random.Default
    val defStrLen = defStr.length
    val sb = StringBuilder()
    for (i in 1..len) {
      val num = random.nextInt(defStrLen)
      sb.append(defStr[num])
    }
    return sb.toString()
  }

}