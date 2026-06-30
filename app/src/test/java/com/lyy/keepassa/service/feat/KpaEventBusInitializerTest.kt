/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.feat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 回归测试：修复 Crashlytics issue `9876295920b0ecc328c595d53a6ebfd6`。
 *
 * 根因：[com.lyy.keepassa.service.feat.KpaSdkService.preInitSdk] 调
 * `EventBus.builder().addIndex(KpaEventBusIndex()).installDefaultEventBus()`。
 * 默认 EventBus 实例只能 install 一次，若其他代码已通过 `EventBus.getDefault()` 惰性创建，
 * 再 install 会抛 `org.greenrobot.eventbus.EventBusException`。
 *
 * 修复：抽出 [KpaEventBusInitializer.installDefault]，try/catch 吞掉重复初始化异常。
 *
 * 注：EventBus 默认实例是进程级单例，测试运行后状态会泄漏到同 JVM 的其它测试，
 * 因此本测试只验证"两次调用都不抛异常"。
 */
class KpaEventBusInitializerTest {

  @Test fun installDefault_calledTwice_doesNotThrow() {
    val first = KpaEventBusInitializer.installDefault()
    val second = KpaEventBusInitializer.installDefault()

    assertTrue(first || second)
    assertFalse(first && second)
  }
}
