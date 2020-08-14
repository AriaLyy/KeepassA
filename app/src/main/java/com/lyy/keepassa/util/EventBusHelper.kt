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