/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.service.autofill

/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale

object PackageVerifier {
  val TAG = javaClass.simpleName

  /**
   * Verifies if a package is valid by matching its certificate with the previously stored
   * certificate.
   */
  fun isValidPackage(
    context: Context,
    packageName: String
  ): Boolean {
    val hash: String
    try {
      hash = getCertificateHash(context, packageName)
      Timber.d( "Hash for $packageName: $hash")
    } catch (e: Exception) {
      e.printStackTrace()
      Timber.w("Error getting hash for $packageName: $e")
      return false
    }

    return verifyHash(context, packageName, hash)
  }

  @SuppressLint("PackageManagerGetSignatures")
  private fun getCertificateHash(
    context: Context,
    packageName: String
  ): String {
    val pm = context.packageManager
    val signatures = if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
      pm.getPackageInfo(
        packageName,
        PackageManager.GET_SIGNING_CERTIFICATES
      ).signingInfo.apkContentsSigners
    } else {
      pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
    }

    val cert = signatures[0].toByteArray()
    ByteArrayInputStream(cert).use { input ->
      val factory = CertificateFactory.getInstance("X509")
      val x509 = factory.generateCertificate(input) as X509Certificate
      val md = MessageDigest.getInstance("SHA256")
      val publicKey = md.digest(x509.encoded)
      return toHexFormat(publicKey)
    }
  }

  private fun toHexFormat(bytes: ByteArray): String {
    val builder = StringBuilder(bytes.size * 2)
    for (i in bytes.indices) {
      var hex = Integer.toHexString(bytes[i].toInt())
      val length = hex.length
      if (length == 1) {
        hex = "0$hex"
      }
      if (length > 2) {
        hex = hex.substring(length - 2, length)
      }
      builder.append(hex.toUpperCase(Locale.ROOT))
      if (i < bytes.size - 1) {
        builder.append(':')
      }
    }
    return builder.toString()
  }

  private fun verifyHash(
    context: Context,
    packageName: String,
    hash: String
  ): Boolean {
    val prefs = context.applicationContext.getSharedPreferences(
      "package-hashes", Context.MODE_PRIVATE
    )
    if (!prefs.contains(packageName)) {
      Timber.d( "Creating intial hash for $packageName")
      prefs.edit()
        .putString(packageName, hash)
        .apply()
      return true
    }

    val existingHash = prefs.getString(packageName, null)
    if (hash != existingHash) {
      Timber.w("hash mismatch for ${packageName}: expected ${existingHash}, got  $hash")
      return false
    }
    return true
  }
}