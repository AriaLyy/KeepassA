package com.lyy.keepassa.view.detail.card

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.card.MaterialCardView
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.LayoutEntryCardNoteBinding

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
    binding.expandTv.findViewById<TextView>(R.id.expandable_text).typeface =
      ResourcesCompat.getFont(context, R.font.roboto_thinitalic)
  }
}