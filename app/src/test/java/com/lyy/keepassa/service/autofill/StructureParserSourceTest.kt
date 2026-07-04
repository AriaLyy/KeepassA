/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class StructureParserSourceTest {

  @Test fun browserNativeEditTextClassificationSkipsSearchOrUrlFields() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt").readText()

    assertTrue(
      "Browser native EditText classification must not treat address/search fields as username fields.",
      source.contains(
        "browserStrategy.shouldClassifyNativeEditTextVirtualNodes &&\n" +
          "        classIsEditText(className) &&\n" +
          "        !isLikelySearchOrUrlField(viewNode)"
      )
    )
  }
}
