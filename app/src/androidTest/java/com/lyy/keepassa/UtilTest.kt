/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa

import com.arialyy.frame.util.KeyStoreUtil
import com.blankj.utilcode.util.EncryptUtils
import com.lyy.keepassa.view.StorageType
import org.junit.Test

class UtilTest {

  @Test
  fun emunTEst(){
    println(StorageType.AFS.name)
  }

  @Test
  fun keyStoreUtil(){
    val keyStoreUtil = KeyStoreUtil()

    val p = keyStoreUtil.encryptData(keyStoreUtil.getEncryptCipher(), "123456")
    println("密文： ${p.first}")
    val end = keyStoreUtil.decryptData(keyStoreUtil.getDecryptCipher(p.second), p.first)
    println("明文：$end")

  }
}