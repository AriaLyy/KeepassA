/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.util

/**
 * 域名匹配规则
 *
 * 项目自有规则,替换 KeepassLib Frame 中带 TLD 白名单的旧规则。
 * 旧规则 `(\w*\.?){1}\.(com|net|org|...)$` 缺 .club/.dev/.io/.app 等新 TLD,
 * 不匹配时 `find()?.value.toString()` 会得到字符串 "null",导致条目匹配全部失败。
 *
 * 本规则不限制 TLD,结尾取 `[\w-]{2,}` 即可支持所有域名(含国际化新 gTLD)。
 *
 * 用法:
 * ```
 * val top = Regex(RegularRule.DOMAIN_TOP, RegexOption.IGNORE_CASE).find(url)?.value
 * ```
 */
object RegularRule {

  /**
   * 一级域名,如 `example.com`、`ubits.club`
   */
  const val DOMAIN_TOP = "([\\w-]+\\.){1}[\\w-]{2,}$"

  /**
   * 二级域名,如 `sub.example.com`、`login.ubits.club`
   */
  const val DOMAIN_SECOND = "([\\w-]+\\.){2}[\\w-]{2,}$"

  /**
   * 三级域名,如 `a.sub.example.com`
   */
  const val DOMAIN_THIRD = "([\\w-]+\\.){3}[\\w-]{2,}$"
}
