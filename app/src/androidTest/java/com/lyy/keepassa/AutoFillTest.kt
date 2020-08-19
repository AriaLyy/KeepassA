/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa

import androidx.autofill.HintConstants
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.full.memberProperties

@RunWith(AndroidJUnit4::class)
class AutoFillTest {

  @Test
  fun getMember() {
    val hintClazz = HintConstants::class
    hintClazz.memberProperties.forEach { member->
      println("${member.name} -> ${member.call()}")
    }
  }
}