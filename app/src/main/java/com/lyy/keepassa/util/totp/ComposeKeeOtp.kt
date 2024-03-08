package com.lyy.keepassa.util.totp

import android.net.Uri
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.EncodeUtils
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm.SHA256
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm.SHA512

object ComposeKeeOtp : IOtpCompose {
  override fun getOtpPass(entry: PwEntryV4): Pair<Int, String?> {
    // keeotp
    val keepOtp = entry.strings["otp"]
    if (keepOtp != null) {
      val uri = Uri.parse("otp://laoyuyu.me/?$keepOtp")
      val key = uri.getQueryParameter("key")
      val type = uri.getQueryParameter("type")
      val len = uri.getQueryParameter("size") ?: TokenCalculator.TOTP_DEFAULT_DIGITS.toString()
      val hashMode = uri.getQueryParameter("otpHashMode")
      val encoding = uri.getQueryParameter("encoding")
      val counter = uri.getQueryParameter("counter") ?: "0"
      val period = uri.getQueryParameter("step") ?: TokenCalculator.TOTP_DEFAULT_PERIOD.toString()

      val algorithm = when (hashMode) {
        "Sha256" -> SHA256
        "Sha512" -> SHA512
        else -> HashAlgorithm.SHA1
      }
      val seedByte = when (encoding) {
        "Base64" -> {
          EncodeUtils.base64Decode(key)
        }

        "UTF8" -> {
          key!!.toByteArray(Charsets.UTF_8)
        }

        "Hex" -> {
          // hex
          ConvertUtils.hexString2Bytes(key)
        }

        else -> {
          // base32
          Base32String.decode(key)
        }
      }

      val token = when (type) {
        "Hotp" -> {
          TokenCalculator.HOTP(seedByte, counter.toLong(), len.toInt(), algorithm)
        }

        "Steam" -> {
          TokenCalculator.TOTP_Steam(seedByte, period.toInt(), len.toInt(), algorithm)
        }

        else -> {
          // totp
          TokenCalculator.TOTP_RFC6238(seedByte, period.toInt(), len.toInt(), algorithm)
        }
      }
      return Pair(if (type == "Hotp") counter.toInt() else period.toInt(), token)
    }
    return Pair(TokenCalculator.TOTP_DEFAULT_PERIOD, null)
  }
}