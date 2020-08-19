/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.arialyy.frame.base.BaseDialog
import com.davemorrissey.labs.subscaleview.ImageSource
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.DialogImgViewerBinding

/**
 * 图片浏览对话框
 */
class ImgViewerDialog(
  val byteArray: ByteArray
) : BaseDialog<DialogImgViewerBinding>() {
  init {
    setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog)
  }

  override fun setLayoutId(): Int {
    return R.layout.dialog_img_viewer
  }

  override fun initData() {
    super.initData()
    binding.imageView.setImage(
        ImageSource.bitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
    )
  }

  override fun dismiss() {
    super.dismiss()
    binding.imageView.recycle()
  }
}