package com.lyy.keepassa.view.dialog

import android.content.Intent
import android.net.Uri
import android.view.View
import com.arialyy.frame.base.BaseDialog
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.DialogDonateBinding
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
    binding.ivClose.setOnClickListener(this)
    RichText.fromMarkdown(getString(R.string.donate_desc))
        .urlClick { url ->
          startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
          })
          return@urlClick true
        }
        .into(binding.tvDesc)
  }

  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.rlAliPay -> {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
          data = Uri.parse("https://qr.alipay.com/fkx19330ftk0okdlwzdk968")
        })
      }
      R.id.rlPayPal -> {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
          data = Uri.parse("https://www.paypal.com/paypalme/arialyy")
        })
      }
    }
    dismiss()
  }
}