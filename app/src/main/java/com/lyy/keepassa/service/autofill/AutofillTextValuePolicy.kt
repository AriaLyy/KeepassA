package com.lyy.keepassa.service.autofill

internal object AutofillTextValuePolicy {
  fun valueForRole(
    role: AutofillFieldRole,
    username: String,
    password: String,
    totp: String?
  ): String? {
    return when (role) {
      AutofillFieldRole.USERNAME -> username
      AutofillFieldRole.PASSWORD -> password
      AutofillFieldRole.TOTP -> totp?.takeIf { it.isNotBlank() }
    }
  }
}
