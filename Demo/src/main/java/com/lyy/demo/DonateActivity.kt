/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.demo

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import com.arialyy.frame.core.AbsActivity
import com.lyy.demo.databinding.ActivityDonateBinding

/**
 * 参考地址：https://github.com/CankingApp/MiniPay/blob/master/minipay/src/main/java/com/canking/minipay/ZhiActivity.java
 */
class DonateActivity : AbsActivity<ActivityDonateBinding>() {
  override fun setLayoutId(): Int {
    return R.layout.activity_donate
  }

  fun onClick(view: View) {
    when (view.id) {
      R.id.aliPay -> {
//        startAliPayIntentUrl("https://qr.alipay.com/FKX016770URBZGZSR37U37")
        startAliPayIntentUrl("https://qr.alipay.com/fkx19330ftk0okdlwzdk968")
      }
      R.id.wxPay -> {

      }
    }
  }

  override fun onRestart() {
    super.onRestart()
    Log.d(TAG, "B onRestart")
  }

  override fun onStart() {
    super.onStart()
    Log.d(TAG, "B onStart")
  }

  override fun onResume() {
    super.onResume()
    Log.d(TAG, "B onResume")
  }

  override fun onPause() {
    super.onPause()
    Log.d(TAG, "B onPause")
  }

  override fun onStop() {
    super.onStop()
    Log.d(TAG, "B onStop")
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.d(TAG, "B onDestroy")
  }

  /**
   * 打开 Intent Scheme Url
   *
   * @param qrCodeUrl Intent 跳转地址
   * @return 是否成功调用
   */
  private fun startAliPayIntentUrl(
    qrCodeUrl: String
  ): Boolean {
    return try {
      val cn = ComponentName(
          "com.eg.android.AlipayGphone",
          "com.alipay.mobile.quinox.SchemeLauncherActivity"
      )
      val intent = Intent(Intent.ACTION_VIEW).apply {
        component = cn
        data =
          Uri.parse("alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=${qrCodeUrl}?_s=web-other&_t=${System.currentTimeMillis()}")
      }
      startActivity(intent)
      true
    } catch (e: ActivityNotFoundException) {
      false
    }
  }
}