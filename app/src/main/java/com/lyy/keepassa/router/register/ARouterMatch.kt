/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.router.register

import com.alibaba.android.arouter.core.LogisticsCenter
import com.flyjingfish.android_aop_annotation.ProceedJoinPoint
import com.flyjingfish.android_aop_annotation.anno.AndroidAopMatchClassMethod
import com.flyjingfish.android_aop_annotation.base.MatchClassMethod
import com.flyjingfish.android_aop_annotation.enums.MatchType
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 19:17 2025/7/29
 **/
@AndroidAopMatchClassMethod(
  targetClassName = "com.alibaba.android.arouter.core.LogisticsCenter",
  methodName = ["loadRouterMap"],
  type = MatchType.SELF
)
class ARouterMatch : MatchClassMethod {
  override fun invoke(joinPoint: ProceedJoinPoint, methodName: String): Any? {
    val any = joinPoint.proceed()
    val registerMethod =
      LogisticsCenter::class.java.getDeclaredMethod("register", java.lang.String::class.java)
    registerMethod.isAccessible = true
    val classNameSet = AlibabaCollect.getClassNameSet()
    classNameSet.forEach {
      registerMethod.invoke(null, it)
      Timber.e("registerMethod=$it")
    }
    return any
  }
}