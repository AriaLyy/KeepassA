/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

import android.content.Intent
import androidx.fragment.app.FragmentActivity

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/4/25
 **/
interface IOpenDbDelegate {

  fun startFlow(fragment: ChangeDbFragment)

  fun onResume()

  fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  )

  fun destroy()

}