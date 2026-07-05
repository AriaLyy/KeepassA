package com.lyy.keepassa.service.autofill

internal object AutofillSaveFieldPolicy {
  fun shouldSaveAsPassword(isPassword: Boolean, isTotp: Boolean): Boolean {
    return isPassword && !isTotp
  }

  fun shouldSaveAsUsername(isPassword: Boolean, isTotp: Boolean): Boolean {
    return !isPassword && !isTotp
  }
}
