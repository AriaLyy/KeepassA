/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import timber.log.Timber
import kotlin.random.Random

/**
 * 创建密码工具
 */
class PasswordBuildUtil private constructor() {

  companion object {
    @Volatile
    private var INSTANCE: PasswordBuildUtil? = null
    private const val LOWER_CHAR = "lowerChar"
    private const val UP_CHAR = "upChar"
    private const val NUM_CHAR = "numChar"
    private const val SYMBOL_CHAR = "symbolChar"
    private const val BRACKET_CHAR = "bracketChar"
    private const val SPACE_CHAR = "spaceChar"
    private const val UNDERLINE = "underline"
    private const val MINUS = "minus"

    @Synchronized
    fun getInstance(): PasswordBuildUtil {
      if (INSTANCE == null) {
        synchronized(PasswordBuildUtil::class.java) {
          INSTANCE = PasswordBuildUtil()
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

  private val charList = arrayListOf<String>()

  // 默认字符串
  // private var defStr: StringBuffer = StringBuffer()

  /**
   * 清空字符
   */
  public fun clear(): PasswordBuildUtil {
    charList.clear()
    return this
  }

  /**
   * 使用小写字符
   */
  public fun addLowerChar(): PasswordBuildUtil {
    charList.add(lowerChar)
    return this
  }

  /**
   * 使用大写字符
   */
  public fun addUpChar(): PasswordBuildUtil {
    charList.add(upChar)
    return this
  }

  /**
   * 使用数字
   */
  public fun addNumChar(): PasswordBuildUtil {
    charList.add(numChar)
    return this
  }

  /**
   * 使用特殊字符
   */
  public fun addSymbolChar(): PasswordBuildUtil {
    charList.add(symbolChar)
    return this
  }

  /**
   * 使用括号
   */
  public fun addBracketChar(): PasswordBuildUtil {
    charList.add(bracketChar)
    return this
  }

  /**
   * 使用空格
   */
  public fun addSpaceChar(): PasswordBuildUtil {
    charList.add(spaceChar)
    return this
  }

  /**
   * 使用下划线
   */
  public fun addUnderline(): PasswordBuildUtil {
    charList.add(underline)
    return this
  }

  /**
   * 使用减号
   */
  public fun addMinus(): PasswordBuildUtil {
    charList.add(minus)
    return this
  }

  /**
   * 构建密码，如果密码长度小于选择的[charList]长度，则修改其长度为[charList]的长度
   * @param len 密码长度
   * @return 如果[种子][charList]没有设置，或长度小于1 返回""
   */
  public fun builder(len: Int): String {
    if (charList.isEmpty()) {
      return ""
    }

    if (len < 1) {
      return ""
    }
    val realLen = if (len < charList.size) {
      Timber.w("密码长度小于选择的特殊符号种类，将生成和特殊符号种类数量一致长度的密码")
      charList.size
    } else {
      len
    }

    val cloneList = arrayListOf<String>()
    cloneList.addAll(charList)

    val random = Random.Default
    val sb = StringBuilder()
    for (i in 1..realLen) {

      // 确保每种类型都有
      if (cloneList.isNotEmpty()) {
        val charIndex = random.nextInt(cloneList.size)
        val char = cloneList[charIndex]
        sb.append(char[random.nextInt(char.length)])
        cloneList.remove(char)
        continue
      }
      val charIndex = random.nextInt(charList.size)
      val char = charList[charIndex]
      sb.append(char[random.nextInt(char.length)])
    }

    return sb.toString()
  }
}