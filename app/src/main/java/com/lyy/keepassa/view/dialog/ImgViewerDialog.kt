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