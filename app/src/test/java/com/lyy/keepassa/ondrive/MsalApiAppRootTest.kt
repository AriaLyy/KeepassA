/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.ondrive

import com.lyy.keepassa.util.cloud.OneDriveUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.GET

class MsalApiAppRootTest {

  @Test
  fun api_hasEndpointForAppRootFolderInitialization() {
    val appRootGetPaths = MsalApi::class.java.declaredMethods
      .mapNotNull { method -> method.getAnnotation(GET::class.java)?.value }
      .filter { path -> path.endsWith("/drive/special/${OneDriveUtil.APP_ROOT_DIR}") }

    assertEquals(
      listOf("users/{userId}/drive/special/${OneDriveUtil.APP_ROOT_DIR}"),
      appRootGetPaths
    )
  }
}
