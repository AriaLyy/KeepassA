package com.lyy.keepassa.util.totp

import com.blankj.utilcode.util.ConvertUtils
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.entity.HmacOtpBean
import com.lyy.keepassa.entity.KeepassBean
import com.lyy.keepassa.entity.TimeOtp2Bean
import com.lyy.keepassa.util.getKeepassBean
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm

object ComposeKeepass : IOtpCompose {

  const val TimeOtp_Secret = "TimeOtp-Secret"
  const val TimeOtp_Length = "TimeOtp-Length"
  const val TimeOtp_Algorithm = "TimeOtp-Algorithm"
  const val TimeOtp_Period = "TimeOtp-Period"
  const val TimeOtp_Secret_Hex = "TimeOtp-Secret-Hex"
  const val TimeOtp_Secret_Base32 = "TimeOtp-Secret-Base32"
  const val TimeOtp_Secret_Base64 = "TimeOtp-Secret-Base64"
  const val HMAC_SHA_1 = "HMAC-SHA-1"
  const val HMAC_SHA_256 = "HMAC-SHA-256"
  const val HMAC_SHA_512 = "HMAC-SHA-512"

  //hOtp
  const val HmacOtp_Counter = "HmacOtp-Counter"
  const val HmacOtp_Secret = "HmacOtp-Secret"
  const val HmacOtp_Algorithm = "HmacOtp-Algorithm"
  const val HmacOtp_Secret_Hex = "HmacOtp-Secret-Hex"
  const val HmacOtp_Secret_Base32 = "HmacOtp-Secret-Base32"
  const val HmacOtp_Secret_Base64 = "HmacOtp-Secret-Base64"
  const val HmacOtp_Length = "HmacOtp-Length"

  override fun getOtpPass(entry: PwEntryV4): Pair<Int, String?> {
    val bean = entry.getKeepassBean()
    if (bean.hmac != null) return handleHotp(bean.hmac)

    if (bean.otpBean != null) return handleTotp(bean.otpBean)
    return Pair(-1, null)
  }

  private fun handleHotp(hmacBean: HmacOtpBean): Pair<Int, String?> {
    val secret = when (hmacBean.secretType) {
      SecretHexType.BASE_32 -> {
        Base32String.decode(hmacBean.secret)
      }

      SecretHexType.BASE_64 -> {
        Base32String.decode(hmacBean.secret)
      }

      SecretHexType.HEX -> {
        ConvertUtils.hexString2Bytes(hmacBean.secret)
      }

      else -> {
        hmacBean.secret.toByteArray(Charsets.UTF_8)
      }
    }

    return Pair(
      hmacBean.len,
      TokenCalculator.HOTP(secret, hmacBean.counter.toLong(), hmacBean.len, hmacBean.algorithm)
    )
  }

  private fun handleTotp(otpBean: TimeOtp2Bean): Pair<Int, String?> {
    val secret = when (otpBean.secretType) {
      SecretHexType.BASE_32 -> {
        Base32String.decode(otpBean.secret)
      }

      SecretHexType.BASE_64 -> {
        Base32String.decode(otpBean.secret)
      }

      SecretHexType.HEX -> {
        ConvertUtils.hexString2Bytes(otpBean.secret)
      }

      else -> {
        otpBean.secret.toByteArray(Charsets.UTF_8)
      }
    }

    val token = TokenCalculator.TOTP_RFC6238(
      secret,
      otpBean.period,
      otpBean.digits,
      otpBean.algorithm
    )
    return Pair(otpBean.digits, token)
  }

  fun getSecretType(secretType: SecretHexType): String {
    return when (secretType) {
      SecretHexType.UTF_8 -> TimeOtp_Secret
      SecretHexType.HEX -> TimeOtp_Secret_Hex
      SecretHexType.BASE_32 -> TimeOtp_Secret_Base32
      SecretHexType.BASE_64 -> TimeOtp_Secret_Base64
    }
  }


}