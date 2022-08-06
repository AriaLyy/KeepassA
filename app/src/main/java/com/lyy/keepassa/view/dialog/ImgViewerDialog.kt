/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.graphics.BitmapFactory
import androidx.fragment.app.DialogFragment
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.ToastUtils
import com.davemorrissey.labs.subscaleview.ImageSource
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogImgViewerBinding
import com.lyy.keepassa.util.isInvalid

/**
 * 图片浏览对话框
 */
@Route(path = "/dialog/imgViewer")
class ImgViewerDialog() : BaseDialog<DialogImgViewerBinding>() {
  init {
    setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog)
  }

  @Autowired(name = "imgByteArray")
  lateinit var imgByteArray: ByteArray

  override fun setLayoutId(): Int {
    return R.layout.dialog_img_viewer
  }

  override fun initData() {
    super.initData()
    ARouter.getInstance().inject(this)
    binding.dialog = this
    val bm = BitmapFactory.decodeByteArray(imgByteArray, 0, imgByteArray.size)
    if (bm.isInvalid()){
      ToastUtils.showLong(ResUtil.getString(R.string.invalid_img))
      dismiss()
      return
    }
    binding.imageView.setImage(
      ImageSource.bitmap(bm)
    )
  }

  override fun dismiss() {
    super.dismiss()
    binding.imageView.recycle()
  }
}