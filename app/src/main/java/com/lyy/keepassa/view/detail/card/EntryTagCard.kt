package com.lyy.keepassa.view.detail.card

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.LayoutEntryCardTagBinding

/**
 * @Author laoyuyu
 * @Description
 * @Date 11:26 AM 2023/9/27
 **/
class EntryTagCard(context: Context, attributeSet: AttributeSet) :
  MaterialCardView(context, attributeSet) {
  private val layoutInflater = LayoutInflater.from(context)
  private val binding = LayoutEntryCardTagBinding.inflate(layoutInflater, this, true)

  fun bindData(pwEntryV4: PwEntryV4) {
    if (pwEntryV4.tags.isBlank()) {
      visibility = GONE
      return
    }
    visibility = VISIBLE
    val tagList = pwEntryV4.tags.split(",")
    if (binding.chipGroup.childCount > 0){
      binding.chipGroup.removeAllViews()
    }
    tagList.forEachIndexed { index, s ->
      buildChip(index, s)
    }
  }

  private fun buildChip(index: Int, tag: String) {
    val chip = layoutInflater.inflate(
      R.layout.layout_chip_harvest,
      binding.chipGroup,
      false
    ) as Chip
    chip.id = index
    chip.stateListAnimator = null
    chip.text = tag
    // chip.setOnCheckedChangeListener(this)
    binding.chipGroup.addView(chip, index)
  }
}
