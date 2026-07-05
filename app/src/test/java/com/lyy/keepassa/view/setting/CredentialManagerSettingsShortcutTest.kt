/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.setting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialManagerSettingsShortcutTest {

  @Test fun api35AndAboveTryCredentialProviderSettingsBeforeFallbacks() {
    val candidates = CredentialManagerSettingsShortcut.candidateActions(sdkInt = 35)

    assertEquals("android.settings.CREDENTIAL_PROVIDER", candidates[0].action)
    assertTrue(candidates[0].isDirectCredentialProvider)
    assertEquals("android.settings.SECURITY_SETTINGS", candidates[1].action)
    assertEquals("android.settings.SETTINGS", candidates[2].action)
  }

  @Test fun lowerApiLevelsStillTryLiteralCredentialProviderActionBeforeFallbacks() {
    val candidates = CredentialManagerSettingsShortcut.candidateActions(sdkInt = 34)

    assertEquals("android.settings.CREDENTIAL_PROVIDER", candidates[0].action)
    assertTrue(candidates[0].isDirectCredentialProvider)
    assertEquals("android.settings.SECURITY_SETTINGS", candidates[1].action)
    assertEquals("android.settings.SETTINGS", candidates[2].action)
  }

  @Test fun candidateActionsDoNotContainDuplicates() {
    val actions = CredentialManagerSettingsShortcut.candidateActions(sdkInt = 35)
      .map { it.action }

    assertEquals(actions.distinct(), actions)
  }
}
