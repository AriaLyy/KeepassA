package com.lyy.keepassa.view.detail

import com.blankj.utilcode.util.AppUtils
import com.lyy.keepassa.base.AbsViewBindingAdapter
import com.lyy.keepassa.databinding.ItemAppIconBinding
import com.lyy.keepassa.util.loadImg

class AppIconAdapter : AbsViewBindingAdapter<String, ItemAppIconBinding>() {
  override fun bindData(binding: ItemAppIconBinding, item: String) {
    binding.ivIcon.apply {
      // val drawable = ResUtil.getDrawable(R.drawable.ic_app)
      val drawable = AppUtils.getAppIcon(item)
      loadImg(drawable)
    }
  }
}