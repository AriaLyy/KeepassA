/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.view.View
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogDonateBinding
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.service.play.PlayServiceUtil
import com.lyy.keepassa.util.PlayUtil
import com.lyy.keepassa.widget.DrawableTextView
import com.lyy.keepassa.widget.toPx
import com.zzhoujay.richtext.RichText

/**
 * 捐赠对话框
 */
class DonateDialog : BaseDialog<DialogDonateBinding>(), View.OnClickListener {
  override fun setLayoutId(): Int {
    return R.layout.dialog_donate
  }

  override fun initData() {
    super.initData()
    binding.rlAliPay.setOnClickListener(this)
    binding.rlPayPal.setOnClickListener(this)
    binding.rlPayPal.setOnClickListener(this)
    binding.ivClose.setOnClickListener(this)
    RichText.fromMarkdown(getString(R.string.donate_desc))
        .urlClick { url ->
          startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
          })
          return@urlClick true
        }
        .into(binding.tvDesc)

    binding.title.setDrawable(
        DrawableTextView.LEFT,
        ResUtil.getSvgIcon(R.drawable.ic_favorite_24px, R.color.text_blue_color),
        24.toPx(),
        24.toPx()
    )
    if (PlayUtil.playServiceExist(requireActivity())){
      binding.rlPlay.visibility = View.VISIBLE
    }
  }

  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.rlAliPay -> {
        startAliPay()
      }
      R.id.rlPayPal -> {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
          data = Uri.parse("https://www.paypal.com/paypalme/arialyy")
        })
      }
      R.id.rlPlay ->{
        Routerfit.create(DialogRouter::class.java).toPlayDonateDialog()
      }
    }
    dismiss()
  }

  private fun startAliPay() {
    val qrCode = "https://qr.alipay.com/fkx19330ftk0okdlwzdk968"
    if (startAliPayIntentUrl(qrCode)) {
      return
    }
    startActivity(Intent(Intent.ACTION_VIEW).apply {
      data = Uri.parse(qrCode)
    })
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