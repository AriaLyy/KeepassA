/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main.chain

import android.os.Build
import android.text.Html
import android.view.autofill.AutofillManager
import android.widget.Button
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.KeyConstance
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.service.autofill.ChromeAutofillPromptPolicy
import com.lyy.keepassa.service.autofill.ChromeAutofillSupport
import com.lyy.keepassa.util.PermissionCooldown
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import timber.log.Timber

class ChromeAutofillPermissionsChain : IMainDialogInterceptor {

  private val cooldown =
    PermissionCooldown(KeyConstance.KEY_CHROME_AUTOFILL_PERMISSION_REJECTED_AT)

  override fun intercept(chain: DialogChain): MainDialogResponse {
    Timber.d("ChromeAutofillPermissionsChain")
    val ac = chain.activity
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return chain.proceed(ac)
    }

    val am = ac.getSystemService(AutofillManager::class.java)
    val isKeepassAutofillEnabled = isKeepassAutofillEnabled(am)
    val chromeState = ChromeAutofillSupport.thirdPartyModeState(ac)
    if (!ChromeAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = isKeepassAutofillEnabled,
        chromeState = chromeState,
        isInCooldown = cooldown.isInCooldown(),
        sdkInt = Build.VERSION.SDK_INT
      )
    ) {
      return chain.proceed(ac)
    }

    val msg = Html.fromHtml(ResUtil.getString(R.string.hint_chrome_autofill_third_party_mode))
    Routerfit.create(DialogRouter::class.java).showMsgDialog(
      msgContent = msg,
      showCancelBt = true,
      cancelText = ResUtil.getString(R.string.cancel),
      enterText = ResUtil.getString(R.string.open_setting),
      btnClickListener = object : OnMsgBtClickListener {
        override fun onEnter(v: Button) {
          ChromeAutofillSupport.openSettings(ac)
        }

        override fun onCancel(v: Button) {
          cooldown.recordRejection()
        }
      }
    )
    return MainDialogResponse(MainDialogResponse.RESPONSE_OK)
  }

  private fun isKeepassAutofillEnabled(am: AutofillManager?): Boolean {
    return try {
      am?.isAutofillSupported == true && am.hasEnabledAutofillServices()
    } catch (e: Throwable) {
      Timber.e(e, "check KeepassA autofill service failed")
      false
    }
  }
}
