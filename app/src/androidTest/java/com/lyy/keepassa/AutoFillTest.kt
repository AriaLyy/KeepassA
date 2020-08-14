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