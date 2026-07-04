package com.lyy.keepassa.view.detail.card

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.arialyy.frame.util.ResUtil
import com.google.android.material.card.MaterialCardView
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.LayoutEntryCardNoteBinding
import com.lyy.keepassa.util.KpaUtil

/**
 * @Author laoyuyu
 * @Description
 * @Date 11:03 AM 2023/9/26
 **/
class EntryNoteCard(context: Context, attributeSet: AttributeSet) :
  MaterialCardView(context, attributeSet) {
  private val binding = LayoutEntryCardNoteBinding.inflate(LayoutInflater.from(context), this, true)

  fun bindData(entryV4: PwEntryV4) {
    visibility = if (entryV4.notes.isBlank()) GONE else VISIBLE
    binding.expandTv.text = entryV4.notes
    val inner = binding.expandTv.findViewById<TextView>(com.lyy.widget.R.id.expandable_text)
    inner.typeface = ResourcesCompat.getFont(context, R.font.roboto_thinitalic)
    // ExpandableTextView1 内部硬编码了 #666666,夜间不可读,这里覆盖
    inner.setTextColor(
      ResUtil.getColor(
        if (KpaUtil.isNightMode()) R.color.text_black_color else R.color.text_black_grey_color
      )
    )
  }

  override fun setBackgroundColor(color: Int) {
    setCardBackgroundColor(color)
  }
}