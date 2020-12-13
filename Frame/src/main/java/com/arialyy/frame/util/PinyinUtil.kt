/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.arialyy.frame.util

import java.io.UnsupportedEncodingException

/**
 * 拼音工具
 * https://www.cnblogs.com/yydcdut/p/3885916.html
 */
object PinyinUtil {
  private const val GB_SP_DIFF = 160

  /**
   * 存放国标一级汉字不同读音的起始区位码
   */
  private val secPosValueList = intArrayOf(
      1601, 1637, 1833, 2078, 2274, 2302,
      2433, 2594, 2787, 3106, 3212, 3472, 3635, 3722, 3730, 3858, 4027,
      4086, 4390, 4558, 4684, 4925, 5249, 5600
  )

  /**
   * 存放国标一级汉字不同读音的起始区位码对应读音
   */
  private val firstLetter = charArrayOf(
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
      'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'w', 'x',
      'y', 'z'
  )

  /**
   * 获取首字母
   */
  fun getFirstSpellChar(str: CharSequence): Char? {
    for (element in str) {
      // 判断是否为汉字，如果左移7为为0就不是汉字，否则是汉字
      if (element.toInt() shr 7 == 0) {
        return element
      }
      // 处理汉字的
      return getFirstLetter(element)
    }
    return "".toCharArray()[0]
  }

  /**
   * 获取字符串中所有字符的英文字母
   */
  fun getSpells(characters: String): String? {
    val buffer = StringBuffer()
    for (element in characters) {
      // 判断是否为汉字，如果左移7为为0就不是汉字，否则是汉字
      if (element.toInt() shr 7 == 0) {
        buffer.append(element)
        continue
      }
      // 处理汉字的
      val spell = getFirstLetter(element)
      buffer.append(spell.toString())
    }
    return buffer.toString()
  }

  /**
   * 获取一个汉字的首字母
   */
  private fun getFirstLetter(ch: Char): Char? {
    val uniCode: ByteArray
    uniCode = try {
      ch.toString()
          .toByteArray(charset("GBK"))
    } catch (e: UnsupportedEncodingException) {
      e.printStackTrace()
      return null
    }
    return if (uniCode[0] in 1..127) { // 非汉字
      null
    } else {
      convert(uniCode)
    }
  }

  /**
   * 获取一个汉字的拼音首字母。 GB码两个字节分别减去160，转换成10进制码组合就可以得到区位码
   * 例如汉字“你”的GB码是0xC4/0xE3，分别减去0xA0（160）就是0x24/0x43
   * 0x24转成10进制就是36，0x43是67，那么它的区位码就是3667，在对照表中读音为‘n’
   */
  private fun convert(bytes: ByteArray): Char {
    var result = '-'
    var i = 0
    while (i < bytes.size) {
      bytes[i] = ((bytes[i] - GB_SP_DIFF).toByte())
      i++
    }

    val secPosValue: Int = bytes[0] * 100 + bytes[1]
    i = 0
    while (i < 23) {
      if (secPosValue >= secPosValueList[i] && secPosValue < secPosValueList[i + 1]) {
        result = firstLetter[i]
        break
      }
      i++
    }
    return result
  }

}