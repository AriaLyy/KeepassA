package com.lyy.keepassa.view.main.chain

import com.arialyy.frame.router.Routerfit
import com.lyy.keepassa.base.KeyConstance
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.CommonKVStorage
import org.joda.time.DateTime
import org.joda.time.Days
import timber.log.Timber
import kotlin.math.abs

/**
 * @Author laoyuyu
 * @Description
 * @Date 4:17 PM 2023/5/10
 **/
class TipChain : IMainDialogInterceptor {
  override fun intercept(chain: DialogChain): MainDialogResponse {

    val dontShowTip = CommonKVStorage.getBoolean(KeyConstance.KEY_DONT_SHOW_TIP, false)
    val lastStartTime = CommonKVStorage.getLong(KeyConstance.KEY_LAST_TIP_START_TIME)
    val diffDay = abs(Days.daysBetween(DateTime.now(), DateTime(lastStartTime)).days)
    Timber.d("TipChain, dontShowTip = $dontShowTip, lastStartTime = $lastStartTime, diffDay = $diffDay")

    if (!dontShowTip && diffDay >= 1) {
      CommonKVStorage.put(KeyConstance.KEY_LAST_TIP_START_TIME, System.currentTimeMillis())
      Routerfit.create(DialogRouter::class.java).showTipDialog()
    }

    return MainDialogResponse(MainDialogResponse.RESPONSE_OK)
  }
}