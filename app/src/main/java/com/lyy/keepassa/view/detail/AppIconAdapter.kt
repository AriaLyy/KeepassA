package com.lyy.keepassa.view.detail

import androidx.palette.graphics.Palette
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.AppUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.imageview.ShapeableImageView
import com.lyy.keepassa.R
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.loadImg

class AppIconAdapter:BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_app_icon) {
  override fun convert(holder: BaseViewHolder, item: String) {

    holder.getView<ShapeableImageView>(R.id.ivIcon).apply {
      // val drawable = ResUtil.getDrawable(R.drawable.ic_app)
      val drawable = AppUtils.getAppIcon(item)
      loadImg(drawable)
    }
  }
}