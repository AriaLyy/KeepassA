/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.router

import android.app.Dialog
import android.content.Context
import android.os.Parcelable
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.DialogFragment
import com.arialyy.frame.core.AbsFrame
import com.arialyy.frame.router.DialogArg
import com.arialyy.frame.router.RouterArgName
import com.arialyy.frame.router.RouterPath
import com.didi.drouter.api.DRouter
import com.didi.drouter.api.Extend
import com.didi.drouter.router.Request
import timber.log.Timber
import java.io.Serializable
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:10 PM 2023/4/21
 **/
class DRouterfit {

  @Suppress("UNCHECKED_CAST")
  fun <T> create(routerService: Class<T>): T {
    return Proxy.newProxyInstance(
      routerService.classLoader,
      arrayOf<Class<*>>(routerService)
    ) { _, method, args ->
      return@newProxyInstance createProxy(method, args, null)
    } as T
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> create(routerService: Class<T>, context: Context): T {
    return Proxy.newProxyInstance(
      routerService.classLoader,
      arrayOf<Class<*>>(routerService)
    ) { _, method, args ->
      return@newProxyInstance createProxy(method, args, context)
    } as T
  }

  private fun createProxy(method: Method, args: Array<Any>?, context: Context?): Any? {
    var curRouterPath: RouterPath? = null
    var showDialog = false
    for (annotation in method.annotations) {
      if (annotation is RouterPath) {
        curRouterPath = annotation
      }
      if (annotation is DialogArg) {
        showDialog = annotation.showDialog
      }
    }

    curRouterPath ?: return null

    val request = DRouter.build(curRouterPath.path)

    if (args != null) {
      for (i in args.indices) {
        var argName: String? = null
        var isObject = false
        var isFlag = false
        for (annotation in method.parameterAnnotations[i]) {
          if (annotation is RouterArgName) {
            argName = annotation.name
            isObject = annotation.isObject
            isFlag = annotation.isFlag
            break
          }
        }
        argName ?: break

        setArgs(request, argName, args[i], isObject, isFlag)
      }
    }
    val result = if (context == null) postcard.navigation() else postcard.navigation(context)

    if (showDialog) {
      when (result) {
        is Dialog -> {
          result.show()
        }
        is DialogFragment -> {
          AbsFrame.getInstance().currentActivity?.let {
            result.show(
              it.supportFragmentManager,
              result::javaClass.name
            )
          }
        }
      }
    }

    return result
  }

  private fun setArgs(
    pos: Request,
    name: String,
    value: Any?,
    isObject: Boolean,
    isFlag: Boolean
  ) {
    if (value == null) {
      return
    }

    if (isObject) {
      Timber.d("isObject")
      pos.putAddition(name, value)
      return
    }
    if (isFlag) {
      pos.putExtra(Extend.START_ACTIVITY_FLAGS, value as Int)
      return
    }
    when (value) {
      is String -> pos.putExtra(name, value)

      is Boolean -> pos.putExtra(name, value)

      is Int -> pos.putExtra(name, value)

      is Long -> pos.putExtra(name, value)

      is Float -> pos.putExtra(name, value)

      is Double -> pos.putExtra(name, value)

      is Byte -> pos.putExtra(name, value)

      is ByteArray -> pos.putExtra(name, value)

      is Char -> pos.putExtra(name, value)

      is CharArray -> pos.putExtra(name, value)

      is Serializable -> pos.putExtra(name, value)

      is Parcelable -> pos.putExtra(name, value)

      is CharSequence -> pos.putExtra(name, value)

      is ActivityOptionsCompat -> pos.putExtra(Extend.START_ACTIVITY_OPTIONS, value.toBundle())

      else -> pos.putAddition(name, value)
    }
  }
}