package com.lyy.keepassa.service.autofill

import android.util.Pair
import java.util.Locale

internal object AutofillTotpFieldPolicy {
  const val AUTOFILL_HINT_TOTP = "keepassa:totp"

  private val chineseTokens = listOf("验证码", "动态码", "一次性密码", "二次验证", "两步验证")
  private val strongNormalizedTokens = listOf(
    "otp",
    "totp",
    "2fa",
    "mfa",
    "onetimecode",
    "authenticator",
    "twostep",
    "twofactor"
  )
  private val codeQualifiers = listOf(
    "verification",
    "auth",
    "authentication",
    "twofactor",
    "secondfactor"
  )

  fun isTotpField(
    autofillHints: Array<String>?,
    idEntry: String?,
    hint: CharSequence?,
    htmlAttributes: List<Pair<String, String>>?
  ): Boolean {
    val tokens = ArrayList<String>()
    autofillHints?.forEach(tokens::add)
    idEntry?.let(tokens::add)
    hint?.toString()?.let(tokens::add)
    htmlAttributes?.forEach { attribute ->
      attribute.first?.let(tokens::add)
      attribute.second?.let(tokens::add)
    }
    return tokens.any(::isTotpToken)
  }

  fun isTotpToken(value: CharSequence?): Boolean {
    val raw = value?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    val lower = raw.lowercase(Locale.ROOT)
    if (chineseTokens.any { lower.contains(it) }) {
      return true
    }

    val normalized = lower.replace(Regex("[^a-z0-9]"), "")
    if (normalized.isEmpty()) {
      return false
    }
    if (strongNormalizedTokens.any { normalized == it || normalized.contains(it) }) {
      return true
    }
    return normalized.contains("code") && codeQualifiers.any { normalized.contains(it) }
  }
}
