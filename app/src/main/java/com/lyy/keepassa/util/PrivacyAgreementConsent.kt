/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.util

import com.lyy.keepassa.base.Constance

object PrivacyAgreementConsent {

  fun shouldShow(): Boolean = !isAcceptedCurrentVersion()

  fun isAcceptedCurrentVersion(): Boolean {
    return CommonKVStorage.getInt(Constance.PRIVACY_AGREEMENT_ACCEPTED_VERSION, 0) >=
      Constance.PRIVACY_AGREEMENT_VERSION
  }

  fun acceptCurrentVersion() {
    CommonKVStorage.put(Constance.IS_AGREE_PRIVACY_AGREEMENT, true)
    CommonKVStorage.put(
      Constance.PRIVACY_AGREEMENT_ACCEPTED_VERSION,
      Constance.PRIVACY_AGREEMENT_VERSION
    )
  }
}
