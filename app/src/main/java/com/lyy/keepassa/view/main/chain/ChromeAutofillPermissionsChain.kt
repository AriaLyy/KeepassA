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
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.service.autofill.BrowserThirdPartyAutofillIntegration
import com.lyy.keepassa.service.autofill.BrowserThirdPartyAutofillPromptPolicy
import com.lyy.keepassa.service.autofill.BrowserThirdPartyAutofillSupport
import com.lyy.keepassa.service.autofill.KeepassAutofillServiceStatus
import com.lyy.keepassa.util.PermissionCooldown
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import timber.log.Timber

class BrowserAutofillPermissionsChain : IMainDialogInterceptor {

  private val cooldowns = BrowserThirdPartyAutofillSupport.integrations.associate {
    it.packageName to PermissionCooldown(it.cooldownStorageKey)
  }

  override fun intercept(chain: DialogChain): MainDialogResponse {
    Timber.d("BrowserAutofillPermissionsChain")
    val ac = chain.activity
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return chain.proceed(ac)
    }

    val am = ac.getSystemService(AutofillManager::class.java)
    val isKeepassAutofillEnabled = KeepassAutofillServiceStatus.isEnabled(ac, am)
    val promptableIntegration = BrowserThirdPartyAutofillSupport.integrations.firstOrNull {
      val cooldown = cooldownFor(it)
      BrowserThirdPartyAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = isKeepassAutofillEnabled,
        browserState = BrowserThirdPartyAutofillSupport.thirdPartyModeState(ac, it),
        isInCooldown = cooldown.isInCooldown(),
        sdkInt = Build.VERSION.SDK_INT
      )
    }
    if (promptableIntegration == null) {
      return chain.proceed(ac)
    }

    val msg = Html.fromHtml(
      ac.getString(
        R.string.hint_browser_third_party_autofill_mode,
        promptableIntegration.displayName
      )
    )
    Routerfit.create(DialogRouter::class.java).showMsgDialog(
      msgContent = msg,
      showCancelBt = true,
      cancelText = ResUtil.getString(R.string.cancel),
      enterText = ResUtil.getString(R.string.open_setting),
      btnClickListener = object : OnMsgBtClickListener {
        override fun onEnter(v: Button) {
          BrowserThirdPartyAutofillSupport.openSettings(ac, promptableIntegration)
        }

        override fun onCancel(v: Button) {
          cooldownFor(promptableIntegration).recordRejection()
        }
      }
    )
    return MainDialogResponse(MainDialogResponse.RESPONSE_OK)
  }
  private fun cooldownFor(integration: BrowserThirdPartyAutofillIntegration): PermissionCooldown {
    return cooldowns[integration.packageName] ?: PermissionCooldown(integration.cooldownStorageKey)
  }
}

class ChromeAutofillPermissionsChain : IMainDialogInterceptor {
  private val delegate = BrowserAutofillPermissionsChain()

  override fun intercept(chain: DialogChain): MainDialogResponse {
    return delegate.intercept(chain)
  }
}
