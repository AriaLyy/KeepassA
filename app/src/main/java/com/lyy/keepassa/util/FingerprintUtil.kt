/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Build.VERSION
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.Nullable
import androidx.biometric.BiometricManager
import timber.log.Timber
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * 指纹工具
 *
 */
object FingerprintUtil {

  /**
   * 是否支持生物识别：指纹，面部
   * @return true 支持，false 硬件不支持，或者用户没有设置指纹
   */
  fun hasBiometricPrompt(context: Context): Boolean {
    if (VERSION.SDK_INT <= Build.VERSION_CODES.M) {
      return false
    }
    val biometricManager = BiometricManager.from(context)
    var can = false
    when (biometricManager.canAuthenticate()) {
      BiometricManager.BIOMETRIC_SUCCESS -> {
        Timber.d("App can authenticate using biometrics.")
        can = true
      }

      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
        Timber.e("No biometric features available on this device.")
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
        Timber.e("Biometric features are currently unavailable.")
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
        Timber.e("The user hasn't associated any biometric credentials with their account.")
    }
    return can
  }


  /**
   * 生成密钥对
   */
  @TargetApi(Build.VERSION_CODES.M)
  @Throws(Exception::class) fun generateKeyPair(
    keyName: String,
    invalidatedByBiometricEnrollment: Boolean
  ): KeyPair? {
    val keyPairGenerator: KeyPairGenerator =
      KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
    val builder: KeyGenParameterSpec.Builder = KeyGenParameterSpec.Builder(
        keyName,
        KeyProperties.PURPOSE_SIGN
    )
        .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
        .setDigests(
            KeyProperties.DIGEST_SHA256,
            KeyProperties.DIGEST_SHA384,
            KeyProperties.DIGEST_SHA512
        ) // Require the user to authenticate with a biometric to authorize every use of the key
        .setUserAuthenticationRequired(true)

    // Generated keys will be invalidated if the biometric templates are added more to user device
    if (Build.VERSION.SDK_INT >= 24) {
      builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment)
    }
    keyPairGenerator.initialize(builder.build())
    return keyPairGenerator.generateKeyPair()
  }

  /**
   * 获取密钥对
   */
  @TargetApi(Build.VERSION_CODES.M)
  @Throws(java.lang.Exception::class)
  fun getKeyPair(keyName: String): KeyPair? {
    val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)
    if (keyStore.containsAlias(keyName)) {
      // Get public key
      val publicKey: PublicKey = keyStore.getCertificate(keyName).publicKey
      // Get private key
      val privateKey: PrivateKey = keyStore.getKey(keyName, null) as PrivateKey
      // Return a key pair
      return KeyPair(publicKey, privateKey)
    }
    return null
  }

  @TargetApi(Build.VERSION_CODES.M)
  @Nullable @Throws(java.lang.Exception::class)
  fun initSignature(keyName: String): Signature? {
    val keyPair = getKeyPair(keyName)
    if (keyPair != null) {
      val signature: Signature = Signature.getInstance("SHA256withECDSA")
      signature.initSign(keyPair.private)
      return signature
    }
    return null
  }

}