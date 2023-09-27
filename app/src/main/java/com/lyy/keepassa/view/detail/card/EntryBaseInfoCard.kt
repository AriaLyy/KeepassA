package com.lyy.keepassa.view.detail.card

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.LayoutEntryCardBaseInfoBinding
import com.lyy.keepassa.util.ClipboardUtil
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.doClick
import com.lyy.keepassa.widget.BubbleTextView
import com.lyy.keepassa.widget.BubbleTextView.OnIconClickListener

/**
 * @Author laoyuyu
 * @Description
 * @Date 10:19 AM 2023/9/26
 **/
class EntryBaseInfoCard(context: Context, attributeSet: AttributeSet) :
  MaterialCardView(context, attributeSet) {
  private val binding =
    LayoutEntryCardBaseInfoBinding.inflate(LayoutInflater.from(context), this, true)

  fun bindData(entry: PwEntryV4) {
    val userName = KdbUtil.getUserName(entry)
    binding.tvUserName.text = userName
    binding.tvUserName.doClick {
      ClipboardUtil.get()
        .copyDataToClip(userName)
      HitUtil.toaskShort(context.getString(R.string.hint_copy_user))
    }
    handlePass(entry)
    if (entry.url.isBlank()) {
      binding.tvUrl.visibility = GONE
    } else {
      binding.tvUrl.visibility = VISIBLE
      binding.tvUrl.text = entry.url
      binding.tvUrl.doClick {
        KpaUtil.openUrlWithBrowser(entry.url)
      }
    }
  }

  private fun handlePass(entry: PwEntryV4) {
    val pass = KdbUtil.getPassword(entry)
    binding.tvPass.text = pass
    binding.tvPass.isSelected = true
    binding.tvPass.setOnIconClickListener(object : OnIconClickListener {
      override fun onClick(view: BubbleTextView, index: Int) {
        if (index != BubbleTextView.LOCATION_RIGHT) {
          return
        }
        binding.tvPass.isSelected = !binding.tvPass.isSelected
        binding.tvPass.inputType = if (binding.tvPass.isSelected) {
          InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
          InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
      }
    })
    binding.tvPass.doClick {
      ClipboardUtil.get()
        .copyDataToClip(pass)
      HitUtil.toaskShort(context.getString(R.string.hint_copy_pass))
    }
  }
}