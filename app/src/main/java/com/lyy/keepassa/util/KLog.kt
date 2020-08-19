/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Arrays
import kotlin.collections.Map.Entry

/**
 * Created by Aria.Lao on 2017/10/25.
 * Aria日志工具
 */
object KLog {
  const val DEBUG = true
  const val LOG_LEVEL_VERBOSE = 2
  const val LOG_LEVEL_DEBUG = 3
  const val LOG_LEVEL_INFO = 4
  const val LOG_LEVEL_WARN = 5
  const val LOG_LEVEL_ERROR = 6
  const val LOG_LEVEL_ASSERT = 7
  const val LOG_CLOSE = 8
  const val LOG_DEFAULT = LOG_LEVEL_DEBUG
  var LOG_LEVEL = LOG_DEFAULT
  fun v(
    tag: String,
    msg: String
  ): Int {
    return println(Log.VERBOSE, tag, msg)
  }

  fun d(
    tag: String,
    msg: String
  ): Int {
    return println(Log.DEBUG, tag, msg)
  }

  fun i(
    tag: String,
    msg: String
  ): Int {
    return println(Log.INFO, tag, msg)
  }

  fun w(
    tag: String,
    msg: String
  ): Int {
    return println(Log.WARN, tag, msg)
  }

  fun e(
    tag: String,
    msg: String
  ): Int {
    return println(Log.ERROR, tag, msg)
  }

  fun e(
    tag: String?,
    msg: String?,
    e: Throwable?
  ) {
    Log.e(tag, msg, e)
  }

  /**
   * 打印MAp，debug级别日志
   */
  fun m(
    tag: String,
    map: Map<*, *>
  ) {
    if (LOG_LEVEL <= Log.DEBUG) {
      val set: Set<*> = map.entries
      if (set.size < 1) {
        d(tag, "[]")
        return
      }
      val s = arrayOfNulls<String>(set.size)
      for ((i, aSet) in set.withIndex()) {
        val entry = aSet as Entry<*, *>
        s[i] = "${entry.key.toString()} = ${entry.value},"
      }
      println(Log.DEBUG, tag, s.contentToString())
    }
  }

  /**
   * 打印JSON，debug级别日志
   */
  fun j(
    tag: String,
    jsonStr: String
  ) {
    if (LOG_LEVEL <= Log.DEBUG) {
      val message: String
      message = try {
        when {
          jsonStr.startsWith("{") -> {
            val jsonObject = JSONObject(jsonStr)
            jsonObject.toString(4) //这个是核心方法
          }
          jsonStr.startsWith("[") -> {
            val jsonArray = JSONArray(jsonStr)
            jsonArray.toString(4)
          }
          else -> {
            jsonStr
          }
        }
      } catch (e: JSONException) {
        jsonStr
      }
      println(Log.DEBUG, tag, message)
    }
  }

  private fun bundleToString(
    builder: StringBuilder,
    data: Bundle
  ) {
    val keySet = data.keySet()
    builder.append("[Bundle with ")
        .append(keySet.size)
        .append(" keys:")
    for (key in keySet) {
      builder.append(' ')
          .append(key)
          .append('=')
      val value = data.get(key)
      if (value is Bundle) {
        bundleToString(builder, value)
      } else {
        val string = if (value is Array<*>) Arrays.toString(value) else value
        builder.append(string)
      }
    }
    builder.append(']')
  }

  fun b(data: Bundle?): String {
    if (data == null) {
      return "N/A"
    }
    val builder = StringBuilder()
    bundleToString(builder, data)
    return builder.toString()
  }

  /**
   * 将异常信息转换为字符串
   */
  fun getExceptionString(ex: Throwable?): String {
    if (ex == null) {
      return ""
    }
    val err = StringBuilder()
    err.append("ExceptionDetailed:\n")
    err.append("====================Exception Info====================\n")
    err.append(ex.toString())
    err.append("\n")
    val stack = ex.stackTrace
    for (stackTraceElement in stack) {
      err.append(stackTraceElement.toString())
          .append("\n")
    }
    val cause = ex.cause
    if (cause != null) {
      err.append("【Caused by】: ")
      err.append(cause.toString())
      err.append("\n")
      val stackTrace = cause.stackTrace
      for (stackTraceElement in stackTrace) {
        err.append(stackTraceElement.toString())
            .append("\n")
      }
    }
    err.append("===================================================")
    return err.toString()
  }

  private fun println(
    level: Int,
    tag: String,
    msg: String
  ): Int {
    return if (LOG_LEVEL <= level) {
      Log.println(level, tag, if (TextUtils.isEmpty(msg)) "null" else msg)
    } else {
      -1
    }
  }
}