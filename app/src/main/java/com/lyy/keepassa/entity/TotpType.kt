/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.entity

enum class TotpType(val value: String) {

  DEFAULT("default"),
  STEAM("steam"),
  CUSTOM("custom");

  companion object {
    fun from(s: String): TotpType {
      val tt = values().find { it.value == s }
      return tt ?: DEFAULT
    }
  }
}
