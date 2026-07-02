/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

/**
 * 用户拒绝权限后的冷却期组件。在 [cooldownDays] 天内不再重复弹窗打扰用户。
 *
 * 每个调用方通过自己的 [storageKey] 隔离状态(如通知权限、自动填充权限 各自独立计数)。
 */
class PermissionCooldown(
    private val storageKey: String,
    private val cooldownDays: Long = DEFAULT_COOLDOWN_DAYS,
) {

    fun isInCooldown(): Boolean {
        val rejectedAt = CommonKVStorage.getLong(storageKey, 0L)
        if (rejectedAt <= 0L) return false
        val elapsed = System.currentTimeMillis() - rejectedAt
        return elapsed in 0..(cooldownDays * DAY_MS)
    }

    fun recordRejection() {
        CommonKVStorage.put(storageKey, System.currentTimeMillis())
    }

    companion object {
        const val DEFAULT_COOLDOWN_DAYS = 7L
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
