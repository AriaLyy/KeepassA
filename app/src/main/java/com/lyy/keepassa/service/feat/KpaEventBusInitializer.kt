/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.feat

import com.lyy.keepassa.KpaEventBusIndex
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.EventBusException
import timber.log.Timber

/**
 * 安装 EventBus 默认实例，吞掉重复初始化抛出的 [EventBusException]。
 *
 * 修复 Crashlytics issue `9876295920b0ecc328c595d53a6ebfd6`:
 * 若默认实例已被其他模块/进程惰性创建，再次调 `installDefaultEventBus` 会抛
 * `Default instance already exists`。多进程或多次 SDK 初始化场景下必现。
 */
internal object KpaEventBusInitializer {

  /**
   * 注册 [KpaEventBusIndex] 到默认 EventBus 实例。
   *
   * @return true 表示本次成功安装；false 表示默认实例已存在，安全跳过。
   */
  fun installDefault(): Boolean {
    return try {
      EventBus.builder().addIndex(KpaEventBusIndex()).installDefaultEventBus()
      true
    } catch (e: EventBusException) {
      Timber.w(e, "Default EventBus already exists, skip installing index")
      false
    }
  }
}
