/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/4/25
 **/
interface IOpenDbDelegate : LifecycleObserver {

  fun startFlow(fragment: ChangeDbFragment)

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
  fun onResume()

  fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  )

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  fun destroy()

}