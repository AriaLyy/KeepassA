/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.dialog

import android.view.View
import androidx.annotation.Keep

/**
 * @Author laoyuyu
 * @Description
 * @Date 10:35 AM 2023/10/26
 **/
@Keep
interface DialogBtnClicker {
  fun onEnter(v: View){}

  fun onCancel(v: View){}
}