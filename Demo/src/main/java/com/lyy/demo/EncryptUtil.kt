/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.demo

import com.google.crypto.tink.Aead
import com.google.crypto.tink.DeterministicAead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.config.TinkConfig

/**
 * 加密工具
 * https://developer.android.com/topic/security/data
 */
object EncryptUtil {
  var AEAD: Aead? = null

  /**
   *
   * 加密字符串，并将其保存到配置文件中
   *
   * @param str 明文
   * @param pass 密钥，一般是放在服务器端，或放在keystore
   * @return 密文
   */
  fun encryptStr(
    str: String, pass:String
  ): String? {
    val temp = getAead()?.encrypt(
        str.toByteArray(Charsets.UTF_8),
        pass.toByteArray(Charsets.UTF_8)
    )
    if (temp != null) {
      return String(temp, Charsets.UTF_8)
    }
    return null
  }

  /**
   * 解密字符串
   *
   * @param str 密文
   *  @param pass 密钥，一般是放在服务器端，或放在keystore
   * @return 明文
   */
  fun decryptionStr(str: String, pass: String): String? {
    val temp = getAead()?.decrypt(
        str.toByteArray(Charsets.UTF_8),
        pass.toByteArray(Charsets.UTF_8)
    )
    if (temp != null) {
      return String(temp, Charsets.UTF_8)
    }
    return null
  }

  /**
   * @param dbPass keepass数据库密码
   */
  fun getAead(): Aead? {
    if (AEAD == null) {
      TinkConfig.register()
      // 生成密钥
      val keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES256_GCM)
      AEAD = keysetHandle.getPrimitive(Aead::class.java)
    }
    return AEAD
  }

}