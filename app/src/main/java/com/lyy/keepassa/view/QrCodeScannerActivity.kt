/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view

import android.os.Bundle
import android.view.KeyEvent
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivityQrCodeScannerBinding

/**
 * @Author laoyuyu
 * @Description
 * @Date 2022/1/11
 **/
internal class QrCodeScannerActivity : BaseActivity<ActivityQrCodeScannerBinding>() {
  private lateinit var capture: KpaCaptureManager

  override fun setLayoutId(): Int {
    return R.layout.activity_qr_code_scanner
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    capture = KpaCaptureManager(this, binding.barcodeView)
    capture.initializeFromIntent(this.intent, savedInstanceState)
    capture.decode()
  }

  override fun onResume() {
    super.onResume()
    capture.onResume()
  }

  override fun onPause() {
    super.onPause()
    capture.onPause()
  }

  override fun onDestroy() {
    super.onDestroy()
    capture.onDestroy()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    capture.onSaveInstanceState(outState)
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    capture.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    return capture.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
  }
}