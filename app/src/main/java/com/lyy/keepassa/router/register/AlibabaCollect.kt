/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.router.register

import com.alibaba.android.arouter.facade.template.IInterceptorGroup
import com.alibaba.android.arouter.facade.template.IProviderGroup
import com.alibaba.android.arouter.facade.template.IRouteRoot
import com.flyjingfish.android_aop_annotation.anno.AndroidAopCollectMethod
import com.flyjingfish.android_aop_annotation.enums.CollectType
import timber.log.Timber

object AlibabaCollect {
  private val classNameSet = mutableSetOf<String>()

  @AndroidAopCollectMethod(
    regex = "com.alibaba.android.arouter.routes.*?",
    collectType = CollectType.DIRECT_EXTENDS
  )
  @JvmStatic
  fun collectIRouteRoot(sub: Class<out IRouteRoot>) {
    Timber.e("collectIRouteRoot=$sub")
    classNameSet.add(sub.name)
  }

  @AndroidAopCollectMethod(
    regex = "com.alibaba.android.arouter.routes.*?",
    collectType = CollectType.DIRECT_EXTENDS
  )
  @JvmStatic
  fun collectIProviderGroup(sub: Class<out IProviderGroup>) {
    Timber.e("collectIProviderGroup=$sub")
    classNameSet.add(sub.name)
  }

  @AndroidAopCollectMethod(
    regex = "com.alibaba.android.arouter.routes.*?",
    collectType = CollectType.DIRECT_EXTENDS
  )
  @JvmStatic
  fun collectIInterceptorGroup(sub: Class<out IInterceptorGroup>) {
    Timber.e("collectIInterceptorGroup=$sub")
    classNameSet.add(sub.name)
  }

  fun getClassNameSet(): MutableSet<String> {
    return classNameSet
  }
}