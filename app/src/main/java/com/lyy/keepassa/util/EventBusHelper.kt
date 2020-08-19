/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import android.app.Activity
import androidx.fragment.app.Fragment
import com.lyy.keepassa.view.menu.IPopMenu
import org.greenrobot.eventbus.EventBus

object EventBusHelper {

  fun reg(activity: Activity) {
    if (!EventBus.getDefault().isRegistered(activity)) {
      EventBus.getDefault().register(activity)
    }
  }

  fun unReg(activity: Activity) {
    if (EventBus.getDefault().isRegistered(activity)) {
      EventBus.getDefault().unregister(activity)
    }
  }

  fun reg(fragment: Fragment) {
    if (!EventBus.getDefault().isRegistered(fragment)) {
      EventBus.getDefault().register(fragment)
    }
  }

  fun unReg(fragment: Fragment) {
    if (EventBus.getDefault().isRegistered(fragment)) {
      EventBus.getDefault().unregister(fragment)
    }
  }


  fun reg(popMenu: IPopMenu) {
    if (!EventBus.getDefault().isRegistered(popMenu)) {
      EventBus.getDefault().register(popMenu)
    }
  }

  fun unReg(popMenu: IPopMenu) {
    if (EventBus.getDefault().isRegistered(popMenu)) {
      EventBus.getDefault().unregister(popMenu)
    }
  }

}