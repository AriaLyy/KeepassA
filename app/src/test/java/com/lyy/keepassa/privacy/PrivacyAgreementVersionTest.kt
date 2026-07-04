/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.privacy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyAgreementVersionTest {

  @Test fun constanceDefinesCurrentPrivacyAgreementVersionAndAcceptedVersionKey() {
    val constance = File("src/main/java/com/lyy/keepassa/base/Constance.kt").readText()

    assertTrue(constance.contains("PRIVACY_AGREEMENT_VERSION"))
    assertTrue(constance.contains("PRIVACY_AGREEMENT_ACCEPTED_VERSION"))
    assertTrue(constance.contains("const val PRIVACY_AGREEMENT_VERSION = 2"))
  }

  @Test fun launcherModuleUsesVersionedConsentForShowingAndAcceptingAgreement() {
    val launcherModule = File("src/main/java/com/lyy/keepassa/view/launcher/LauncherModule.kt")
      .readText()
    val showDecision = launcherModule
      .substringAfter("private fun isNeedShowPrPrivacyAgreement()")
      .substringBefore("fun showPrivacyAgreement")
    val acceptBlock = launcherModule
      .substringAfter("override fun onEnter(v: Button)")
      .substringBefore("override fun onCancel")

    assertTrue(showDecision.contains("PrivacyAgreementConsent.shouldShow()"))
    assertFalse(showDecision.contains("KpaUtil.isChina()"))
    assertFalse(showDecision.contains("IS_AGREE_PRIVACY_AGREEMENT"))

    assertTrue(acceptBlock.contains("PrivacyAgreementConsent.acceptCurrentVersion()"))
    assertFalse(acceptBlock.contains("CommonKVStorage.put(Constance.IS_AGREE_PRIVACY_AGREEMENT, true)"))
  }

  @Test fun baseAppInitializesThirdSdkOnlyAfterCurrentPrivacyAgreementVersionIsAccepted() {
    val baseApp = File("src/main/java/com/lyy/keepassa/base/BaseApp.java").readText()

    assertTrue(baseApp.contains("PrivacyAgreementConsent.INSTANCE.isAcceptedCurrentVersion()"))
    assertFalse(baseApp.contains("getBoolean(Constance.IS_AGREE_PRIVACY_AGREEMENT"))
  }

  @Test fun privacyAgreementConsentStoresCurrentVersionAndKeepsLegacyBooleanForCompatibility() {
    val consent = File("src/main/java/com/lyy/keepassa/util/PrivacyAgreementConsent.kt").readText()

    assertTrue(consent.contains("getInt(Constance.PRIVACY_AGREEMENT_ACCEPTED_VERSION, 0)"))
    assertTrue(consent.contains("CommonKVStorage.put("))
    assertTrue(consent.contains("Constance.PRIVACY_AGREEMENT_ACCEPTED_VERSION"))
    assertTrue(consent.contains("Constance.PRIVACY_AGREEMENT_VERSION"))
    assertTrue(consent.contains("CommonKVStorage.put(Constance.IS_AGREE_PRIVACY_AGREEMENT, true)"))
  }
}
